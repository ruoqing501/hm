package dev.lackluster.mihelper.hook.rules.systemui.batteryicon.nubia


import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.ArrayListClass
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.hook.compat.type.java.StringClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.BatteryIconPercentSwapHook
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarTemperatureHook
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.getResID
import dev.lackluster.mihelper.utils.factory.hasEnable
import dev.lackluster.mihelper.hook.compat.type.java.FloatType
object BatteryIconAdjuster : YukiBaseHooker() {
    private const val TAG = "BatteryIconAdjuster"

    private val battery_style by lazy {
        Prefs.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_STYLE, 0)
    }

    private val battery_percentage_symbol_style by lazy {
        Prefs.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_PERCENTAGE_SYMBOL_STYLE, 0)
    }

    private val status_bar_battery_layout_width by lazy {
        Prefs.getInt(Pref.Key.SystemUI.IconTurner.STATUS_BAR_BATTERY_LAYOUT_WIDTH, 30)
    }

    private val battery_color_switch by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.BATTERY_ICON_COLOR_SWITCH, false)
    }



    override fun onHook() {
        // 过滤AOD界面
//        when (battery_style) {
//            1, 4 -> loadHooker(BatteryIconPercentSwapHook) // 提前加载
//        }
        loadHooker(BatteryIconPercentSwapHook) // 提前加载
        if(battery_style == 1) {
            hideBatteryIconPercentage() // 隐藏电量百分比

        }
        if(battery_style == 2) {
            hideBatteryIconPercentage() // 隐藏电量百分比

        }
        if(battery_style == 4) {
            YLog.debug("$TAG Swapping battery icon and text")
            hideBatteryIconPercentage() // 隐藏电量百分比
        }

//        updateBatteryLayout()
          updateBatteryColor() // 根据系统，更新电量颜色
//        swapBatteryIconAndText() //用于显示 电池图标+文本
    }



    // 隐藏电量百分比
    private fun hideBatteryIconPercentage() {
//        hasEnable(Pref.Key.SystemUI.StatusBar.HIDE_BATTERY_PERCENTAGE_ICON) {
        if(battery_percentage_symbol_style==1){
            val bVLayout = "com.zte.mifavor.views.MFVBatteryViewLayout".toClassOrNull()
            val updateLevel = bVLayout?.method {
                name = "updateBatteryLevelText"
            }
            updateLevel?.hook {
                before {
                    // 仅处理目标区域 不包含AOD熄屏界面
                    if (!isTargetBatteryLayout(instance as View)) return@before
                    val mCurrentUsedBatteryLevelView =
                        bVLayout.getDeclaredField("mCurrentUsedBatteryLevelView")
                            .get(instance) as TextView
                    //typo? LOL
                    val mBatteryLevel =
                        bVLayout.getDeclaredField("mBateryLevel").getInt(instance)

                    mCurrentUsedBatteryLevelView.text = mBatteryLevel.toString()
                    result = null
                }
            }
        }

//        }
    }



    // 打印父级层级
    private fun printParentHierarchy(view: View, tag: String = "") {
        val sb = StringBuilder()
        var current: ViewParent? = view.parent
        while (current != null) {
            if (current is View) {
                sb.insert(0, "${current.javaClass.simpleName} -> ")
            } else {
                sb.insert(0, "${current.javaClass.simpleName} -> ")
            }
            current = current.parent
        }
        sb.append(view.javaClass.simpleName)
        YLog.debug("$TAG View hierarchy: $sb")
    }

    private fun updateBatteryColor() {
       val battery_style by lazy {
            Prefs.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_STYLE, 0)
        }
        if(battery_style !=0) return
        val batteryLayoutClass = "com.zte.mifavor.views.MFVBatteryViewLayout".toClassOrNull() ?: return
        batteryLayoutClass.method {
            name = "onDarkChanged"
            param(ArrayListClass, FloatType, IntType)
        }.hook {
            after {
                val instance = this.instance as  View
                val showMode = instance.current().field { name = "mShowMode" }.int()
                if (showMode == 0) {
                    // 获取外部文本视图
                    val outsideTextId = appContext !!.resources.getIdentifier(
                        "mfv_battery_level_outside",
                        "id",
                        appContext !!.packageName
                    )
                    val outsideTextView = instance.findViewById<TextView>(outsideTextId)
                    // 设置颜色为当前 tint（即 onDarkChanged 的第三个参数）
                    val tint = args[2] as Int
                    outsideTextView?.setTextColor(tint)
                    YLog.debug("$TAG Set battery icon color to $tint")
                }
            }
        }
    }

