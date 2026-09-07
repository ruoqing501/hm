package dev.lackluster.mihelper

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import dev.lackluster.mihelper.utils.Prefs
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class HelperApplication : Application(), XposedServiceHelper.OnServiceListener {

    @Volatile
    private var service: XposedService? = null

    private val localPrefs: SharedPreferences by lazy {
        getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
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
