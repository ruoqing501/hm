package dev.lackluster.redmagichelper.hook.rules.settings.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object DisableBatteryPercentageDisplayItem : YukiBaseHooker() {
    private const val TAG = "DisableBatteryPercentageDisplayItem"

    override fun onHook() {
        // 1. 处理ZTE控制器：让 isAvailable 始终返回 false
        "com.zte.settings.notification.MfvBatteryPercentagePreferenceController".toClassOrNull()?.method {
            name = "isAvailable"
            returnType = BooleanType
        }?.hook {
            before {
                if (!Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.DISABLE_ATTERY_PERCENTAGE_DISPLAY_SETTING_OPTION, false)) return@before
                result = false
            }
        } ?: YLog.warn("$TAG 未找到 MfvBatteryPercentagePreferenceController 类")
    }
}
