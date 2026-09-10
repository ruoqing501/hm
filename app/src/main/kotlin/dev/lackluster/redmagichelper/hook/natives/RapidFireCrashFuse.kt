package dev.lackluster.redmagichelper.hook.natives

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dev.lackluster.redmagichelper.hook.compat.log.YLog

/** Persistent crash marker protecting system_server from repeated native installs. */
internal object RapidFireCrashFuse {
    private const val TAG = "RapidFireCrashFuse"

    const val PENDING = "rmh_tgk_fuse_pending"
    const val ATTEMPTS = "rmh_tgk_fuse_attempts"
    const val FUSED = "rmh_tgk_fuse_tripped"
    private const val STARTED_AT = "rmh_tgk_fuse_started_at"

    private const val MAX_ATTEMPTS = 3

    @Volatile
    private var startupChecked = false

    @Synchronized
    fun onSystemStart(context: Context?) {
        if (startupChecked || context == null) return
        startupChecked = true
        try {
            var attempts = Settings.Global.getInt(context.contentResolver, ATTEMPTS, 0)
            val pending = Settings.Global.getInt(context.contentResolver, PENDING, 0)
            if (pending == 1) attempts++
            Settings.Global.putInt(context.contentResolver, PENDING, 0)
            Settings.Global.putInt(context.contentResolver, ATTEMPTS, attempts)
            if (attempts >= MAX_ATTEMPTS) {
                Settings.Global.putInt(context.contentResolver, FUSED, 1)
            }
            publish(if (attempts >= MAX_ATTEMPTS) "fused" else "ready", attempts)
        } catch (ignored: Throwable) {
            // A missing SettingsProvider is treated as unavailable by beforeInstall().
        }
    }

    @Synchronized
    fun beforeInstall(context: Context?): Boolean {
        if (context == null) return false
        onSystemStart(context)
        return try {
            if (Settings.Global.getInt(context.contentResolver, FUSED, 0) == 1) {
                publish(
                    "fused",
                    Settings.Global.getInt(context.contentResolver, ATTEMPTS, MAX_ATTEMPTS)
                )
                return false
            }
            if (!Settings.Global.putInt(context.contentResolver, PENDING, 1)) return false
            Settings.Global.putLong(
                context.contentResolver, STARTED_AT, System.currentTimeMillis()
            )
            publish("pending", Settings.Global.getInt(context.contentResolver, ATTEMPTS, 0))
            true
        } catch (ignored: Throwable) {
            false
        }
    }

    fun installationFailed(context: Context?) {
        if (context == null) return
        try {
            Settings.Global.putInt(context.contentResolver, PENDING, 0)
        } catch (ignored: Throwable) {
        }
    }

    fun armStableClear(context: Context?) {
        if (context == null) return
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!TgkRapidFireNative.isLoaded) return@postDelayed
                try {
                    Settings.Global.putInt(context.contentResolver, PENDING, 0)
                    Settings.Global.putInt(context.contentResolver, ATTEMPTS, 0)
                    publish("stable", 0)
                } catch (ignored: Throwable) {
                }
            }, 60_000L)
        } catch (ignored: Throwable) {
        }
    }

    fun isFused(context: Context?): Boolean {
        if (context == null) return true
        onSystemStart(context)
        return try {
            Settings.Global.getInt(context.contentResolver, FUSED, 0) == 1
        } catch (ignored: Throwable) {
            true
        }
    }

    private fun publish(state: String, attempts: Int) {
        YLog.debug(tag = TAG, msg = "rmh_tgk_rapid_fire_fuse_state=$state|attempts=$attempts")
    }
}
