package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.HookParam
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import java.text.SimpleDateFormat
import java.util.*
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarPullDownPeriod

object StatusBarClockPeriod : YukiBaseHooker() {
    private val isPeriod by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_PERIOD, false)
    }
    private val isWeek by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_WEEK, false)
    }
    private val isMonthDay by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_MONTH_DAY, false)
    }


    // 目标资源ID和类名
    private const  val TARGET_RESOURCE_ID = "clock"
    private const  val TARGET_CLASS_NAME = "Clock"

    // 时间格式常量
    private const val TIME_PATTERN = "^\\d{1,2}:\\d{2}(:\\d{2})?$"
    private const val TIME_WITH_AMPM_PATTERN = "^\\d{1,2}:\\d{2}(:\\d{2})?\\s*[AP]M$"

    // 中文时段定义
    private val chinesePeriods = listOf("凌晨", "上午", "中午", "下午", "傍晚", "晚上")

    override fun onHook() {
        // 如果没有开启任何功能，则不进行Hook
        if (!isPeriod && !isWeek && !isMonthDay) return

        // Hook Clock 类的 updateClock 方法
        "com.android.systemui.statusbar.policy.Clock".toClass().method {
            name = "updateClock"
        }.hook {
            after {

                handleClockUpdate(this)
            }
        }
    }
    /**
    ◦ 获取TextView的资源ID名称

     */
    private fun getResourceIdName(view: android.view.View): String {
        return try {
            val resId = view.id
            if (resId <= 0) {
                "NO_ID"
            } else {
                try {
                    view.resources.getResourceEntryName(resId)
                } catch (e: android.content.res.Resources.NotFoundException) {
                    "0x${Integer.toHexString(resId)}"
                }
            }
        } catch (e: Exception) {
            "UNKNOWN_ID"
        }
    }
    private fun isStatusBarClock(view: View): Boolean {
        try {
            // 获取直接父容器
            val directParent = view.parent
            if (directParent !is View) return false

            val parentId = getResourceIdName(directParent as View)
            if (parentId != "status_bar_start_side_except_heads_up") {
//                YLog.debug("[RedMagic-StatusBarClock] 直接父容器ID不匹配: ${parentId}, 期望: status_bar_start_side_except_heads_up")
                return false
            }

            // 检查祖父容器
            val grandParent = directParent.parent
            if (grandParent !is View) return false

            val grandParentId = getResourceIdName(grandParent as View)
            if (grandParentId != "status_bar_start_side_content") {
//                YLog.debug("[RedMagic-StatusBarClock] 祖父容器ID不匹配: $grandParentId, 期望: status_bar_start_side_content")
                return false
            }

            // 检查曾祖父容器
            val greatGrandParent = grandParent.parent
            if (greatGrandParent !is View) return false

            val greatGrandParentId = getResourceIdName(greatGrandParent as View)
            if (greatGrandParentId != "status_bar_start_side_container") {
//                YLog.debug("[RedMagic-StatusBarClock] 曾祖父容器ID不匹配: $greatGrandParentId, 期望: status_bar_start_side_container")
                return false
            }

            // 检查高高祖父容器
            val greatGreatGrandParent = greatGrandParent.parent
            if (greatGreatGrandParent !is View) return false

            val greatGreatGrandParentId = getResourceIdName(greatGreatGrandParent as View)
            if (greatGreatGrandParentId != "status_bar_contents") {
//                YLog.debug("[RedMagic-StatusBarClock] 高高祖父容器ID不匹配: $greatGreatGrandParentId, 期望: status_bar_contents")
                return false
            }

//            YLog.debug("[RedMagic-StatusBarClock] 找到状态栏时钟，视图层级验证通过")
            return true

        } catch (e: Exception) {
//            YLog.debug("[RedMagic-StatusBarClock] 检查状态栏时钟时出错: ${e.message}")
            return false
        }
    }

    private fun handleClockUpdate(param: HookParam) {
        val clockView = param.instance as? TextView ?: return
        val originalText = clockView.text.toString()

        // 只处理时间格式的文本
        if (!isTimeText(originalText)) return

        // 处理时间文本
        val newText = processTimeText(originalText, clockView.context)
        if(isStatusBarClock(clockView) ){
            if (originalText != newText) {
                clockView.text = newText
                param.result = null
            }
        }

    }

    private fun isTimeText(text: String): Boolean {
        if (text.isEmpty()) return false

        // 检查标准时间格式 (HH:MM 或 HH:MM:SS)
        if (Regex(TIME_PATTERN).matches(text)) {
            return true
        }

        // 检查带AM/PM的时间格式
        if (Regex(TIME_WITH_AMPM_PATTERN, RegexOption.IGNORE_CASE).matches(text)) {
            return true
        }

        return false
    }

    private fun processTimeText(originalText: String, context: Context): String {
        if (!isPeriod && !isWeek && !isMonthDay) return originalText

        val now = Calendar.getInstance()
        val isZh = isZh(context)
        val timePart = originalText.trim()

        return buildString {
            if (isZh) {
                // 中文格式：月日 星期 时段 时间
                if (isMonthDay) {
                    append(getMonthDay(isZh, now.time))
                    append(" ")
                }
                if (isWeek) {
                    append(getWeekday(isZh, now.time))
                    append(" ")
                }
                if (isPeriod && !containsPeriod(originalText, isZh)) {
                    append(getPeriod(isZh, now.time))
                    append(" ")
                }
                append(timePart)
            } else {
                // 英文格式：时间 时段 星期 月日
                append(timePart)
                if (isPeriod && !containsPeriod(originalText, isZh)) {
                    append(" ")
                    append(getPeriod(isZh, now.time))
                }
                if (isWeek) {
                    append(" ")
                    append(getWeekday(isZh, now.time))
                }
                if (isMonthDay) {
                    append(" ")
                    append(getMonthDay(isZh, now.time))
                }
            }
        }.trim()
    }

    @SuppressLint("SimpleDateFormat")
    private fun getPeriod(isZh: Boolean, nowTime: Date): String {
        return if (isZh) {
            when (SimpleDateFormat("HH").format(nowTime).toInt()) {
                in 0..5 -> "凌晨"
                in 6..8 -> "早上"
                in 9..11 -> "上午"
                12 -> "中午"
                in 13..17 -> "下午"
                18 -> "傍晚"
                in 19..23 -> "晚上"
                else -> ""
            }
        } else {
            SimpleDateFormat("a", Locale.ENGLISH).format(nowTime)
        }
    }

    private fun getWeekday(isZh: Boolean, date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        return if (isZh) {
            when (dayOfWeek) {
                Calendar.SUNDAY -> "周日"
                Calendar.MONDAY -> "周一"
                Calendar.TUESDAY -> "周二"
                Calendar.WEDNESDAY -> "周三"
                Calendar.THURSDAY -> "周四"
                Calendar.FRIDAY -> "周五"
                Calendar.SATURDAY -> "周六"
                else -> ""
            }
        } else {
            SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        }
    }

    private fun getMonthDay(isZh: Boolean, date: Date): String {
        return if (isZh) {
            SimpleDateFormat("M月d日", Locale.CHINA).format(date)
        } else {
            SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
        }
    }

    private fun containsPeriod(text: String, isZh: Boolean): Boolean {
        return if (isZh) {
            chinesePeriods.any { period ->
                text.contains(period, ignoreCase = true)
            }
        } else {
            Regex("[AP]\\.?M\\.?", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }
    }

    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        return locale.language.endsWith("zh")
    }
}