package dev.lackluster.redmagichelper.hook.rules.systemui.features


import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.getResID

object UnHideClipBoardOverlay : YukiBaseHooker() {
    private const val TAG = "UnHideClipBoardOverlay"


    override fun onHook() {
        "com.android.systemui.clipboardoverlay.ClipboardListener".toClassOrNull()?.method {
            name = "forceSuppressOverlay"
            modifiers { isStatic }
        }?.hook {
            before {
                if (!Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.UNHIDE_CLIPBOARD_OVERLAY, false)) return@before
                result = false
                YLog.debug("$TAG forceSuppressOverlay")
            }
        }
    }
}


