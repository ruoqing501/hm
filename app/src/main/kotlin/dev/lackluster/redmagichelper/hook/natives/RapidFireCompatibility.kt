package dev.lackluster.redmagichelper.hook.natives

import android.content.Context
import android.os.Build
import dev.lackluster.redmagichelper.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import java.util.UUID

/** Versioned compatibility identity, guided-test session and unlock token. */
internal object RapidFireCompatibility {
    const val SESSION_MAX_MS = 10L * 60L * 1000L
    const val STABILITY_REQUIRED_MS = 10_000L
    const val TEST_CPS = 20
    const val INPUT_READER_PATH = "/system/lib64/libinputreader.so"
    const val SYMBOL_SIGNATURE =
        "android::EventProducer::updateTgkRapidFireData(int)/v1"

    private const val TOKEN_PREFIX = "RFT2"
    private const val SESSION_PREFIX = "RFS2"

    // Cross-process evidence channels (no config provider in this project):
    // the app mirrors the active session into Settings.Global so system_server
    // can observe phase transitions; system_server witnesses are Settings.Global
    // values; gamelauncher witnesses go to logcat (see utils/rapidfire).
    const val GLOBAL_SESSION_KEY = "rmh_tgk_rf_session"
    const val EVIDENCE_SYSTEM_INSTALLED = "rmh_tgk_rapid_fire_system_installed"
    const val EVIDENCE_TEST_SYSTEM_PREFIX = "rmh_tgk_test_system_"
    const val EVIDENCE_TEST_NATIVE_PREFIX = "rmh_tgk_test_native_"
    const val EVIDENCE_TEST_CADENCE_PREFIX = "rmh_tgk_test_cadence_"
    const val EVIDENCE_TEST_STABILITY = "rmh_tgk_test_stability"
    const val LOG_GAMESPACE_INSTALLED = "rmh_tgk_rapid_fire_installed"

    // Mirrors LS_Augment's ConfigSchema.VERSION; the fingerprint schema stays at 1.
    private const val CONFIG_SCHEMA_VERSION = 1

    private val FINGERPRINT_PATTERN = Regex("[0-9a-f]{64}")
    private val SESSION_ID_PATTERN = Regex("[0-9a-f]{32}")

    @Volatile
    private var cachedNativeHash: String? = null

    @Volatile
    private var cachedFingerprint: String? = null

    @Volatile
    private var cachedFingerprintAt = 0L

    enum class State {
        UNTESTED, PREFLIGHT, NEEDS_RESTART, WAIT_LEFT, WAIT_RIGHT,
        VERIFYING, PASSED, FAILED, FUSED
    }

    fun currentFingerprint(context: Context?): String {
        val now = System.currentTimeMillis()
        cachedFingerprint?.let { if (now - cachedFingerprintAt < 5_000L) return it }
        return synchronized(this) {
            val cached = cachedFingerprint
            if (cached != null && now - cachedFingerprintAt < 5_000L) {
                cached
            } else {
                val source = StringBuilder()
                    .append("sdk=").append(Build.VERSION.SDK_INT)
                    .append("|release=").append(Build.VERSION.RELEASE)
                    .append("|security=").append(Build.VERSION.SECURITY_PATCH)
                    .append("|buildId=").append(Build.ID)
                    .append("|fingerprint=").append(Build.FINGERPRINT)
                    .append("|abi=").append(Build.SUPPORTED_ABIS.firstOrNull() ?: "")
                    .append("|gamespace=").append(packageVersion(context, "cn.nubia.gamelauncher"))
                    .append("|gameassist=").append(packageVersion(context, "cn.nubia.gameassist"))
                    .append("|inputreader=").append(inputReaderSha256())
                    .append("|symbol=").append(SYMBOL_SIGNATURE)
                    .append("|module=").append(BuildConfig.VERSION_NAME)
                    .append("|schema=").append(CONFIG_SCHEMA_VERSION)
                val value = sha256(source.toString())
                cachedFingerprint = value
                cachedFingerprintAt = now
                value
            }
        }
    }

