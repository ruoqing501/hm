package dev.lackluster.mihelper.hook.rules.systemui.features


import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType
import dev.lackluster.mihelper.hook.compat.type.java.StringClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.getResID
import dev.lackluster.mihelper.utils.factory.hasEnable

object UnHideClipBoardOverlay : YukiBaseHooker() {
    private const val TAG = "UnHideClipBoardOverlay"


    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.UNHIDE_CLIPBOARD_OVERLAY) {
            YLog.debug("$TAG 功能开启已开启")
            "com.android.systemui.clipboardoverlay.ClipboardListener".toClassOrNull()?.method {
                name = "forceSuppressOverlay"
                modifiers { isStatic }
            }?.hook {
                before {
                    result = false
                    YLog.debug("$TAG forceSuppressOverlay")
                }
            }
        }
    }
}


