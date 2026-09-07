package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia


import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewParent
import android.widget.TextView
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.constructor
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class Clock: YukiBaseHooker() {

    private val TAG = "NubiaTestClock"

    companion object {
        private var highFrequencyTimer: Timer? = null
        private var mainHandler: Handler? = null
        /**
         * 用于持有 TextView 的弱引用，避免内存泄漏。
         *
         * 该变量通过 WeakReference 包装 TextView 对象，确保在不需要时可以被垃圾回收器回收，
         * 从而防止因强引用导致的内存泄漏问题。
         */
        private var clockViewRef: WeakReference<TextView>? = null
    }

    private val statusBarPrefs by lazy { prefs("systemui\\status_bar\\status_bar_clock") }
//    private val clockEnabled by lazy { statusBarPrefs.getBoolean("status_bar_clock", false) }

    private val clockEnabled by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.TIME_INDICATOR, false)
    }
    //    private val clockStyleSelectedOption by lazy { statusBarPrefs.getInt("ClockStyleSelectedOption", 0) }
    private val clockStyleSelectedOption by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_GEEK, false)
    }
//    private val showYears by lazy { statusBarPrefs.getBoolean("ShowYears", false) }

    private val showYears by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.SHOW_YEARS, false)
    }
    //    private val showMonth by lazy { statusBarPrefs.getBoolean("ShowMonth", false) }
    private val showMonth by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.SHOW_MONTH, false)
    }
    //    private val showDay by lazy { statusBarPrefs.getBoolean("ShowDay", false) }
    private val showDay by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.SHOW_DAY, false)
    }
//    private val showWeek by lazy { statusBarPrefs.getBoolean("ShowWeek", false) }

    private val showWeek by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.SHOW_WEEK, false)
    }
//    private val showCNHour by lazy { statusBarPrefs.getBoolean("ShowCNHour", false) }

    private val showCNHour by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.SHOW_CN_HOUR, false)
    }
    //    private val showtimePeriod by lazy { statusBarPrefs.getBoolean("Showtime_period", false) }
    private val showtimePeriod by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.SHOW_PERIOD, false)
    }
//    private val showSeconds by lazy { statusBarPrefs.getBoolean("ShowSeconds", false) }

    private val showSeconds by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.SHOW_SECONDS, false)
    }
//    private val showMillisecond by lazy { statusBarPrefs.getBoolean("ShowMillisecond", false) }

    private val showMillisecond by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.SHOW_MILLISECOND, false)
    }
//    private val hideSpace by lazy { statusBarPrefs.getBoolean("HideSpace", false) }

    private val hideSpace by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.HIDE_SPACE, false)
    }
    //    private val dualRow by lazy { statusBarPrefs.getBoolean("DualRow", false) }
    private val dualRow by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.DUAL_ROW, false)
    }
//    private val fontSize by lazy { statusBarPrefs.getFloat("ClockSize", 0f) }

    private val fontSize by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.CLOCK_SIZE, 0).toFloat()
    }

    private val updateSpeed by lazy { statusBarPrefs.getFloat("ClockUpdateSpeed", 0f) } //一般未实现.默认为0则不跟新，前端无开关
//    private val customClockStyle by lazy { statusBarPrefs.getString("CustomClockStyle", "HH:mm") }

    private val customClockStyle by lazy {
        Prefs.getString(Pref.Key.SystemUI.StatusBar.CLOCK_GEEK_FORMAT, "HH:mm").toString()
    }
//    private val customAlignment by lazy { statusBarPrefs.getInt("alignment", 0) }

    private val customAlignment by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.ALIGNMENT,  0)
    }
