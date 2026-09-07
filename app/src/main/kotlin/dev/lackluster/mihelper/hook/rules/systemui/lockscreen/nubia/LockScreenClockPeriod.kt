package dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.HookParam
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import java.text.SimpleDateFormat
import java.util.*

object LockScreenClockPeriod : YukiBaseHooker() {
    // 时段（开关的状态） 锁屏
    private val showPeriod by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.DISPLAY_PERIOD, false)
    }
    // 时段（开关的状态） 熄屏
    private val screenOffShowPeriod by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.SCREEN_OFF_PERIOD, false)
    }
    private val hookHistory = mutableListOf<String>()
    private var cachedDateViews = mutableListOf<TextView>()

    // 定义多个目标组件
    private val TIME_TARGETS = listOf(
        TargetInfo("clock_view", "TextClock", "时间"),  // 锁屏状态视图时间
        TargetInfo("clock", "TextClock", "时间")        // 锁屏主时钟视图时间
    )



    private val DATE_TARGETS = listOf(
        TargetInfo("date_view", "DateView", "日期"),    // 锁屏状态视图日期
        TargetInfo("date", "TextClock", "日期")         // 锁屏主时钟视图日期
    )

    data class TargetInfo(
        val resourceId: String,
        val className: String,
        val type: String
    )

    @SuppressLint("PrivateApi", "SimpleDateFormat")
    override fun onHook() {
        if (!showPeriod) return

        YLog.debug("[WooBox-LockScreenClock] 开始Hook锁屏时钟和日期显示功能")

        try {
            // 清空缓存
            cachedDateViews.clear()
//            if (screenOffShowPeriod) {
            // Hook 熄屏状态下在时间后，时段的显示
//                hookTextClockOnTimeChanged()
//            }


            // Hook 日期组件的刷新
            hookDateRefresh()

            YLog.debug("[WooBox-LockScreenClock] Hook锁屏时钟和日期显示功能完成")
        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenClock] Hook锁屏时钟和日期显示时出错: ${e.message}", e)
        }
    }

    /**
     * Hook TextClock的onTimeChanged方法
     */
    @SuppressLint("DiscouragedPrivateApi")
    private fun hookTextClockOnTimeChanged() {
        try {
            // Hook onTimeChanged方法
            "android.widget.TextClock".toClass().apply {
                method {
                    name = "onTimeChanged"
                    emptyParam()
                }.hook {
                    after {
                        val textClock = this.instance as? android.widget.TextClock ?: return@after
                        val context = textClock.context

                        // 获取组件信息
                        val resourceId = getResourceIdName(textClock)
                        val className = textClock.javaClass.simpleName
                        val currentText = textClock.text.toString()

                        // 检查是否是时间目标(熄屏模式下)
                        TIME_TARGETS.find { it.resourceId == resourceId && it.className == className }
                            ?.let { target ->
                                if (isTimeText(currentText) && isInLockScreen(textClock, target)) {
                                    val newText = processTimeText(currentText, context)
                                    if (currentText != newText) {
                                        textClock.text = newText
                                        logHookCall(textClock, target, currentText, newText)

                                        // 时间变化时，更新所有日期组件的时段显示
                                        updateAllDateComponentsWithCurrentTime(context)
                                    }
                                }
                            }
                    }
                }

                // 如果找不到onTimeChanged，尝试找refreshTime
                try {
                    method {
                        name = "refreshTime"
                        emptyParam()
                    }.hook {
                        after {
                            val textClock = this.instance as? android.widget.TextClock ?: return@after
                            val context = textClock.context

                            // 获取组件信息
                            val resourceId = getResourceIdName(textClock)
                            val className = textClock.javaClass.simpleName
                            val currentText = textClock.text.toString()

                            // 检查是否是时间目标
                            TIME_TARGETS.find { it.resourceId == resourceId && it.className == className }
                                ?.let { target ->
                                    if (isTimeText(currentText) && isInLockScreen(textClock, target)) {
                                        val newText = processTimeText(currentText, context)
                                        if (currentText != newText) {
                                            textClock.text = newText
                                            logHookCall(textClock, target, currentText, newText)

                                            // 时间变化时，更新所有日期组件的时段显示
                                            updateAllDateComponentsWithCurrentTime(context)
                                        }
                                    }
                                }
                        }
                    }
                } catch (e: Exception) {
                    YLog.debug("[WooBox-LockScreenClock] 找不到refreshTime方法")
                }
            }

        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenClock] Hook TextClock方法时出错: ${e.message}")
        }
    }

    /**
     * Hook日期组件的刷新方法
     */
