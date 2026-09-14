package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.LinearLayout
import android.widget.TextView
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import java.io.FileInputStream
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * 独立的两套显示：
 * 1. 设备温度：电池/CPU/GPU 温度（使用设备温度设置）
 * 2. 电池信息：电池温度/电流/功率（使用电池信息设置）
 * 两者互不干扰，可同时显示，位置独立。
 */
@SuppressLint("DiscouragedApi")
object StatusBarTemperatureHook : YukiBaseHooker() {

    // ========================= 设备温度设置 =========================
    private val status_bar_display_temperature_switch by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMPERATURE_SWITCH, false)
    }
    private val status_bar_display_temp_battery by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_BATTERY, false)
    }
    private val status_bar_display_temp_cpu by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_CPU, false)
    }
    private val status_bar_display_temp_gpu by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_GPU, false)
    }
    private val status_bar_display_temp_font_size by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_FONT_SIZE, 8)
    }
    private val status_bar_display_temp_hide_unit by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_HIDE_UNIT, false)
    }
    private val status_bar_display_temp_display_mode by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_DISPLAY_MODE, 0)
    }
    private val status_bar_temperature_location by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_TEMPERATURE_LOCATION, 0)
    }

    // ========================= 电池信息设置 =========================
    private val status_bar_display_battery_info by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO, false)
    }
    private val status_bar_battery_info_location by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_BATTERY_INFO_LOCATION, 0)
    }
    private val is_battery_info_temperature by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION1, false)
    }
    private val is_battery_info_electricity by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION2, false)
    }
    private val is_battery_info_power by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION3, false)
    }
    private val status_bar_display_battery_info_font_size by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_FONT_SIZE, 8)
    }

    // 电池信息-值为false的时候，则为单排模式，值为true的时候，则为双排模式
    private val status_bar_display_battery_info_layout by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_LAYOUT, false)
    }

    // 电池信息-双排模式下，显示的位置0表示：电池温度（上排）+功耗（下排） 1表示：电流（上排）+功率（下排）
    private val status_bar_display_battery_info_dual_display_mode by lazy {
        Prefs.getInt(
            Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_DUAL_DISPLAY_MODE,
            0
        )
    }

    // 电池信息-双排模式下，显示的位置: 左侧 ，右侧 ，居中
    private val status_bar_battery_info_location_dual by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_BATTERY_INFO_LOCATION_DUAL, 0)
    }

    // 电池信息-双排模式下，字体大小
    private val status_bar_display_battery_info_font_size_dual by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_FONT_SIZE_DUAL, 8)
    }

    // 电池信息-双排模式下，隐藏标题前缀
    private val status_bar_display_battery_info_hide_title_dual by lazy {
        Prefs.getBoolean(
            Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_HIDE_TITLE_DUAL,
            false
        )
    }

    // 电池信息-单排模式下，隐藏标题前缀
    private val status_bar_display_battery_info_hide_title_single by lazy {
        Prefs.getBoolean(
            Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_HIDE_TITLE_SINGLE,
            false
        )
    }

    // 电池信息-单排模式下，是否为充电模式 0 表示任何场景 1表示充电
    private val status_bar_display_battery_info_is_charge_mode_single by lazy {
        Prefs.getInt(
            Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_IS_CHARGE_MODE_SINGLE,
            0
        )
    }

    // 电池信息-双排模式下，是否为充电模式  0 表示任何场景 1表示充电
    private val status_bar_display_battery_info_is_charge_mode_dual by lazy {
        Prefs.getInt(
            Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_IS_CHARGE_MODE_DUAL,
            0
        )
    }


    // 电池信息-单排模式下，温度显示模式 0 表示整数， 1表示显示小数，保留1为小数
    private val status_bar_display_battery_info_temp_mode_single by lazy {
        Prefs.getInt(
            Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_TEMP_MODE_SINGLE,
            0
        )
    }

    // 电池信息-双排模式下，温度显示模式 0 表示整数， 1表示显示小数，保留1为小数
    private val status_bar_display_battery_info_temp_mode_dual by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_TEMP_MODE_DUAL, 0)
    }


    // 电池信息-单排模式下，设置固定宽度以防相邻元素左右抖动
    private val status_bar_battery_info_fixed_width_single by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_BATTERY_INFO_FIXED_WIDTH_SINGLE, -1)
    }

    // 电池信息-双排模式下，设置固定宽度以防相邻元素左右抖动
    private val status_bar_battery_info_fixed_width_dual by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_BATTERY_INFO_FIXED_WIDTH_DUAL, -1)
    }


    // ========================= 常量定义 =========================
    private const val TAG = "StatusBarTemperatureHook"

    // 0x7F100005 0x7F100004
    private val TAG_TEMP_CONTAINER = 0x7F100005           // 设备温度容器
    private val TAG_BATTERY_INFO_CONTAINER = 0x7F100006   // 电池信息容器（默认单排）
    private val TAG_BATTERY_INFO_CONTAINER_DUAL = 0x7F100007   // 电池信息容器（双排垂直）
    private const val UPDATE_INTERVAL = 1000L
    private const val TEMP_UNIT = "℃"

    // 设备温度标题
    private const val BATTERY_TITLE = "B:"
    private const val CPU_TITLE = "C:"
    private const val GPU_TITLE = "G:"

    // 电池信息标题
    private const val BATTERY_INFO_TEMP_TITLE = "B:"        // 电池温度可无前缀，或自行定义
    private const val CURRENT_TITLE = "I:"
    private const val POWER_TITLE = "P:"

    // 温度文件路径（CPU/GPU）
    private const val BATTERY_TEMP_PATH = "/sys/class/power_supply/battery/temp"
    private const val CPU_TEMP_PATH = "/sys/devices/virtual/thermal/thermal_zone0/temp"
    private const val GPU_TEMP_PATH =
        "/sys/devices/platform/soc/3d00000.qcom,kgsl-3d0/kgsl/kgsl-3d0/temp"

    // uevent 文件路径（包含电池所有信息）
    private const val BATTERY_UEVENT_PATH = "/sys/class/power_supply/battery/uevent"

    // 设备温度各子项 Tag
    private val TAG_TEMP_BATTERY = 0x7F100011
    private val TAG_TEMP_CPU = 0x7F100012
    private val TAG_TEMP_GPU = 0x7F100013

    // 电池信息各子项 Tag
    private val TAG_BATTERY_INFO_TEMP = 0x7F100031
    private val TAG_BATTERY_INFO_CURRENT = 0x7F100032
    private val TAG_BATTERY_INFO_POWER = 0x7F100033

    private data class TempItem(
        val title: String,
        val path: String,
        val divisor: Int,
        val tag: Int
    )

    // 设备温度项列表
    private val tempItems = listOf(
        TempItem(BATTERY_TITLE, BATTERY_TEMP_PATH, 10, TAG_TEMP_BATTERY),
        TempItem(CPU_TITLE, CPU_TEMP_PATH, 1000, TAG_TEMP_CPU),
        TempItem(GPU_TITLE, GPU_TEMP_PATH, 1000, TAG_TEMP_GPU)
    )

    // 网格重排开启时，温度/电池信息由 StatusBarGridHook 自绘，整体跳过避免重复显示
    private val gridLayoutEnabled by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBarGrid.SWITCH, false)
    }

    override fun onHook() {
        if (gridLayoutEnabled) {
            YLog.info(tag = TAG, msg = "状态栏网格重排已启用，跳过温度/电池信息挂载")
            return
        }
        "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass().method {
            name = "onFinishInflate"
        }.hook {
            after {
                val statusBarView = instance as ViewGroup


                // 设备温度模块（如果启用）
                if (status_bar_display_temperature_switch) {
                    when (status_bar_temperature_location) {
                        0 -> addTemperatureContainer(statusBarView)      // 时钟旁边
                        1 -> addTemperatureInsideBattery(statusBarView)  // 电池内部
                        2 -> addTemperatureCenter(statusBarView)         // 居中显示
                        else -> addTemperatureContainer(statusBarView)
                    }
                }

                // ========== 电池信息模块（修改点） ==========
                // 打印View信息以及父类的信息
                if (status_bar_display_battery_info) {
                    // 根据双排模式开关选择位置变量
                    val location = if (status_bar_display_battery_info_layout) {
                        status_bar_battery_info_location_dual   // 双排模式使用独立位置
                    } else {
                        status_bar_battery_info_location        // 单排模式沿用旧位置
                    }
                    when (location) {
                        0 -> addBatteryInfoContainer(statusBarView)      // 时钟旁边（左侧）
                        1 -> addBatteryInfoInsideBattery(statusBarView)  // 电池内部（右侧）
                        2 -> addBatteryInfoCenter(statusBarView)         // 居中显示
                        else -> addBatteryInfoContainer(statusBarView)
                    }
                }
            }
        }
    }



    private fun buildBatteryInfoText(
        tag: Int,
        valueStr: String,
        isSingleMode: Boolean,
        unit: String
    ): String {
        val title = when (tag) {
            TAG_BATTERY_INFO_TEMP -> BATTERY_INFO_TEMP_TITLE
            TAG_BATTERY_INFO_CURRENT -> CURRENT_TITLE
            TAG_BATTERY_INFO_POWER -> POWER_TITLE
            else -> ""
        }
        val hideTitle = if (isSingleMode) {
            status_bar_display_battery_info_hide_title_single
        } else {
            status_bar_display_battery_info_hide_title_dual
        }
        return if (hideTitle) {
            "$valueStr$unit"
        } else {
            "$title$valueStr$unit"
        }
    }

    // ========================= 设备温度容器添加（保持不变）=========================

    private fun addTemperatureContainer(statusBarView: ViewGroup) {
        if (!status_bar_display_temp_battery && !status_bar_display_temp_cpu && !status_bar_display_temp_gpu) {
            YLog.debug(
                tag = TAG,
                msg = "No temperature items enabled, skipping device temp container"
            )
            return
        }
        if (statusBarView.findViewWithTag<View>(TAG_TEMP_CONTAINER) != null) {
            YLog.debug(tag = TAG, msg = "Temperature container already exists")
            return
        }

        val context = statusBarView.context
        val leftContainerId = context.resources.getIdentifier(
            "status_bar_start_side_except_heads_up",
            "id",
            context.packageName
        ) ?: return
        val leftContainer = statusBarView.findViewById<ViewGroup>(leftContainerId) ?: return

        val clockId = context.resources.getIdentifier("clock", "id", context.packageName) ?: return
        val clockView = statusBarView.findViewById<TextView>(clockId) ?: return

        val container = LinearLayout(context).apply {
            tag = TAG_TEMP_CONTAINER
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val clockLp = clockView.layoutParams as? ViewGroup.MarginLayoutParams
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = clockLp?.marginStart ?: 0
                marginEnd = clockLp?.marginEnd ?: 0
            }
            layoutParams = lp
        }

        tempItems.forEach { item ->
            val enabled = when (item.tag) {
                TAG_TEMP_BATTERY -> status_bar_display_temp_battery
                TAG_TEMP_CPU -> status_bar_display_temp_cpu
                TAG_TEMP_GPU -> status_bar_display_temp_gpu
                else -> false
            }
            if (enabled) {
                val textView = createTempTextView(context, clockView, item.title, item.tag)
                container.addView(textView)
                registerDarkReceiver(statusBarView, textView)
                startTempUpdater(textView, item.path, item.divisor)
            }
        }

        if (container.childCount == 0) return

        // 新增：启动锁屏可见性控制
        startTemperatureVisibilityController(container)

        val clockIndex = leftContainer.indexOfChild(clockView)
        if (clockIndex >= 0) {
            leftContainer.addView(container, clockIndex + 1)
        } else {
            leftContainer.addView(container)
        }
    }

    private fun addTemperatureCenter(statusBarView: ViewGroup) {
        if (!status_bar_display_temp_battery && !status_bar_display_temp_cpu && !status_bar_display_temp_gpu) return
        val context = statusBarView.context
        val contentsId =
            context.resources.getIdentifier("status_bar_contents", "id", context.packageName)
                ?: return
        val contents = statusBarView.findViewById<ViewGroup>(contentsId) ?: return
        val clockId = context.resources.getIdentifier("clock", "id", context.packageName) ?: return
        val clockView = statusBarView.findViewById<TextView>(clockId) ?: return

        val centerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            layoutParams = lp
        }

        tempItems.forEach { item ->
            val enabled = when (item.tag) {
                TAG_TEMP_BATTERY -> status_bar_display_temp_battery
                TAG_TEMP_CPU -> status_bar_display_temp_cpu
                TAG_TEMP_GPU -> status_bar_display_temp_gpu
                else -> false
            }
            if (enabled) {
                val textView = createTempTextView(context, clockView, item.title, item.tag)
                centerContainer.addView(textView)
                registerDarkReceiver(statusBarView, textView)
                startTempUpdater(textView, item.path, item.divisor)
            }
        }

        if (centerContainer.childCount == 0) return

        // 新增：启动锁屏可见性控制
        startTemperatureVisibilityController(centerContainer)

        // 尝试放在 cutout 之后或 end 容器之前
        val cutoutId =
            context.resources.getIdentifier("cutout_space_view", "id", context.packageName)
        if (cutoutId != 0) {
            val cutoutView = statusBarView.findViewById<View>(cutoutId)
            if (cutoutView != null) {
                val cutoutIndex = contents.indexOfChild(cutoutView)
                if (cutoutIndex >= 0) {
                    contents.addView(centerContainer, cutoutIndex + 1)
                    return
                }
            }
        }
        val endId = context.resources.getIdentifier(
            "status_bar_end_side_container",
            "id",
            context.packageName
        )
        if (endId != 0) {
            val endView = statusBarView.findViewById<View>(endId)
            if (endView != null) {
                val endIndex = contents.indexOfChild(endView)
                if (endIndex >= 0) {
                    contents.addView(centerContainer, endIndex)
                    return
                }
            }
        }
        contents.addView(centerContainer)
    }

    private fun addTemperatureInsideBattery(statusBarView: ViewGroup) {
        if (!status_bar_display_temp_battery && !status_bar_display_temp_cpu && !status_bar_display_temp_gpu) return
        val context = statusBarView.context
        val systemIconsId =
            context.resources.getIdentifier("system_icons", "id", context.packageName) ?: return
        val systemIcons = statusBarView.findViewById<ViewGroup>(systemIconsId) ?: return
        val batteryId =
            context.resources.getIdentifier("battery", "id", context.packageName) ?: return
        val batteryView = statusBarView.findViewById<View>(batteryId) ?: return

        if (systemIcons.findViewWithTag<View>(TAG_TEMP_CONTAINER) != null) return

        val clockId = context.resources.getIdentifier("clock", "id", context.packageName) ?: return
        val clockView = statusBarView.findViewById<TextView>(clockId) ?: return

        val container = LinearLayout(context).apply {
            tag = TAG_TEMP_CONTAINER
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dpToPx(context, 2)
                marginEnd = dpToPx(context, 2)
            }
            layoutParams = lp
        }

        tempItems.forEach { item ->
            val enabled = when (item.tag) {
                TAG_TEMP_BATTERY -> status_bar_display_temp_battery
                TAG_TEMP_CPU -> status_bar_display_temp_cpu
                TAG_TEMP_GPU -> status_bar_display_temp_gpu
                else -> false
            }
            if (enabled) {
                val textView = createTempTextView(context, clockView, item.title, item.tag)
                container.addView(textView)
                registerDarkReceiver(statusBarView, textView)
                startTempUpdater(textView, item.path, item.divisor)
            }
        }

        if (container.childCount == 0) return

        // 新增：启动锁屏可见性控制
        startTemperatureVisibilityController(container)
        val batteryIndex = systemIcons.indexOfChild(batteryView)
        if (batteryIndex >= 0) {
            systemIcons.addView(container, batteryIndex - 1)
        } else {
            systemIcons.addView(container)
        }
    }

    // ========================= 电池信息容器添加（修改点：统一更新器）=========================

    private fun addBatteryInfoContainer(statusBarView: ViewGroup) {
        // 双排模式下不需要检查单项开关
        if (!status_bar_display_battery_info_layout) {
            // 单排模式：如果没有任何项启用，直接返回
            if (!is_battery_info_temperature && !is_battery_info_electricity && !is_battery_info_power) {
                YLog.debug(tag = TAG, msg = "No battery info items enabled, skipping container")
                return
            }
        }
        if (statusBarView.findViewWithTag<View>(TAG_BATTERY_INFO_CONTAINER) != null) return

        val context = statusBarView.context
        val leftContainerId = context.resources.getIdentifier(
            "status_bar_start_side_except_heads_up",
            "id",
            context.packageName
        ) ?: return
        val leftContainer = statusBarView.findViewById<ViewGroup>(leftContainerId) ?: return

        val clockId = context.resources.getIdentifier("clock", "id", context.packageName) ?: return
        val clockView = statusBarView.findViewById<TextView>(clockId) ?: return

        val container = LinearLayout(context).apply {
            tag = TAG_BATTERY_INFO_CONTAINER
            gravity = Gravity.CENTER_VERTICAL
            val clockLp = clockView.layoutParams as? ViewGroup.MarginLayoutParams

            // 获取当前模式对应的固定宽度（单位 dp）
            val fixedWidthDp = if (status_bar_display_battery_info_layout) {
                status_bar_battery_info_fixed_width_dual
            } else {
                status_bar_battery_info_fixed_width_single
            }
            val width = if (fixedWidthDp > 0) {
                // 将 dp 转换为像素
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    fixedWidthDp.toFloat(),
                    context.resources.displayMetrics
                ).toInt()
            } else {
                LinearLayout.LayoutParams.WRAP_CONTENT
            }
            val lp = LinearLayout.LayoutParams(
                width,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = clockLp?.marginStart ?: 0
                marginEnd = clockLp?.marginEnd ?: 0
            }
            layoutParams = lp
        }

        if (status_bar_display_battery_info_layout) {
            // 双排模式：垂直布局
            container.orientation = LinearLayout.VERTICAL
            container.gravity = Gravity.CENTER_VERTICAL

            val dualFontSize = status_bar_display_battery_info_font_size_dual.toFloat()
            // 隐藏标题前缀
            when (status_bar_display_battery_info_dual_display_mode) {
                0 -> { // 电池温度（上排）+功耗（下排）
                    // 上排：电池温度
                    val topView = TextView(context).apply {
                        tag = TAG_BATTERY_INFO_TEMP
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, dualFontSize)
                        setTextColor(clockView.currentTextColor)
                        gravity = Gravity.CENTER_HORIZONTAL
                        typeface = clockView.typeface
                        letterSpacing = clockView.letterSpacing
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                        }
                        text = if (status_bar_display_battery_info_hide_title_dual) {
                            "--${TEMP_UNIT}"
                        } else {
                            "${BATTERY_INFO_TEMP_TITLE}--${TEMP_UNIT}"
                        }
                    }
                    container.addView(topView)
                    registerDarkReceiver(statusBarView, topView)
                    // 移除独立更新器：startBatteryInfoTempUpdater(topView)

                    // 下排：功率
                    val bottomView = TextView(context).apply {
                        tag = TAG_BATTERY_INFO_POWER
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, dualFontSize)
                        setTextColor(clockView.currentTextColor)
                        gravity = Gravity.CENTER_HORIZONTAL
                        typeface = clockView.typeface
                        letterSpacing = clockView.letterSpacing
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                        }
                        text = if (status_bar_display_battery_info_hide_title_dual) {
                            "--W"
                        } else {
                            "${POWER_TITLE}--W"
                        }
                    }
                    container.addView(bottomView)
                    registerDarkReceiver(statusBarView, bottomView)
                    // 移除独立更新器：startBatteryInfoPowerUpdater(bottomView)
                }

                1 -> { // 电流（上排）+功率（下排）
                    // 上排：电流
                    val topView = TextView(context).apply {
                        tag = TAG_BATTERY_INFO_CURRENT
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, dualFontSize)
                        setTextColor(clockView.currentTextColor)
                        gravity = Gravity.CENTER_HORIZONTAL
                        typeface = clockView.typeface
                        letterSpacing = clockView.letterSpacing
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                        }
                        text = if (status_bar_display_battery_info_hide_title_dual) {
                            "--mA"
                        } else {
                            "${CURRENT_TITLE}--mA"
                        }
                    }
                    container.addView(topView)
                    registerDarkReceiver(statusBarView, topView)
                    // 移除独立更新器：startBatteryInfoCurrentUpdater(topView)

                    // 下排：功率
                    val bottomView = TextView(context).apply {
                        tag = TAG_BATTERY_INFO_POWER
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, dualFontSize)
                        setTextColor(clockView.currentTextColor)
                        gravity = Gravity.CENTER_HORIZONTAL
                        typeface = clockView.typeface
                        letterSpacing = clockView.letterSpacing
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                        }
                        text = if (status_bar_display_battery_info_hide_title_dual) {
                            "--W"
                        } else {
                            "${POWER_TITLE}--W"
                        }
                    }
                    container.addView(bottomView)
                    registerDarkReceiver(statusBarView, bottomView)
                    // 移除独立更新器：startBatteryInfoPowerUpdater(bottomView)
                }
            }
        } else {
            // 单排模式：水平布局，原有逻辑
            container.orientation = LinearLayout.HORIZONTAL
            if (is_battery_info_temperature) {
                val textView = createBatteryInfoTempTextView(context, clockView)
                container.addView(textView)
                registerDarkReceiver(statusBarView, textView)
                // 移除独立更新器：startBatteryInfoTempUpdater(textView)
            }
            if (is_battery_info_electricity) {
                val textView = createBatteryInfoCurrentTextView(context, clockView)
                container.addView(textView)
                registerDarkReceiver(statusBarView, textView)
                // 移除独立更新器：startBatteryInfoCurrentUpdater(textView)
            }
            if (is_battery_info_power) {
                val textView = createBatteryInfoPowerTextView(context, clockView)
                container.addView(textView)
                registerDarkReceiver(statusBarView, textView)
                // 移除独立更新器：startBatteryInfoPowerUpdater(textView)
            }
        }

        if (container.childCount == 0) return

        // 新增：启动锁屏可见性控制
