package dev.lackluster.redmagichelper.hook.natives

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import dev.lackluster.redmagichelper.hook.compat.XposedEnv
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import java.io.File

/**
 * Small system_server bridge for structurally verified libinputreader layouts.
 *
 * The native library is loaded lazily on the first request above the OEM
 * ceiling. The native probe verifies the actual function and derives its field
 * offsets. File hashes invalidate old test results but never decide eligibility.
 */
internal object TgkRapidFireNative {
    private const val TAG = "TgkRapidFireNative"

    private const val LIBRARY_NAME = "rmh_tgk"
    private const val NATIVE_LIBRARY_NAME = "librmh_tgk.so"

    @Volatile
    private var attempted = false

    @Volatile
    var isLoaded = false
        private set

    private var state = "not_loaded"
    private var configuredLeft = -1
    private var configuredRight = -1

    @Synchronized
    fun ensureInstalled(context: Context?): Boolean {
        if (attempted) return isLoaded
        attempted = true

        val sha256 = inputReaderSha256
        YLog.debug(tag = TAG, msg = "rmh_tgk_rapid_fire_native_sha256=$sha256")
        val profile = RapidFireCompatibility.nativeProfile()
        if (profile == null) {
            state = "incompatible|stage=structure|sha256=" + safe(sha256)
            writeState()
            return false
        }

        try {
            if (!RapidFireCrashFuse.beforeInstall(context)) {
                state = "fused_or_marker_unavailable"
                writeState()
                return false
            }
            val info = runCatching { XposedEnv.module.moduleApplicationInfo }.getOrNull()
            val nativeDir = info?.nativeLibraryDir
            loadLibrary(info, nativeDir, NATIVE_LIBRARY_NAME, LIBRARY_NAME)
            state = nativeInstall() ?: ""
            isLoaded = state.startsWith("installed|")
            if (!isLoaded && state.isEmpty()) state = "error|empty_native_state"
            if (isLoaded) {
                YLog.debug(tag = TAG, msg = "rmh_tgk_rapid_fire_native_last_error cleared")
                RapidFireCrashFuse.armStableClear(context)
            } else {
                RapidFireCrashFuse.installationFailed(context)
            }
        } catch (error: Throwable) {
            state = "error|load=" + error.javaClass.simpleName + ":" + safe(error.message)
            isLoaded = false
            RapidFireCrashFuse.installationFailed(context)
        }
        writeState()
        return isLoaded
    }

    /** Returns the SHA-256 of the system input-reader library on this device. */
    val inputReaderSha256: String
        get() = RapidFireCompatibility.inputReaderSha256()

    @Synchronized
    fun configureKeys(context: Context?, leftKey: Int, rightKey: Int) {
        if (!isLoaded || leftKey <= 0 || rightKey <= 0 || leftKey == rightKey) return
        try {
            if (configuredLeft != leftKey || configuredRight != rightKey) {
                nativeClearTargets()
                nativeConfigureKeys(leftKey, rightKey)
                configuredLeft = leftKey
                configuredRight = rightKey
            }
        } catch (error: Throwable) {
            state = "error|configure_keys=" + error.javaClass.simpleName
            writeState()
        }
    }

    @Synchronized
    fun configureTestKey(context: Context?, side: String?, keyCode: Int) {
        if (!isLoaded || keyCode <= 0) return
        val left = if ("left" == side) keyCode else configuredLeft
        val right = if ("right" == side) keyCode else configuredRight
        if (left > 0 && right > 0 && left == right) return
        try {
            nativeClearTargets()
            nativeConfigureKeys(left, right)
            configuredLeft = left
            configuredRight = right
        } catch (error: Throwable) {
            state = "error|configure_test=" + error.javaClass.simpleName
            writeState()
        }
    }

    fun setTarget(context: Context?, keyCode: Int, cps: Int) {
        if (!isLoaded) return
        try {
            nativeSetTarget(keyCode, cps.coerceIn(0, 50))
        } catch (error: Throwable) {
            state = "error|set_target=" + error.javaClass.simpleName
            writeState()
        }
    }

