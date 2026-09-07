package dev.lackluster.mihelper.hook.rules.settings.nubia

import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object DisableBatteryPercentageDisplayItem : YukiBaseHooker() {
    private const val TAG = "DisableBatteryPercentageDisplayItem"

    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.IconTurner.DISABLE_ATTERY_PERCENTAGE_DISPLAY_SETTING_OPTION) {
            YLog.debug("$TAG 功能已开启，开始禁用电池百分比显示选项")

            // 1. 处理ZTE控制器：让 isAvailable 始终返回 false
            "com.zte.settings.notification.MfvBatteryPercentagePreferenceController".toClassOrNull()?.method {
                name = "isAvailable"
                returnType = BooleanType
            }?.hook {
                replaceTo(false)
                YLog.debug("$TAG 已强制 MfvBatteryPercentagePreferenceController.isAvailable = false")
            } ?: YLog.warn("$TAG 未找到 MfvBatteryPercentagePreferenceController 类")
        }
    }
}