package dev.lackluster.mihelper.hook.rules.desktop

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.factory.toClass
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.isSystemInDarkMode
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

@SuppressLint("StaticFieldLeak")
object RecentTasksHook : YukiBaseHooker() {

    private const val TAG = "RecentTasksHook"

    // 显示风格：0 = 简洁单行 (xx.xxG/xx.xxG)，1 = 三行详细（含百分比）
    //private val displayStyle = 1  // 你可以根据需要修改此值，或接入配置

    // 总开关
    private val enabled by lazy {
        Prefs.getBoolean(Pref.Key.SystemDesktop.SYSTEM_DESKTOP_RECENT_TASK_DISPLAY_MEMORY, false)
    }
    // 显示风格：0 = 简洁单行 (xx.xxG/xx.xxG)，1 = 三行详细（含百分比）
    private val displayStyle by lazy {
        Prefs.getInt( Pref.Key.SystemDesktop.DISPLAY_STYLE, 0)
    }

    // 如果displayStyle = 1 则根据 它的索引值来决定显示的内容
    // 0 表示：  剩余内存+已用内存+总内存
    // 1 表示：  剩余内存+已用内存
    // 2 表示：  剩余内存
    private val memoryDisplayStyle by lazy {
        Prefs.getInt( Pref.Key.SystemDesktop.MEMORY_DISPLAY_STYLE, 0)
    }



    // 简约风格的布局
    // 横竖屏模式下的字体大小
    private val portraitscreenMemoryFontSize by lazy {
        Prefs.getFloat( Pref.Key.SystemDesktop.SIMPLE_PORTRAITSCREEN_MEMORY_FONT_SIZE, 10f)
    }
    private val landsacpeMemoryFontSize by lazy {
        Prefs.getFloat( Pref.Key.SystemDesktop.SIMPLE_LANDSACPE_MEMORY_FONT_SIZE, 8f)
    }
    // 横竖屏模式下的高度
    private val landsacpe_component_height by lazy {
        Prefs.getInt( Pref.Key.SystemDesktop.SIMPLE_LANDSACPE_COMPONENT_HEIGHT, 80)
    }
    private val portraitscreen_component_height by lazy {
        Prefs.getInt(Pref.Key.SystemDesktop.SIMPLE_PORTRAITSCREEN_COMPONENT_HEIGHT, 85)
    }

    // 经典风格的布局
    // 横竖屏模式下的字体大小
    private val portraitscreenMemoryFontSize2 by lazy {
        Prefs.getFloat( Pref.Key.SystemDesktop.CLASSICS_PORTRAITSCREEN_MEMORY_FONT_SIZE, 10f)
    }
    private val landsacpeMemoryFontSize2 by lazy {
        Prefs.getFloat( Pref.Key.SystemDesktop.CLASSICS_LANDSACPE_MEMORY_FONT_SIZE, 8f)
    }
    // 横竖屏模式下的高度
    private val landsacpe_component_height2 by lazy {
        Prefs.getInt( Pref.Key.SystemDesktop.CLASSICS_LANDSACPE_COMPONENT_HEIGHT, 80)
    }
    private val portraitscreen_component_height2 by lazy {
        Prefs.getInt(Pref.Key.SystemDesktop.CLASSICS_PORTRAITSCREEN_COMPONENT_HEIGHT, 85)
    }







