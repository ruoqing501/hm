package dev.lackluster.redmagichelper.hook.rules.gamehelpmodule

import java.io.File
import java.util.Objects
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Single-worker cache coordinator; playback callers only consume published files. */
class ComboMotionCache {
    companion object {
        const val MAX_COLD_WAIT_MS = 200L
        private val PUBLISHED_TTL_NANOS = TimeUnit.HOURS.toNanos(6L)
        private const val MAX_PUBLISHED_ENTRIES = 24
    }

    private val jobs = ConcurrentHashMap<String, CompletableFuture<ComboMotionFileScaler.Result>>()
    private val published = ConcurrentHashMap<String, Published>()
    private val generations = ConcurrentHashMap<String, Long>()
    private val worker = ThreadPoolExecutor(
        1, 1, 30L, TimeUnit.SECONDS, LinkedBlockingQueue(32),
        CacheThreadFactory(), ThreadPoolExecutor.AbortPolicy()
    )

    init {
        worker.allowCoreThreadTimeOut(true)
    }

    fun prepare(cacheDir: File?, sourcePath: String?, rate: Float, invalidate: Boolean) {
        val alias = Request.alias(sourcePath, rate)
        if (alias.isEmpty() || cacheDir == null || !ComboSpeedPolicy.isValidRate(rate)) return
        var generation = generations[alias] ?: 0L
        if (invalidate) {
            generation = generations.merge(alias, 1L) { old, inc -> old + inc }!!
            published.remove(alias)
        }
        schedule(cacheDir, sourcePath, rate, alias, generation)
    }

    fun lookupOrSchedule(
        cacheDir: File?, sourcePath: String?,
        rate: Float, mayWait: Boolean
    ): ComboMotionFileScaler.Result {
        val startedAt = System.nanoTime()
        val alias = Request.alias(sourcePath, rate)
        if (alias.isEmpty() || cacheDir == null || !ComboSpeedPolicy.isValidRate(rate)) {
            return ComboMotionFileScaler.Result.failure("invalid_request")
        }
        val generation = generations[alias] ?: 0L
        val hot = published[alias]
        if (!mayWait && hot != null && hot.request.generation == generation && hot.isFresh()) {
            return hot.result.asPublishedHit()
        }
        if (hot != null && (!hot.isFresh() || hot.request.generation != generation)) {
            published.remove(alias, hot)
        }

        // A playback call on the main thread performs no path canonicalization,
        // stat or content read. It only queues worker-side preparation and keeps
        // this playback on the OEM source when no published entry is available.
        if (!mayWait) {
            prepare(cacheDir, sourcePath, rate, false)
            return ComboMotionFileScaler.Result.failure("cold_cache")
        }

        val future = schedule(cacheDir, sourcePath, rate, alias, generation)
            ?: return ComboMotionFileScaler.Result.failure("queue_full")
        return try {
            val elapsedNanos = System.nanoTime() - startedAt
            val remainingNanos = TimeUnit.MILLISECONDS.toNanos(MAX_COLD_WAIT_MS) - elapsedNanos
            if (remainingNanos <= 0L) {
                return ComboMotionFileScaler.Result.failure("cold_cache_timeout")
            }
            val result = future.get(remainingNanos, TimeUnit.NANOSECONDS)
            if ((generations[alias] ?: 0L) != generation) {
                return ComboMotionFileScaler.Result.failure("cache_invalidated")
            }
            if (result.success) result
            else ComboMotionFileScaler.Result.failure(result.error)
        } catch (timeout: TimeoutException) {
            ComboMotionFileScaler.Result.failure("cold_cache_timeout")
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            ComboMotionFileScaler.Result.failure("cold_cache_interrupted")
        } catch (failed: ExecutionException) {
            ComboMotionFileScaler.Result.failure("cache_worker_failed")
        }
    }

