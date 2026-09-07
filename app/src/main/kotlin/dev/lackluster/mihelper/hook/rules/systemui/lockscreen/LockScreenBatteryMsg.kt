package dev.lackluster.mihelper.hook.rules.systemui.lockscreen

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ViewGroupClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import java.io.FileInputStream
import java.util.Locale
import kotlin.math.abs

/**
 * 在锁屏界面底部显示详细的充电信息（双排）
 * 上排：电池温度
 * 下排：电流 · 电压 · 功率
 */
object LockScreenBatteryMsg : YukiBaseHooker() {

    private const val TAG = "LockScreenBatteryMsg"

    // 开关
    private val enable by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.SHOW_CHARGING_INFO, false)
    }

    // 显示电流
    private val showCurrent by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.SHOW_CHARGING_C_MORE, false)
    }
    // 显示电压
    private val showVoltage by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.SHOW_CHARGING_V_MORE, false)
    }
    // 显示功率
    private val showPower by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.SHOW_CHARGING_P_MORE, false)
    }

    // 显示温度
    private val showTemperature by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.SHOW_BATTERY_TEMPERATURE, false)
    }

    // 温度单位（℃/℉），此处固定为℃
    private val tempUnit = "℃"

    // 字体大小（dp）
    private val fontSizeDp by lazy {
        Prefs.getInt(Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_BATTERY_DETAIL_FONT_SIZE, 12)
    }

    // 上排与下排间距（dp）
    private val lineSpacingDp by lazy {
        Prefs.getInt(Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_BATTERY_DETAIL_LINE_SPACING, 2)
    }

    // 更新间隔单位是秒
    private val UPDATE_INTERVAL_SEC by lazy {
        Prefs.getInt(Pref.Key.SystemUI.LockScreen.SHOW_REFRESH_INTERVAL_TIME, 1)
    }
    //private const val UPDATE_INTERVAL = 1000L
    // 转换为毫秒
    private val UPDATE_INTERVAL_MS get() = UPDATE_INTERVAL_SEC * 1000L

    // 电池 uevent 路径
    private const val BATTERY_UEVENT_PATH = "/sys/class/power_supply/battery/uevent"

    // 自定义视图的 tag
    private val TAG_CUSTOM_BATTERY_DETAIL = 0x7F100100

    override fun onHook() {
        if (!enable) return

        // Hook KeyguardIndicationController 的 setIndicationArea 方法，在布局加载后添加自定义视图
        "com.android.systemui.statusbar.KeyguardIndicationController".toClass().method {
            name = "setIndicationArea"
            param(ViewGroupClass)
        }.hook {
            after {
                val indicationArea = args[0] as ViewGroup
                val controller = instance
                // 检查是否已经添加过，避免重复
                if (indicationArea.findViewWithTag<View>(TAG_CUSTOM_BATTERY_DETAIL) != null) return@after

                // 创建自定义视图并添加到 IndicationArea 底部
                val customView = createDetailView(indicationArea.context)
                indicationArea.addView(customView)

                // 启动定时器更新数据
                startUpdater(customView, controller)
            }
        }
    }

    /**
     * 创建垂直双排布局
     */
    private fun createDetailView(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            tag = TAG_CUSTOM_BATTERY_DETAIL
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val padding = dpToPx(context, 4)
            setPadding(padding, padding, padding, padding)

            // 上排：温度
            val tempView = TextView(context).apply {
                id = View.generateViewId()
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeDp.toFloat())
                setTextColor(Color.WHITE) // 初始颜色，后续会动态更新
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            // 下排：电流、电压、功率组合
            val powerView = TextView(context).apply {
                id = View.generateViewId()
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeDp.toFloat())
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(context, lineSpacingDp)
                }
            }

            addView(tempView)
            addView(powerView)
        }
    }

    /**
     * 启动定时器，读取电池数据并更新视图
     */
    private fun startUpdater(container: LinearLayout, controller: Any) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                // 获取锁屏状态
                val context = container.context
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                val isLocked = keyguardManager.isKeyguardLocked
                // 获取充电状态和电池数据
                val uevent = readUeventMap()
                val status = uevent["POWER_SUPPLY_STATUS"]
                val isCharging = status == "Charging" || status == "Full"

                // 决定是否显示：锁屏且充电时显示
                val shouldShow = isLocked && isCharging
                container.visibility = if (shouldShow) View.VISIBLE else View.GONE

                if (shouldShow) {
                    // 读取数据
                    val temp = uevent["POWER_SUPPLY_TEMP"]?.toIntOrNull()?.let { it / 10.0 }
                    val rawCurrent = uevent["POWER_SUPPLY_CURRENT_NOW"]?.toIntOrNull()
                    val rawVoltage = uevent["POWER_SUPPLY_VOLTAGE_NOW"]?.toIntOrNull()

                    // 格式化并更新 UI
                    updateViews(container, temp, rawCurrent, rawVoltage, controller)
                }

                //handler.postDelayed(this, UPDATE_INTERVAL)
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
        handler.post(runnable)

        // 当视图被移除时停止更新
        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                handler.removeCallbacks(runnable)
            }
        })
    }

    /**
     * 更新两个 TextView 的内容
     */
    //private fun updateViews(container: LinearLayout, temp: Double?, rawCurrent: Int?, rawVoltage: Int?, controller: Any) {
    //    val tempView = container.getChildAt(0) as TextView
    //    val powerView = container.getChildAt(1) as TextView
    //
    //    // 温度文本
    //    val tempText = if (showTemperature && temp != null) {
    //        String.format(Locale.US, "%.1f%s", temp, tempUnit)
    //    } else {
    //        ""
    //    }
    //    tempView.text = tempText
    //    tempView.visibility = if (tempText.isNotEmpty()) View.VISIBLE else View.GONE
    //
    //    // 下排文本：电流 · 电压 · 功率
    //    val currentText = if (rawCurrent != null) {
    //        val currentMa = abs(rawCurrent) / 1000.0
    //        val (value, unit) = if (currentMa >= 1000) {
    //            currentMa / 1000.0 to "A"
    //        } else {
    //            currentMa to "mA"
    //        }
    //        val valueStr = if (unit == "A") String.format(Locale.US, "%.2f", value) else String.format(Locale.US, "%.0f", value)
    //        "$valueStr$unit"
    //    } else {
    //        null
    //    }
    //    val voltageText = if (rawVoltage != null) {
    //        val voltageV = rawVoltage / 1_000_000.0
    //        String.format(Locale.US, "%.2fV", voltageV)
    //    } else {
    //        null
    //    }
    //    val powerText = if (rawCurrent != null && rawVoltage != null) {
    //        val currentA = abs(rawCurrent) / 1_000_000.0
    //        val voltageV = rawVoltage / 1_000_000.0
    //        val powerW = currentA * voltageV
    //        String.format(Locale.US, "%.2fW", powerW)
    //    } else {
    //        null
    //    }
    //
    //    //val parts = mutableListOf<String>()
    //    //currentText?.let { parts.add(it) }
    //    //voltageText?.let { parts.add(it) }
    //    //powerText?.let { parts.add(it) }
    //    //val finalText = parts.joinToString(" · ")
    //    //powerView.text = finalText
    //
    //    val parts = mutableListOf<String>()
    //    if (showCurrent) {
    //        currentText?.let { parts.add(it) }
    //    }
    //    if (showVoltage) {
    //        voltageText?.let { parts.add(it) }
    //    }
    //    if (showPower) {
    //        powerText?.let { parts.add(it) }
    //    }
    //    val finalText = parts.joinToString(" · ")
    //    powerView.text = finalText
    //
    //    // 动态更新文本颜色（跟随锁屏指示颜色）
    //    try {
    //        val colorStateList = controller.javaClass.getMethod("getInitialTextColorState").invoke(controller) as? android.content.res.ColorStateList
    //        val color = colorStateList?.defaultColor ?: Color.WHITE
    //        tempView.setTextColor(color)
    //        powerView.setTextColor(color)
    //    } catch (e: Exception) {
    //        // 反射失败则保持白色
    //    }
    //}
    private fun updateViews(container: LinearLayout, temp: Double?, rawCurrent: Int?, rawVoltage: Int?, controller: Any) {
        val tempView = container.getChildAt(0) as TextView
        val powerView = container.getChildAt(1) as TextView

        // 温度文本（已由 showTemperature 控制）
        val tempText = if (showTemperature && temp != null) {
            String.format(Locale.US, "%.1f%s", temp, tempUnit)
        } else {
            ""
        }
        tempView.text = tempText
        tempView.visibility = if (tempText.isNotEmpty()) View.VISIBLE else View.GONE

        // 下排文本：按开关分别决定是否显示
        val currentText = if (rawCurrent != null) {
            val currentMa = abs(rawCurrent) / 1000.0
            val (value, unit) = if (currentMa >= 1000) {
                currentMa / 1000.0 to "A"
            } else {
                currentMa to "mA"
            }
            val valueStr = if (unit == "A") String.format(Locale.US, "%.2f", value) else String.format(Locale.US, "%.0f", value)
            "$valueStr$unit"
        } else {
            null
        }
        val voltageText = if (rawVoltage != null) {
            val voltageV = rawVoltage / 1_000_000.0
            String.format(Locale.US, "%.2fV", voltageV)
        } else {
            null
        }
        val powerText = if (rawCurrent != null && rawVoltage != null) {
            val currentA = abs(rawCurrent) / 1_000_000.0
            val voltageV = rawVoltage / 1_000_000.0
            val powerW = currentA * voltageV
            String.format(Locale.US, "%.2fW", powerW)
        } else {
            null
        }

        // 根据配置开关决定是否添加到列表
        val parts = mutableListOf<String>()
        if (showCurrent) {
            currentText?.let { parts.add(it) }
        }
        if (showVoltage) {
            voltageText?.let { parts.add(it) }
        }
        if (showPower) {
            powerText?.let { parts.add(it) }
        }
        val finalText = parts.joinToString(" · ")
        powerView.text = finalText

        // 动态更新文本颜色（保持不变）
        try {
            val colorStateList = controller.javaClass.getMethod("getInitialTextColorState").invoke(controller) as? android.content.res.ColorStateList
            val color = colorStateList?.defaultColor ?: Color.WHITE
            tempView.setTextColor(color)
            powerView.setTextColor(color)
        } catch (e: Exception) {
            // 反射失败则保持白色
        }
    }

    /**
     * 读取 uevent 文件，返回 Map
     */
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

    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}