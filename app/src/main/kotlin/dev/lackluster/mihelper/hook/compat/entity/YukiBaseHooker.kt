@file:Suppress("unused")

package dev.lackluster.mihelper.hook.compat.entity

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import dev.lackluster.mihelper.hook.compat.XposedEnv
import dev.lackluster.mihelper.hook.compat.factory.ConstructorFinder
import dev.lackluster.mihelper.hook.compat.factory.FieldFinder
import dev.lackluster.mihelper.hook.compat.factory.HookScope
import dev.lackluster.mihelper.hook.compat.factory.MethodFinder
import dev.lackluster.mihelper.hook.compat.factory.toClass as toClassExt
import dev.lackluster.mihelper.hook.compat.factory.toClassOrNull as toClassOrNullExt
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.param.PackageParam

/**
 * Drop-in replacement for YukiHookAPI's YukiBaseHooker backed by the libxposed modern API.
 */
abstract class YukiBaseHooker {

    lateinit var packageParam: PackageParam
        internal set

    val packageName: String get() = packageParam.packageName
    val appClassLoader: ClassLoader get() = packageParam.appClassLoader
    val appInfo: ApplicationInfo? get() = packageParam.appInfo
    val systemContext: Context get() = packageParam.systemContext

    /** Host app context (the Application of the hooked process if created, else a package context). */
    val appContext: Context?
        get() = packageParam.appContext ?: runCatching {
            systemContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
        }.getOrNull()

    abstract fun onHook()

    fun loadHooker(hooker: YukiBaseHooker) {
        hooker.packageParam = packageParam
        runCatching { hooker.onHook() }
            .onFailure { YLog.error("Failed to load hooker ${hooker.javaClass.name}", it) }
    }

    fun loadApp(hooker: YukiBaseHooker) = loadHooker(hooker)

    fun loadApp(name: String = packageName, hooker: YukiBaseHooker) = loadHooker(hooker)

    fun loadApp(name: String = packageName, initiate: YukiBaseHooker.() -> Unit) {
        val hooker = object : YukiBaseHooker() {
            override fun onHook() = initiate()
        }
        loadHooker(hooker)
    }

    fun prefs(name: String): SharedPreferences = packageParam.prefs(name)

    fun findClass(name: String, classLoader: ClassLoader = appClassLoader): Class<*> =
        packageParam.findClass(name, classLoader)

    fun String.toClass(classLoader: ClassLoader = appClassLoader): Class<*> =
        toClassExt(classLoader)

    fun String.toClassOrNull(classLoader: ClassLoader = appClassLoader): Class<*>? =
        toClassOrNullExt(classLoader)

    fun Class<*>.method(init: MethodFinder.() -> Unit): MethodFinder =
        MethodFinder(this).apply(init)

    fun Class<*>.method(): MethodFinder = MethodFinder(this)

    fun Class<*>.constructor(init: ConstructorFinder.() -> Unit): ConstructorFinder =
        ConstructorFinder(this).apply(init)

    fun Class<*>.constructor(): ConstructorFinder = ConstructorFinder(this)

    fun Class<*>.field(init: FieldFinder.() -> Unit): FieldFinder =
        FieldFinder(this).apply(init)

    fun Class<*>.field(): FieldFinder = FieldFinder(this)

    /** Hooks a resolved method directly (e.g. found via DexKit). */
    fun java.lang.reflect.Method.hook(init: HookScope.() -> Unit) {
        val scope = HookScope().apply(init)
        runCatching { scope.install(this@hook) }
            .onFailure { YLog.error("Failed to hook $this@hook", it) }
    }

    /** Hooks a resolved constructor directly. */
    fun java.lang.reflect.Constructor<*>.hook(init: HookScope.() -> Unit) {
        val scope = HookScope().apply(init)
        runCatching { scope.install(this@hook) }
            .onFailure { YLog.error("Failed to hook $this@hook", it) }
    }
}