//    private fun updateBatteryLayout() {
//        val batteryLayoutClass = "com.zte.mifavor.views.MFVBatteryViewLayout".toClassOrNull() ?: return
//        batteryLayoutClass.method {
//            name = "updateBatteryLayout"
//            param(IntType, BooleanType)
//        }.hook {
//            before {
////                val args = this.args
////                when (battery_style) {
////                    0 -> args[0] = 1      // 强制数字在电池内
////                    1 -> args[0] = 2      // 文本+图标
////                    2 -> args[0] = 2      // 图标+文本
//////                    4 -> {
//////                        args[0] = 2      // 仅文本  //这个时候图标在电池的内部
//////                    }
////                    3 -> args[0] = 0      // 仅图标
////                    5 -> args[0] = 3      // 不显示电池图标加文本
////                }
//
////                if(!battery_color_switch){ //如果没有设置电池图标颜色-则调用方法，恢复默认的颜色
////                    batteryLayoutClass.method {
////                        name = "updateColor"
////                    }.get(instance).call()
////                    YLog.debug("$TAG Reset battery icon color")
////
////                }
//            }
//            after {
//                when (battery_style) {
////                    0->{
//////                        // 设置调用充电图标颜色。防止颜色异常
////                        batteryLayoutClass.method {
////                            name = "updateChargeViewColor"
////                        }.get( instance).call()
////                        // 更新电量值颜色
//////                        val updateBatteryLevelColor = batteryLayoutClass.method {
//////                            name = "updateBatteryLevelColor"
//////                        }.get(instance).call()
////
////                        // 得到电池的数值的视图，并根据该视图动态设置颜色
//////                        val sbView = this.instance as RelativeLayout
//////                       val outsideTextId by lazy {
//////                            appContext!!.resources.getIdentifier("mfv_battery_level_outside", "id", appContext!!.packageName)
//////                        }
//////                        // 获取 mfv_battery_level_outside 的资源 ID
//////
//////                        if (outsideTextId != 0) {
//////                            val outsideTextView = sbView.findViewById<TextView>(outsideTextId)
//////                            if (outsideTextView != null) {
//////                                // 调用 registerDarkReceiver 并传入该 TextView
//////                                registerDarkReceiver(outsideTextView)
//////                            } else {
//////                                YLog.debug("$TAG mfv_battery_level_outside not found in view")
//////                            }
//////                        } else {
//////                            YLog.debug("$TAG Resource ID for mfv_battery_level_outside not found")
//////                        }
////
////                    }
//
//
////                    2 ->{
////                        val textViewField = batteryLayoutClass.getDeclaredField("mCurrentUsedBatteryLevelView")
////                        textViewField.isAccessible = true
////                        val textView = textViewField.get(instance) as? TextView ?: return@after
////                        val textParams = textView.layoutParams as? RelativeLayout.LayoutParams ?: return@after
////                        val textBottom = textParams.bottomMargin
////                        textParams.bottomMargin = textBottom-12 // 调整电池文本和电池图标尽量贴合图标对齐
////
////                    }
//
//
////                    1 -> {
////
//////                          loadHooker(BatteryIconPercentSwapHook)
////////                        args[0] = 2 //强制类型改成2 文本+图标
//////                        val instance = this.instance as View
//////                        // 电池电量百分比：不显示数字（0） 显示在电池内（1） 显示在电池外（2） 不显示电池图标（3）
//////                        // 只处理 显示在电池外（2），
//////                        val showMode = instance.current().field { name = "mShowMode" }.int()
//////                        if (showMode != 2){
//////                            YLog.debug("$TAG Skipped battery icon and text swap for non-target view")
//////                            return@after
//////                        }
//////                        // 仅处理目标区域
//////                        if (!isTargetBatteryLayout(instance)) return@after
//////
//////                        // --- 原有的交换逻辑保持不变 ---
//////                        val containerField = batteryLayoutClass.getDeclaredField("mBatteryContainer")
//////                        containerField.isAccessible = true
//////                        val container = containerField.get(instance) as? ViewGroup ?: return@after
//////
//////                        val textViewField = batteryLayoutClass.getDeclaredField("mCurrentUsedBatteryLevelView")
//////                        textViewField.isAccessible = true
//////                        val textView = textViewField.get(instance) as? TextView ?: return@after
//////
//////                        val containerParams = container.layoutParams as? RelativeLayout.LayoutParams ?: return@after
//////                        val textParams = textView.layoutParams as? RelativeLayout.LayoutParams ?: return@after
//////
//////                        // 保存边距...
//////                        val containerLeft = containerParams.leftMargin
//////                        val containerRight = containerParams.rightMargin
//////                        val containerTop = containerParams.topMargin
//////                        val containerBottom = containerParams.bottomMargin
//////                        val textLeft = textParams.leftMargin
//////                        val textRight = textParams.rightMargin
//////                        val textTop = textParams.topMargin
//////                        val textBottom = textParams.bottomMargin
//////
////////                        // 移除所有规则
////////                        arrayOf(
////////                            RelativeLayout.ALIGN_PARENT_START, RelativeLayout.ALIGN_PARENT_END,
////////                            RelativeLayout.LEFT_OF, RelativeLayout.RIGHT_OF,
////////                            RelativeLayout.START_OF, RelativeLayout.END_OF,
////////                            RelativeLayout.CENTER_IN_PARENT, RelativeLayout.CENTER_HORIZONTAL
////////                        ).forEach { rule ->
////////                            containerParams.removeRule(rule)
////////                            textParams.removeRule(rule)
////////                        }
////////                        // 设置规则
////////                        containerParams.addRule(RelativeLayout.LEFT_OF, textView.id)
////////                        textParams.addRule(RelativeLayout.ALIGN_PARENT_END)
//////
//////                        // 移除所有规则
//////                        arrayOf(
//////                            RelativeLayout.ALIGN_PARENT_START, RelativeLayout.ALIGN_PARENT_END,
//////                            RelativeLayout.LEFT_OF, RelativeLayout.RIGHT_OF,
//////                            RelativeLayout.START_OF, RelativeLayout.END_OF,
//////                            RelativeLayout.CENTER_IN_PARENT, RelativeLayout.CENTER_HORIZONTAL,
//////                            RelativeLayout.CENTER_VERTICAL  // 添加垂直居中规则
//////                        ).forEach { rule ->
//////                            containerParams.removeRule(rule)
//////                            textParams.removeRule(rule)
//////                        }
//////                        // 设置规则 - 添加垂直居中对齐
//////                        containerParams.addRule(RelativeLayout.LEFT_OF, textView.id)
//////                        containerParams.addRule(RelativeLayout.CENTER_VERTICAL)  // 垂直居中
//////                        textParams.addRule(RelativeLayout.ALIGN_PARENT_END)
//////                        textParams.addRule(RelativeLayout.CENTER_VERTICAL)  // 垂直居中
//////
//////                        containerParams.leftMargin = containerLeft
//////                        containerParams.rightMargin = containerRight
//////                        containerParams.topMargin = containerTop
//////                        containerParams.bottomMargin = containerBottom
//////
//////                        textParams.leftMargin = textLeft
//////                        textParams.rightMargin = textRight
//////                        textParams.topMargin = textTop
////////                        textParams.bottomMargin = textBottom
//////                        textParams.bottomMargin = textBottom-12 // 调整电池文本和电池图标尽量贴合图标对齐
//////
//////                        container.layoutParams = containerParams
//////                        textView.layoutParams = textParams
//////                        // --- 交换逻辑结束 ---
//////
//////                        // ========== 新增：设置固定宽度 ==========
//////                        // 从Prefs读取配置的宽度（dp），若未设置则使用默认值40.0dp
//////                        // 如果不设置会导致其他的图标被挤出屏幕，导致不显示其他的图标
//////                        status_bar_battery_layout_width
//////                        val fixedWidthDp = status_bar_battery_layout_width
////////                        var fixedWidthDp = 40.0
////////                        if(battery_percentage_symbol_style==0){
////////                            fixedWidthDp = 45.0 //设置了开启百分比选项
////////                        }
//////                        val fixedWidthPx = android.util.TypedValue.applyDimension(
//////                            android.util.TypedValue.COMPLEX_UNIT_DIP,
//////                            fixedWidthDp.toFloat(),
//////                            instance.resources.displayMetrics
//////                        ).toInt()
//////
//////                        val batteryLayoutParams = instance.layoutParams
//////                        batteryLayoutParams.width = fixedWidthPx
//////                        // 如果父布局是LinearLayout且有weight，确保移除权重
//////                        if (batteryLayoutParams is android.widget.LinearLayout.LayoutParams) {
//////                            batteryLayoutParams.weight = 0f
//////                        }
//////
//////                        instance.layoutParams = batteryLayoutParams
//////
//////                        YLog.debug("$TAG Set battery layout fixed width to ${fixedWidthDp}dp")
////
//////                        val textViewField = batteryLayoutClass.getDeclaredField("mCurrentUsedBatteryLevelView")
//////                        textViewField.isAccessible = true
//////                        val textView = textViewField.get(instance) as? TextView ?: return@after
//////                        val textParams = textView.layoutParams as? RelativeLayout.LayoutParams ?: return@after
//////                        val textBottom = textParams.bottomMargin
//////                        textParams.bottomMargin = textBottom-12 // 调整电池文本和电池图标尽量贴合图标对齐
////                    }
////                    4 ->{
////                        // 隐藏电池图标
////                        // 仅处理目标区域 //不包含AOD熄屏界面
//////                        if (!isTargetBatteryLayout(instance as  View)) return@after
//////                        val containerField = batteryLayoutClass.getDeclaredField("mBatteryContainer")
//////                        containerField.isAccessible = true
//////                        val container = containerField.get(instance) as? ViewGroup
//////                        container?.visibility = View.GONE
//////
//////                        // 改成外部
////////                        // 充电图标显示在外部
//////                        val mOutsideChargeView = batteryLayoutClass.getDeclaredField("mOutsideChargeView").get(instance) as ImageView
//////
//////                        mOutsideChargeView.visibility = View.VISIBLE
//////
//////                        val mCurrentChargeView = batteryLayoutClass.getDeclaredField("mCurrentChargeView")
//////                        val chargeIndicator = ("com.zte.mifavor.views.ChargeIndicator").toClass()
//////
//////                        chargeIndicator.getDeclaredMethod(
//////                            "updateVisibility", BooleanType
//////                        ).invoke(mOutsideChargeView, true)
//////                        mCurrentChargeView.set(instance, mOutsideChargeView)
////
//////                        loadHooker(BatteryIconPercentSwapHook)
////
//////                        // 调整电池文本和电池图标尽量贴合图标对齐
//////                        val textViewField = batteryLayoutClass.getDeclaredField("mCurrentUsedBatteryLevelView")
//////                        textViewField.isAccessible = true
//////                        val textView = textViewField.get(instance) as? TextView ?: return@after
//////                        val textParams = textView.layoutParams as? RelativeLayout.LayoutParams ?: return@after
//////                        val textBottom = textParams.bottomMargin
//////                        textParams.bottomMargin = textBottom-12 // 调整电池文本和电池图标尽量贴合图标对齐
////                    }
//                }
////                // 设置调用充电图标颜色。防止颜色异常
////                batteryLayoutClass.method {
////                    name = "updateChargeViewColor"
////                }.get( instance).call()
////
////                if(!battery_color_switch){ //如果没有设置电池图标颜色-则调用方法，恢复默认的颜色
////                    batteryLayoutClass.method {
////                        name = "updateColor"
////                    }.get(instance).call()
////                    YLog.debug("$TAG Reset battery icon color")
////
////                }
//            }
//
//        }
//
//    }





    /**
     * 判断当前 MFVBatteryViewLayout 是否位于目标区域（状态栏、锁屏、控制中心头部）
     */
    private fun isTargetBatteryLayout(view: View): Boolean {
        var parent = view.parent
        while (parent != null) {
            val className = parent.javaClass.name
            if (className.contains("KeyguardStatusBarView") ||
                className.contains("PhoneStatusBarView") ||
                className.contains("CCHeaderView")) {
                return true
            }
            parent = parent.parent
        }
        return false
    }
}
