package dev.lackluster.mihelper.hook.compat.log

import android.util.Log
import dev.lackluster.mihelper.hook.compat.XposedEnv

/**
 * YukiHookAPI-style logger facade backed by [io.github.libxposed.api.XposedInterface.log].
 */
object YLog {
    var tag = "RedMagicHelper"

    private fun log(priority: Int, msg: String, e: Throwable?, tag: String?) {
        val finalTag = tag ?: this.tag
        if (XposedEnv.isReady) {
            if (e != null) XposedEnv.module.log(priority, finalTag, msg, e)
            else XposedEnv.module.log(priority, finalTag, msg)
        } else {
            Log.println(priority, finalTag, if (e != null) "$msg\n${Log.getStackTraceString(e)}" else msg)
        }
    }

    fun debug(msg: String = "", e: Throwable? = null, tag: String? = null) = log(Log.DEBUG, msg, e, tag)
    fun info(msg: String = "", e: Throwable? = null, tag: String? = null) = log(Log.INFO, msg, e, tag)
    fun warn(msg: String = "", e: Throwable? = null, tag: String? = null) = log(Log.WARN, msg, e, tag)
    fun error(msg: String = "", e: Throwable? = null, tag: String? = null) = log(Log.ERROR, msg, e, tag)
}
