package dev.lackluster.redmagichelper.hook.compat

import io.github.libxposed.api.XposedModule
import dev.lackluster.redmagichelper.hook.compat.param.PackageParam

/**
 * Holds the current [XposedModule] instance and the per-process [PackageParam]
 * so the compat DSL can reach libxposed APIs from anywhere.
 */
object XposedEnv {
    @Volatile
    private var moduleRef: XposedModule? = null

    var module: XposedModule
        get() = moduleRef ?: error("XposedEnv.module accessed before onModuleLoaded")
        set(value) { moduleRef = value }

    val isReady: Boolean get() = moduleRef != null

    /** PackageParam of the current hooked process, set before hookers are loaded. */
    @Volatile
    var currentParam: PackageParam? = null

    /** ClassLoader used by the DSL when no explicit one is given. */
    val classLoader: ClassLoader
        get() = currentParam?.appClassLoader ?: module.javaClass.classLoader
}
