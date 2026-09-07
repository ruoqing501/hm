package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.children
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.hook.rules.systemui.batteryicon.nubia.BatteryIconAdjuster
import dev.lackluster.mihelper.utils.Prefs

object BatteryIconPercentSwapHook : YukiBaseHooker() {
    private const val TAG = "BatteryIconPercentSwapHook"

    // 使用更可靠的方式获取资源ID
    private val BATTERY_LAYOUT_ID by lazy {
        appContext?.resources?.getIdentifier("mfv_battery_layout", "id", appContext?.packageName)
            ?: 0
    }

    private val BATTERY_LEVEL_OUTSIDE_ID by lazy {
        appContext?.resources?.getIdentifier("battery_level_outside", "id", appContext?.packageName)
            ?: 0
    }

    private val BATTERY_CONTAINER_ID by lazy {
        appContext?.resources?.getIdentifier("battery_container", "id", appContext?.packageName)
            ?: 0
    }

    private val BATTERY_ICON_VIEW_ID by lazy {
        appContext?.resources?.getIdentifier("mfv_battery_icon", "id", appContext?.packageName) ?: 0
    }
    private val battery_style by lazy {
        Prefs.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_STYLE, 0)
    }

    /**
     * 判断当前 MFVBatteryViewLayout 是否位于目标区域（状态栏、锁屏、控制中心头部）
     */
    private fun isTargetBatteryLayout(view: View): Boolean {
        var parent = view.parent
        while (parent != null) {
            val className = parent.javaClass.name
            if (className.contains("KeyguardStatusBarView") ||
                className.contains("PhoneStatusBarView") ||
                className.contains("CCHeaderView")
            ) {
                return true
            }
            parent = parent.parent
        }
        return false
    }

    override fun onHook() {
        // 拦截系统设置中的选项
        "com.zte.mifavor.views.MFVBatteryViewLayout".toClass().method {
            name = "getLevelDisplayMode"
            returnType = IntType
        }.hook {
            after {
                // 拦截系统设置中的选项
                val original = result as? Int
                val batteryStyle = Prefs.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_STYLE, 0)
                val newMode = when (batteryStyle) {
                    0 -> 1      // 数字在电池内
                    1 -> 2      // 数字+图标（外部）
                    2 -> 2      // 先强行设置成系统设置的第2个选择，数字+图标    为后续->图标+文本做准备
                    3 -> 0      // 仅图标
                    4 -> 2      // 先强行设置成系统设置的第2个选择，数字+图标    为后续  仅数字做准备
                    5 -> 3      // 隐藏整个电池布局
                    else -> original
                }
                YLog.debug(tag = TAG, msg = "getLevelDisplayMode: $original -> $newMode")
                result = newMode
            }
        }
        "com.zte.mifavor.views.MFVBatteryViewLayout".toClass().method {
            name = "updateBatteryLayout"
            param(IntType, BooleanType)
        }.hook {
            after {
                val batteryLayout = instance as ViewGroup
                if (!isTargetBatteryLayout(batteryLayout)) return@after
                // 根据自己的app的选项，如果用户选择了第1个选项，则交换图标和数字，
                // 因为在调用updateBatteryLayout之前，它的第1个参数就是通过getLevelDisplayMode调用的返回值，
                // 我们getLevelDisplayMode中拦截了系统设置中的选项，如果在当前自己app中选择了第1个选项，则自动选择系统设置中的第2个选项（数字+图标）
                // 现在当前的视图就是数字+图标，所以需要交换图标和数字,这里默认的充电图标在电池内部

                // 当自己的app中选择了第4项（仅数字）
                // 因为在调用updateBatteryLayout之前，它的第1个参数就是通过getLevelDisplayMode调用的返回值，
                // 我们getLevelDisplayMode中拦截了系统设置中的选项，如果在当前自己app中选择了第4个选项，则自动选择系统设置中的第2个选项（数字+图标,充电图标在电池内部）
                // 现在当前的视图就是数字+图标，所以隐藏电池图标，并且让充电图标显示在外部
                when (Prefs.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_STYLE, 0)) {
                    1 -> swapBatteryIconAndPercent(batteryLayout)
                    4 -> {
                        hideBatteryIconIfNeeded(batteryLayout)      // 隐藏电池图标容器
                        batteryChargeIconOutside(batteryLayout)     // 将充电图标移到外部
                    }

                }
            }
        }


    }
    private fun batteryChargeIconOutside(layout: ViewGroup) {
        val batteryLayoutClass = "com.zte.mifavor.views.MFVBatteryViewLayout".toClass()

        try {
            // 从 layout 获取 mOutsideChargeView 字段（ChargeIndicator 类型）
            val mOutsideChargeView =
                batteryLayoutClass.field { name = "mOutsideChargeView" }.get(layout)
                    .cast<ImageView>() ?: return

            // 显示外部充电图标
            mOutsideChargeView.visibility = View.VISIBLE

            // 调用 ChargeIndicator.updateVisibility 方法 - 直接在对象上调用
            mOutsideChargeView.current().method {
                name = "updateVisibility"
                param(BooleanType)
            }.call(true)

            // 将当前充电视图片设置为外部可见 - 设置 layout 的 mCurrentChargeView 字段
            layout.current().field { name = "mCurrentChargeView" }.set(mOutsideChargeView)

            // 设置调用充电图标颜色。防止颜色异常 - 在 layout 上调用方法
            layout.current().method {
                name = "updateChargeViewColor"
            }.call()

            YLog.debug(tag = TAG, msg = "Battery charge icon moved to outside")
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Error moving charge icon outside: ${e.message}")
        }
    }

    // 新增：根据配置隐藏电池图标，只显示百分比
    private fun hideBatteryIconIfNeeded(layout: ViewGroup) {
        val battery_style by lazy {
            Prefs.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_STYLE, 0)
        }
        if (battery_style != 4) return

        YLog.debug(tag = TAG, msg = "Hiding battery icon")

        val container = findBatteryContainer(layout) ?: run {
            YLog.debug(tag = TAG, msg = "Battery container not found for hiding")
            return
        }

        // 隐藏电池图标容器
        if (container.visibility != View.GONE) {
            container.visibility = View.GONE
            YLog.debug(tag = TAG, msg = "Battery icon container hidden")
        }

        // 调整百分比视图的位置，使其靠左对齐（可根据需要调整）
        val levelView = findBatteryLevelView(layout)
        if (levelView != null) {
            val params = levelView.layoutParams as? RelativeLayout.LayoutParams
            if (params != null) {
                // 清除可能依赖于图标的规则
                params.removeRule(RelativeLayout.END_OF)
                params.removeRule(RelativeLayout.START_OF)
                // 设置为与父布局开始对齐，使百分比显示在最左侧
                params.addRule(RelativeLayout.ALIGN_PARENT_START)
                levelView.layoutParams = params
                YLog.debug(tag = TAG, msg = "Battery level view alignment adjusted")
            }
        }

        layout.requestLayout()
    }

    private fun swapBatteryIconAndPercent(layout: ViewGroup) {
        val battery_style by lazy {
            Prefs.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_STYLE, 0)
        }
        if (battery_style != 1) return
        val levelOutside = findBatteryLevelView(layout)
        val container = findBatteryContainer(layout)

        if (levelOutside == null || container == null) {
            YLog.debug(tag = TAG, msg = "Required views not found")
            return
        }

        try {
            val children = layout.children.toList()
            val indexLevel = children.indexOf(levelOutside)
            val indexContainer = children.indexOf(container)
            val childCount = children.size

            // 安全检查
            if (indexLevel < 0 || indexContainer < 0) {
                YLog.error(tag = TAG, msg = "Views not direct children")
                return
            }

            YLog.debug(
                tag = TAG,
                msg = "Index - Level: $indexLevel, Container: $indexContainer, Count: $childCount"
            )

            // 使用 RelativeLayout 的规则来交换顺序
            if (indexContainer < indexLevel) {
                // 当前顺序: [图标][百分比] -> 想要 [百分比][图标]
                setRelativeOrder(layout, container, levelOutside, false)
            } else if (indexContainer > indexLevel) {
                // 当前顺序: [百分比][图标] -> 想要 [图标][百分比]
                setRelativeOrder(layout, container, levelOutside, true)
            }
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Error swapping: ${e.message}")
        }
    }

    private fun setRelativeOrder(
        layout: ViewGroup,
        container: View,
        levelView: View,
        iconFirst: Boolean
    ) {
        try {
            val containerParams = container.layoutParams as? RelativeLayout.LayoutParams
            val levelParams = levelView.layoutParams as? RelativeLayout.LayoutParams
            // 是否双排
//            val isStatusBarDual =
//                Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW, false)
//            if (isStatusBarDual) {
//                levelParams?.bottomMargin -= 12 // 如果开启了双排显示，则调整电池文本和电池图标尽量贴合图标对齐
//            }

            if (containerParams != null && levelParams != null) {
                // 清除现有规则
                containerParams.removeRule(RelativeLayout.END_OF)
                containerParams.removeRule(RelativeLayout.START_OF)
                containerParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
                containerParams.removeRule(RelativeLayout.ALIGN_PARENT_END)

                levelParams.removeRule(RelativeLayout.END_OF)
                levelParams.removeRule(RelativeLayout.START_OF)
                levelParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
                levelParams.removeRule(RelativeLayout.ALIGN_PARENT_END)

                if (iconFirst) {
                    // 图标在左，百分比在右
                    containerParams.addRule(RelativeLayout.ALIGN_PARENT_START)
                    levelParams.addRule(RelativeLayout.END_OF, container.id)
                    YLog.debug(tag = TAG, msg = "Set order: icon first")
                } else {
                    // 百分比在左，图标在右
                    levelParams.addRule(RelativeLayout.ALIGN_PARENT_START)
                    containerParams.addRule(RelativeLayout.END_OF, levelView.id)
                    YLog.debug(tag = TAG, msg = "Set order: percent first")
                }

                container.layoutParams = containerParams
                levelView.layoutParams = levelParams
                layout.requestLayout()
            }
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Error setting order: ${e.message}")
        }
    }

    private fun findBatteryLevelView(layout: ViewGroup): View? {
        // 尝试通过ID查找
        if (BATTERY_LEVEL_OUTSIDE_ID != 0) {
            layout.findViewById<View>(BATTERY_LEVEL_OUTSIDE_ID)?.let { return it }
        }

        // 通过类型查找
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child.javaClass.simpleName.contains("MFVBatteryLevelView")) {
                return child
            }
        }

        return null
    }

    private fun findBatteryContainer(layout: ViewGroup): View? {
        // 尝试通过ID查找
        if (BATTERY_CONTAINER_ID != 0) {
            layout.findViewById<View>(BATTERY_CONTAINER_ID)?.let { return it }
        }

        // 查找 FrameLayout 作为容器
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is FrameLayout && child.id == BATTERY_CONTAINER_ID) {
                return child
            }
        }

        // 查找包含 BatteryIconView 的容器
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is ViewGroup) {
                for (j in 0 until child.childCount) {
                    val grandChild = child.getChildAt(j)
                    if (grandChild.javaClass.simpleName.contains("MFVBatteryMeterView")) {
                        return child
                    }
                }
            }
        }

        return null
    }

}