    private var memoryTextView: WeakReference<TextView>? = null
    private var activityRef: WeakReference<Activity>? = null
    private var activityManager: ActivityManager? = null

    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMemoryText()
            updateHandler.postDelayed(this, 1000) // 1 秒更新一次
        }
    }

    private val memoryInfo = ActivityManager.MemoryInfo()

    private var darkModeReceiver: BroadcastReceiver? = null

    // 改为 WeakReference，避免跨实例引用问题
    private var clearAllLayoutIdParent: WeakReference<RelativeLayout>? = null

    // 保存上一次应用的旋转角度，避免重复设置
    private var lastRotation = -1


    @SuppressLint("ResourceType", "RtlHardcoded")
    override fun onHook() {
        // 总开关
        if (!enabled) return
        "com.android.quickstep.views.OverviewActionsView".toClass().method {
            name = "onFinishInflate"
        }.hook {
            after {
                val view = instance<View>()
                val context = view.context

                // 找到清除所有任务的布局容器
                val clearAllLayoutId = appContext!!.resources.getIdentifier("remove_all_button_layout", "id", appContext!!.packageName)
                val clearAllLayout = view.findViewById(clearAllLayoutId) as? LinearLayout ?: return@after

                // 避免重复添加
                if (clearAllLayout.findViewWithTag<View>("memory_text_view") != null) {
                    return@after
                }

                // 设置为垂直方向
                clearAllLayout.orientation = LinearLayout.VERTICAL

                // 获取原始 LayoutParams，并保存原始固定宽高作为最小尺寸
                val originalParams = clearAllLayout.layoutParams
                val originalWidth = originalParams.width
                val originalHeight = originalParams.height

                if (originalWidth > 0 && originalHeight > 0) {
                    clearAllLayout.minimumWidth = originalWidth
                    clearAllLayout.minimumHeight = originalHeight
                }

                // 将 LayoutParams 的宽高改为 WRAP_CONTENT
                originalParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                originalParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                clearAllLayout.layoutParams = originalParams

                // 找到父布局 RelativeLayout
                val clearAllLayoutIdParentID = appContext!!.resources.getIdentifier("action_buttons", "id", appContext!!.packageName)
                val parentLayout = view.findViewById(clearAllLayoutIdParentID) as? RelativeLayout ?: return@after
                clearAllLayoutIdParent = WeakReference(parentLayout)

                // 创建显示内存信息的 TextView，使用等宽粗体字体以保证对齐
                val textView = TextView(context).apply {
                    id = View.generateViewId()
                    tag = "memory_text_view"
                    textSize = 20f
                    //gravity = Gravity.LEFT // 左对齐，可根据需要调整
                    gravity = Gravity.CENTER // 左对齐，可根据需要调整
                    //setTypeface(null, Typeface.BOLD)
                    // 等宽字体
                    setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD))
                }
                // 注册系统暗黑模式监听器
                registerDarkReceiver(textView, context)

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = dpToPx(0, view)
                textView.layoutParams = params
                clearAllLayout.addView(textView)

                memoryTextView = WeakReference(textView)

                val activity = context.findActivity()
                activityRef = WeakReference(activity)
                activityManager = activity?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

                updateHandler.post(updateRunnable)

                // 监听 Activity 销毁，停止更新
                activity?.let {
                    "android.app.Activity".toClass().method {
                        name = "onDestroy"
                    }.hook {
                        before {
                            if (instance<Activity>() == it) {
                                updateHandler.removeCallbacks(updateRunnable)
                                unregisterDarkReceiver(it)
                            }
                        }
                    }
                }
            }
        }

        // 监听方向变化（RecentsOrientedState.update）
        "com.android.quickstep.util.RecentsOrientedState".toClass().method {
            name = "update"
            paramCount = 2
            param(IntType, IntType)
        }.hook {
            after {
                val touchRotation = args[0] as Int
                val displayRotation = args[1] as Int
                YLog.debug(tag = TAG, msg = "Orientation update: touchRotation=$touchRotation, displayRotation=$displayRotation")

                // 在主线程更新布局
                activityRef?.get()?.runOnUiThread {
                    applyLayoutForRotation(displayRotation)
                }
            }
        }
    }

    /**
     * 根据当前旋转角度应用布局参数
     */
    private fun applyLayoutForRotation(rotation: Int) {
        //if (lastRotation == rotation) return
        //lastRotation = rotation

        val textView = memoryTextView?.get() ?: return
        val parent = clearAllLayoutIdParent?.get() ?: return

        val isLandscape = rotation == 1 || rotation == 3
        // 横屏
        if (isLandscape) {
            // 简约风格的布局
            if(displayStyle==0){
                // 字体大小
                //textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, landsacpeMemoryFontSize)
                // 布局高度
                //setParentLayoutParams(parent, heightDp = 80, topMarginDp = 0)
                setParentLayoutParams(parent, heightDp = landsacpe_component_height, topMarginDp = 0)
            // 经典风格的布局
            }else if (displayStyle==1){
                // 字体大小
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, landsacpeMemoryFontSize2)
                // 布局高度
                setParentLayoutParams(parent, heightDp = landsacpe_component_height2, topMarginDp = 0)
            }

        // 竖屏
        } else {
            // 简约风格的布局
            if(displayStyle==0){
                //textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, portraitscreenMemoryFontSize)
                //setParentLayoutParams(parent, heightDp = 80, topMarginDp = 10)
                //setParentLayoutParams(parent, heightDp = 85, topMarginDp = 0)
                setParentLayoutParams(parent, heightDp = portraitscreen_component_height, topMarginDp = 0)
            // 经典风格的布局
            }else if (displayStyle==1){
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, portraitscreenMemoryFontSize2)
                setParentLayoutParams(parent, heightDp = portraitscreen_component_height2, topMarginDp = 0)
            }
        }
    }

    // 辅助方法：设置父布局高度和顶部边距
    private fun setParentLayoutParams(parent: View, heightDp: Int, topMarginDp: Int) {
        YLog.debug(tag = TAG, msg = "Setting parent layout params: height=$heightDp, topMargin=$topMarginDp")
        val params = parent.layoutParams ?: return
        val heightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            heightDp.toFloat(),
            parent.context.resources.displayMetrics
        ).toInt()
        params.height = heightPx

        if (params is ViewGroup.MarginLayoutParams) {
            val marginPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                topMarginDp.toFloat(),
                parent.context.resources.displayMetrics
            ).toInt()
            params.topMargin = marginPx
        }
        parent.layoutParams = params
    }

    //private fun updateTextColorBasedOnDarkMode(textView: TextView, context: Context?) {
    //    val isDark = context?.isSystemInDarkMode ?: false
    //    val textColor = if (isDark) Color.WHITE else Color.BLACK
    //    textView.setTextColor(textColor)
    //}
    private fun updateTextColorBasedOnDarkMode(textView: TextView, context: Context?) {
        val isDark = context?.isSystemInDarkMode ?: false

        val fontColorOption = Prefs.getInt(Pref.Key.SystemDesktop.MEMORY_FONT_COLOR_OPTION, 0)

        val textColor = when (fontColorOption) {
            1 -> {
                val colorKey = if (isDark)
                    Pref.Key.SystemDesktop.DARK_THEME_COLOR
                else
                    Pref.Key.SystemDesktop.LIGHT_THEME_COLOR
                Prefs.getInt(colorKey, if (isDark) Color.WHITE else Color.BLACK)
            }
            0 -> {
                if (isDark) Color.WHITE else Color.BLACK
            }
            else -> {
                if (isDark) Color.WHITE else Color.BLACK
            }
        }

        textView.setTextColor(textColor)
    }


    @SuppressLint("PrivateApi")
    private fun registerDarkReceiver(tempView: TextView, context: Context) {
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                        // 更新文字颜色
                        updateTextColorBasedOnDarkMode(tempView, context)

                        // 重新应用基于当前方向的布局
                        val rotation = context?.display?.rotation ?: 0
                        activityRef?.get()?.runOnUiThread {
                            // 在横屏模式下，切换进入最近任务界面的时候，它会导致布局异常，如果注释了这里就好了，这是为什么？
                            //applyLayoutForRotation(rotation)
                        }
                    }
                }
            }

            darkModeReceiver = receiver

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            }

            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

            // 初始化颜色
            updateTextColorBasedOnDarkMode(tempView, context)

            YLog.debug(tag = TAG, msg = "Dark mode receiver registered for view tag ${tempView.tag}")
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Error registering dark receiver: ${e.message}")
            tempView.setTextColor(Color.WHITE)
        }
    }

    private fun unregisterDarkReceiver(activity: Activity) {
        try {
            darkModeReceiver?.let {
                activity.unregisterReceiver(it)
                darkModeReceiver = null
                YLog.debug(tag = TAG, msg = "Dark mode receiver unregistered")
            }
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Error unregistering dark receiver: ${e.message}")
        }
    }

    private fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }

    private fun dpToPx(dp: Int, view: View): Int {
        return (dp * view.resources.displayMetrics.density).toInt()
    }

    fun updateMemoryText() {
        val textView = memoryTextView?.get() ?: return
        val am = activityManager ?: return

        // 根据显示风格选择格式化方法
        val text = when (displayStyle) {
            //0 -> getMemoryTextSimple(am)      // 简洁单行-手动计算
            0 -> getMemoryTextSimple(am, textView.context)      // 简洁单行
            //1 -> getMemoryTextMultiline(am)   // 三行详细-手动计算
            1 -> getMemoryTextMultiline(am,textView.context)   // 三行详细
            else -> getMemoryTextSimple(am)   // 默认简洁
        }

        activityRef?.get()?.runOnUiThread {
            textView.text = text
        }
    }

    // 简洁单行格式：可用GB/总GB
    private fun getMemoryTextSimple(am: ActivityManager): String {
        am.getMemoryInfo(memoryInfo)
        val availMem = memoryInfo.availMem
        val totalMem = memoryInfo.totalMem
        val divisor = 1024.0 * 1024 * 1024 // 1 GB
        val availGB = availMem / divisor
        val totalGB = totalMem / divisor
        return "%.2fGB/%.2fGB".format(availGB, totalGB)
    }

    private fun getMemoryTextSimple(am: ActivityManager, context: Context): String {
        am.getMemoryInfo(memoryInfo)
        val availStr = Formatter.formatFileSize(context, memoryInfo.availMem)
        val totalStr = Formatter.formatFileSize(context, memoryInfo.totalMem)
        return "$availStr/$totalStr"
    }

    // 三行详细格式：可用GB 可用%，已用GB 已用%，总GB
    //private fun getMemoryTextMultiline(am: ActivityManager): String {
    //    am.getMemoryInfo(memoryInfo)
    //    val availMem = memoryInfo.availMem
    //    val totalMem = memoryInfo.totalMem
    //    val usedMem = totalMem - availMem
    //
    //    val divisor = 1024.0 * 1024 * 1024 // 1 GB
    //
    //    val availGB = availMem / divisor
    //    val usedGB = usedMem / divisor
    //    val totalGB = totalMem / divisor
    //
    //    val availPercent = (availMem * 100.0 / totalMem).roundToInt()
    //    val usedPercent = 100 - availPercent
    //
    //    // 格式化：GB 值保留两位小数，百分比占 3 位右对齐并加百分号
    //    return """
    //        ${"%.2f".format(availGB)}GB ${"%3d%%".format(availPercent)}
    //        ${"%.2f".format(usedGB)}GB ${"%3d%%".format(usedPercent)}
    //        ${"%.2f".format(totalGB)}GB
    //    """.trimIndent()
    //}