//        startTemperatureVisibilityController(container)
        // 放在时钟后面
        val clockIndex = leftContainer.indexOfChild(clockView)
        if (clockIndex >= 0) {
            leftContainer.addView(container, clockIndex + 1)
        } else {
            leftContainer.addView(container)
        }

        // 启动统一更新器（代替原有的独立更新器和充电监听器）
        startUnifiedBatteryInfoUpdater(container)
    }

    private fun addBatteryInfoCenter(statusBarView: ViewGroup) {
        if (!status_bar_display_battery_info_layout) {
            if (!is_battery_info_temperature && !is_battery_info_electricity && !is_battery_info_power) return
        }
        val context = statusBarView.context
        val contentsId =
            context.resources.getIdentifier("status_bar_contents", "id", context.packageName)
                ?: return
        val contents = statusBarView.findViewById<ViewGroup>(contentsId) ?: return
        val clockId = context.resources.getIdentifier("clock", "id", context.packageName) ?: return
        val clockView = statusBarView.findViewById<TextView>(clockId) ?: return
        // 获取当前模式对应的固定宽度（单位 dp）
        val fixedWidthDp = if (status_bar_display_battery_info_layout) {
            status_bar_battery_info_fixed_width_dual
        } else {
            status_bar_battery_info_fixed_width_single
        }
        val width = if (fixedWidthDp > 0) {
            // 将 dp 转换为像素
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                fixedWidthDp.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        } else {
            LinearLayout.LayoutParams.WRAP_CONTENT
        }
        val centerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            layoutParams = lp
        }

        if (status_bar_display_battery_info_layout) {
            // 双排模式：在居中容器中再放一个垂直布局
            val dualContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    width,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.CENTER
                }
            }
            val dualFontSize = status_bar_display_battery_info_font_size_dual.toFloat()
            when (status_bar_display_battery_info_dual_display_mode) {
                0 -> {
                    val topView = createDualTextView(
                        context,
                        clockView,
                        TAG_BATTERY_INFO_TEMP,
                        if (status_bar_display_battery_info_hide_title_dual) "--${TEMP_UNIT}" else "${BATTERY_INFO_TEMP_TITLE}--${TEMP_UNIT}",
                        dualFontSize
                    )
                    dualContainer.addView(topView)
                    registerDarkReceiver(statusBarView, topView)

                    val bottomView = createDualTextView(
                        context,
                        clockView,
                        TAG_BATTERY_INFO_POWER,
                        if (status_bar_display_battery_info_hide_title_dual) "--W" else "${POWER_TITLE}--W",
                        dualFontSize
                    )
                    dualContainer.addView(bottomView)
                    registerDarkReceiver(statusBarView, bottomView)
                }

                1 -> {
                    val topView = createDualTextView(
                        context,
                        clockView,
                        TAG_BATTERY_INFO_CURRENT,
                        if (status_bar_display_battery_info_hide_title_dual) "--mA" else "${CURRENT_TITLE}--mA",
                        dualFontSize
                    )
                    dualContainer.addView(topView)
                    registerDarkReceiver(statusBarView, topView)

                    val bottomView = createDualTextView(
                        context,
                        clockView,
                        TAG_BATTERY_INFO_POWER,
                        if (status_bar_display_battery_info_hide_title_dual) "--W" else "${POWER_TITLE}--W",
                        dualFontSize
                    )
                    dualContainer.addView(bottomView)
                    registerDarkReceiver(statusBarView, bottomView)
                }
            }
            centerContainer.addView(dualContainer)
        } else {
            // 单排模式
            if (is_battery_info_temperature) {
                val textView = createBatteryInfoTempTextView(context, clockView)
                centerContainer.addView(textView)
                registerDarkReceiver(statusBarView, textView)
            }
            if (is_battery_info_electricity) {
                val textView = createBatteryInfoCurrentTextView(context, clockView)
                centerContainer.addView(textView)
                registerDarkReceiver(statusBarView, textView)
            }
            if (is_battery_info_power) {
                val textView = createBatteryInfoPowerTextView(context, clockView)
                centerContainer.addView(textView)
                registerDarkReceiver(statusBarView, textView)
            }
        }

        if (centerContainer.childCount == 0) return

        // 新增：启动锁屏可见性控制
