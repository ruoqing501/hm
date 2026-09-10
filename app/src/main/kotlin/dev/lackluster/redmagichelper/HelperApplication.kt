package dev.lackluster.redmagichelper

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.ScreenOffHideExecutor
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlin.concurrent.thread

class HelperApplication : Application(), XposedServiceHelper.OnServiceListener {

    @Volatile
    private var service: XposedService? = null

    private val localPrefs: SharedPreferences by lazy {
        getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
    }

    /**
     * 熄屏自动隐藏的兜底通道:system_server 侧无法直接 su 时会把意图写入
     * Settings.Global,这里观察该标记并用本进程的 root 权限代为执行。
     */
    private val pendingHideObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            drainPendingHide("observer")
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        Prefs.initApp(this)
        XposedServiceHelper.registerListener(this)
        // Mirror every settings-UI write into the framework-served remote preferences.
        localPrefs.registerOnSharedPreferenceChangeListener { prefs, key ->
            if (key != null) pushKeyToRemote(prefs, key)
        }
        runCatching {
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(ScreenOffHideExecutor.PENDING_KEY),
                false,
                pendingHideObserver
            )
        }
        drainPendingHide("app_start")
    }

    private fun drainPendingHide(source: String) {
        if (!Prefs.getBoolean(Pref.Key.Other.SCREEN_OFF_HIDE_ENABLED, false)) return
        val pending = runCatching {
            Settings.Global.getInt(contentResolver, ScreenOffHideExecutor.PENDING_KEY, 0)
        }.getOrDefault(0)
        if (pending != 1) return
        thread(name = "rmh-screen-off-hide") {
            // 屏幕已重新点亮时不再执行隐藏,保持 fail-closed
            val power = getSystemService(PowerManager::class.java)
            if (power?.isInteractive == true) {
                ScreenOffHideExecutor.clearPending()
                Log.i("RedMagicHelper", "screen-off hide pending dropped: screen on (source=$source)")
                return@thread
            }
            val targets = Prefs.getStringSet(Pref.Key.Other.SCREEN_OFF_HIDE_TARGETS, mutableSetOf())
            val outcome = ScreenOffHideExecutor.hideAll(targets)
            ScreenOffHideExecutor.clearPending()
            if (outcome.success) {
                Log.i("RedMagicHelper", "screen-off hide fallback done (source=$source): ${outcome.message}")
            } else {
                Log.w("RedMagicHelper", "screen-off hide fallback failed (source=$source): ${outcome.message}")
            }
        }
    }

    override fun onServiceBind(service: XposedService) {
        this.service = service
        syncAllToRemote(service)
    }

    override fun onServiceDied(service: XposedService) {
        if (this.service === service) this.service = null
    }

    /** Push the whole local preference file, also covering migration from legacy installs. */
    private fun syncAllToRemote(service: XposedService) {
        runCatching {
            val editor = service.getRemotePreferences(Prefs.NAME).edit()
            localPrefs.all.forEach { (key, value) -> editor.putAny(key, value) }
            editor.apply()
        }
    }

    private fun pushKeyToRemote(prefs: SharedPreferences, key: String) {
        val service = service ?: return
        runCatching {
            val editor = service.getRemotePreferences(Prefs.NAME).edit()
            editor.putAny(key, prefs.all[key])
            editor.apply()
        }
    }

    private fun SharedPreferences.Editor.putAny(key: String, value: Any?) {
        when (value) {
            is Boolean -> putBoolean(key, value)
            is Int -> putInt(key, value)
            is Float -> putFloat(key, value)
            is Long -> putLong(key, value)
            is String -> putString(key, value)
            is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
            null -> remove(key)
            else -> putString(key, value.toString())
        }
    }
}
