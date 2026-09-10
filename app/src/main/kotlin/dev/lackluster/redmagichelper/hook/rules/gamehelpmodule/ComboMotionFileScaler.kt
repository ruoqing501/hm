package dev.lackluster.redmagichelper.hook.rules.gamehelpmodule

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Arrays

/** Worker-side transformation for one-key-combo recordings. Ported from LS_Augment. */
object ComboMotionFileScaler {
    const val MAX_FILE_BYTES = 16L * 1024L * 1024L
    const val ALGORITHM_VERSION = "motion-time-v3"
    private const val CACHE_DIRECTORY = "rmh_combo"
    private const val CACHE_FILE_PREFIX = "motion-v3-"

    /** This method is called only by [ComboMotionCache]'s single worker. */
    fun scale(cacheDir: File?, request: ComboMotionCache.Request?): Result {
        if (cacheDir == null || request == null || !request.valid) {
            return Result.failure("invalid_request")
        }
        if (!ComboSpeedPolicy.isValidRate(request.rate)) {
            return Result.failure("invalid_rate")
        }

        val source = File(request.normalizedPath)
        if (!source.isFile) return Result.failure("source_missing")
        if (source.length() != request.size || source.lastModified() != request.modifiedAt) {
            return Result.failure("source_changed_before_read")
        }

        var pending: File? = null
        try {
            val directory = File(cacheDir, CACHE_DIRECTORY)
            if ((!directory.isDirectory && !directory.mkdirs()) || !directory.isDirectory) {
                return Result.failure("cache_unavailable")
            }

            val sourceBytes = readBytes(source, MAX_FILE_BYTES)
            if (source.length() != request.size || source.lastModified() != request.modifiedAt) {
                return Result.failure("source_changed_during_read")
            }
            val contentDigest = sha256(sourceBytes)
            if (contentDigest != request.contentDigest) {
                return Result.failure("source_digest_changed")
            }
            val identity = sha256(
                (ALGORITHM_VERSION + "\n" +
                        request.normalizedPath + "\n" + request.size + "\n" +
                        request.modifiedAt + "\n" + request.contentDigest + "\n" +
                        Math.round(request.rate)).toByteArray(StandardCharsets.UTF_8)
            )
            val destination = File(directory, "$CACHE_FILE_PREFIX$identity.json")
            cleanupWorkerSide(directory, destination)
            if (isUsableCache(destination)) {
                return Result.cacheHit(destination.absolutePath, identity)
            }

            val root = JSONObject(String(sourceBytes, StandardCharsets.UTF_8))
            val events = root.optJSONArray("events")
            if (events == null || events.length() == 0) {
                return Result.failure("events_missing")
            }

            var origin = Long.MAX_VALUE
            var sourceLastSample = Long.MIN_VALUE
            for (index in 0 until events.length()) {
                val event = events.optJSONObject(index)
                if (event == null || !event.has("sampleEventTime") || !event.has("downTime")) {
                    return Result.failure("event_schema_$index")
                }
                val sampleTime = event.getLong("sampleEventTime")
                val downTime = event.getLong("downTime")
                if (sampleTime < 0L || downTime < 0L) {
                    return Result.failure("event_time_$index")
                }
                origin = minOf(origin, minOf(sampleTime, downTime))
                sourceLastSample = maxOf(sourceLastSample, sampleTime)
            }

            var outputLastSample = Long.MIN_VALUE
            for (index in 0 until events.length()) {
                val event = events.getJSONObject(index)
                val scaledDown = ComboSpeedPolicy.scaleTimestamp(
                    event.getLong("downTime"), origin, request.rate
                )
                val scaledSample = ComboSpeedPolicy.scaleTimestamp(
                    event.getLong("sampleEventTime"), origin, request.rate
                )
                event.put("downTime", scaledDown)
                event.put("sampleEventTime", scaledSample)
                outputLastSample = maxOf(outputLastSample, scaledSample)
            }

            val output = root.toString().toByteArray(StandardCharsets.UTF_8)
            if (output.isEmpty() || output.size > MAX_FILE_BYTES * 2L) {
                return Result.failure("output_size_${output.size}")
            }
            pending = File(
                directory,
                "$CACHE_FILE_PREFIX$identity.pending-${java.lang.Long.toUnsignedString(System.nanoTime())}"
            )
            writeAndSync(pending, output)
            publishAtomically(pending, destination)
            pending = null

            val sourceSpan = maxOf(0L, sourceLastSample - origin)
            val outputSpan = maxOf(0L, outputLastSample - origin)
            return Result.generated(
                destination.absolutePath, identity,
                events.length(), sourceSpan, outputSpan
            )
        } catch (error: Throwable) {
            return Result.failure(error.javaClass.simpleName)
        } finally {
            if (pending != null && pending.isFile) pending.delete()
        }
    }