//        startTemperatureVisibilityController(centerContainer)
        val cutoutId =
            context.resources.getIdentifier("cutout_space_view", "id", context.packageName)
        if (cutoutId != 0) {
            val cutoutView = statusBarView.findViewById<View>(cutoutId)
            if (cutoutView != null) {
                val cutoutIndex = contents.indexOfChild(cutoutView)
                if (cutoutIndex >= 0) {
                    contents.addView(centerContainer, cutoutIndex + 1)
                    // 启动统一更新器
                    startUnifiedBatteryInfoUpdater(centerContainer)
                    return
                }
            }
        }
        val endId = context.resources.getIdentifier(
            "status_bar_end_side_container",
            "id",
            context.packageName
        )
        if (endId != 0) {
            val endView = statusBarView.findViewById<View>(endId)
            if (endView != null) {
                val endIndex = contents.indexOfChild(endView)
                if (endIndex >= 0) {
                    contents.addView(centerContainer, endIndex)
                    startUnifiedBatteryInfoUpdater(centerContainer)
                    return
                }
            }
        }
        contents.addView(centerContainer)
        startUnifiedBatteryInfoUpdater(centerContainer)
    }

    private fun addBatteryInfoInsideBattery(statusBarView: ViewGroup) {
        if (!status_bar_display_battery_info_layout) {
            if (!is_battery_info_temperature && !is_battery_info_electricity && !is_battery_info_power) return
        }
        val context = statusBarView.context
        val systemIconsId =
            context.resources.getIdentifier("system_icons", "id", context.packageName) ?: return
        val systemIcons = statusBarView.findViewById<ViewGroup>(systemIconsId) ?: return
        val batteryId =
            context.resources.getIdentifier("battery", "id", context.packageName) ?: return
        val batteryView = statusBarView.findViewById<View>(batteryId) ?: return

        if (systemIcons.findViewWithTag<View>(TAG_BATTERY_INFO_CONTAINER) != null) return

        val clockId = context.resources.getIdentifier("clock", "id", context.packageName) ?: return
        val clockView = statusBarView.findViewById<TextView>(clockId) ?: return
        // 获取当前模式对应的固定宽度（单位 dp）
        val fixedWidthDp = if (status_bar_display_battery_info_layout) {
            status_bar_battery_info_fixed_width_dual
        } else {
            status_bar_battery_info_fixed_width_single
        }
        val width = if (fixedWidthDp > 0) {
            // 将 dp 转换为像素
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                fixedWidthDp.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        } else {
            LinearLayout.LayoutParams.WRAP_CONTENT
        }
        val container = LinearLayout(context).apply {
            tag = TAG_BATTERY_INFO_CONTAINER
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                width,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dpToPx(context, 2)
                marginEnd = dpToPx(context, 2)
            }
            layoutParams = lp
        }

        if (status_bar_display_battery_info_layout) {
            container.orientation = LinearLayout.VERTICAL
            val dualFontSize = status_bar_display_battery_info_font_size_dual.toFloat()
            when (status_bar_display_battery_info_dual_display_mode) {
                0 -> {
                    val topView = createDualTextView(
                        context,
                        clockView,
                        TAG_BATTERY_INFO_TEMP,
                        if (status_bar_display_battery_info_hide_title_dual) "--${TEMP_UNIT}" else "${BATTERY_INFO_TEMP_TITLE}--${TEMP_UNIT}",
                        dualFontSize
                    )
                    container.addView(topView)
                    registerDarkReceiver(statusBarView, topView)

                    val bottomView = createDualTextView(
                        context,
                        clockView,
                        TAG_BATTERY_INFO_POWER,
                        if (status_bar_display_battery_info_hide_title_dual) "--W" else "${POWER_TITLE}--W",
                        dualFontSize
                    )
                    container.addView(bottomView)
                    registerDarkReceiver(statusBarView, bottomView)
                }

                1 -> {
                    val topView = createDualTextView(
                        context,
                        clockView,
                        TAG_BATTERY_INFO_CURRENT,
                        if (status_bar_display_battery_info_hide_title_dual) "--mA" else "${CURRENT_TITLE}--mA",
                        dualFontSize
                    )
                    container.addView(topView)
                    registerDarkReceiver(statusBarView, topView)

                    val bottomView = createDualTextView(
                        context,
                        clockView,
                        TAG_BATTERY_INFO_POWER,
                        if (status_bar_display_battery_info_hide_title_dual) "--W" else "${POWER_TITLE}--W",
                        dualFontSize
                    )
                    container.addView(bottomView)
                    registerDarkReceiver(statusBarView, bottomView)
                }
            }
        } else {
            container.orientation = LinearLayout.HORIZONTAL
            if (is_battery_info_temperature) {
                val textView = createBatteryInfoTempTextView(context, clockView)
                container.addView(textView)
                registerDarkReceiver(statusBarView, textView)
            }
            if (is_battery_info_electricity) {
                val textView = createBatteryInfoCurrentTextView(context, clockView)
                container.addView(textView)
                registerDarkReceiver(statusBarView, textView)
            }
            if (is_battery_info_power) {
                val textView = createBatteryInfoPowerTextView(context, clockView)
                container.addView(textView)
                registerDarkReceiver(statusBarView, textView)
            }
        }

        if (container.childCount == 0) return

        // 新增：启动锁屏可见性控制
