package dev.lackluster.redmagichelper.hook.rules.systemui.screenoff.nubia

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextClock
import android.widget.TextView
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

object ScreenOffPeriodModifier : YukiBaseHooker() {
    // 时段（开关的状态）
    private val screenOffShowPeriod get() =
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.SCREEN_OFF_PERIOD, false)
    // 时段字体大小
    private val screenOffShowPeriodFontSize get() =
        Prefs.getFloat(Pref.Key.SystemUI.ScreenOff.SCREEN_OFF_PERIOD_FONT_SIZE_SETTINGS, 0.6f)

    // 存储创建的时段TextView，避免重复创建
    private val periodTextViews = WeakHashMap<TextClock, TextView>()

    private val hookHistory = mutableListOf<String>()
    private val hookedTextClocks = HashSet<Int>()

    @SuppressLint("PrivateApi", "SimpleDateFormat")
    override fun onHook() {
        // 熄屏显秒由 AodSecondUpdate 实现，这里只管时段
        YLog.debug("[ScreenOffClockShowSeconds] 开始Hook锁屏时钟和日期显示功能")
        try {
            // Hook TextClock类
            hookTextClock()

            // 记录Hook历史（调试用）
            logHookHistory()

            YLog.debug("[ScreenOffClockShowSeconds] Hook锁屏时钟和日期显示功能完成")
        } catch (e: Exception) {
            YLog.debug("[ScreenOffClockShowSeconds] Hook锁屏时钟和日期显示时出错: ${e.message}", e)
        }
    }

    /**
     * Hook TextClock类
     */
    private fun hookTextClock() {
        try {
            YLog.debug("[ScreenOffClockShowSeconds] 寻找TextClock类")

            // 使用 YukiHookAPI 的 toClass() 方法获取类
            "android.widget.TextClock".toClass().apply {
//                // Hook onTimeChanged 方法
                method {
                    name = "onTimeChanged"
                    emptyParam()
                }.hook {
                    after {
                        val textClock = this.instance as? TextClock ?: return@after
                        handleTextClockUpdate(textClock)
                    }
                }

                // 尝试 Hook refresh 方法
                try {
                    method {
                        name = "refresh"
                        emptyParam()
                    }.hook {
                        after {
                            val textClock = this.instance as? TextClock ?: return@after
                            handleTextClockUpdate(textClock)
                        }
                    }
                    YLog.debug("[ScreenOffClockShowSeconds] 成功Hook refresh 方法")
                } catch (e: Exception) {
                    YLog.debug("[ScreenOffClockShowSeconds] 未能Hook refresh 方法: ${e.message}")
                }

//                 Hook setFormat12Hour 方法
                method {
                    name = "setFormat12Hour"
                    param(CharSequence::class.java)
                }.hook {
                    before {
                        val textClock = this.instance as? TextClock ?: return@before
                        val format = this.args[0] as? CharSequence
                        // 防止递归调用，记录原始格式
                        if (!isRecursiveCall(textClock)) {
                            handleFormatChange(textClock, format?.toString(), false)
                        }
                    }
                }

                // Hook setFormat24Hour 方法
                method {
                    name = "setFormat24Hour"
                    param(CharSequence::class.java)
                }.hook {
                    before {
                        val textClock = this.instance as? TextClock ?: return@before
                        val format = this.args[0] as? CharSequence
                        // 防止递归调用，记录原始格式
                        if (!isRecursiveCall(textClock)) {
                            handleFormatChange(textClock, format?.toString(), true)
                        }
                    }
                }
            }

            YLog.debug("[ScreenOffClockShowSeconds] 成功Hook TextClock类")
        } catch (e: Exception) {
            YLog.debug("[ScreenOffClockShowSeconds] Hook TextClock时出错: ${e.message}")
        }
    }

    /**
     * 处理TextClock更新
     */
    private fun handleTextClockUpdate(textClock: TextClock) {
        // 获取当前资源的ID名称
        val resourceIdName = getResourceIdName(textClock)

        // 只处理时钟资源（可以根据需要调整）
        if (resourceIdName != "clock" && resourceIdName != "NO_ID" && !resourceIdName.contains("clock")) {
            // 非时钟组件，跳过
            return
        }

        // 获取上下文
        val context = textClock.context
        val is24Hour = DateFormat.is24HourFormat(context)
        val isZh = isZh(context)

        //每秒记录一次日志，并记录显示的时间精确到秒
        if (screenOffShowPeriod) {
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)
            YLog.debug("[ScreenOffClockShowSeconds] TextClock更新: 时间=$currentTime, 资源ID=$resourceIdName, 24小时制=$is24Hour, 中文环境=$isZh")
        }


        // 处理时段显示（创建独立的TextView）
        handlePeriodDisplay(textClock, isZh)
    }

    /**
     * 处理时段显示 - 创建独立的TextView
     */
    private fun handlePeriodDisplay(textClock: TextClock, isZh: Boolean) {
        if (!screenOffShowPeriod) {
            // 如果关闭了功能，移除已创建的TextView
            removePeriodTextView(textClock)
            return
        }

        // 创建或获取时段TextView
        val periodTextView = getOrCreatePeriodTextView(textClock)

        // 更新时段文本
        updatePeriodText(periodTextView, isZh)

        // 调整时段TextView的位置和大小
        adjustPeriodTextView(textClock, periodTextView)
    }

    /**
     * 获取或创建时段TextView
     */
    private fun getOrCreatePeriodTextView(textClock: TextClock): TextView {
        return periodTextViews[textClock] ?: run {
            // 获取父容器
            val parent = textClock.parent as? ViewGroup
            if (parent != null) {
                // 创建新的TextView用于显示时段
                val periodTextView = TextView(textClock.context)
                periodTextView.id = View.generateViewId() // 生成唯一的ID
                periodTextView.tag = "WooBox_ScreenOffPeriodTextView" // 设置标签以便识别

                // 获取时钟TextView的字体大小
                val clockTextSize = textClock.textSize

                // 设置时段TextView的字体大小为时钟的30%
//                val periodTextSize = clockTextSize * 0.3f
//                val periodTextSize = clockTextSize * 0.6f
                val periodTextSize = clockTextSize * screenOffShowPeriodFontSize
                periodTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, periodTextSize)

                // 设置文本颜色与时钟一致
                periodTextView.setTextColor(textClock.currentTextColor)

                // 设置可见性
                periodTextView.visibility = View.VISIBLE

                // 获取父布局类型并设置相应的布局参数
                val layoutParams = when (parent) {
                    is RelativeLayout -> {
                        val params = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        )
                        // 设置在时钟TextView的右侧
                        params.addRule(RelativeLayout.RIGHT_OF, textClock.id)
                        params.addRule(RelativeLayout.ALIGN_BASELINE, textClock.id)
                        params
                    }
                    is LinearLayout -> {
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params
                    }
                    else -> {
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                }

                // 添加左边距
                val margin = (clockTextSize * 0.1f).toInt() // 10%的字体大小作为间距
                if (layoutParams is ViewGroup.MarginLayoutParams) {
                    layoutParams.leftMargin = margin
                }

                periodTextView.layoutParams = layoutParams

                // 添加到父容器中
                parent.addView(periodTextView)

                // 存储到映射中
                periodTextViews[textClock] = periodTextView

                YLog.debug("[ScreenOffClockShowSeconds] 创建熄屏时钟时段TextView: ID=${periodTextView.id}, 字体大小=${periodTextSize}px")

                periodTextView
            } else {
                // 如果找不到父容器，返回一个虚拟的TextView
                TextView(textClock.context).apply {
                    visibility = View.GONE
                }
            }
        }
    }

    /**
     * 移除时段TextView
     */
    private fun removePeriodTextView(textClock: TextClock) {
        periodTextViews[textClock]?.let { periodTextView ->
            try {
                val parent = periodTextView.parent as? ViewGroup
                parent?.removeView(periodTextView)
                periodTextViews.remove(textClock)
                YLog.debug("[ScreenOffClockShowSeconds] 移除熄屏时钟时段TextView")
            } catch (e: Exception) {
                // 忽略异常
            }
        }
    }

    /**
     * 更新时段文本
     */
    private fun updatePeriodText(periodTextView: TextView, isZh: Boolean) {
        val periodText = getCurrentPeriod(isZh)
        periodTextView.text = periodText
    }

    /**
     * 调整时段TextView的位置和属性
     */
    private fun adjustPeriodTextView(textClock: TextClock, periodTextView: TextView) {
        try {
            // 确保时段TextView可见
            periodTextView.visibility = View.VISIBLE

            // 获取时钟TextView的当前字体大小
            val clockTextSize = textClock.textSize

            // 更新时段TextView的字体大小为时钟的30%
//            val periodTextSize = clockTextSize * 0.3f
//            val periodTextSize = clockTextSize * 0.6f
            val periodTextSize = clockTextSize * screenOffShowPeriodFontSize

            periodTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, periodTextSize)

            // 更新文本颜色与时钟一致
            periodTextView.setTextColor(textClock.currentTextColor)

            // 更新布局参数（如果父布局是RelativeLayout）
            val parent = periodTextView.parent
            if (parent is RelativeLayout) {
                val layoutParams = periodTextView.layoutParams as? RelativeLayout.LayoutParams
                if (layoutParams != null) {
                    // 更新左边距
                    val margin = (clockTextSize * 0.1f).toInt()
                    layoutParams.leftMargin = margin

                    // 确保与时钟TextView基线对齐
                    layoutParams.addRule(RelativeLayout.RIGHT_OF, textClock.id)
                    layoutParams.addRule(RelativeLayout.ALIGN_BASELINE, textClock.id)

                    periodTextView.layoutParams = layoutParams
                }
            }
        } catch (e: Exception) {
            // 忽略异常
        }
    }

    /**
     * 获取当前时段
     */
    @SuppressLint("SimpleDateFormat")
    private fun getCurrentPeriod(isZh: Boolean): String {
        val now = Calendar.getInstance().time
        val hour = SimpleDateFormat("HH").format(now).toInt()

        return if (isZh) {
            when (hour) {
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
            // 英文环境下使用AM/PM
            SimpleDateFormat("a", Locale.ENGLISH).format(now)
        }
    }

    /**
     * 处理格式变化
     */
    private fun handleFormatChange(textClock: TextClock, originalFormat: String?, is24Hour: Boolean) {
        // 获取当前资源的ID名称
        val resourceIdName = getResourceIdName(textClock)

        // 只处理时钟资源
        if (resourceIdName != "clock" && resourceIdName != "NO_ID" && !resourceIdName.contains("clock")) {
            return
        }

        // 获取上下文
        val context = textClock.context
        val isZh = isZh(context)






        // 处理时段显示（更新独立的TextView）
        if (screenOffShowPeriod) {
            val periodTextView = periodTextViews[textClock]
            if (periodTextView != null) {
                updatePeriodText(periodTextView, isZh)
            }
        }
    }

    /**
     * 防止递归调用的辅助方法
     */
    private val recursiveTextClocks = WeakHashMap<TextClock, Boolean>()

    private fun isRecursiveCall(textClock: TextClock): Boolean {
        return recursiveTextClocks[textClock] == true
    }

    /**
     * 检查是否为中文环境
     */
    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locales.get(0)
        val language = locale.language
        return language.endsWith("zh")
    }

    /**
     * 获取TextView的资源ID名称
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

    /**
     * 记录所有hook历史
     */
    private fun logHookHistory() {
        if (hookHistory.isNotEmpty()) {
            YLog.debug("[ScreenOffClockShowSeconds] ====== Hook历史记录 ======")
            hookHistory.forEachIndexed { index, entry ->
                YLog.debug("[ScreenOffClockShowSeconds] ${index + 1}. $entry")
            }
            YLog.debug("[ScreenOffClockShowSeconds] 总共Hook了 ${hookHistory.size} 次时钟显示")
            YLog.debug("[ScreenOffClockShowSeconds] ====== Hook历史记录结束 ======")
        } else {
            YLog.debug("[ScreenOffClockShowSeconds] 没有时钟显示被Hook")
        }
    }
}