//    private fun hookDateRefresh() {
//        try {
//            // Hook Android 12+ 的日期刷新
//            val dateViewClass = try {
//                "com.android.systemui.statusbar.policy.DateView".toClass()
//            } catch (e: ClassNotFoundException) {
//                // 如果找不到DateView，尝试其他可能的类
//                try {
//                    "android.widget.TextClock".toClass()
//                } catch (e2: ClassNotFoundException) {
//                    YLog.debug("[WooBox-LockScreenClock] 找不到日期相关类")
//                    return
//                }
//            }
//
//            // 尝试Hook refresh方法
//            dateViewClass.apply {
//                method {
//                    name = "refresh"
//                    emptyParam()
//                }.hook {
//                    after {
//                        handleDateViewHook(this)
//                    }
//                }
//
//                method {
//                    name = "onTimeChanged"
//                    emptyParam()
//                }.hook {
//                    after {
//                        handleDateViewHook(this)
//                    }
//                }
//
//                method {
//                    name = "updateClock"
//                    emptyParam()
//                }.hook {
//                    after {
//                        handleDateViewHook(this)
//                    }
//                }
//
//                method {
//                    name = "updateDate"
//                    emptyParam()
//                }.hook {
//                    after {
//                        handleDateViewHook(this)
//                    }
//                }
//            }
//
//        } catch (e: Exception) {
//            YLog.debug("[WooBox-LockScreenClock] Hook日期刷新时出错: ${e.message}")
//        }
//    }
    private fun hookDateRefresh() {
        try {
            // Hook Android 12+ 的日期刷新
            val dateViewClass = try {
                "com.android.systemui.statusbar.policy.DateView".toClass()
            } catch (e: ClassNotFoundException) {
                // 如果找不到 DateView，尝试其他可能的类
                try {
                    "android.widget.TextClock".toClass()
                } catch (e2: ClassNotFoundException) {
                    YLog.debug("[WooBox-LockScreenClock] 找不到日期相关类")
                    return
                }
            }

            // 尝试 Hook refresh 方法
            dateViewClass.apply {
//                try {
//                    method {
//                        name = "refresh"
//                        emptyParam()
//                    }.hook {
//                        after {
//                            handleDateViewHook(this)
//                        }
//                    }
//                } catch (e: Exception) {
//                    YLog.debug("[WooBox-LockScreenClock] 找不到 refresh 方法")
//                }
//
//                try {
//                    method {
//                        name = "onTimeChanged"
//                        emptyParam()
//                    }.hook {
//                        after {
//                            handleDateViewHook(this)
//                        }
//                    }
//                } catch (e: Exception) {
//                    YLog.debug("[WooBox-LockScreenClock] 找不到 onTimeChanged 方法")
//                }

                try {
                    method {
                        name = "updateClock"
                        emptyParam()
                    }.hook {
                        after {
                            handleDateViewHook(this)
                        }
                    }
                } catch (e: Exception) {
                    YLog.debug("[WooBox-LockScreenClock] 找不到 updateClock 方法")
                }

//                try {
//                    method {
//                        name = "updateDate"
//                        emptyParam()
//                    }.hook {
//                        after {
//                            handleDateViewHook(this)
//                        }
//                    }
//                } catch (e: Exception) {
//                    YLog.debug("[WooBox-LockScreenClock] 找不到 updateDate 方法")
//                }
            }

        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenClock] Hook 日期刷新时出错：${e.message}")
        }
    }


    private fun handleDateViewHook(hookParam: HookParam) {
        val view = hookParam.instance as? TextView ?: return
        val context = view.context

        val resourceId = getResourceIdName(view)
        val className = view.javaClass.simpleName

        DATE_TARGETS.find { it.resourceId == resourceId && it.className == className }
            ?.let { target ->
                if (isInLockScreen(view, target)) {
                    val currentText = view.text.toString()
                    if (isDateText(currentText)) {
                        val newText = processDateTextWithCurrentTime(currentText, context)
                        if (currentText != newText) {
                            view.text = newText
                            logHookCall(view, target, currentText, newText)
                        }
                    }
                }
            }
    }

    /**
     * 检查是否在锁屏相关界面
     */
    private fun isInLockScreen(view: View, target: TargetInfo): Boolean {
        return when (target.resourceId) {
            "date_view" -> isInKeyguardStatusView(view)    // 锁屏状态视图日期
            "date" -> isInKeyguardClockView(view)          // 锁屏主时钟视图日期
            else -> true  // 时间组件总是尝试处理
        }
    }

    /**
     * 检查是否在KeyguardStatusView中（锁屏状态视图）
     */
    private fun isInKeyguardStatusView(view: View): Boolean {
        try {
            var parent: View? = view
            var depth = 0

            while (parent != null && depth < 20) {
                val className = parent.javaClass.simpleName
                val resourceId = getResourceIdName(parent)

                if (className == "KeyguardStatusView" && resourceId == "keyguard_status_view") {
                    return true
                }

                parent = parent.parent as? View
                depth++
            }
        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenClock] 检查KeyguardStatusView时出错: ${e.message}")
        }
        return false
    }

    /**
     * 检查是否在KeyguardClockView中（锁屏主时钟视图）
     */
    private fun isInKeyguardClockView(view: View): Boolean {
        try {
            var parent: View? = view
            var depth = 0

            while (parent != null && depth < 20) {
                val className = parent.javaClass.simpleName

                if (className == "KeyguardClockView") {
                    return true
                }

                parent = parent.parent as? View
                depth++
            }
        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenClock] 检查KeyguardClockView时出错: ${e.message}")
        }
        return false
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
        val timeWithAmPmPattern =
            Regex("^\\d{1,2}:\\d{2}(:\\d{2})?\\s*[AP]M$", RegexOption.IGNORE_CASE)
        if (timeWithAmPmPattern.matches(text)) {
            return true
        }

        return false
    }

    /**
     * 判断是否为日期格式文本
     */
    private fun isDateText(text: String): Boolean {
        if (text.isEmpty()) return false

        // 检查是否包含月和日
        val datePatterns = listOf(
            "\\d{1,2}月\\d{1,2}日.*",  // 中文日期格式
            "\\d{4}-\\d{1,2}-\\d{1,2}.*",  // 年-月-日
            "\\d{1,2}/\\d{1,2}/\\d{4}.*",  // 月/日/年
            "\\d{1,2} \\w{3} \\d{4}.*"     // 1 Jan 2026
        )

        return datePatterns.any { pattern ->
            Regex(pattern).matches(text)
        } || text.contains("星期") || text.contains("周")
    }

    /**
     * 处理时间文本
     */
    @SuppressLint("SimpleDateFormat")
    private fun processTimeText(originalText: String, context: Context): String {
        if (!showPeriod) return originalText

        try {
            val nowTime = Calendar.getInstance().time
            val isZh = isZh(context)
            YLog.debug("[WooBox-LockScreenClock] 处理时间文本: 原始文本='$originalText', 语言环境: ${if (isZh) "中文" else "非中文"}")

            // 获取当前时段
            val period = getPeriod(isZh, nowTime)
            if (period.isEmpty()) {
                return originalText // 如果时段为空，直接返回原文本
            }

            // 移除所有已有时段信息，获取纯净的时间部分
            val cleanedText = removeAllPeriods(originalText, isZh).trim()

            // 如果清理后是空字符串，说明原始文本只有时段，返回原始文本
            if (cleanedText.isEmpty()) {
                return originalText
            }

            return if (isZh) {
                // 中文格式：时段 + 时间
                "$period$cleanedText"
            } else {
                // 英文格式：时间 + 时段
                "$cleanedText $period"
            }
        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenClock] 处理时间文本时出错: ${e.message}, 原始文本: $originalText")
            return originalText
        }
    }

    /**
     * 处理日期文本（使用当前时间计算时段）
     */
    @SuppressLint("SimpleDateFormat")
    private fun processDateTextWithCurrentTime(originalText: String, context: Context): String {
        if (!showPeriod) return originalText

        try {
            val isZh = isZh(context)
            YLog.debug("[WooBox-LockScreenClock] 处理日期文本(带当前时间): 原始文本='$originalText', 语言环境: ${if (isZh) "中文" else "非中文"}")

            // 修复：添加 else 分支以满足 Kotlin 表达式语法要求
            val result = if (isZh) {
                val nowTime = Calendar.getInstance().time
                val period = getPeriod(isZh, nowTime)
                if (period.isNotEmpty()) {
                    // 移除所有已有时段，然后添加新时段
                    val cleanedText = removeAllPeriods(originalText, isZh)
                    if (cleanedText.isNotEmpty()) {
                        "$cleanedText $period"
                    } else {
                        originalText // 如果清理后为空，返回原始文本
                    }
                } else {
                    originalText // 如果时段为空，返回原始文本
                }
            } else {
                originalText // 非中文环境下直接返回原始文本
            }

            return result
        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenClock] 处理日期文本时出错: ${e.message}, 原始文本: $originalText")
            return originalText
        }
    }

    /**
     * 移除所有时段信息，返回纯净的时间/日期文本
     */
    private fun removeAllPeriods(text: String, isZh: Boolean): String {
        var result = text

        if (isZh) {
            // 移除中文时段
            val chinesePeriods = listOf("凌晨", "早上", "上午", "中午", "下午", "傍晚", "晚上")
            chinesePeriods.forEach { period ->
                // 使用正则表达式移除时段及可能的空格
                val regex = Regex("\\s*$period\\s*")
                result = result.replace(regex, "")
            }
        } else {
            // 移除英文的AM/PM，包括带点和空格的各种变体
            val englishPeriodRegex = Regex(
                "\\s*(AM|PM|A\\.M\\.|P\\.M\\.|am|pm|a\\.m\\.|p\\.m\\.)\\s*",
                RegexOption.IGNORE_CASE
            )
            result = result.replace(englishPeriodRegex, "")
        }

        return result.trim()
    }

    /**
     * 处理日期文本（兼容旧方法）
     */
    @SuppressLint("SimpleDateFormat")
    private fun processDateText(originalText: String, context: Context): String {
        if (!showPeriod) {
            return originalText
        }

        try {
            val isZh = isZh(context)
            val languageStr = if (isZh) "中文" else "非中文"
            YLog.debug("[WooBox-LockScreenClock] 处理日期文本: 原始文本='$originalText', 语言环境: $languageStr")

            if (isZh) {
                val nowTime = Calendar.getInstance().time
                val period = getPeriod(isZh, nowTime)
                if (period.isNotEmpty()) {
                    // 移除所有已有时段，然后添加新时段
                    val cleanedText = removeAllPeriods(originalText, isZh)
                    if (cleanedText.isNotEmpty()) {
                        return "$cleanedText $period"
                    }
                }
            }

            return originalText
        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenClock] 处理日期文本时出错: ${e.message}, 原始文本: $originalText")
            return originalText
        }
    }

    /**
     * 更新所有日期组件的时段显示
     */
    private fun updateAllDateComponentsWithCurrentTime(context: Context) {
        try {
            YLog.debug("[WooBox-LockScreenClock] 开始更新所有日期组件的时段显示")

            // 更新缓存的日期组件
            val dateViewsToUpdate = mutableListOf<TextView>()
            dateViewsToUpdate.addAll(cachedDateViews)

            // 清理无效的视图引用
            cachedDateViews.removeAll { it.context == null }

            var updatedCount = 0
            for (dateView in dateViewsToUpdate) {
                try {
                    if (dateView.context != null && dateView.isAttachedToWindow) {
                        val originalText = dateView.text.toString()
                        if (isDateText(originalText)) {
                            val newText = processDateTextWithCurrentTime(originalText, context)
                            if (originalText != newText) {
                                dateView.text = newText
                                updatedCount++

                                YLog.debug("[WooBox-LockScreenClock] " +
                                        "更新日期组件: ${getResourceIdName(dateView)}, " +
                                        "原始='$originalText', 新='$newText'")
                            }
                        }
                    }
                } catch (e: Exception) {
                    YLog.debug("[WooBox-LockScreenClock] 更新单个日期组件时出错: ${e.message}")
                }
            }

            YLog.debug("[WooBox-LockScreenClock] 更新完成，共更新了 $updatedCount 个日期组件")

        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenClock] 更新所有日期组件时出错: ${e.message}")
        }
    }

    /**
     * 获取时段信息
     */
    @SuppressLint("SimpleDateFormat")
    private fun getPeriod(isZh: Boolean, nowTime: Date): String {
        var period = ""

        if (isZh) {
            when (SimpleDateFormat("HH").format(nowTime).toInt()) {
                in 0..5 -> period = "凌晨"
                in 6..8 -> period = "早上"
                in 9..11 -> period = "上午"
                12 -> period = "中午"
                in 13..17 -> period = "下午"
                18 -> period = "傍晚"
                in 19..23 -> period = "晚上"
            }
        } else {
            period = SimpleDateFormat("a").format(nowTime)
        }

        return period
    }

    /**
     * 检查文本是否已经包含时段信息
     */
    private fun containsPeriod(text: String, isZh: Boolean): Boolean {
        if (isZh) {
            val chinesePeriods = listOf("凌晨", "上午", "中午", "下午", "傍晚", "晚上")
            return chinesePeriods.any { period ->
                text.contains(period, ignoreCase = true)
            }
        } else {
            val englishPeriodPatterns = listOf(
                Regex("\\b[AP]\\.?M\\.?\\b", RegexOption.IGNORE_CASE),
                Regex("\\bam\\b|\\bpm\\b", RegexOption.IGNORE_CASE)
            )
            return englishPeriodPatterns.any { pattern ->
                pattern.containsMatchIn(text)
            }
        }
    }

    /**
     * 移除文本中的中文时段
     */
    private fun removeChinesePeriod(text: String): String {
        val chinesePeriods = listOf("凌晨", "上午", "中午", "下午", "傍晚", "晚上")
        var result = text
        chinesePeriods.forEach { period ->
            result = result.replace(period, "")
        }
        return result
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
     * 记录Hook调用
     */
    private fun logHookCall(
        view: TextView,
        target: TargetInfo,
        originalText: String,
        newText: String
    ) {
        val logEntry = buildString {
            append("[${SimpleDateFormat("HH:mm:ss.SSS").format(Date())}] ")
            append("锁屏${target.type}setText调用 -> ")
            append("资源ID='${target.resourceId}', ")
            append("类名='${target.className}', ")
            append("可见性='${getVisibility(view)}', ")
            append("当前文本='$originalText', ")
            append("新文本='$newText', ")
            append("位置=${getViewPosition(view)}")
        }

        YLog.debug("[WooBox-LockScreenClock] $logEntry")
        YLog.debug("[WooBox-LockScreenClock] 视图层级:\n${getViewPath(view)}")

        addToHookHistory(logEntry)
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
     * 获取视图的可见性
     */
    private fun getVisibility(view: android.view.View): String {
        return when (view.visibility) {
            android.view.View.VISIBLE -> "VISIBLE"
            android.view.View.INVISIBLE -> "INVISIBLE"
            android.view.View.GONE -> "GONE"
            else -> "UNKNOWN"
        }
    }

    /**
     * 获取视图位置
     */
    private fun getViewPosition(view: android.view.View): String {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val width = view.width
        val height = view.height

        return "($x,$y,${x + width},${y + height})"
    }

    /**
     * 获取从当前视图到根视图的完整路径
     */
    private fun getViewPath(view: View, maxDepth: Int = 15): String {
        val result = StringBuilder()
        var current: View? = view
        var depth = 0

        result.append("当前视图路径 (从下到上):\n")

        while (current != null && depth < maxDepth) {
            val indent = "  ".repeat(depth)
            val className = current.javaClass.simpleName
            val resourceId = getResourceIdName(current)

            result.append("$indent└─ $className (ID: $resourceId)\n")

            val parent = current.parent
            current = if (parent is View) parent else null
            depth++
        }

        if (depth >= maxDepth) {
            result.append("  ... (已达最大深度 $maxDepth)\n")
        }

        return result.toString()
    }

    /**
     * 添加Hook历史记录
     */
    private fun addToHookHistory(logEntry: String) {
        // 只保留最近的50条记录
        if (hookHistory.size >= 50) {
            hookHistory.removeAt(0)
        }
        hookHistory.add(logEntry)
    }
}