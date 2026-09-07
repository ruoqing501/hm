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
import dev.lackluster.redmagichelper.utils.factory.hasEnable

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


