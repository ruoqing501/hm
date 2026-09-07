package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.HookParam
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import java.text.SimpleDateFormat
import java.util.*

object StatusBarPullDownPeriod : YukiBaseHooker() {
    // 下拉状态栏时段[索引0表示第1个选项]
    private val statusBarPullDownPeriodTextType by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_PULL_DOWN_PERIOD, 0)
    }

    // 存储创建的时段TextView，避免重复创建
    private val periodTextViews = mutableMapOf<View, TextView>()

    // 目标资源ID和类名
    private const val TARGET_RESOURCE_ID = "clock"
    private const val TARGET_CLASS_NAME = "Clock"


    override fun onHook() {
        // 如果默认第1个选项【不显示时段】，则不进行hook处理
        if (statusBarPullDownPeriodTextType == 0) {
            YLog.debug("[RedMagicToolStausBarPullDownPeriod:默认第1个选项【不显示时段】，不进行hook处理]")
            return
        }

        // 方法1: Hook Clock 类的 updateClock 方法
        val clockClass = "com.android.systemui.statusbar.policy.Clock".toClassOrNull()
        if (clockClass == null) {
            YLog.debug("[RedMagicToolStausBarPullDownPeriod:Clock类没有找到]")
            return
        }
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:找到Clock类]")
        clockClass.apply {
            method {
                name = "updateClock"
            }.hook {
                after {
                    updateClock(this)
                }
            }
        }
    }
    @SuppressLint("PrivateApi")
    private fun updateClock(param: HookParam) {
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:进入updateClock的Hook]")
        // 得到时间对象
        val clockView = param.instance as? TextView ?: return
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:获取到了时间对象${clockView.text}]")
        // 获取视图的简单类名
        val clockViewClassName = clockView.javaClass.simpleName
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:简单类名是${clockViewClassName}]")
        // 检查是否是目标类(Clock)
        if (clockViewClassName != TARGET_CLASS_NAME) {
            YLog.debug("[RedMagicToolStausBarPullDownPeriod:不是目标类Clock，跳过]")
            return
        }
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:是Clock类]")
        // 获取资源ID名称
        val resourceIdName = getResourceIdName(clockView)
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:资源ID名称是${resourceIdName}]")
        // 检查是否是目标资源ID (clock)
        if (resourceIdName != TARGET_RESOURCE_ID) {
            YLog.debug("[RedMagicToolStausBarPullDownPeriod:不是目标资源ID clock，跳过]")
            return
        }
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:是目标资源ID clock]")
        // 检查是否是快速设置面板的时钟（暂时注释掉）

        if (!isQuickSettingPanelClock(clockView)) {
            YLog.debug("[RedMagicToolStausBarPullDownPeriod:不是快速设置面板时钟，跳过]")
            return
        }

        YLog.debug("[RedMagicToolStausBarPullDownPeriod:通过检查，开始处理]")
        // 获取原始文本
        val originalTextStr = clockView.text.toString()
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:获取到了原始文本${originalTextStr}]")

        // 只处理时间格式的文本
        if (!isTimeText(originalTextStr)) {
            YLog.debug("[RedMagicToolStausBarPullDownPeriod:不是时间文本，跳过]")
            return
        }
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:是时间文本]")

        // 获取时段信息
        val periodText = getPeriodText(clockView.context)
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:获取到了时段文本${periodText}]")

        // 创建或获取时段TextView
        val periodTextView = getOrCreatePeriodTextView(clockView)
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:获取到了时段TextView${periodTextView}]")

        // 设置时段文本
        periodTextView.text = periodText
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:设置时段文本${periodText}]")

        // 调整时段TextView的位置和大小
        adjustPeriodTextView(clockView, periodTextView)
        YLog.debug("[RedMagicToolStausBarPullDownPeriod:调整完成]")
    }

    /**
     * 获取时段文本
     */
    @SuppressLint("SimpleDateFormat")
    private fun getPeriodText(context: Context): String {
        val nowTime = Calendar.getInstance().time
        val isZh = isZh(context)

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

    private fun getOrCreatePeriodTextView(clockTextView: TextView): TextView {
        return periodTextViews[clockTextView] ?: run {
            // 获取父容器（time_group）
            val parent = clockTextView.parent as? ViewGroup
            if (parent != null) {
                // 创建新的TextView用于显示时段
                val periodTextView = TextView(clockTextView.context)
                periodTextView.id = View.generateViewId() // 生成唯一的ID
                periodTextView.tag = "WooBox_PeriodTextView" // 设置标签以便识别

                // 获取时钟TextView的字体大小
                val clockTextSize = clockTextView.textSize

                // 设置时段TextView的字体大小为时钟的20%
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
                periodTextViews[clockTextView] = periodTextView

                YLog.debug("[WooBox-QuickSettingPanelClock] 创建时段TextView: ID=${periodTextView.id}, 字体大小=${periodTextSize}px (时钟字体大小的20%)")

                periodTextView
            } else {
                // 如果找不到父容器，返回一个虚拟的TextView（不应该发生）
                TextView(clockTextView.context).apply {
                    visibility = View.GONE
                }
            }
        }
    }

    /**
     * 调整时段TextView的位置和属性
     */
    private fun adjustPeriodTextView(clockTextView: TextView, periodTextView: TextView) {
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
     * 检查是否为快速设置面板时钟
     * 根据视图层级进行匹配
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
     * 判断是否为时间格式文本
     */
    private fun isTimeText(text: String): Boolean {
        if (text.isEmpty()) return false

        // 检查是否匹配时间格式 (HH:MM 或 H:MM 或 HH:MM:SS 或 H:MM:SS)
        val timePattern = Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$")
        if (timePattern.matches(text)) {
            return true
        }

        // 检查是否包含AM/PM（支持带秒和不带秒）
        val timeWithAmPmPattern = Regex("^\\d{1,2}:\\d{2}(:\\d{2})?\\s*[AP]M$", RegexOption.IGNORE_CASE)
        if (timeWithAmPmPattern.matches(text)) {
            return true
        }

        return false
    }

    /**
     * 检查是否为中文环境
     */
    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        val language = locale.language
        return language.endsWith("zh")
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