    private fun readBytes(source: File, maximumBytes: Long): ByteArray {
        FileInputStream(source).use { input ->
            ByteArrayOutputStream(minOf(source.length(), 64L * 1024L).toInt()).use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    total += read
                    if (total > maximumBytes) throw IOException("motion_too_large")
                    output.write(buffer, 0, read)
                }
                return output.toByteArray()
            }
        }
    }

    /** Worker-side source fingerprint used as part of every in-memory and disk cache key. */
    fun contentDigest(source: File, maximumBytes: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(source).use { input ->
            val buffer = ByteArray(8192)
            var total = 0L
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                total += read
                if (total > maximumBytes) throw IOException("motion_too_large")
                digest.update(buffer, 0, read)
            }
        }
        return hex(digest.digest())
    }

    private fun writeAndSync(file: File, value: ByteArray) {
        FileOutputStream(file, false).use { output ->
            output.write(value)
            output.flush()
            output.fd.sync()
        }
    }

    private fun publishAtomically(pending: File, destination: File) {
        try {
            Files.move(
                pending.toPath(), destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
        } catch (unsupported: AtomicMoveNotSupportedException) {
            // Publishing through a copy/non-atomic fallback could expose a
            // truncated JSON file after power loss. Keep this playback on the
            // OEM source instead.
            throw IOException("atomic_move_unsupported", unsupported)
        }
    }

    private fun isUsableCache(file: File): Boolean {
        if (!file.isFile || file.length() <= 0L ||
            file.length() > MAX_FILE_BYTES * 2L
        ) return false
        return try {
            val root = JSONObject(
                String(readBytes(file, MAX_FILE_BYTES * 2L), StandardCharsets.UTF_8)
            )
            val events = root.optJSONArray("events")
            if (events == null || events.length() == 0) return false
            for (index in 0 until events.length()) {
                val event = events.optJSONObject(index)
                if (event == null || !event.has("sampleEventTime") ||
                    !event.has("downTime") ||
                    event.getLong("sampleEventTime") < 0L ||
                    event.getLong("downTime") < 0L
                ) return false
            }
            true
        } catch (ignored: Throwable) {
            false
        }
    }

    /** Cache cleanup is intentionally worker-only and never runs in a playback hook. */
    private fun cleanupWorkerSide(directory: File, keep: File) {
        var files = directory.listFiles() ?: return
        val now = System.currentTimeMillis()
        for (file in files) {
            if (!file.isFile || file == keep) continue
            val name = file.name
            if (name.contains(".pending-") || name.startsWith("motion-v2-") ||
                (now - file.lastModified()) > 7L * 24L * 60L * 60L * 1000L
            ) {
                file.delete()
            }
        }
        files = directory.listFiles { _, name ->
            name.startsWith(CACHE_FILE_PREFIX) && name.endsWith(".json")
        } ?: return
        if (files.size <= 32) return
        Arrays.sort(files, compareByDescending<File> { it.lastModified() })
        for (index in 32 until files.size) {
            if (files[index] != keep) files[index].delete()
        }
    }

    private fun sha256(value: ByteArray): String =
        hex(MessageDigest.getInstance("SHA-256").digest(value))

    private fun hex(digest: ByteArray): String {
        val out = StringBuilder(digest.size * 2)
        for (item in digest) {
            val value = item.toInt() and 0xff
            if (value < 0x10) out.append('0')
            out.append(Integer.toHexString(value))
        }
        return out.toString()
    }

    class Result private constructor(
        val success: Boolean,
        val cacheHit: Boolean,
        val outputPath: String,
        val cacheIdentity: String,
        val eventCount: Int,
        val sourceSpanMs: Long,
        val outputSpanMs: Long,
        val error: String,
    ) {
        fun asPublishedHit(): Result =
            if (success) Result(true, true, outputPath, cacheIdentity,
                eventCount, sourceSpanMs, outputSpanMs, "")
            else this

        companion object {
            fun generated(
                outputPath: String, cacheIdentity: String, eventCount: Int,
                sourceSpanMs: Long, outputSpanMs: Long
            ) = Result(true, false, outputPath, cacheIdentity, eventCount,
                sourceSpanMs, outputSpanMs, "")

            fun cacheHit(outputPath: String, cacheIdentity: String) =
                Result(true, true, outputPath, cacheIdentity, 0, 0L, 0L, "")

            fun failure(error: String) =
                Result(false, false, "", "", 0, 0L, 0L, error)
        }
    }
}
