package dev.lackluster.mihelper.utils.nubia

import dev.lackluster.mihelper.hook.compat.XposedEnv
import dev.lackluster.mihelper.hook.compat.log.YLog

object Deoptimizer {

    fun deoptimizeMethod(tag: String, c: Class<*>, methodName: String) {
        for (m in c.declaredMethods) {
            if (m.name == methodName) {
                runCatching { XposedEnv.module.deoptimize(m) }
                    .onSuccess { YLog.debug("$tag Method deoptimized: $m") }
                    .onFailure { YLog.warn("$tag Failed to deoptimize: $m", it) }
            }
        }
    }

    fun deoptimizeMethods(tag: String, c: Class<*>, vararg methodName: String) {
        for (name in methodName) {
            deoptimizeMethod(tag, c, name)
        }
    }

    fun deoptimizeAllMethods(tag: String, c: Class<*>) {
        for (m in c.declaredMethods) {
            runCatching { XposedEnv.module.deoptimize(m) }
                .onSuccess { YLog.debug("$tag Method deoptimized: $m") }
                .onFailure { YLog.warn("$tag Failed to deoptimize: $m", it) }
        }
    }
}
