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

object NoVibrateVolKeyLongPress : YukiBaseHooker() {
    private const val TAG = "NoVibrateVolKeyLongPress"


    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.NO_VIBRATE_VOLKEY_LONG_PRESS) {
            "com.zte.adapt.mifavor.volume.VolumeDialogImplAdapt".toClassOrNull()?.method {
                name = "richTapVibrateForVolumeKeyLongPress"
            }?.hook {
                before {
                    result = null
                    YLog.debug("$TAG richTapVibrateForVolumeKeyLongPress")
                }

            }
        }
    }
}


