package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia

import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.data.Pref.Key.SystemUI.IconTurner
import dev.lackluster.mihelper.utils.Prefs

object StatusBarIgnoreSysHideIcon : YukiBaseHooker() {
    private const val  TAG = "StatusBarIgnoreSysHideIcon"
    private val ignoreSystem = Prefs.getBoolean(IconTurner.NUBIA_IGNORE_SYS_HIDE, false)

    override fun onHook() {
        //小米的默认的方法
        if (ignoreSystem) {
          val statusBarIconView =  "com.android.systemui.statusbar.StatusBarIconView".toClassOrNull()?.apply {
                method {
                    name = "isIconBlocked"
                }.hook {
                    before {

                        result = true
//                        YLog.debug("$TAG StatusBarIconView.isIconBlocked")
                    }
                }

            }
        }

    }
}