package dev.lackluster.mihelper.hook.rules.doubleApp

import android.annotation.SuppressLint
import android.content.Context
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
// 双开任意应用
object DoubleAnyApp : YukiBaseHooker() {

    private const val TAG = "NubiaDoubleApp"

    @SuppressLint("PrivateApi")
    override fun onHook() = hasEnable(Pref.Key.Other.DOUBLE_ANY_APP) {
        val updateUtilsClass = "com.zte.cn.doubleapp.common.UpdateUtils".toClass()

        updateUtilsClass.method {
            name = "getSupportApps"
        }.hook {
            before {
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