/*    private fun getMemoryTextMultiline(am: ActivityManager): String {
        am.getMemoryInfo(memoryInfo)
        val availMem = memoryInfo.availMem
        val totalMem = memoryInfo.totalMem
        val usedMem = totalMem - availMem

        val divisor = 1024.0 * 1024 * 1024 // 1 GB

        val availGB = availMem / divisor
        val usedGB = usedMem / divisor
        val totalGB = totalMem / divisor

        val availPercent = (availMem * 100.0 / totalMem).roundToInt()
        val usedPercent = 100 - availPercent

        // 格式化：每行开头添加标题，GB值保留两位小数，百分比占3位右对齐并加百分号
        return """
        剩余内存 ${"%.2f".format(availGB)}GB ${"%3d%%".format(availPercent)}
        已用内存 ${"%.2f".format(usedGB)}GB ${"%3d%%".format(usedPercent)}
        总内存 ${"%.2f".format(totalGB)}GB
    """.trimIndent()
    }*/
    private fun getMemoryTextMultiline(am: ActivityManager): String {
        am.getMemoryInfo(memoryInfo)
        val availMem = memoryInfo.availMem
        val totalMem = memoryInfo.totalMem
        val usedMem = totalMem - availMem

        val divisor = 1024.0 * 1024 * 1024 // 1 GB

        val availGB = availMem / divisor
        val usedGB = usedMem / divisor
        val totalGB = totalMem / divisor

        val availPercent = (availMem * 100.0 / totalMem).roundToInt()
        val usedPercent = 100 - availPercent

        // 根据 memoryDisplayStyle 决定显示内容
        return when (memoryDisplayStyle) {
            0 -> {
                // 0 表示：剩余内存 + 已用内存 + 总内存
                """
                剩余内存 ${"%.2f".format(availGB)}GB ${"%3d%%".format(availPercent)}                已用内存 ${"%.2f".format(usedGB)}GB ${"%3d%%".format(usedPercent)}                总内存 ${"%.2f".format(totalGB)}GB
            """.trimIndent()
            }
            1 -> {
                // 1 表示：剩余内存 + 已用内存
                """
                剩余内存 ${"%.2f".format(availGB)}GB ${"%3d%%".format(availPercent)}                已用内存 ${"%.2f".format(usedGB)}GB ${"%3d%%".format(usedPercent)}            """.trimIndent()
            }
            2 -> {
                // 2 表示：剩余内存
                """
                剩余内存 ${"%.2f".format(availGB)}GB ${"%3d%%".format(availPercent)}            """.trimIndent()
            }
            else -> {
                // 默认：剩余内存 + 已用内存 + 总内存
                """
                剩余内存 ${"%.2f".format(availGB)}GB ${"%3d%%".format(availPercent)}                已用内存 ${"%.2f".format(usedGB)}GB ${"%3d%%".format(usedPercent)}                总内存 ${"%.2f".format(totalGB)}GB
            """.trimIndent()
            }
        }
    }
   /* private fun getMemoryTextMultiline(am: ActivityManager, context: Context): String {
        am.getMemoryInfo(memoryInfo)
        val availMem = memoryInfo.availMem
        val totalMem = memoryInfo.totalMem
        val usedMem = totalMem - availMem

        // 使用系统格式化工具自动转换单位（如 2.34 GB 或 512 MB）
        val availStr = Formatter.formatFileSize(context, availMem)
        val usedStr = Formatter.formatFileSize(context, usedMem)
        val totalStr = Formatter.formatFileSize(context, totalMem)

        val availPercent = (availMem * 100.0 / totalMem).roundToInt()
        val usedPercent = 100 - availPercent

        // 拼接三行文本，内存大小和百分比之间用空格分隔，百分比右对齐占3位
        return """
        剩余内存 $availStr ${"%3d%%".format(availPercent)}
        已用内存 $usedStr ${"%3d%%".format(usedPercent)}
        总内存 $totalStr
    """.trimIndent()
    }*/

    private fun getMemoryTextMultiline(am: ActivityManager, context: Context): String {
        am.getMemoryInfo(memoryInfo)
        val availMem = memoryInfo.availMem
        val totalMem = memoryInfo.totalMem
        val usedMem = totalMem - availMem

        // 使用系统格式化工具自动转换单位（如 2.34 GB 或 512 MB）
        val availStr = Formatter.formatFileSize(context, availMem)
        val usedStr = Formatter.formatFileSize(context, usedMem)
        val totalStr = Formatter.formatFileSize(context, totalMem)

        val availPercent = (availMem * 100.0 / totalMem).roundToInt()
        val usedPercent = 100 - availPercent

        // 根据 memoryDisplayStyle 决定显示内容
        return when (memoryDisplayStyle) {
            0 -> {
                // 0 表示：剩余内存 + 已用内存 + 总内存
                """
                剩余内存 $availStr ${"%3d%%".format(availPercent)}
                已用内存 $usedStr ${"%3d%%".format(usedPercent)}
                总内存 $totalStr
            """.trimIndent()
            }
            1 -> {
                // 1 表示：剩余内存 + 已用内存
                """
                剩余内存 $availStr ${"%3d%%".format(availPercent)}
                已用内存 $usedStr ${"%3d%%".format(usedPercent)}
            """.trimIndent()
            }
            2 -> {
                // 2 表示：剩余内存
                """
                剩余内存 $availStr ${"%3d%%".format(availPercent)}
            """.trimIndent()
            }
            else -> {
                // 默认：剩余内存 + 已用内存 + 总内存
                """
                剩余内存 $availStr ${"%3d%%".format(availPercent)}
                已用内存 $usedStr ${"%3d%%".format(usedPercent)}
                总内存 $totalStr
            """.trimIndent()
            }
        }
    }



    // 以下为可选的系统风格单行格式，若需要可取消注释并添加对应分支
    /*
    private fun getMemoryTextSystem(am: ActivityManager, context: Context): String {
        am.getMemoryInfo(memoryInfo)
        val availStr = Formatter.formatFileSize(context, memoryInfo.availMem)
        val totalStr = Formatter.formatFileSize(context, memoryInfo.totalMem)
        return "$availStr/$totalStr"
    }
    */
}