    /**
     * Capture, hash, parse, scale, sync, cleanup and publication all execute on
     * the same worker. Callers only enqueue and optionally wait for the part of
     * the fixed 200 ms budget that remains after entering lookupOrSchedule.
     */
    private fun schedule(
        cacheDir: File, sourcePath: String?,
        rate: Float, alias: String, generation: Long
    ): CompletableFuture<ComboMotionFileScaler.Result>? {
        val key = alias + '\u0000' + cacheDir.absolutePath + '\u0000' + generation
        jobs[key]?.let { return it }
        val created = CompletableFuture<ComboMotionFileScaler.Result>()
        jobs.putIfAbsent(key, created)?.let { return it }
        try {
            worker.execute {
                val result: ComboMotionFileScaler.Result = try {
                    if ((generations[alias] ?: 0L) != generation) {
                        ComboMotionFileScaler.Result.failure("cache_invalidated")
                    } else {
                        val request = Request.capture(cacheDir, sourcePath, rate, generation)
                        if (!request.valid) {
                            ComboMotionFileScaler.Result.failure(request.error)
                        } else {
                            val ready = published[alias]
                            if (ready != null && ready.isFresh() && ready.request == request) {
                                ready.result.asPublishedHit()
                            } else {
                                if (ready != null) published.remove(alias, ready)
                                var scaled = ComboMotionFileScaler.scale(cacheDir, request)
                                val currentGeneration = generations[alias] ?: 0L
                                if (scaled.success && currentGeneration == generation) {
                                    published[alias] = Published(request, scaled)
                                    trimPublished(alias)
                                } else if (scaled.success) {
                                    scaled = ComboMotionFileScaler.Result.failure("cache_invalidated")
                                }
                                scaled
                            }
                        }
                    }
                } catch (error: Throwable) {
                    ComboMotionFileScaler.Result.failure(error.javaClass.simpleName)
                }
                created.complete(result)
                jobs.remove(key, created)
            }
            return created
        } catch (rejected: RejectedExecutionException) {
            jobs.remove(key, created)
            created.complete(ComboMotionFileScaler.Result.failure("queue_full"))
            return null
        }
    }

    private fun trimPublished(keepAlias: String) {
        if (published.size <= MAX_PUBLISHED_ENTRIES) return
        for (alias in published.keys) {
            if (published.size <= MAX_PUBLISHED_ENTRIES) return
            if (alias != keepAlias) published.remove(alias)
        }
    }

    class Request private constructor(
        val valid: Boolean,
        val error: String,
        val cachePath: String,
        val normalizedPath: String,
        val size: Long,
        val modifiedAt: Long,
        val contentDigest: String,
        val rate: Float,
        val inputAlias: String,
        val generation: Long,
    ) {
        companion object {
            fun capture(
                cacheDir: File?, sourcePath: String?,
                rate: Float, generation: Long
            ): Request {
                if (cacheDir == null || sourcePath == null || sourcePath.trim().isEmpty()) {
                    return invalid("missing_path", generation)
                }
                if (!ComboSpeedPolicy.isValidRate(rate)) return invalid("invalid_rate", generation)
                return try {
                    val source = File(sourcePath).canonicalFile
                    val size = source.length()
                    if (!source.isFile) return invalid("source_missing", generation)
                    if (size <= 0L || size > ComboMotionFileScaler.MAX_FILE_BYTES) {
                        return invalid("source_size_$size", generation)
                    }
                    val modifiedAt = source.lastModified()
                    val contentDigest = ComboMotionFileScaler.contentDigest(
                        source, ComboMotionFileScaler.MAX_FILE_BYTES
                    )
                    if (source.length() != size || source.lastModified() != modifiedAt) {
                        return invalid("source_changed_during_fingerprint", generation)
                    }
                    Request(
                        true, "", cacheDir.canonicalPath,
                        source.path, size, modifiedAt, contentDigest, rate,
                        alias(sourcePath, rate), generation
                    )
                } catch (error: Throwable) {
                    invalid(error.javaClass.simpleName, generation)
                }
            }

            fun alias(sourcePath: String?, rate: Float): String {
                if (sourcePath == null || sourcePath.trim().isEmpty() ||
                    !ComboSpeedPolicy.isValidRate(rate)
                ) return ""
                return try {
                    val path = File(sourcePath.trim()).absoluteFile.toPath().normalize().toString()
                    path + '\u0000' + Math.round(rate)
                } catch (ignored: Throwable) {
                    ""
                }
            }

            private fun invalid(error: String, generation: Long) =
                Request(false, error, "", "", -1L, -1L, "", 1f, "", generation)
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Request) return false
            return valid == other.valid && size == other.size &&
                    modifiedAt == other.modifiedAt && generation == other.generation &&
                    rate.compareTo(other.rate) == 0 &&
                    cachePath == other.cachePath &&
                    normalizedPath == other.normalizedPath &&
                    contentDigest == other.contentDigest
        }

        override fun hashCode(): Int =
            Objects.hash(valid, cachePath, normalizedPath, size, modifiedAt,
                contentDigest, rate, generation)
    }

    private class Published(
        val request: Request,
        val result: ComboMotionFileScaler.Result,
    ) {
        val publishedAtNanos: Long = System.nanoTime()

        fun isFresh(): Boolean {
            val age = System.nanoTime() - publishedAtNanos
            return age >= 0L && age <= PUBLISHED_TTL_NANOS
        }
    }

    private class CacheThreadFactory : ThreadFactory {
        override fun newThread(runnable: Runnable): Thread {
            val thread = Thread(runnable, "RMH-ComboCache")
            thread.isDaemon = true
            thread.priority = Thread.NORM_PRIORITY - 1
            return thread
        }
    }
}
