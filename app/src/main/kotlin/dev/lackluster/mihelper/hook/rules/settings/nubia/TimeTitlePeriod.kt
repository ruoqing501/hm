package com.u9521.wooboxforredmagicos.hook.app.systemui.view.textclock

import android.annotation.SuppressLint
import android.content.Context
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import java.util.Calendar

object TimeTitlePeriod : YukiBaseHooker() {



    @SuppressLint("PrivateApi")
    override fun onHook() {
        // 显示时段开关
        hasEnable(Pref.Key.NubiaSystemSettings.TIME_PICKER_PERIOD) {
            YLog.debug("[WooBox-TimeTitlePeriod] 开始Hook时间选择器时段显示功能")

            try {
                // Hook TimePreferenceController 的 getSummary 方法
                "com.android.settings.datetime.TimePreferenceController".toClass().apply {
                    method {
                        name = "getSummary"
                    }.hook {
                        after {
                            val originalSummary = result as? String ?: return@after

                            // 获取当前时间
                            val calendar = Calendar.getInstance()
                            val hour = calendar.get(Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(Calendar.MINUTE)

                            // 获取时段描述
                            val timePeriod = getTimePeriodDescription(hour, minute)

                            // 格式化时间（移除AM/PM，添加时段）
                            val formattedTime = formatTimeWithPeriod(originalSummary, timePeriod, hour, minute)

                            result = formattedTime

                            YLog.debug("[WooBox-TimeTitlePeriod] 更新时间显示: $originalSummary -> $formattedTime")
                        }
                    }
                }
                YLog.debug("[WooBox-TimeTitlePeriod] Hook时间选择器时段显示功能完成")
            } catch (e: Exception) {
                YLog.debug("[WooBox-TimeTitlePeriod] Hook时间选择器时段显示时出错: ${e.message}", e)
            }
        }
    }

    /**
     * 根据小时和分钟获取时段描述
     */
    private fun getTimePeriodDescription(hour: Int, minute: Int): String {
        return when (hour) {
            in 0..5 -> "凌晨"      // 0点到5点（包含0和5点）
            in 6..8 -> "早上"      // 6点到8点（包含6和8点）
            in 9..11 -> "上午"     // 9点到11点（包含9和11点）
            12 -> "中午"           // 12点
            in 13..17 -> "下午"    // 13点到17点（包含13和17点）
            18 -> "傍晚"           // 18点
            in 19..23 -> "晚上"    // 19点到23点（包含19和23点）
            else -> "未知"         // 如果hour超出0-23范围时的备用处理
        }
    }

    /**
     * 格式化时间显示，添加时段描述
     */
    private fun formatTimeWithPeriod(originalTime: String, period: String, hour: Int, minute: Int): String {
        // 移除原有的AM/PM标记
        var cleanedTime = originalTime
            .replace("AM", "")
            .replace("PM", "")
            .replace("上午", "")
            .replace("下午", "")
            .trim()

        // 如果清理后为空，则使用标准时间格式
        if (cleanedTime.isEmpty()) {
            cleanedTime = String.format("%02d:%02d", hour, minute)
        }

        return "$period $cleanedTime"
    }

}