package dev.lackluster.redmagichelper.hook.rules.doubleApp

import android.annotation.SuppressLint
import android.content.Context
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
// 双开任意应用
object DoubleAnyApp : YukiBaseHooker() {

    private const val TAG = "NubiaDoubleApp"

    @SuppressLint("PrivateApi")
    override fun onHook() {
        val updateUtilsClass = "com.zte.cn.doubleapp.common.UpdateUtils".toClass()

        updateUtilsClass.method {
            name = "getSupportApps"
        }.hook {
            before {
                if (!Prefs.getBoolean(Pref.Key.Other.DOUBLE_ANY_APP, false)) return@before
                val context = instance.current().field {
                    name = "mContext"
                }.cast<Context>() ?: return@before
                val packages = context.packageManager.getInstalledPackages(0)
                this.result = packages.map { it.packageName }
                YLog.debug(tag = TAG, msg = "getSupportApps: ${this.result}")
            }
        }
    }
}
