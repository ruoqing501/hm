package dev.lackluster.redmagichelper.utils

import android.content.Context
import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule

/**
 * Preference access with two backends:
 * - hooked processes: read-only remote preferences served by the Xposed framework ([initHook])
 * - module app process: local SharedPreferences written by the settings UI ([initApp])
 *
 * The app side mirrors every local write into the remote preferences via
 * [dev.lackluster.redmagichelper.HelperApplication], so hooked processes observe changes.
 */
object Prefs {
    const val NAME = "config"

    @Volatile
    private var remote: SharedPreferences? = null

    @Volatile
    private var local: SharedPreferences? = null

    fun initHook(module: XposedModule) {
        if (remote != null) return
        remote = runCatching { module.getRemotePreferences(NAME) }.getOrNull()
    }

    fun initApp(context: Context) {
        if (local != null) return
        local = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    private fun backend(): SharedPreferences? = remote ?: local

    fun getBoolean(key: String, defValue: Boolean): Boolean =
        runCatching { backend()?.getBoolean(key, defValue) }.getOrNull() ?: defValue

    fun getInt(key: String, defValue: Int): Int =
        runCatching { backend()?.getInt(key, defValue) }.getOrNull() ?: defValue

    fun getFloat(key: String, defValue: Float): Float =
        runCatching { backend()?.getFloat(key, defValue) }.getOrNull() ?: defValue

    fun getLong(key: String, defValue: Long): Long =
        runCatching { backend()?.getLong(key, defValue) }.getOrNull() ?: defValue

    fun getString(key: String, defValue: String?): String? =
        runCatching { backend()?.getString(key, defValue) }.getOrNull() ?: defValue

    fun getStringSet(key: String, defValue: MutableSet<String>): MutableSet<String> =
        runCatching { backend()?.getStringSet(key, defValue) }.getOrNull() ?: defValue

    fun contains(key: String): Boolean =
        runCatching { backend()?.contains(key) }.getOrNull() ?: false
}