//    private val clockLeftPadding by lazy { statusBarPrefs.getFloat("LeftPadding", 0f) }

    private val clockLeftPadding by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.NUBIA_CLOCK_PADDING_LEFT, 0).toFloat()
    }
    //    private val clockRightPadding by lazy { statusBarPrefs.getFloat("RightPadding", 0f) }
    private val clockRightPadding by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.NUBIA_CLOCK_PADDING_RIGHT, 0).toFloat()
    }
    //    private val clockTopPadding by lazy { statusBarPrefs.getFloat("TopPadding", 0f) }
    private val clockTopPadding by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.NUBIA_CLOCK_PADDING_TOP, 0).toFloat()
    }
    //    private val clockBottomPadding by lazy { statusBarPrefs.getFloat("BottomPadding", 0f) }
    private val clockBottomPadding by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.NUBIA_CLOCK_PADDING_DOWN, 0).toFloat()
    }

    private lateinit var hookContext: Context
    private val sdfCache = mutableMapOf<String, SimpleDateFormat>()
    private fun getFormatter(pattern: String): SimpleDateFormat =
        sdfCache.getOrPut(pattern) { SimpleDateFormat(pattern) }

//    @SuppressLint("SetTextI18n")
    override fun onHook() {
        if (!clockEnabled) return

        loadApp("com.android.systemui") {
            val clockClass = "com.android.systemui.statusbar.policy.Clock".toClass()

            hookConstructor(clockClass)
            hookGetSmallTime(clockClass)
        }
    }

    /** Hook 构造函数 */
    private fun hookConstructor(clockClass: Class<*>) {
        clockClass.constructor {
            modifiers { isPublic }
            param("android.content.Context", "android.util.AttributeSet", IntType)
        }.hook {
            after {
                hookContext = args(0).cast<Context>()!!
                val clockView = instance<TextView>()
                // 关键修改：同时判断id和父视图链（仅处理PhoneStatusBarView下的Clock）
                val isTargetClock = clockView.resources.getResourceEntryName(clockView.id) == "clock"
//                        && isPhoneStatusBarClock(clockView)
                if (!isTargetClock) return@after

                setupClockView(clockView)
                clockViewRef = WeakReference(clockView)

                if (updateSpeed > 0f) {
                    setupHighFrequencyUpdate()
                }
            }
        }
    }


    /**
     * 递归打印 View 的父节点链
     */
    private fun printViewHierarchy(view: View) {
        val sb = StringBuilder()
        var current: ViewParent? = view.parent
        while (current != null) {
            sb.insert(0, "${current::class.java.simpleName} -> ")
            current = current.parent
        }
//        YLog.debug("$TAG View hierarchy: $sb")
    }
    /**
     * 检查View的父节点链中是否包含PhoneStatusBarView（仅处理该视图下的Clock）
     */
    private fun isPhoneStatusBarClock(view: View): Boolean {
        var currentParent: ViewParent? = view.parent
        while (currentParent != null) {
            // 匹配PhoneStatusBarView类名（日志中明确的目标父视图）
//            YLog.debug("Parent class: ${currentParent::class.java.simpleName}")
            if (currentParent::class.java.simpleName == "PhoneStatusBarView") {
                return true
            }
            currentParent = currentParent.parent
        }
        return false
    }


    /** Hook getSmallTime 方法，进行自定义格式化 */
    private fun hookGetSmallTime(clockClass: Class<*>) {
        clockClass.method {
            modifiers { isPrivate; isFinal }
            name = "getSmallTime"
            emptyParam()
            returnType = "java.lang.CharSequence"
        }.hook {
            before {
                val clockView = instance<TextView>()
                val isTargetClock = clockView.resources.getResourceEntryName(clockView.id) == "clock" &&
                        isPhoneStatusBarClock(clockView)
                if(isTargetClock && showSeconds){

                    // 强制将时间显示秒 hook mShowSeconds 将字段强制改为true
                    val mShowSeconds = instance.javaClass.getDeclaredField("mShowSeconds").apply {
                        isAccessible = true
                        set(instance, true)
                    }
//                    YLog.debug("$TAG set mShowSeconds to true: $mShowSeconds")

                }
            }
            after {
                val clockView = instance<TextView>()
                // 关键修改：同时判断id和父视图链（仅处理PhoneStatusBarView下的Clock）
                val isTargetClock = clockView.resources.getResourceEntryName(clockView.id) == "clock"
                        && isPhoneStatusBarClock(clockView)
                if (!isTargetClock) return@after





                val now = Calendar.getInstance().time
                result = if (!clockStyleSelectedOption) {
                    val dateStr = getDate(now)
                    val timeStr = getTime(now)
                    val newline = if (dualRow) "\n" else ""
                    dateStr + newline + timeStr
                } else {
                    getCustomDate(now, customClockStyle)
                }

                printViewHierarchy(clockView)
                applyClockViewStyle(clockView)
            }
        }
    }



    private fun applyClockViewStyle(clockView: TextView) {
        clockView.apply {
            if (dualRow) {
//                YLog.debug("$TAG applyClockViewStyle")
                val defaultSize = if (fontSize != 0f) fontSize else 8F
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, defaultSize)
                setLineSpacing(0F, 0.8F)
            } else {
                if (fontSize != 0f) {
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize)
                }
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun setupClockView(clockView: TextView) {
        clockView.apply {
            // 关键修改：同时判断id和父视图链（仅处理PhoneStatusBarView下的Clock）
            val isTargetClock = resources.getResourceEntryName(id) == "clock"
//                    && isPhoneStatusBarClock(this)
            if (!isTargetClock) return@apply

            if (mainHandler == null) {
                mainHandler = Handler(context.mainLooper)
            }

            isSingleLine = false
            gravity = when (customAlignment) {
                0 -> Gravity.CENTER
                1 -> Gravity.TOP
                2 -> Gravity.BOTTOM
                3 -> Gravity.START
                4 -> Gravity.END
                5 -> Gravity.CENTER_HORIZONTAL
                6 -> Gravity.CENTER_VERTICAL
                7 -> Gravity.FILL
                8 -> Gravity.FILL_HORIZONTAL
                9 -> Gravity.FILL_VERTICAL
                else -> Gravity.CENTER
            }

            @SuppressLint("SuspiciousIndentation")
            fun dp(value: Float): Int =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

                setPadding(
                    if (clockLeftPadding != 0f) dp(clockLeftPadding) else paddingLeft,
                    if (clockTopPadding != 0f) dp(clockTopPadding) else paddingTop,
                    if (clockRightPadding != 0f) dp(clockRightPadding) else paddingRight,
                    if (clockBottomPadding != 0f) dp(clockBottomPadding) else paddingBottom
                )


                if (dualRow) {
                    val defaultSize = if (fontSize != 0f) fontSize else 8F
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, defaultSize)
                    setLineSpacing(0F, 0.8F)
                } else {
                    if (fontSize != 0f) setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize)
                }



        }
    }
    /** 设置高频更新 */
    private fun setupHighFrequencyUpdate() {
        highFrequencyTimer?.cancel()
        highFrequencyTimer = null

        highFrequencyTimer = Timer("ClockUpdateTimer", true).apply {
            schedule(object : TimerTask() {
                override fun run() {
                    mainHandler?.post {
                        val clockView = clockViewRef?.get() ?: return@post
                        // 额外检查：确保是目标视图才更新
//                        if (!isPhoneStatusBarClock(clockView)) return@post

                        val now = Calendar.getInstance().time
//                        val newText = if (clockStyleSelectedOption == 0) {
                        val newText = if (!clockStyleSelectedOption) {
                            val dateStr = getDate(now)
                            val timeStr = getTime(now)
                            val newline = if (dualRow) "\n" else ""
                            dateStr + newline + timeStr
                        } else {
                            getCustomDate(now, customClockStyle)
                        }
                        clockView.text = newText
                    }
                }
            }, 1000 - (System.currentTimeMillis() % 1000), updateSpeed.toLong())
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getCustomDate(now: Date, format: String): String = getFormatter(format).format(now)

    @SuppressLint("SimpleDateFormat")
    private fun getDate(now: Date): String {
        var dateFormat = ""
        if (isZh(hookContext)) {
            if (showYears) dateFormat += "yy年"
            if (showMonth) dateFormat += "M月"
            if (showDay) dateFormat += "d日"
            if (showWeek) dateFormat += "E"
            if (!hideSpace && !dualRow) dateFormat += " "
        } else {
            if (showYears) {
                dateFormat += "yy"
                if (showMonth || showDay) dateFormat += "/"
            }
            if (showMonth) {
                dateFormat += "M"
                if (showDay) dateFormat += "/"
            }
            if (showDay) dateFormat += "d"
            if (showWeek) dateFormat += " E"
            if (!hideSpace && !dualRow) dateFormat += " "
        }
        return if (dateFormat.trim().isNotEmpty()) getFormatter(dateFormat).format(now) else ""
    }

    @SuppressLint("SimpleDateFormat")
    private fun getTime(now: Date): String {
        var timeFormatPattern = if (is24(hookContext)) "HH:mm" else "hh:mm"
        if (showSeconds) timeFormatPattern += ":ss"
        if (showMillisecond) timeFormatPattern += ".SSS"

        var timeFormat = getFormatter(timeFormatPattern).format(now)
        timeFormat = if (isZh(hookContext)) getPeriod(now) + timeFormat else timeFormat + getPeriod(now)
        timeFormat = getDoubleHour(now) + timeFormat
        return timeFormat
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDoubleHour(now: Date): String {
        var doubleHour = ""
        if (showCNHour) {
            when (getFormatter("HH").format(now)) {
                "23", "00" -> doubleHour = "子时"
                "01", "02" -> doubleHour = "丑时"
                "03", "04" -> doubleHour = "寅时"
                "05", "06" -> doubleHour = "卯时"
                "07", "08" -> doubleHour = "辰时"
                "09", "10" -> doubleHour = "巳时"
                "11", "12" -> doubleHour = "午时"
                "13", "14" -> doubleHour = "未时"
                "15", "16" -> doubleHour = "申时"
                "17", "18" -> doubleHour = "酉时"
                "19", "20" -> doubleHour = "戌时"
                "21", "22" -> doubleHour = "亥时"
            }
            if (!hideSpace) doubleHour += " "
        }
        return doubleHour
    }

//    @SuppressLint("SimpleDateFormat")
//    private fun getPeriod(now: Date): String {
//        var period = ""
//        if (showtimePeriod) {
//            if (isZh(hookContext)) {
//                when (getFormatter("HH").format(now)) {
//                    in "00".."05" -> period = "凌晨"
//                    in "06".."11" -> period = "上午"
//                    "12" -> period = "中午"
//                    in "13".."17" -> period = "下午"
//                    "18" -> period = "傍晚"
//                    in "19".."23" -> period = "晚上"
//                }
//                if (!hideSpace) period += " "
//            } else {
//                period = " " + getFormatter("a").format(now)
//            }
//        }
//        return period
//    }

    @SuppressLint("SimpleDateFormat")
    private fun getPeriod(now: Date): String {
        var period = ""
        if (showtimePeriod) {
            if (isZh(hookContext)) {
                when (getFormatter("HH").format(now)) {
                    in "00".."05" -> period = "凌晨"
                    in "06".."08" -> period = "早上"
                    in "09".."11" -> period = "上午"
                    "12" -> period = "中午"
                    in "13".."17" -> period = "下午"
                    "18" -> period = "傍晚"
                    in "19".."23" -> period = "晚上"
                }
                if (!hideSpace) period += " "
            } else {
                period = " " + getFormatter("a").format(now)
            }
        }
        return period
    }

    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locales[0]
        return locale.language.endsWith("zh")
    }

    private fun is24(context: Context): Boolean =
        Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24) == "24"
}
