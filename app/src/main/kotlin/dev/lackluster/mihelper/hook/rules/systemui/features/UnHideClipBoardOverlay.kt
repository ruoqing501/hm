package dev.lackluster.mihelper.hook.rules.systemui.features


import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.StringClass
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