    @Synchronized
    fun clearTargets(context: Context?) {
        if (!isLoaded) {
            configuredLeft = -1
            configuredRight = -1
            if (!attempted) {
                state = "not_loaded"
                YLog.debug(tag = TAG, msg = "rmh_tgk_rapid_fire_native_last_error cleared")
            }
            writeState()
            return
        }
        try {
            nativeClearTargets()
            nativeArmCadence(null, -1)
            configuredLeft = -1
            configuredRight = -1
            state = nativeState() ?: ""
            writeState()
        } catch (error: Throwable) {
            state = "error|clear_targets=" + error.javaClass.simpleName
            writeState()
        }
    }

    fun state(context: Context?): String {
        if (isLoaded) {
            try {
                state = nativeState() ?: ""
            } catch (ignored: Throwable) {
            }
        }
        writeState()
        return state
    }

    /** Read-only native observation used only by the explicit compatibility test. */
    @Synchronized
    fun observation(): String {
        if (!isLoaded) return ""
        return try {
            nativeObservation() ?: ""
        } catch (ignored: Throwable) {
            ""
        }
    }

    fun armCadence(id: String?, phase: String?, code: Int) {
        if (isLoaded) {
            try {
                nativeArmCadence("$id:$phase:$code", code)
            } catch (ignored: Throwable) {
            }
        }
    }

    fun cadence(): String {
        if (isLoaded) {
            try {
                return nativeCadence() ?: "ready=0"
            } catch (ignored: Throwable) {
            }
        }
        return "ready=0"
    }

    private fun loadLibrary(
        info: ApplicationInfo?, nativeDir: String?,
        fileName: String, libraryName: String
    ) {
        val candidates = LinkedHashSet<File>()
        addCandidate(candidates, nativeDir, fileName)

        // libxposed may expose an APK-internal nativeLibraryDir to the module
        // class loader even when PackageManager extracted the libraries. Derive
        // the real, package-owned extraction directory from sourceDir instead of
        // relying on the injected class loader's lookup path.
        val sourceApk = info?.sourceDir?.let { File(it) }
        val codeDir = sourceApk?.parentFile
        val extractedRoot = codeDir?.let { File(it, "lib") }
        if (extractedRoot != null) {
            for (abi in Build.SUPPORTED_ABIS) {
                addCandidate(candidates, File(extractedRoot, abi).path, fileName)
                val runtimeArch = runtimeArchDirectory(abi)
                if (runtimeArch != abi) {
                    addCandidate(candidates, File(extractedRoot, runtimeArch).path, fileName)
                }
            }
        }

        var firstError: UnsatisfiedLinkError? = null
        for (candidate in candidates) {
            if (!candidate.isFile) continue
            try {
                System.load(candidate.absolutePath)
                return
            } catch (error: UnsatisfiedLinkError) {
                if (firstError == null) firstError = error
            }
        }
        try {
            System.loadLibrary(libraryName)
        } catch (error: UnsatisfiedLinkError) {
            firstError?.let { error.addSuppressed(it) }
            throw error
        }
    }

    private fun addCandidate(candidates: MutableSet<File>, directory: String?, fileName: String) {
        if (directory.isNullOrEmpty()) return
        candidates.add(File(directory, fileName))
    }

    private fun runtimeArchDirectory(abi: String): String = when (abi) {
        "arm64-v8a" -> "arm64"
        "armeabi-v7a", "armeabi" -> "arm"
        else -> abi
    }

    private fun writeState() {
        YLog.debug(tag = TAG, msg = "rmh_tgk_rapid_fire_native_state=$state")
    }

    private fun safe(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        return value.replace('|', '_').replace('\n', ' ')
    }

    private external fun nativeInstall(): String?
    private external fun nativeConfigureKeys(leftKeyCode: Int, rightKeyCode: Int)
    private external fun nativeClearTargets()
    private external fun nativeSetTarget(keyCode: Int, cps: Int)
    private external fun nativeState(): String?
    private external fun nativeObservation(): String?
    private external fun nativeArmCadence(identity: String?, keyCode: Int)
    private external fun nativeCadence(): String?
}