//        startTemperatureVisibilityController(container)
        val batteryIndex = systemIcons.indexOfChild(batteryView)
        if (batteryIndex >= 0) {
            systemIcons.addView(container, batteryIndex - 1)
        } else {
            systemIcons.addView(container)
        }
        startUnifiedBatteryInfoUpdater(container)
    }

    // 辅助函数：创建双排模式下的 TextView（简化重复代码）
    private fun createDualTextView(
        context: android.content.Context,
        clockView: TextView,
        tag: Int,
        initialText: String,
        fontSizeSp: Float
    ): TextView {
        return TextView(context).apply {
            this.tag = tag
            setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
            setTextColor(clockView.currentTextColor)
            gravity = Gravity.CENTER_HORIZONTAL
            typeface = clockView.typeface
            letterSpacing = clockView.letterSpacing
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            text = initialText
        }
    }

    // ========================= 创建 TextView（设备温度） =========================

    private fun createTempTextView(
        context: android.content.Context,
        clockView: TextView,
        prefix: String,
        tag: Int
    ): TextView {
        return TextView(context).apply {
            this.tag = tag
            setTextSize(TypedValue.COMPLEX_UNIT_SP, status_bar_display_temp_font_size.toFloat())
            setTextColor(clockView.currentTextColor)
            gravity = clockView.gravity
            typeface = clockView.typeface
            letterSpacing = clockView.letterSpacing

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dpToPx(context, 2)
                marginEnd = dpToPx(context, 2)
            }
            layoutParams = lp

            text = formatTempPlaceholder(prefix)
        }
    }

    // ========================= 创建 TextView（电池信息单排） =========================

    private fun createBatteryInfoTempTextView(
        context: android.content.Context,
        clockView: TextView
    ): TextView {
        return TextView(context).apply {
            tag = TAG_BATTERY_INFO_TEMP
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                status_bar_display_battery_info_font_size.toFloat()
            )
            setTextColor(clockView.currentTextColor)
            gravity = clockView.gravity
            typeface = clockView.typeface
            letterSpacing = clockView.letterSpacing

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dpToPx(context, 2)
                marginEnd = dpToPx(context, 2)
            }
            layoutParams = lp

            // 根据配置决定是否显示标题前缀
            text = if (status_bar_display_battery_info_hide_title_single) {
                // 隐藏标题：只显示数值和单位
                "--${TEMP_UNIT}"
            } else {
                // 显示标题：显示标题前缀+数值+单位
                "${BATTERY_INFO_TEMP_TITLE}--${TEMP_UNIT}"
            }
        }
    }

    private fun createBatteryInfoCurrentTextView(
        context: android.content.Context,
        clockView: TextView
    ): TextView {
        return TextView(context).apply {
            tag = TAG_BATTERY_INFO_CURRENT
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                status_bar_display_battery_info_font_size.toFloat()
            )
            setTextColor(clockView.currentTextColor)
            gravity = clockView.gravity
            typeface = clockView.typeface
            letterSpacing = clockView.letterSpacing

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dpToPx(context, 2)
                marginEnd = dpToPx(context, 2)
            }
            layoutParams = lp

            // 根据配置决定是否显示标题前缀
            text = if (status_bar_display_battery_info_hide_title_single) {
                // 隐藏标题：只显示数值和单位
                "--mA"
            } else {
                // 显示标题：显示标题前缀+数值+单位
                "${CURRENT_TITLE}--mA"
            }
        }
    }

    private fun createBatteryInfoPowerTextView(
        context: android.content.Context,
        clockView: TextView
    ): TextView {
        return TextView(context).apply {
            tag = TAG_BATTERY_INFO_POWER
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                status_bar_display_battery_info_font_size.toFloat()
            )
            setTextColor(clockView.currentTextColor)
            gravity = clockView.gravity
            typeface = clockView.typeface
            letterSpacing = clockView.letterSpacing

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dpToPx(context, 2)
                marginEnd = dpToPx(context, 2)
            }
            layoutParams = lp

            // 根据配置决定是否显示标题前缀
            text = if (status_bar_display_battery_info_hide_title_single) {
                // 隐藏标题：只显示数值和单位
                "--W"
            } else {
                // 显示标题：显示标题前缀+数值+单位
                "${POWER_TITLE}--W"
            }
        }
    }

    // ========================= 工具方法 =========================

    private fun dpToPx(context: android.content.Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    //@SuppressLint("PrivateApi")
    //private fun registerDarkReceiver(statusBarView: ViewGroup, tempView: TextView) {
    //    try {
    //        val darkIconDispatcherClass =
    //            "com.android.systemui.plugins.DarkIconDispatcher".toClass()
    //        val dependencyClass = "com.android.systemui.Dependency".toClass()
    //        val darkReceiverInterface = darkIconDispatcherClass.classLoader.loadClass(
    //            "com.android.systemui.plugins.DarkIconDispatcher\$DarkReceiver"
    //        )
    //
    //        val getMethod = dependencyClass.getDeclaredMethod("get", Class::class.java)
    //            .apply { isAccessible = true }
    //        val darkIconDispatcher = getMethod.invoke(null, darkIconDispatcherClass)
    //            ?: run {
    //                YLog.warn(tag = TAG, msg = "DarkIconDispatcher instance is null")
    //                return
    //            }
    //
    //        val getTintMethod = darkIconDispatcherClass.getMethod(
    //            "getTint",
    //            Collection::class.java,
    //            View::class.java,
    //            Int::class.javaPrimitiveType
    //        )
    //
    //        val darkReceiver = java.lang.reflect.Proxy.newProxyInstance(
    //            darkReceiverInterface.classLoader,
    //            arrayOf(darkReceiverInterface)
    //        ) { _, method, args ->
    //            when (method.name) {
    //                "onDarkChanged" -> {
    //                    if (args != null && args.size >= 3) {
    //                        val areas = args[0] as? Collection<*> ?: return@newProxyInstance null
    //                        val darkIntensity = args[1] as Float
    //                        val tint = args[2] as Int
    //                        val finalTint = getTintMethod.invoke(null, areas, tempView, tint) as Int
    //                        tempView.setTextColor(finalTint)
    //                    }
    //                    null
    //                }
    //
    //                "onDarkChangedWithContrast" -> null
    //                else -> {
    //                    when (method.returnType) {
    //                        Void.TYPE -> null
    //                        Boolean::class.javaPrimitiveType -> false
    //                        Int::class.javaPrimitiveType -> 0
    //                        Long::class.javaPrimitiveType -> 0L
    //                        Float::class.javaPrimitiveType -> 0f
    //                        Double::class.javaPrimitiveType -> 0.0
    //                        else -> null
    //                    }
    //                }
    //            }
    //        }
    //
    //        val addReceiverMethod = darkIconDispatcherClass.declaredMethods.find {
    //            it.name == "addDarkReceiver" && it.parameterCount == 1 && it.parameterTypes[0] == darkReceiverInterface
    //        } ?: run {
    //            YLog.error(tag = TAG, msg = "addDarkReceiver method not found")
    //            return
    //        }
    //        addReceiverMethod.isAccessible = true
    //        addReceiverMethod.invoke(darkIconDispatcher, darkReceiver)
    //
    //        YLog.debug(tag = TAG, msg = "Dark receiver registered for view tag ${tempView.tag}")
    //    } catch (e: Exception) {
    //        YLog.error(tag = TAG, msg = "Error registering dark receiver: ${e.message}")
    //        // 执行失败，换1个方法执行
    //    }
    //}

    @SuppressLint("PrivateApi")
    private fun registerDarkReceiver(statusBarView: ViewGroup, tempView: TextView) {
        try {
            val darkIconDispatcherClass = "com.android.systemui.plugins.DarkIconDispatcher".toClass()
            val dependencyClass = "com.android.systemui.Dependency".toClass()
            val darkReceiverInterface = darkIconDispatcherClass.classLoader?.loadClass(
                "com.android.systemui.plugins.DarkIconDispatcher\$DarkReceiver"
            ) ?: run {
                YLog.warn(tag = TAG, msg = "DarkIconDispatcher classLoader is null")
                return
            }

            // 通过 Dependency.get 获取 DarkIconDispatcher 实例
            val getMethod = dependencyClass.getDeclaredMethod("get", Class::class.java).apply {
                isAccessible = true
            }
            val darkIconDispatcher = getMethod.invoke(null, darkIconDispatcherClass)
                ?: run {
                    YLog.warn(tag = TAG, msg = "DarkIconDispatcher instance is null")
                    return
                }

            // 动态查找 getTint 方法：优先 ArrayList，其次 Collection
            val getTintMethod = try {
                darkIconDispatcherClass.getMethod(
                    "getTint",
                    ArrayList::class.java,
                    View::class.java,
                    Int::class.javaPrimitiveType
                )
            } catch (e1: NoSuchMethodException) {
                try {
                    darkIconDispatcherClass.getMethod(
                        "getTint",
                        Collection::class.java,
                        View::class.java,
                        Int::class.javaPrimitiveType
                    )
                } catch (e2: NoSuchMethodException) {
                    YLog.error(tag = TAG, msg = "getTint method not found in DarkIconDispatcher")
                    return
                }
            }

            // 创建动态代理 DarkReceiver
            val darkReceiver = java.lang.reflect.Proxy.newProxyInstance(
                darkReceiverInterface.classLoader,
                arrayOf(darkReceiverInterface)
            ) { _, method, args ->
                when (method.name) {
                    "onDarkChanged" -> {
                        if (args != null && args.size >= 3) {
                            @Suppress("UNCHECKED_CAST")
                            val areas = args[0] as? ArrayList<Rect> ?: return@newProxyInstance null
                            val tint = args[2] as Int
                            val finalTint = getTintMethod.invoke(null, areas, tempView, tint) as Int
                            tempView.setTextColor(finalTint)
                        }
                        null
                    }
                    "onDarkChangedWithContrast" -> null  // 红魔11新增的默认方法，忽略
                    else -> {
                        when (method.returnType) {
                            Void.TYPE -> null
                            Boolean::class.javaPrimitiveType -> false
                            Int::class.javaPrimitiveType -> 0
                            Long::class.javaPrimitiveType -> 0L
                            Float::class.javaPrimitiveType -> 0f
                            Double::class.javaPrimitiveType -> 0.0
                            else -> null
                        }
                    }
                }
            }

            // 查找 addDarkReceiver(DarkReceiver) 方法
            val addReceiverMethod = darkIconDispatcherClass.declaredMethods.find {
                it.name == "addDarkReceiver" &&
                        it.parameterCount == 1 &&
                        it.parameterTypes[0] == darkReceiverInterface
            } ?: run {
                YLog.error(tag = TAG, msg = "addDarkReceiver method not found")
                return
            }
            addReceiverMethod.isAccessible = true
            addReceiverMethod.invoke(darkIconDispatcher, darkReceiver)

            YLog.debug(tag = TAG, msg = "Dark receiver registered for view tag ${tempView.tag}")
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Error registering dark receiver: ${e.message}")
        }
    }

    // ========================= 读取 uevent =========================
    private fun readUeventMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            FileInputStream(BATTERY_UEVENT_PATH).use { fis ->
                fis.bufferedReader().forEachLine { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        map[parts[0]] = parts[1]
                    }
                }
            }
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Failed to read uevent: ${e.message}")
        }
        return map
    }

    private fun readIntFromFile(path: String): Int? {
        return try {
            FileInputStream(path).use { fis ->
                fis.bufferedReader().use { it.readText() }.trim().toIntOrNull()
            }
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Failed to read $path: ${e.message}")
            null
        }
    }

    // ========================= 设备温度更新器 =========================

    private fun startTempUpdater(textView: TextView, path: String, divisor: Int) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val prefix = when (textView.tag) {
                    TAG_TEMP_BATTERY -> BATTERY_TITLE
                    TAG_TEMP_CPU -> CPU_TITLE
                    TAG_TEMP_GPU -> GPU_TITLE
                    else -> ""
                }
                val celsius = if (textView.tag == TAG_TEMP_BATTERY) {
                    val uevent = readUeventMap()
                    uevent["POWER_SUPPLY_TEMP"]?.toIntOrNull()?.toFloat()?.div(10)
                } else {
                    readIntFromFile(path)?.toFloat()?.div(divisor)
                }
                textView.text = if (celsius != null) {
                    formatTemp(prefix, celsius)
                } else {
                    formatTempPlaceholder(prefix)
                }
                handler.postDelayed(this, UPDATE_INTERVAL)
            }
        }
        handler.post(runnable)

        textView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                handler.removeCallbacks(runnable)
            }
        })
    }

    // ========================= 电池信息统一更新器（新增）=========================

    /**
     * 启动统一的电池信息更新器，同时控制容器可见性（基于充电状态）
     */
    private fun startUnifiedBatteryInfoUpdater(container: ViewGroup) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                // 读取 uevent
                val uevent = readUeventMap()
                val tempVal = uevent["POWER_SUPPLY_TEMP"]?.toIntOrNull()?.let { it / 10.0 }
                val rawCurrent = uevent["POWER_SUPPLY_CURRENT_NOW"]?.toIntOrNull()
                val rawVoltage = uevent["POWER_SUPPLY_VOLTAGE_NOW"]?.toIntOrNull()
                val status = uevent["POWER_SUPPLY_STATUS"]
                val isCharging = status == "Charging" || status == "Full"

                // 判断单/双排模式：直接使用全局配置，而非容器方向
                val isSingleMode = !status_bar_display_battery_info_layout

                // 更新所有子 TextView 的文本
                updateBatteryInfoTextViews(container, tempVal, rawCurrent, rawVoltage, isSingleMode)

                // 锁屏状态判断
                val keyguardManager = container.context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                val isLocked = keyguardManager.isKeyguardLocked

                val shouldShow = if (isLocked) {
                    false  // 锁屏时隐藏
                } else {
                    if (isSingleMode) {
                        if (status_bar_display_battery_info_is_charge_mode_single == 1) isCharging else true
                    } else {
                        if (status_bar_display_battery_info_is_charge_mode_dual == 1) isCharging else true
                    }
                }
                container.visibility = if (shouldShow) View.VISIBLE else View.GONE

                handler.postDelayed(this, UPDATE_INTERVAL)
            }
        }
        handler.post(runnable)

        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                handler.removeCallbacks(runnable)
            }
        })
    }

    /**
     * 递归遍历视图树，根据 tag 更新 TextView 的文本
     */
