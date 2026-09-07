package dev.lackluster.mihelper.hook.compat.param

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import dev.lackluster.mihelper.hook.compat.log.YLog

/**
 * Per-process context of the hooked package, replacing YukiHookAPI's PackageParam.
 */
class PackageParam(
    val packageName: String,
    val appClassLoader: ClassLoader,
    val appInfo: ApplicationInfo?,
) {
    /** The "android" package context of the current process (ActivityThread.getSystemContext). */
    val systemContext: Context by lazy {
        val activityThread = Class.forName("android.app.ActivityThread")
            .getMethod("currentActivityThread")
            .invoke(null)
            ?: error("ActivityThread.currentActivityThread() is null")
        activityThread.javaClass.getMethod("getSystemContext").invoke(activityThread) as Context
    }

    /** The host app's Application context, if it has been created already. */
    val appContext: Context?
        get() = runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
                .getMethod("currentActivityThread")
                .invoke(null) ?: return null
            activityThread.javaClass.getMethod("currentApplication")
                .invoke(activityThread) as? Application
        }.getOrNull()

    fun findClass(name: String, classLoader: ClassLoader = appClassLoader): Class<*> =
        classLoader.loadClass(name)

    fun findClassOrNull(name: String, classLoader: ClassLoader = appClassLoader): Class<*>? =
        runCatching { classLoader.loadClass(name) }.getOrNull()

    fun String.toClass(classLoader: ClassLoader = appClassLoader): Class<*> =
        classLoader.loadClass(this)

    fun String.toClassOrNull(classLoader: ClassLoader = appClassLoader): Class<*>? =
        runCatching { classLoader.loadClass(this) }.getOrNull()

    /** Reads the host app's own SharedPreferences. */
    fun prefs(name: String): SharedPreferences {
        val context = appContext ?: runCatching {
            systemContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
        }.getOrElse {
            YLog.warn("prefs($name): host context unavailable, falling back to system context")
            systemContext
        }
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }
}
