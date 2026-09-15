package dev.lackluster.redmagichelper.hook.rules.systemui.font


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.text.format.DateFormat.is24HourFormat
import android.view.View
import android.widget.TextView
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.rules.systemui.test.TextViewAnalyzer
import dev.lackluster.redmagichelper.utils.Prefs
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Date

object NubiaFont : YukiBaseHooker() {
    private const val TAG = "NubiaFont"
    private val lockScreenFonPath get() =
        Prefs.getString(Pref.Key.SystemUI.FontWeight.FONT_PATH, "/system/fonts/AndroidClock.ttf")

    private val lockScreenFontZoom get() =
        Prefs.getFloat(Pref.Key.SystemUI.FontWeight.LOCK_SCREEN_FONT_SIZE_ZOOM, -1f)


    override fun onHook() {
//        lockScreenClockFontHooker()
//        keyGuardShow()
        defaultLockScreenSetting()
        hookAllLockScreenClockFonts()
    }

    /** 锁屏时钟字体开关（回调内实时读取，切换即时生效） */
    private val lockScreenClockEnabled get() =
        Prefs.getBoolean(Pref.Key.SystemUI.FontWeight.LOCKSCREEN_CLOCK, false)


    //   private fun keyGuardShow(){
//        "com.android.systemui.keyguard.KeyguardViewMediator".toClass().method {
//            name = "updateActivityLockScreenState"
//            param(BooleanType,BooleanType, StringClass)
//        }.hook{
//            after {
//                // 为 false 的时候，不在打盹界面，进入锁屏界面，如果为 true 的时候，进入打盹模式
//                val isIntoLockScreenView =args[1] as?  Boolean
//                val lockScreenViewPrompt = "setupLocked - keyguard service enabled"
//                val matchString = args[2] as? String
//                if (isIntoLockScreenView == false || matchString?.contains(lockScreenViewPrompt) == true){
//                    lockScreenClockFontHooker()
//                }
//            }
//        }
//    }
    private fun defaultLockScreenSetting() {
        // 1. Hook LockScreenClockDefault.updateClockFont
        "com.zte.mifavor.keyguard.settings.LockScreenClockDefault".toClass().method {
            name = "updateClockFont"
            param("com.zte.mifavor.keyguard.personalclock.MyClockStyleModel\$StyleData".toClass())
        }.hook {
            after {
                if (!lockScreenClockEnabled) return@after
                try {
                    // 使用 Java 反射获取 mClockView 字段
                    val clazz = instance.javaClass
                    val clockViewField: Field = try {
                        clazz.getDeclaredField("mClockView")
                    } catch (e: NoSuchFieldException) {
                        // 如果当前类找不到，尝试从父类查找
                        clazz.superclass?.getDeclaredField("mClockView")
                            ?: throw NoSuchFieldException("mClockView field not found")
                    }
                    clockViewField.isAccessible = true
                    val clockView = clockViewField.get(instance) as? View ?: return@after

                    // 判断是否为 TextView 或其子类 (TextClock 继承自 TextView)
                    if (clockView !is TextView) return@after

                    // 设置自定义字体
                    clockView.typeface = Typeface.createFromFile(lockScreenFonPath)

                    // 如果设置了字体缩放
                    if (lockScreenFontZoom != -1f) {
                        val originalSize = clockView.textSize
                        clockView.setTextSize(
                            android.util.TypedValue.COMPLEX_UNIT_PX,
                            originalSize * lockScreenFontZoom
                        )
                    }

                    YLog.debug(tag = TAG, msg = "锁屏时钟字体设置成功")
                } catch (e: Exception) {
                    YLog.debug(tag = TAG, msg = "锁屏时钟字体设置失败：${e.message}")
                }
            }
        }
    }

    /**
     * Hook 所有锁屏时钟的 updateClockFont方法
     */
    private fun hookAllLockScreenClockFonts() {

// 锁屏时钟类的公共特征：updateClockFont(MyClockStyleModel.StyleData styleData)
            val lockScreenClockClasses = listOf(
                "com.zte.mifavor.keyguard.settings.LockScreenClockDefault",
                "com.zte.mifavor.keyguard.settings.LockScreenClockArtword",
//            "com.zte.mifavor.keyguard.settings.LockScreenClockClip",
                "com.zte.mifavor.keyguard.settings.LockScreenClockHorizen",
                "com.zte.mifavor.keyguard.settings.LockScreenClockVertical"
            )

            lockScreenClockClasses.forEach { className ->
                try {
                    val clazz = className.toClass()
                    clazz.method {
                        name = "updateClockFont"
                        param("com.zte.mifavor.keyguard.personalclock.MyClockStyleModel\$StyleData".toClass())
                    }.hook {
                        after {
                            if (!lockScreenClockEnabled) return@after
                            try {
                                applyLockScreenFontSettings(instance, className)
                            } catch (e: Exception) {
                                YLog.debug(
                                    tag = TAG,
                                    msg = "锁屏时钟 ${className} 字体设置失败：${e.message}"
                                )
                            }
                        }
                    }
                    YLog.debug(tag = TAG, msg = "成功 Hook 锁屏时钟：$className")
                } catch (e: Exception) {
                    YLog.debug(tag = TAG, msg = "找不到锁屏时钟类：$className, ${e.message}")
                }
            }


    }

