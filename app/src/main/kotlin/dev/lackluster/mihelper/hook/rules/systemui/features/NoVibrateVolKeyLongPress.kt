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