//    private fun updateBatteryInfoTextViews(view: View, temp: Double?, rawCurrent: Int?, rawVoltage: Int?) {
//        when (view) {
//            is TextView -> {
//                when (view.tag) {
//                    TAG_BATTERY_INFO_TEMP -> {
//                        val text = if (temp != null) {
//                            formatBatteryInfoTempTitle(temp.toInt().toString(), TEMP_UNIT)
//                        } else {
//                            formatBatteryInfoTempTitle("--", TEMP_UNIT)
//                        }
//                        view.text = text
//                    }
//                    TAG_BATTERY_INFO_CURRENT -> {
//                        val text = if (rawCurrent != null) {
//                            val currentMa = kotlin.math.abs(rawCurrent) / 1000.0
//                            val (value, unit) = if (currentMa >= 1000) {
//                                currentMa / 1000.0 to "A"
//                            } else {
//                                currentMa to "mA"
//                            }
//                            val valueStr = if (unit == "A") {
//                                String.format(Locale.US, "%.2f", value)
//                            } else {
//                                String.format(Locale.US, "%.0f", value)
//                            }
//                            formatBatteryInfoCurrentTitle(valueStr, unit)
//                        } else {
//                            formatBatteryInfoCurrentTitle("--", "mA")
//                        }
//                        view.text = text
//                    }
//                    TAG_BATTERY_INFO_POWER -> {
//                        val text = if (rawCurrent != null && rawVoltage != null) {
//                            val currentA = kotlin.math.abs(rawCurrent) / 1_000_000.0
//                            val voltageV = rawVoltage / 1_000_000.0
//                            val powerW = currentA * voltageV
//                            val valueStr = String.format(Locale.US, "%.2f", powerW)
//                            formatBatteryInfoPowerTitle(valueStr, "W")
//                        } else {
//                            formatBatteryInfoPowerTitle("--", "W")
//                        }
//                        view.text = text
//                    }
//                }
//            }
//            is ViewGroup -> {
//                for (i in 0 until view.childCount) {
//                    updateBatteryInfoTextViews(view.getChildAt(i), temp, rawCurrent, rawVoltage)
//                }
//            }
//        }
//    }

    private fun updateBatteryInfoTextViews(
        view: View,
        temp: Double?,
        rawCurrent: Int?,
        rawVoltage: Int?,
        isSingleMode: Boolean
    ) {
        when (view) {
            is TextView -> {
                when (view.tag) {
                    TAG_BATTERY_INFO_TEMP -> {
                        val text = if (temp != null) {
                            val tempMode = if (isSingleMode)
                                status_bar_display_battery_info_temp_mode_single
                            else
                                status_bar_display_battery_info_temp_mode_dual
                            val tempStr = if (tempMode == 1) {
                                String.format(Locale.US, "%.1f", temp)
                            } else {
                                String.format(Locale.US, "%.0f", temp)
                            }
                            buildBatteryInfoText(
                                TAG_BATTERY_INFO_TEMP,
                                tempStr,
                                isSingleMode,
                                TEMP_UNIT
                            )
                        } else {
                            buildBatteryInfoText(
                                TAG_BATTERY_INFO_TEMP,
                                "--",
                                isSingleMode,
                                TEMP_UNIT
                            )
                        }
                        view.text = text
                    }

                    TAG_BATTERY_INFO_CURRENT -> {
                        val text = if (rawCurrent != null) {
                            val currentMa = kotlin.math.abs(rawCurrent) / 1000.0
                            val (value, unit) = if (currentMa >= 1000) {
                                currentMa / 1000.0 to "A"
                            } else {
                                currentMa to "mA"
                            }
                            val valueStr = if (unit == "A") {
                                String.format(Locale.US, "%.2f", value)
                            } else {
                                String.format(Locale.US, "%.0f", value)
                            }
                            buildBatteryInfoText(
                                TAG_BATTERY_INFO_CURRENT,
                                valueStr,
                                isSingleMode,
                                unit
                            )
                        } else {
                            buildBatteryInfoText(TAG_BATTERY_INFO_CURRENT, "--", isSingleMode, "mA")
                        }
                        view.text = text
                    }

                    TAG_BATTERY_INFO_POWER -> {
                        val text = if (rawCurrent != null && rawVoltage != null) {
                            val currentA = kotlin.math.abs(rawCurrent) / 1_000_000.0
                            val voltageV = rawVoltage / 1_000_000.0
                            val powerW = currentA * voltageV
                            val valueStr = String.format(Locale.US, "%.2f", powerW)
                            buildBatteryInfoText(
                                TAG_BATTERY_INFO_POWER,
                                valueStr,
                                isSingleMode,
                                "W"
                            )
                        } else {
                            buildBatteryInfoText(TAG_BATTERY_INFO_POWER, "--", isSingleMode, "W")
                        }
                        view.text = text
                    }
                }
            }

            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    updateBatteryInfoTextViews(
                        view.getChildAt(i),
                        temp,
                        rawCurrent,
                        rawVoltage,
                        isSingleMode
                    )
                }
            }
        }
    }

    // ========================= 设备温度格式化 =========================

    private fun formatTemp(prefix: String, celsius: Float): String {
        val format = when (status_bar_display_temp_display_mode) {
            0 -> "%.0f"
            1 -> "%.1f"
            else -> "%.1f"
        }
        val valueStr = String.format(Locale.US, format, celsius)
        return if (status_bar_display_temp_hide_unit) {
            "$prefix$valueStr"
        } else {
            "$prefix$valueStr$TEMP_UNIT"
        }
    }

    private fun formatTempPlaceholder(prefix: String): String {
        return if (status_bar_display_temp_hide_unit) {
            "${prefix}--"
        } else {
            "${prefix}--$TEMP_UNIT"
        }
    }

    // ========================= 电池信息格式化（始终显示单位） =========================

    private fun formatBatteryInfoTitle(title: String, value: String, unit: String = ""): String {
        // 判断当前是单排还是双排模式（通过调用上下文无法直接获得，但此函数仅在统一更新器中使用，
        // 统一更新器会传入正确的值，我们在此保留原逻辑，但实际已由调用者根据 tag 决定显示格式，
        // 此处的隐藏标题逻辑已通过 formatBatteryInfoTempTitle 等内部调用，而它们又调用本函数，
        // 为了保留原有标题隐藏逻辑，需要知道当前模式。然而在统一更新器中无法直接获得模式，
        // 所以我们通过检查调用栈或传入参数？简单起见，我们修改为在 updateBatteryInfoTextViews 中
        // 直接使用 formatBatteryInfoXXXTitle 函数，而这些函数内部会调用本函数，因此本函数仍需判断模式。
        // 但模式信息丢失，我们可以在调用 formatBatteryInfoXXXTitle 时传递一个 isSingleMode 参数，
        // 或者在 startUnifiedBatteryInfoUpdater 中确定 isSingleMode 并作为全局变量？更好的办法：
        // 在 updateBatteryInfoTextViews 中直接计算文本，不依赖 formatBatteryInfoTitle 的隐藏逻辑，
        // 因为隐藏逻辑已经由初始文本决定，后续更新只需替换数值部分。实际上，当前实现中，初始文本已经根据隐藏标题配置设置好了，
        // 例如 "--℃" 或 "B:--℃"。更新时，我们只需替换数值部分，保留前缀或占位符结构。
        // 但目前的 formatBatteryInfoTempTitle 等函数会重新添加标题前缀，导致隐藏标题失效。
        // 因此，我们需要修改更新逻辑，使得更新时保留原始文本结构，仅替换数值部分。
        // 但为了简化，我们仍然使用 formatBatteryInfoTitle，但需要知道是否隐藏标题。由于无法获得模式，
        // 我们可以在更新时从当前 TextView 的文本推断：如果文本以 "--" 开头，说明是隐藏标题模式，否则保留前缀。
        // 但推断可能不准确（例如数值可能也是 "--"）。另一种方法：在 TextView 的 tag 之外，再保存一个标志，
        // 但过于复杂。考虑到隐藏标题是全局配置，我们可以直接使用全局配置变量，但这可能不准确（单双排隐藏配置可能不同）。
        // 为了解决这个问题，我们在 startUnifiedBatteryInfoUpdater 中根据 container 的 orientation 判断模式，
        // 并将 isSingleMode 传递给更新函数，或者作为成员变量。简单方式：在 updateBatteryInfoTextViews 中，
        // 我们不使用 formatBatteryInfoXXXTitle，而是直接构建文本：根据 tag 和原始数据，结合对应的隐藏配置。
        // 这样可以保证隐藏配置的正确性。我们下面修改 updateBatteryInfoTextViews 来实现。
        // 但为了保留原有函数，我们可以重载或修改。我们将在 updateBatteryInfoTextViews 中直接实现，
        // 而不调用 formatBatteryInfoTitle。
        // 因此，下面的 formatBatteryInfoTitle 系列函数在统一更新器中不再使用，但为了保持代码完整性，我们保留它们，
        // 但注明已弃用。
        val isSingleMode = true // 占位，实际不再使用
        return if (isSingleMode) {
            if (status_bar_display_battery_info_hide_title_single) {
                "$value$unit"
            } else {
                "$title$value$unit"
            }
        } else {
            if (status_bar_display_battery_info_hide_title_dual) {
                "$value$unit"
            } else {
                "$title$value$unit"
            }
        }
    }

    // 以下函数保留，但实际更新中不再调用，改用 updateBatteryInfoTextViews 中的逻辑
    private fun formatBatteryInfoTempTitle(value: String, unit: String = ""): String {
        return formatBatteryInfoTitle(BATTERY_INFO_TEMP_TITLE, value, unit)
    }

    private fun formatBatteryInfoCurrentTitle(value: String, unit: String = ""): String {
        return formatBatteryInfoTitle(CURRENT_TITLE, value, unit)
    }

    private fun formatBatteryInfoPowerTitle(value: String, unit: String = ""): String {
        return formatBatteryInfoTitle(POWER_TITLE, value, unit)
    }

    // 注意：以上三个函数在统一更新器中不再使用，因为无法正确判断单双排隐藏配置。
    // 我们在 updateBatteryInfoTextViews 中直接实现文本构建，确保隐藏配置正确。
    // 但为了代码简洁，我们可以在 updateBatteryInfoTextViews 中根据 tag 和全局配置构建文本。
    /**
     * 控制设备温度容器的可见性：锁屏时隐藏，解锁后显示
     */
    private fun startTemperatureVisibilityController(container: ViewGroup) {
        val handler = Handler(Looper.getMainLooper())
        val context = container.context
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val runnable = object : Runnable {
            override fun run() {
                val isLocked = keyguardManager.isKeyguardLocked
                container.visibility = if (isLocked) View.GONE else View.VISIBLE
                handler.postDelayed(this, UPDATE_INTERVAL)
            }
        }
        handler.post(runnable)

        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                handler.removeCallbacks(runnable)
            }
        })
    }
}