    /**
     * 应用锁屏字体设置
     */
    private fun applyLockScreenFontSettings(clockInstance: Any, className: String) {
        val clazz = clockInstance.javaClass

        when (clazz.simpleName) {
            "LockScreenClockDefault" -> {
                try {
                    val clockViewField =
                        clazz.getDeclaredField("mClockView").apply { isAccessible = true }
                    val dateViewField =
                        clazz.getDeclaredField("mDateView").apply { isAccessible = true }

                    (clockViewField.get(clockInstance) as? TextView)?.let { clock ->
                        clock.typeface = Typeface.createFromFile(lockScreenFonPath)
                        if (lockScreenFontZoom != -1f) {
                            clock.setTextSize(
                                android.util.TypedValue.COMPLEX_UNIT_PX,
                                clock.textSize * lockScreenFontZoom
                            )
                        }
                    }

//                    (dateViewField.get(clockInstance) as? TextView)?.let { dateView ->
//                        dateView.typeface = Typeface.createFromFile(lockScreenFonPath)
//                        if (lockScreenFontZoom != -1f) {
//                            dateView.setTextSize(
//                                android.util.TypedValue.COMPLEX_UNIT_PX,
//                                dateView.textSize * lockScreenFontZoom
//                            )
//                        }
//                    }

                    YLog.debug(tag = TAG, msg = "默认锁屏时钟字体设置成功")
                } catch (e: Exception) {
                    YLog.debug(tag = TAG, msg = "默认锁屏时钟字段获取失败：${e.message}")
                }
            }

//            "LockScreenClockArtword" -> {
//                // Artword 类型使用 ImageView 显示时间，不需要设置字体
//                // 但可以设置日期视图
//                try {
//                    val dateViewField = clazz.getDeclaredField("mDateView").apply { isAccessible = true }
//                    (dateViewField.get(clockInstance) as? TextView)?.let { dateView ->
//                        dateView.typeface = Typeface.createFromFile(lockScreenFonPath)
//                        if (lockScreenFontZoom != -1f) {
//                            dateView.setTextSize(
//                                android.util.TypedValue.COMPLEX_UNIT_PX,
//                                dateView.textSize * lockScreenFontZoom
//                            )
//                        }
//                        YLog.debug(tag = TAG, msg = "Artword 锁屏时钟日期视图字体设置成功")
//                    }
//                } catch (e: Exception) {
//                    YLog.debug(tag = TAG, msg = "Artword 锁屏时钟字段获取失败：${e.message}")
//                }
//            }

            "LockScreenClockClip" -> {
                try {
//                    val fields = listOf("mClockHourView", "mClockMinuteView", "mClockMinuteMaskView", "mDateView")
                    val fields =
                        listOf("mClockHourView", "mClockMinuteView", "mClockMinuteMaskView")
                    fields.forEach { fieldName ->
                        try {
                            val field =
                                clazz.getDeclaredField(fieldName).apply { isAccessible = true }
                            (field.get(clockInstance) as? TextView)?.let { clock ->
                                clock.typeface = Typeface.createFromFile(lockScreenFonPath)
                                if (lockScreenFontZoom != -1f) {
                                    clock.setTextSize(
                                        android.util.TypedValue.COMPLEX_UNIT_PX,
                                        clock.textSize * lockScreenFontZoom
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略单个字段找不到的情况
                        }
                    }
                    YLog.debug(tag = TAG, msg = "Clip 锁屏时钟字体设置成功")
                } catch (e: Exception) {
                    YLog.debug(tag = TAG, msg = "Clip 锁屏时钟字段获取失败：${e.message}")
                }
            }

            "LockScreenClockHorizen" -> {
                try {
                    val fields = listOf("mClockHours", "mClockMinute", "mMonthDate", "mDayDate")
                    fields.forEach { fieldName ->
                        try {
                            val field =
                                clazz.getDeclaredField(fieldName).apply { isAccessible = true }
                            (field.get(clockInstance) as? TextView)?.let { clock ->
                                clock.typeface = Typeface.createFromFile(lockScreenFonPath)
                                if (lockScreenFontZoom != -1f) {
                                    clock.setTextSize(
                                        android.util.TypedValue.COMPLEX_UNIT_PX,
                                        clock.textSize * lockScreenFontZoom
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略单个字段找不到的情况
                        }
                    }
                    YLog.debug(tag = TAG, msg = "Horizen 锁屏时钟字体设置成功")
                } catch (e: Exception) {
                    YLog.debug(tag = TAG, msg = "Horizen 锁屏时钟字段获取失败：${e.message}")
                }
            }

            "LockScreenClockVertical" -> {
                try {
//                    val fields = listOf("mClockHours", "mClockMinute", "mDateView")
                    val fields = listOf("mClockHours", "mClockMinute")
                    fields.forEach { fieldName ->
                        try {
                            val field =
                                clazz.getDeclaredField(fieldName).apply { isAccessible = true }
                            (field.get(clockInstance) as? TextView)?.let { clock ->
                                clock.typeface = Typeface.createFromFile(lockScreenFonPath)
                                if (lockScreenFontZoom != -1f) {
                                    clock.setTextSize(
                                        android.util.TypedValue.COMPLEX_UNIT_PX,
                                        clock.textSize * lockScreenFontZoom
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略单个字段找不到的情况
                        }
                    }
                    YLog.debug(tag = TAG, msg = "Vertical 锁屏时钟字体设置成功")
                } catch (e: Exception) {
                    YLog.debug(tag = TAG, msg = "Vertical 锁屏时钟字段获取失败：${e.message}")
                }
            }
        }
    }

    private var lockScreenFound = false

    init {
        lockScreenFound = false
    }

    private fun lockScreenClockFontHooker() {
        // 检查 Prefs 是否开启
        if (!Prefs.getBoolean(Pref.Key.SystemUI.FontWeight.LOCKSCREEN_CLOCK, false)) {
            return
        }
        val textViewClass = "android.widget.TextView".toClass()
        textViewClass.method {
            name = "setText"
            param(CharSequence::class.java)
        }.hook {
            after {

                // 如果已经处理过，直接返回
                if (lockScreenFound) {
                    YLog.debug(tag = TAG, msg = "已处理过")
                    return@after
                }
                val textView = instance as? TextView ?: return@after


                val context = textView.context
                if (context.packageName != "com.android.systemui") return@after

                val resourceId = getResourceIdName(textView)
                val newText = args[0] as? String
                val currentText = textView.text?.toString()

                val isLockScreen = isLockScreenClock(textView)
                YLog.debug(
                    tag = TAG,
                    msg = "资源 ID: $resourceId, 文本：$newText, 当前文本：$currentText"
                )
                if (isLockScreen) {
                    lockScreenFound = true

                    try {
                        val originalSizePx = textView.textSize


                        val typeface = Typeface.createFromFile(lockScreenFonPath)
                        textView.typeface = typeface
//                            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, originalSizePx * 0.8f)
                        if (lockScreenFontZoom == -1f) return@after
                        textView.setTextSize(
                            android.util.TypedValue.COMPLEX_UNIT_PX,
                            originalSizePx * lockScreenFontZoom
                        )
                        YLog.debug(tag = TAG, msg = "字体设置成功")
                    } catch (e: Exception) {
                        YLog.debug(tag = TAG, msg = "字体设置失败：$e")
                    }
                }
            }
        }

    }


    /**
     * 根据视图获取其资源 ID 的字符串名称。
     * 参考状态栏时钟中的实现。
     */
    private fun getResourceIdName(view: View): String {
        return try {
            val resId = view.id
            if (resId <= 0) {
                "NO_ID"
            } else {
                try {
                    view.resources.getResourceEntryName(resId)
                } catch (e: Resources.NotFoundException) {
                    "0x${Integer.toHexString(resId)}"
                }
            }
        } catch (e: Exception) {
            "UNKNOWN_ID"
        }
    }

    /**
     * 判断给定视图是否为锁屏界面中的特定时钟视图（TextClock with ID clock_view）
     * 并逐级验证所有父视图的资源 ID 是否与期望的层级完全一致。
     *
     * @param view 要检查的视图（应为 TextClock 实例）
     * @return 如果视图及其所有父视图的 ID 与锁屏时钟层级完全匹配，则返回 true；否则 false
     */
    private fun isLockScreenClock(view: View): Boolean {
        try {
            // 1. 检查当前视图本身：必须是 TextClock 且 ID 为 clock_view
            if (view !is TextView) {
                YLog.debug(tag = TAG, msg = " 当前视图不是 TextView")
                return false
            }
            val viewIdName = getResourceIdName(view)
            if (viewIdName != "clock_view") {
                YLog.debug(tag = TAG, msg = "视图 ID 不匹配：$viewIdName, 期望：clock_view")
                return false
            }

            // 2. 逐级检查父视图 ID（从直接父容器开始向上）
            // 第 1 级父容器：clock_area
            val parent1 = view.parent
            if (parent1 !is View) return false
            val parent1Id = getResourceIdName(parent1)
            if (parent1Id != "clock_area") {
                YLog.debug(tag = TAG, msg = "父容器 1 ID 不匹配：$parent1Id, 期望：clock_area")
                return false
            }

            // 第 2 级父容器：depth_down_area
            val parent2 = parent1.parent
            if (parent2 !is View) return false
            val parent2Id = getResourceIdName(parent2)
            if (parent2Id != "depth_down_area") {
                YLog.debug(tag = TAG, msg = "父容器 2 ID 不匹配：$parent2Id, 期望：depth_down_area")
                return false
            }

            // 第 3 级父容器：ConstraintLayout（通常无 ID，期望为 NO_ID）
            val parent3 = parent2.parent
            if (parent3 !is View) return false
            val parent3Id = getResourceIdName(parent3)
            if (parent3Id != "NO_ID" && parent3Id != "UNKNOWN_ID") {
                YLog.debug(tag = TAG, msg = "父容器 3 ID 不匹配：$parent3Id, 期望无 ID")
                return false
            }

            // 第 4 级父容器：keyguard_clock_style_default
            val parent4 = parent3.parent
            if (parent4 !is View) return false
            val parent4Id = getResourceIdName(parent4)
            if (parent4Id != "keyguard_clock_style_default") {
                YLog.debug(
                    tag = TAG,
                    msg = "父容器 4 ID 不匹配：$parent4Id, 期望：keyguard_clock_style_default"
                )
                return false
            }

            // 第 5 级父容器：keyguard_clock_container
            val parent5 = parent4.parent
            if (parent5 !is View) return false
            val parent5Id = getResourceIdName(parent5)
            if (parent5Id != "keyguard_clock_container") {
                YLog.debug(
                    tag = TAG,
                    msg = "父容器 5 ID 不匹配：$parent5Id, 期望：keyguard_clock_container"
                )
                return false
            }

            // 第 6 级父容器：status_view_container
            val parent6 = parent5.parent
            if (parent6 !is View) return false
            val parent6Id = getResourceIdName(parent6)
            if (parent6Id != "status_view_container") {
                YLog.debug(
                    tag = TAG,
                    msg = "父容器 6 ID 不匹配：$parent6Id, 期望：status_view_container"
                )
                return false
            }

            // 第 7 级父容器：screen_on_area
            val parent7 = parent6.parent
            if (parent7 !is View) return false
            val parent7Id = getResourceIdName(parent7)
            if (parent7Id != "screen_on_area") {
                YLog.debug(tag = TAG, msg = "父容器 7 ID 不匹配：$parent7Id, 期望：screen_on_area")
                return false
            }

            // 第 8 级父容器：keyguard_status_view
            val parent8 = parent7.parent
            if (parent8 !is View) return false
            val parent8Id = getResourceIdName(parent8)
            if (parent8Id != "keyguard_status_view") {
                YLog.debug(
                    tag = TAG,
                    msg = "父容器 8 ID 不匹配：$parent8Id, 期望：keyguard_status_view"
                )
                return false
            }

            // 第 9 级父容器：notification_panel
            val parent9 = parent8.parent
            if (parent9 !is View) return false
            val parent9Id = getResourceIdName(parent9)
            if (parent9Id != "notification_panel") {
                YLog.debug(
                    tag = TAG,
                    msg = "父容器 9 ID 不匹配：$parent9Id, 期望：notification_panel"
                )
                return false
            }

            // 如果所有层级都匹配
            YLog.debug(tag = TAG, msg = "找到锁屏时钟，视图层级验证通过")
            return true

        } catch (e: Exception) {
            YLog.debug(tag = TAG, msg = "检查锁屏时钟时出错：${e.message}")
            return false
        }
    }
}
