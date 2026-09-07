package dev.lackluster.mihelper.hook.apps.nubia

import android.annotation.SuppressLint
import android.widget.TextClock
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import java.text.SimpleDateFormat
import java.util.Date

object NubiaWeather : YukiBaseHooker() {
    private const val TAG = "NubiaWeather"
    // 显示时段
//    private val showTimePeriod by lazy {
//        Prefs.getBoolean(Pref.Key.SystemDesktop.SYSTEM_TIME_COMPONENT_DESKTOP_SHOW_PERIOD, false)
//    }
    private val  showTimePeriod = true
    // 可能的时段控件 ID（用于显示中文时段）
    private val targetPeriodIds = listOf(
        "widget_am_pm",
    )


    override fun onHook() {
        val textClockClass = TextClock::class.java

        // ------------------ 时段显示 ------------------
        // 拦截 onTimeChanged 方法，更新时段控件的文本
        textClockClass.method {
            name = "onTimeChanged"
        }.hook {
            after {
                val textClock = instance as? TextClock ?: return@after

                // 仅在中文环境下生效
                if (!isZh()) return@after

                // 判断是否为时段控件
                if (!isTargetClock(textClock, targetPeriodIds)) return@after

                val now = Date()
                val period = getPeriod(now)   // 中文时段
                if (showTimePeriod && period.isNotEmpty()) {
                    // 避免无限循环（TextClock 内部可能因文本变化再次触发）
                    if (textClock.text != period) {
                        textClock.text = period
                        YLog.debug(tag = TAG, msg = "Updated period to: $period")
                    }
                }
            }
        }
    }
    /**
     * 判断当前 TextClock 是否属于给定的 ID 列表之一
     */
    private fun isTargetClock(clock: TextClock, idNames: List<String>): Boolean {
        val context = clock.context ?: return false
        return idNames.any { idName ->
            val resId = context.resources.getIdentifier(idName, "id", context.packageName)
            resId != 0 && clock.id == resId
        }
    }

    /**
     * 判断当前系统语言是否为中文
     */
    private fun isZh(): Boolean {
        val locale = appContext?.resources?.configuration?.locale ?: return false
        return locale.language == "zh"
    }

    /**
     * 根据当前时间返回中文时段
     */
    @SuppressLint("SimpleDateFormat")
    private fun getPeriod(now: Date): String {
        if (!showTimePeriod) return ""
        return when (SimpleDateFormat("HH").format(now)) {
            in "00".."05" -> "凌晨"
            in "06".."08" -> "早上"
            in "09".."11" -> "上午"
            "12"          -> "中午"
            in "13".."17" -> "下午"
            "18"          -> "傍晚"
            in "19".."23" -> "晚上"
            else          -> ""
        }
    }


}
