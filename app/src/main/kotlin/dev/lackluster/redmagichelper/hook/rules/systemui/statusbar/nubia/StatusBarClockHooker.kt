package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia


import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.param.HookParam
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import java.text.SimpleDateFormat
import java.util.*

object StatusBarClockHooker : YukiBaseHooker() {
    // 状态栏时钟相关设置
    private val isPeriod by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_PERIOD, false)
    }
    private val isWeek by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_WEEK, false)
    }
    private val isMonthDay by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_MONTH_DAY, false)
    }

    // 下拉状态栏时段设置
    private val statusBarPullDownPeriodTextType by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_PULL_DOWN_PERIOD, 0)
    }

    // 存储创建的下拉状态栏时段TextView，避免重复创建
    private val pullDownPeriodTextViews = mutableMapOf<View, TextView>()

    // 目标资源ID和类名
    private const val TARGET_RESOURCE_ID = "clock"
    private const val TARGET_CLASS_NAME = "Clock"

    // 中文时段定义
    private val chinesePeriods = listOf("凌晨", "上午", "中午", "下午", "傍晚", "晚上")

    // 时间格式常量
    private const val TIME_PATTERN = "^\\d{1,2}:\\d{2}(:\\d{2})?$"
    private const val TIME_WITH_AMPM_PATTERN = "^\\d{1,2}:\\d{2}(:\\d{2})?\\s*[AP]M$"

    override fun onHook() {
        // 如果所有功能都关闭，则不Hook
        if (!isPeriod && !isWeek && !isMonthDay && statusBarPullDownPeriodTextType == 0) {
            YLog.debug("[StatusBarClockHooker] 所有功能都已关闭，跳过Hook")
            return
        }

        YLog.debug("[StatusBarClockHooker] 开始Hook Clock类")

        "com.android.systemui.statusbar.policy.Clock".toClassOrNull()?.apply {
            method { name = "updateClock" }.hook {
                after {
                    handleClockUpdate(this)
                }
            }
        } ?: run {
            YLog.debug("[StatusBarClockHooker] Clock类未找到")
        }
    }

    private fun handleClockUpdate(param: HookParam) {
        val clockView = param.instance as? TextView ?: return
        val originalText = clockView.text.toString()

        // 只处理时间格式的文本
        if (!isTimeText(originalText)) return

        // 判断时钟类型并执行相应处理
        when {
            isStatusBarClock(clockView) -> {
                // 处理状态栏时钟（直接修改文本）
                handleStatusBarClock(clockView, originalText)
            }
            isQuickSettingPanelClock(clockView) -> {
                // 处理下拉状态栏时钟（添加额外TextView）
                handlePullDownClock(clockView, originalText)
            }

        }
    }

    /**
     * 处理状态栏时钟（直接修改文本）
     */
    private fun handleStatusBarClock(clockView: TextView, originalText: String) {
        if (!isPeriod && !isWeek && !isMonthDay) return

        YLog.debug("[StatusBarClockHooker] 处理状态栏时钟: $originalText")

        val newText = processStatusBarTimeText(originalText, clockView.context)
        if (originalText != newText) {
            clockView.text = newText
            YLog.debug("[StatusBarClockHooker] 状态栏时钟更新为: $newText")
        }
    }

    /**
     * 处理下拉状态栏时钟（添加额外TextView）
     */
    private fun handlePullDownClock(clockView: TextView, originalText: String) {
        if (statusBarPullDownPeriodTextType == 0) {
            // 如果关闭了功能，移除已创建的TextView
            removePullDownPeriodTextView(clockView)
            return
        }

//        YLog.debug("[StatusBarClockHooker] 处理下拉状态栏时钟: $originalText")

        // 获取时段文本
        val periodText = getPeriodText(clockView.context, isZh(clockView.context))
//        YLog.debug("[StatusBarClockHooker] 时段文本: $periodText")

        // 创建或获取时段TextView
        val periodTextView = getOrCreatePullDownPeriodTextView(clockView)

        // 设置时段文本
        periodTextView.text = periodText

        // 调整时段TextView的位置和大小
        adjustPullDownPeriodTextView(clockView, periodTextView)
    }

    /**
     * 处理状态栏时间文本
     */
    private fun processStatusBarTimeText(originalText: String, context: Context): String {
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
                    append(getPeriodText(context, isZh))
                    append(" ")
                }
                append(timePart)
            } else {
                // 英文格式：时间 时段 星期 月日
                append(timePart)
                if (isPeriod && !containsPeriod(originalText, isZh)) {
                    append(" ")
                    append(getPeriodText(context, isZh))
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

    /**
     * 获取时段文本
     */
    @SuppressLint("SimpleDateFormat")
    private fun getPeriodText(context: Context, isZh: Boolean): String {
        val nowTime = Calendar.getInstance().time

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

    /**
     * 获取星期文本
     */
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

    /**
     * 获取月日文本
     */
    private fun getMonthDay(isZh: Boolean, date: Date): String {
        return if (isZh) {
            SimpleDateFormat("M月d日", Locale.CHINA).format(date)
        } else {
            SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
        }
    }

    /**
     * 检查是否已经包含时段
     */
    private fun containsPeriod(text: String, isZh: Boolean): Boolean {
        return if (isZh) {
            chinesePeriods.any { period ->
                text.contains(period, ignoreCase = true)
            }
        } else {
            Regex("[AP]\\.?M\\.?", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }
    }

    /**
     * 检查是否为中文环境
     */
    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        return locale.language.endsWith("zh")
    }

    /**
     * 检查是否为时间格式文本
     */
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

    /**
     * 获取或创建下拉状态栏时段TextView
     */
    private fun getOrCreatePullDownPeriodTextView(clockTextView: TextView): TextView {
        return pullDownPeriodTextViews[clockTextView] ?: run {
            // 获取父容器（time_group）
            val parent = clockTextView.parent as? ViewGroup
            if (parent != null) {
                // 创建新的TextView用于显示时段
                val periodTextView = TextView(clockTextView.context)
                periodTextView.id = View.generateViewId() // 生成唯一的ID
                periodTextView.tag = "WooBox_PeriodTextView" // 设置标签以便识别

                // 获取时钟TextView的字体大小
                val clockTextSize = clockTextView.textSize

                // 设置时段TextView的字体大小为时钟的30%
                val periodTextSize = clockTextSize * 0.3f
                periodTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, periodTextSize)

                // 设置文本颜色与时钟一致
                periodTextView.setTextColor(clockTextView.currentTextColor)

                // 设置可见性
                periodTextView.visibility = View.VISIBLE

                // 设置布局参数
                val layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )

                // 设置在时钟TextView的右侧
                layoutParams.addRule(RelativeLayout.RIGHT_OF, clockTextView.id)
                layoutParams.addRule(RelativeLayout.ALIGN_BASELINE, clockTextView.id)

                // 添加左边距
                val margin = (clockTextSize * 0.1f).toInt() // 10%的字体大小作为间距
                layoutParams.leftMargin = margin

                periodTextView.layoutParams = layoutParams

                // 添加到父容器中
                parent.addView(periodTextView)

                // 存储到映射中
                pullDownPeriodTextViews[clockTextView] = periodTextView

                YLog.debug("[StatusBarClockHooker] 创建下拉状态栏时段TextView: ID=${periodTextView.id}, 字体大小=${periodTextSize}px")

                periodTextView
            } else {
                // 如果找不到父容器，返回一个虚拟的TextView
                TextView(clockTextView.context).apply {
                    visibility = View.GONE
                }
            }
        }
    }

    /**
     * 移除下拉状态栏时段TextView
     */
    private fun removePullDownPeriodTextView(clockTextView: TextView) {
        pullDownPeriodTextViews[clockTextView]?.let { periodTextView ->
            try {
                val parent = periodTextView.parent as? ViewGroup
                parent?.removeView(periodTextView)
                pullDownPeriodTextViews.remove(clockTextView)
                YLog.debug("[StatusBarClockHooker] 移除下拉状态栏时段TextView")
            } catch (e: Exception) {
                // 忽略异常
            }
        }
    }

    /**
     * 调整下拉状态栏时段TextView的位置和属性
     */
    private fun adjustPullDownPeriodTextView(clockTextView: TextView, periodTextView: TextView) {
        try {
            // 确保时段TextView可见
            periodTextView.visibility = View.VISIBLE

            // 获取时钟TextView的当前字体大小
            val clockTextSize = clockTextView.textSize

            // 更新时段TextView的字体大小为时钟的30%
            val periodTextSize = clockTextSize * 0.3f
            periodTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, periodTextSize)

            // 更新文本颜色与时钟一致
            periodTextView.setTextColor(clockTextView.currentTextColor)

            // 获取布局参数
            val layoutParams = periodTextView.layoutParams as? RelativeLayout.LayoutParams
            if (layoutParams != null) {
                // 更新左边距
                val margin = (clockTextSize * 0.1f).toInt()
                layoutParams.leftMargin = margin

                // 确保与时钟TextView基线对齐
                layoutParams.addRule(RelativeLayout.RIGHT_OF, clockTextView.id)
                layoutParams.addRule(RelativeLayout.ALIGN_BASELINE, clockTextView.id)

                periodTextView.layoutParams = layoutParams
            }
        } catch (e: Exception) {
            // 忽略异常
        }
    }

    /**
     * 检查是否为状态栏时钟
     */
    private fun isStatusBarClock(view: View): Boolean {
        try {
            // 获取直接父容器
            val directParent = view.parent
            if (directParent !is View) return false

            val parentId = getResourceIdName(directParent as View)
            if (parentId != "status_bar_start_side_except_heads_up") {
                return false
            }

            // 检查祖父容器
            val grandParent = directParent.parent
            if (grandParent !is View) return false

            val grandParentId = getResourceIdName(grandParent as View)
            if (grandParentId != "status_bar_start_side_content") {
                return false
            }

            // 检查曾祖父容器
            val greatGrandParent = grandParent.parent
            if (greatGrandParent !is View) return false

            val greatGrandParentId = getResourceIdName(greatGrandParent as View)
            if (greatGrandParentId != "status_bar_start_side_container") {
                return false
            }

            // 检查高高祖父容器
            val greatGreatGrandParent = greatGrandParent.parent
            if (greatGreatGrandParent !is View) return false

            val greatGreatGrandParentId = getResourceIdName(greatGreatGrandParent as View)
            if (greatGreatGrandParentId != "status_bar_contents") {
                return false
            }

            return true

        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 检查是否为快速设置面板时钟（下拉状态栏时钟）
     */
    private fun isQuickSettingPanelClock(view: View): Boolean {
        try {
            // 检查直接父容器
            val directParent = view.parent
            if (directParent !is View) return false

            val directParentId = getResourceIdName(directParent as View)
            if (directParentId != "time_group") {
                return false
            }

            // 检查祖父容器
            val grandParent = directParent.parent
            if (grandParent !is View) return false

            val grandParentId = getResourceIdName(grandParent as View)
            if (grandParentId != "cc_header") {
                return false
            }

            return true

        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 获取View的资源ID名称
     */
    private fun getResourceIdName(view: View): String {
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
}