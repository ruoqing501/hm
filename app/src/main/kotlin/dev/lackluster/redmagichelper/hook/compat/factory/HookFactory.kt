@file:Suppress("unused")

package dev.lackluster.redmagichelper.hook.compat.factory

import dev.lackluster.redmagichelper.hook.compat.XposedEnv

fun String.toClass(classLoader: ClassLoader = XposedEnv.classLoader): Class<*> =
    classLoader.loadClass(this)

fun String.toClassOrNull(classLoader: ClassLoader = XposedEnv.classLoader): Class<*>? =
    runCatching { classLoader.loadClass(this) }.getOrNull()

fun findClass(name: String, classLoader: ClassLoader = XposedEnv.classLoader): Class<*> =
    classLoader.loadClass(name)

fun findClassOrNull(name: String, classLoader: ClassLoader = XposedEnv.classLoader): Class<*>? =
    runCatching { classLoader.loadClass(name) }.getOrNull()

fun Class<*>.method(init: MethodFinder.() -> Unit): MethodFinder = MethodFinder(this).apply(init)

fun Class<*>.method(): MethodFinder = MethodFinder(this)

fun Class<*>.constructor(init: ConstructorFinder.() -> Unit): ConstructorFinder =
    ConstructorFinder(this).apply(init)

fun Class<*>.constructor(): ConstructorFinder = ConstructorFinder(this)

fun Class<*>.field(init: FieldFinder.() -> Unit): FieldFinder = FieldFinder(this).apply(init)

fun Class<*>.field(): FieldFinder = FieldFinder(this)

/** Hooks a resolved method directly (e.g. found via DexKit). */
fun java.lang.reflect.Method.hook(init: HookScope.() -> Unit) {
    val scope = HookScope().apply(init)
    runCatching { scope.install(this) }
        .onFailure { dev.lackluster.redmagichelper.hook.compat.log.YLog.error("Failed to hook $this", it) }
}

/** Hooks a resolved constructor directly. */
fun java.lang.reflect.Constructor<*>.hook(init: HookScope.() -> Unit) {
    val scope = HookScope().apply(init)
    runCatching { scope.install(this) }
        .onFailure { dev.lackluster.redmagichelper.hook.compat.log.YLog.error("Failed to hook $this", it) }
}