    fun inputReaderSha256(): String {
        cachedNativeHash?.let { return it }
        return synchronized(this) {
            cachedNativeHash?.let { return@synchronized it }
            val value = try {
                FileInputStream(INPUT_READER_PATH).use { input ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read > 0) digest.update(buffer, 0, read)
                    }
                    hex(digest.digest())
                }
            } catch (error: Throwable) {
                "unreadable:" + error.javaClass.simpleName
            }
            cachedNativeHash = value
            value
        }
    }

    fun nativeProfile(): String? =
        if (NativeLayout.inspectFile(File(INPUT_READER_PATH)) != null) NativeLayout.PROFILE else null

    private fun packageVersion(context: Context?, packageName: String): String {
        if (context == null) return "context_missing"
        return try {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            info.versionName + ":" + info.longVersionCode
        } catch (ignored: Throwable) {
            "missing"
        }
    }

    class Token private constructor(
        val fingerprint: String,
        val physicalLeft: Int,
        val upperLeft: Int,
        val systemLeft: Int,
        val physicalRight: Int,
        val upperRight: Int,
        val systemRight: Int,
        val passedAt: Long
    ) {
        fun validFor(currentFingerprint: String): Boolean =
            isStructurallyValid() && fingerprint == currentFingerprint

        fun acceptsUpper(keyCode: Int): Boolean =
            keyCode == upperLeft || keyCode == upperRight

        fun acceptsSystem(keyCode: Int): Boolean =
            keyCode == systemLeft || keyCode == systemRight

        fun serialize(): String {
            val body = "$fingerprint,$physicalLeft,$upperLeft,$systemLeft," +
                "$physicalRight,$upperRight,$systemRight,$passedAt"
            return encode(TOKEN_PREFIX, body)
        }

        private fun isStructurallyValid(): Boolean =
            FINGERPRINT_PATTERN.matches(fingerprint) &&
                code(physicalLeft) && code(upperLeft) && code(systemLeft) &&
                code(physicalRight) && code(upperRight) && code(systemRight) &&
                physicalLeft != physicalRight && upperLeft != upperRight &&
                systemLeft != systemRight && passedAt > 0L

        companion object {
            fun issue(
                fingerprint: String, physicalLeft: Int, upperLeft: Int,
                systemLeft: Int, physicalRight: Int, upperRight: Int, systemRight: Int,
                passedAt: Long
            ): Token? {
                val token = Token(
                    fingerprint, physicalLeft, upperLeft, systemLeft,
                    physicalRight, upperRight, systemRight, passedAt
                )
                return if (token.isStructurallyValid()) token else null
            }

            fun parse(encoded: String?): Token? {
                val body = decode(TOKEN_PREFIX, encoded) ?: return null
                return try {
                    val values = body.split(",")
                    if (values.size != 8) return null
                    issue(
                        values[0], values[1].toInt(), values[2].toInt(),
                        values[3].toInt(), values[4].toInt(), values[5].toInt(),
                        values[6].toInt(), values[7].toLong()
                    )
                } catch (ignored: Throwable) {
                    null
                }
            }
        }
    }

    class Session private constructor(
        val id: String,
        val fingerprint: String,
        val state: State,
        val createdAt: Long,
        val expiresAt: Long,
        val verifyingSince: Long,
        val physicalLeft: Int,
        val upperLeft: Int,
        val systemLeft: Int,
        val physicalRight: Int,
        val upperRight: Int,
        val systemRight: Int
    ) {
        fun withState(value: State, now: Long): Session = Session(
            id, fingerprint, value, createdAt, expiresAt,
            if (value == State.VERIFYING) now else verifyingSince,
            physicalLeft, upperLeft, systemLeft,
            physicalRight, upperRight, systemRight
        )

        fun withLeft(physical: Int, upper: Int, system: Int): Session = Session(
            id, fingerprint, state, createdAt, expiresAt, verifyingSince,
            physical, upper, system, physicalRight, upperRight, systemRight
        )

        fun withRight(physical: Int, upper: Int, system: Int): Session = Session(
            id, fingerprint, state, createdAt, expiresAt, verifyingSince,
            physicalLeft, upperLeft, systemLeft, physical, upper, system
        )

        // A completed test is a historical result, not a ten-minute usage lease.
        fun isExpired(now: Long): Boolean = isPending(state) && now > expiresAt

        fun validFor(currentFingerprint: String): Boolean = fingerprint == currentFingerprint

        fun active(now: Long): Boolean =
            !isExpired(now) && (state == State.WAIT_LEFT ||
                state == State.WAIT_RIGHT || state == State.VERIFYING)

        fun serialize(): String {
            val body = "$id,$fingerprint,${state.name},$createdAt,$expiresAt," +
                "$verifyingSince,$physicalLeft,$upperLeft,$systemLeft," +
                "$physicalRight,$upperRight,$systemRight"
            return encode(SESSION_PREFIX, body)
        }

        private fun isStructurallyValid(): Boolean {
            if (!SESSION_ID_PATTERN.matches(id) ||
                !FINGERPRINT_PATTERN.matches(fingerprint) ||
                createdAt <= 0L || expiresAt <= createdAt ||
                expiresAt - createdAt > SESSION_MAX_MS ||
                !optionalCode(physicalLeft) || !optionalCode(upperLeft) ||
                !optionalCode(systemLeft) || !optionalCode(physicalRight) ||
                !optionalCode(upperRight) || !optionalCode(systemRight) ||
                (verifyingSince != 0L &&
                    (verifyingSince < createdAt || verifyingSince > expiresAt))
            ) {
                return false
            }
            if (state == State.WAIT_RIGHT) {
                return code(physicalLeft) && code(upperLeft) && code(systemLeft)
            }
            if (state == State.VERIFYING || state == State.PASSED) {
                return verifyingSince > 0L &&
                    code(physicalLeft) && code(upperLeft) && code(systemLeft) &&
                    code(physicalRight) && code(upperRight) && code(systemRight) &&
                    physicalLeft != physicalRight && upperLeft != upperRight &&
                    systemLeft != systemRight
            }
            return true
        }

        companion object {
            fun start(fingerprint: String?, now: Long): Session? {
                if (fingerprint == null || !FINGERPRINT_PATTERN.matches(fingerprint)) return null
                return Session(
                    UUID.randomUUID().toString().replace("-", ""), fingerprint,
                    State.PREFLIGHT, now, now + SESSION_MAX_MS, 0L,
                    -1, -1, -1, -1, -1, -1
                )
            }

            fun parse(encoded: String?): Session? {
                val body = decode(SESSION_PREFIX, encoded) ?: return null
                return try {
                    val values = body.split(",")
                    if (values.size != 12 || !SESSION_ID_PATTERN.matches(values[0]) ||
                        !FINGERPRINT_PATTERN.matches(values[1])
                    ) return null
                    val created = values[3].toLong()
                    val expires = values[4].toLong()
                    if (created <= 0 || expires <= created || expires - created > SESSION_MAX_MS) {
                        return null
                    }
                    val session = Session(
                        values[0], values[1], State.valueOf(values[2]),
                        created, expires, values[5].toLong(),
                        values[6].toInt(), values[7].toInt(), values[8].toInt(),
                        values[9].toInt(), values[10].toInt(), values[11].toInt()
                    )
                    if (session.isStructurallyValid()) session else null
                } catch (ignored: Throwable) {
                    null
                }
            }
        }
    }

    // Inlined from LS_Augment's RapidFireLifecyclePolicy.
    private fun isPending(state: State): Boolean =
        state == State.PREFLIGHT || state == State.NEEDS_RESTART ||
            state == State.WAIT_LEFT || state == State.WAIT_RIGHT ||
            state == State.VERIFYING

    private fun code(value: Int): Boolean = value > 0 && value <= 65535

    private fun optionalCode(value: Int): Boolean = value == -1 || code(value)

    private fun encode(prefix: String, body: String): String {
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(body.toByteArray(StandardCharsets.UTF_8))
        return "$prefix.$encoded." + sha256("$prefix|$body")
    }

    private fun decode(prefix: String, encoded: String?): String? {
        if (encoded == null) return null
        val values = encoded.split(".")
        if (values.size != 3 || prefix != values[0]) return null
        return try {
            val body = String(
                Base64.getUrlDecoder().decode(values[1]), StandardCharsets.UTF_8
            )
            if (sha256("$prefix|$body") == values[2]) body else null
        } catch (ignored: Throwable) {
            null
        }
    }

    private fun sha256(value: String): String = try {
        hex(MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8)))
    } catch (ignored: Throwable) {
        ""
    }

    private fun hex(value: ByteArray): String {
        val out = StringBuilder(value.size * 2)
        for (item in value) {
            out.append(String.format(Locale.ROOT, "%02x", item.toInt() and 0xff))
        }
        return out.toString()
    }

    /** Read-only ARM64 control-flow/field-layout probe; no device, ROM or hash allowlist. */
    private object NativeLayout {
        const val SYMBOL = "_ZN7android13EventProducer22updateTgkRapidFireDataEi"
        const val PROFILE = "arm64_tgk_split_phase_v1"

        // Complete split-phase algorithm, including all branch destinations and stores.
        // Only key immediates and object field offsets vary. Keep native probe in sync.
        private val PATTERN = intArrayOf(
            0xd503233f.toInt(), 0xa9be7bfd.toInt(), 0xf9000bf3.toInt(), 0x910003fd.toInt(),
            0x7102283f, 0xaa0003f3.toInt(), 0x540001a0, 0x7102243f, 0x54000941,
            0xb9406268.toInt(), 0x7100191f, 0x5400024b, 0xf9400268.toInt(),
            0x529c2001, 0x529c3802, 0xaa1303e0.toInt(), 0x72a0bea1, 0x72a01c82,
            0x14000025, 0xb9406668.toInt(), 0x7100191f, 0x5400022b,
            0xf9400268.toInt(), 0x529c2001, 0x529c3802, 0xaa1303e0.toInt(),
            0x72a0bea1, 0x72a01c82, 0x14000028, 0x71000d1f, 0x5400022b,
            0xf9400268.toInt(), 0x52984001, 0x529e1002, 0xaa1303e0.toInt(),
            0x72a17d61, 0x72a05f42, 0x14000012, 0x71000d1f, 0x540002ab,
            0xf9400268.toInt(), 0x52984001, 0x529e1002, 0xaa1303e0.toInt(),
            0x72a17d61, 0x72a05f42, 0x14000016, 0x7100051f, 0x540004cb,
            0xf9400268.toInt(), 0x528ca001, 0x52984002, 0xaa1303e0.toInt(),
            0x72a3b9a1.toInt(), 0x72a17d62, 0xf9403d08.toInt(), 0xd63f0100.toInt(),
            0x52800a88, 0x52800a09, 0x1400000d, 0x7100051f, 0x5400036b,
            0xf9400268.toInt(), 0x528ca001, 0x52984002, 0xaa1303e0.toInt(),
            0x72a3b9a1.toInt(), 0x72a17d62, 0xf9403d08.toInt(), 0xd63f0100.toInt(),
            0x52800b88, 0x52800b09, 0x528cccea.toInt(), 0x72acccca,
            0x9b2a7c0a.toInt(), 0xd37ffd4b.toInt(), 0x9361fd4a.toInt(),
            0x0b0b014a, 0x531f794b, 0x0b0a016a, 0xb8296a6a.toInt(),
            0xb8286a6b.toInt(), 0xf9400bf3.toInt(), 0xa8c27bfd.toInt(),
            0xd50323bf.toInt(), 0xd65f03c0.toInt(), 0x2a1f03e0, 0x17ffffe2,
            0x2a1f03e0, 0x17ffffed,
        )

        class Layout internal constructor(code: IntArray) {
            val firstKey = (code[7] ushr 10) and 4095
            val secondKey = (code[4] ushr 10) and 4095
            val firstCount = ((code[9] ushr 10) and 4095) * 4
            val secondCount = ((code[19] ushr 10) and 4095) * 4
            val firstDown = (code[58] ushr 5) and 65535
            val firstUp = (code[57] ushr 5) and 65535
            val secondDown = (code[71] ushr 5) and 65535
            val secondUp = (code[70] ushr 5) and 65535
        }

        fun inspectCode(bytes: ByteArray?): Layout? {
            if (bytes == null || bytes.size < 4) return null
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val prefix = if (buffer.getInt(0) == 0xd503245f.toInt()) 4 else 0 // Optional BTI landing pad.
            if (bytes.size != PATTERN.size * 4 + prefix) return null
            val code = IntArray(PATTERN.size)
            for (i in code.indices) {
                code[i] = buffer.getInt(prefix + i * 4)
                var mask = -1
                if (i == 4 || i == 7 || i == 9 || i == 19) mask = 0x003ffc00.inv()
                if (i == 57 || i == 58 || i == 70 || i == 71) mask = 0x001fffe0.inv()
                if ((code[i] and mask) != (PATTERN[i] and mask)) return null
            }
            val layout = Layout(code)
            if (layout.firstKey == 0 || layout.secondKey == 0 ||
                layout.firstKey == layout.secondKey
            ) return null
            val fields = HashSet<Int>()
            for (offset in intArrayOf(
                layout.firstCount, layout.secondCount, layout.firstDown,
                layout.firstUp, layout.secondDown, layout.secondUp
            )) {
                if (offset < 16 || offset > 1024 || offset % 4 != 0 || !fields.add(offset)) {
                    return null
                }
            }
            return layout
        }

        fun inspectFile(file: File): Layout? {
            return try {
                if (file.length() < 64 || file.length() > 64 * 1024 * 1024) return null
                inspectElf(Files.readAllBytes(file.toPath()))
            } catch (ignored: Exception) {
                null
            }
        }

        fun inspectElf(file: ByteArray?): Layout? {
            return try {
                if (file == null || file.size < 64 || file.size > 64 * 1024 * 1024) return null
                val b = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN)
                if (b.getInt(0) != 0x464c457f || file[4].toInt() != 2 || file[5].toInt() != 1 ||
                    b.getShort(16).toInt() != 3 || b.getShort(18).toInt() != 183 ||
                    b.getShort(58).toInt() != 64
                ) return null
                val table = b.getLong(40)
                val sections = b.getShort(60).toInt() and 65535
                if (sections == 0 || !range(table, sections * 64L, file.size.toLong())) return null
                var foundAddress = -1L
                var found: Layout? = null
                for (i in 0 until sections) {
                    val section = table.toInt() + i * 64
                    val kind = b.getInt(section + 4)
                    if (kind != 2 && kind != 11) continue
                    val offset = b.getLong(section + 24)
                    val size = b.getLong(section + 32)
                    val stride = b.getLong(section + 56)
                    val link = b.getInt(section + 40)
                    if (link < 0 || link >= sections || stride < 24 ||
                        stride > file.size.toLong() ||
                        size % stride != 0L || !range(offset, size, file.size.toLong())
                    ) return null
                    val stringSection = table.toInt() + link * 64
                    val strings = b.getLong(stringSection + 24)
                    val stringSize = b.getLong(stringSection + 32)
                    if (b.getInt(stringSection + 4) != 3 ||
                        !range(strings, stringSize, file.size.toLong())
                    ) return null
                    var n = 0L
                    while (n < size) {
                        val symbol = (offset + n).toInt()
                        val name = b.getInt(symbol)
                        if ((file[symbol + 4].toInt() and 15) == 2 && name >= 0 && name < stringSize) {
                            val start = (strings + name).toInt()
                            var end = start
                            while (end < strings + stringSize && end - start <= 256 &&
                                file[end].toInt() != 0
                            ) end++
                            if (end < strings + stringSize && end - start <= 256 &&
                                SYMBOL == String(
                                    file, start, end - start, StandardCharsets.US_ASCII
                                )
                            ) {
                                val owner = b.getShort(symbol + 6).toInt() and 65535
                                val address = b.getLong(symbol + 8)
                                val codeSize = b.getLong(symbol + 16)
                                if (owner == 0 || owner >= sections || codeSize > 512 ||
                                    codeSize <= 0
                                ) return null
                                val codeSection = table.toInt() + owner * 64
                                val delta = address - b.getLong(codeSection + 16)
                                if ((b.getLong(codeSection + 8) and 4) == 0L ||
                                    !range(delta, codeSize, b.getLong(codeSection + 32))
                                ) return null
                                val codeOffset = b.getLong(codeSection + 24) + delta
                                if (!range(codeOffset, codeSize, file.size.toLong())) return null
                                val layout = inspectCode(
                                    file.copyOfRange(
                                        codeOffset.toInt(), (codeOffset + codeSize).toInt()
                                    )
                                )
                                if (layout == null || (found != null && foundAddress != address)) {
                                    return null
                                }
                                found = layout
                                foundAddress = address
                            }
                        }
                        n += stride
                    }
                }
                found
            } catch (ignored: RuntimeException) {
                null
            }
        }

        private fun range(offset: Long, length: Long, total: Long): Boolean =
            offset >= 0 && length >= 0 && offset <= total && length <= total - offset
    }
}
