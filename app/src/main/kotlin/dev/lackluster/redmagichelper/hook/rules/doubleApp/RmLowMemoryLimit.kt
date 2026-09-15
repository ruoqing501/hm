package dev.lackluster.redmagichelper.hook.rules.doubleApp


import android.content.Context
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

//去除低内存设备两个双开应用限制
object RmLowMemoryLimit: YukiBaseHooker(){
    private const val TAG = "NubiaDoubleApp"

    override fun onHook() {
        "com.zte.cn.doubleapp.common.Utils".toClass().method {
            name = "showLimitedApps"
            param(Context::class.java)
        }.hook(){
            before {
                if (!Prefs.getBoolean(Pref.Key.Other.RM_LOW_MEMORY_LIMIT, false)) return@before
                result =false
                YLog.debug(tag = TAG, msg = "showLimitedApps$：${result}")
            }
        }
    }
}
