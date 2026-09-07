package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

@SuppressLint("DiscouragedApi")
object DualRowsStatusBarHook : YukiBaseHooker() {
    private const val TAG = "DualRowsStatusBarHook"

    // 时间对齐方式
    private val statusBarHorizontalAlignment = Prefs.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_HORIZONTAL_ALIGNMENT, 0)
    // 是否双排
    private val isStatusBarDual = Prefs.getBoolean( Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW, false)
    // 是否时钟独自一行显示
    private val isDualClockAcross = Prefs.getBoolean( Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_CLOCK_ACROSS, false)
    // 左侧双排
    private val isLeftSide = Prefs.getBoolean( Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW_LEFT, false)
    // 右侧双排
    private val isRightSide = Prefs.getBoolean( Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW_RIGHT, false)

    private val statusBarHeight  = Prefs.getFloat( Pref.Key.SystemUI.StatusBar.STATUS_BAR_HEIGHT, -1.0f)
    private val status_bar_system_icon_height by lazy {
        Prefs.getFloat(Pref.Key.SystemUI.StatusBar.STATUS_BAR_SYSTEM_ICON_HEIGHT, -1.0f)
    }

    private val status_bar_left_container_top_margin by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_LEFT_CONTAINER_TOP_MARGIN, -1)
    }
    private val status_bar_left_container_down_margin by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_LEFT_CONTAINER_DOWN_MARGIN, -1)
    }
    private val status_bar_left_container_left_margin by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_LEFT_CONTAINER_LEFT_MARGIN, -1)
    }

    private val status_bar_left_container_right_margin by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_LEFT_CONTAINER_RIGHT_MARGIN, -1)
    }

    private val status_bar_right_container_top_margin by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_RIGHT_CONTAINER_TOP_MARGIN, -1)
    }
    private val status_bar_right_container_down_margin by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_RIGHT_CONTAINER_DOWN_MARGIN, -1)
    }
    private val status_bar_right_container_left_margin by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_RIGHT_CONTAINER_LEFT_MARGIN, -1)
    }

    private val status_bar_right_container_right_margin by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_RIGHT_CONTAINER_RIGHT_MARGIN, -1)
    }



    // 设备温度位置  0 左侧容器 1 右侧容器
    private val status_bar_temperature_location by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_TEMPERATURE_LOCATION, 0)
    }

    // 电池信息-> 0 左侧容器 1 右侧容器

    private val status_bar_battery_info_location by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_BATTERY_INFO_LOCATION, 0)
    }






    // 电池信息-值为false的时候，则为单排模式，值为true的时候，则为双排模式
    private val status_bar_display_battery_info_layout by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_LAYOUT, false)
    }
    // 状态栏布局总开关
    private val enableStatusBarLayout by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_LAYOUT_SWITCH,false)
    }

    // 使用整数作为标记键（避免与系统资源 ID 冲突，使用大于 0x00FFFFFF 的值或自定义范围）
    private val TAG_MODIFIED_LEFT = 0x7F000001
    private val TAG_MODIFIED_RIGHT = 0x7F000002

    // 新增标记常量，用于避免重复交换
    private val TAG_BATTERY_SWAPPED = 0x7F000003


    // 从MonitorDeviceInfoHook 中获取的 Id.根据这个id来获取
    private val TAG_BATTERY_TEMP_CONTAINER = 0x7F100005   // 新增：设备温度容器标签（默认单排）


    private val TAG_BATTERY_INFO_CONTAINER = 0x7F100006   // 新增：单排，电池信息容器

    private val TAG_BATTERY_INFO_CONTAINER_DUAL = 0x7F100007 // 新增：双排，电池信息容器



    // 顶部状态栏容器 类型是 LinearLayout
    private val statusBarContents by lazy {
        appContext!!.resources.getIdentifier("status_bar_contents", "id", appContext!!.packageName)
    }

    // 挖孔空间视图 ID
    private val cutoutSpaceId by lazy {
        appContext!!.resources.getIdentifier("cutout_space_view", "id", appContext!!.packageName)
    }

   // 是否双排显示
    private val enable = Prefs.getBoolean("enable_status_dual", false)

    // 时钟独自一行显示
//    private val isDualClock by lazy {
//        Prefs.getBoolean("isDualClock", false)
//    }

    // 左右布局互换位置
    private val isSwap by lazy {
        Prefs.getBoolean("isSwap", false)
    }

    // 时间是否居中显示
    private val isClockCenter by lazy {
        Prefs.getBoolean("isClockCenter", false)
    }

    // 左侧相关 ID
    /*
        status_bar_start_side_container (FrameLayout) - 顶级父容器
    └── status_bar_start_side_content (FrameLayout) - 子容器
        └── status_bar_start_side_except_heads_up (LinearLayout) - 时钟+通知布局
            ├── clock (TextView) - 时钟视图
            └── notification_icon_area (FrameLayout) - 通知图标区域
    */
    private val statusBarStartSideContainerId by lazy {
        appContext!!.resources.getIdentifier(
            "status_bar_start_side_container",
            "id",
            appContext!!.packageName
        )
    }
    private val statusBarStartSideContentId by lazy {
        appContext!!.resources.getIdentifier(
            "status_bar_start_side_content",
            "id",
            appContext!!.packageName
        )
    }
    private val statusBarStartSideExceptHeadsUpId by lazy {
        appContext!!.resources.getIdentifier(
            "status_bar_start_side_except_heads_up",
            "id",
            appContext!!.packageName
        )
    }
    private val clockId by lazy {
        appContext!!.resources.getIdentifier("clock", "id", appContext!!.packageName)
    }
    private val notificationIconAreaId by lazy {
        appContext!!.resources.getIdentifier(
            "notification_icon_area",
            "id",
            appContext!!.packageName
        )
    }
    // LinearLayout 左侧图标区域
    private val statusLeftSystemIcon by lazy {
        appContext!!.resources.getIdentifier(
            "red_magic_function_icon_container",
            "id",
            appContext!!.packageName
        )
    }

    // 右侧相关 ID
    /*
        status_bar_end_side_container (FrameLayout) - 顶级父容器
    └── status_bar_end_side_content (LinearLayout) - 子容器
        └── system_icons (LinearLayout) - 系统图标容器
            ├── statusIcons (LinearLayout) - 状态图标（不含电池）
            └── battery (LinearLayout) - 电池图标
    */
    private val statusBarEndSideContainerId by lazy {
        appContext!!.resources.getIdentifier(
            "status_bar_end_side_container",
            "id",
            appContext!!.packageName
        )
    }
    private val statusBarEndSideContentId by lazy {
        appContext!!.resources.getIdentifier(
            "status_bar_end_side_content",
            "id",
            appContext!!.packageName
        )
    }
    private val systemIconsId by lazy {
        appContext!!.resources.getIdentifier("system_icons", "id", appContext!!.packageName)
    }
    private val statusIconsId by lazy {
        appContext!!.resources.getIdentifier("statusIcons", "id", appContext!!.packageName)
    }
    private val batteryId by lazy {
        appContext!!.resources.getIdentifier("battery", "id", appContext!!.packageName)
    }

    // 是batteryId的子类，类型为RelativeLayout  MFVBatteryViewLayout
    private val batterySubId by lazy {
        appContext!!.resources.getIdentifier("mfv_battery_layout", "id", appContext!!.packageName)
    }

    // 是batterySubId的子类 MFVBatteryLevelView 类型为TextView 电量显示
    private val batterySubLevelId by lazy {
        appContext!!.resources.getIdentifier(
            "mfv_battery_level_outside",
            "id",
            appContext!!.packageName
        )
    }

    // 是batterySubId的子类 类型为FrameLayout
    private val batterySubBatteryIconId by lazy {
        appContext!!.resources.getIdentifier(
            "mfv_battery_container",
            "id",
            appContext!!.packageName
        )
    }


    override fun onHook() {
        "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass().method {
            name = "onFinishInflate"
        }.hook {
            after {

                val sbView = this.instance as FrameLayout

                // 如果用户设置隐藏状态栏，直接隐藏并跳过后续所有调整
                hasEnable(Pref.Key.SystemUI.StatusBar.HOME_RECENT_HIDE_STATUS_BAR){
                    sbView.visibility = View.GONE
                    YLog.debug(tag = TAG, msg = "Status bar hidden by user preference")
                    return@hasEnable
                }

                if (statusBarHorizontalAlignment ==1){
                    timeLeft(sbView)
                }
                // 如果需要时钟居中，则执行居中操作
                if (statusBarHorizontalAlignment == 2) {
                    timeCenter(sbView)
                }

                if (statusBarHorizontalAlignment == 3) {
                    timeRight(sbView)
                }
                // 检测是否需要双排显示
                if (!isStatusBarDual) return@after
                YLog.debug(tag = TAG, msg = "Starting dual rows status bar hook")

                try {
                    if(isLeftSide){
                        setupLeftDualRowsLayout(sbView)

                    }
                    // 开启了右侧双排，并且显示设备温度容器在右边，则将设备温度显示在双排布局的电池左侧
//                    if(isRightSide && status_bar_temperature_location ==1){
//                        setupRightDualRowsLayout(sbView)
//                    }else if(isRightSide && status_bar_temperature_location !=1){
//                        setupRightDualRowsLayout2(sbView)
//                    }

//                    if (isRightSide) {
//                        setupRightDualRowsLayout(sbView)  // 始终使用完整版布局
//                    }
                    if (isRightSide && (status_bar_temperature_location == 1 || (status_bar_battery_info_location == 1))) {
                        setupRightDualRowsLayout(sbView)
                    } else if (isRightSide) {
                        setupRightDualRowsLayout2(sbView)
                    }



//                    // 如果需要时钟居中，则执行居中操作
//                    if (isClockCenter) {
//                        timeCenter(sbView)
//                    }
                    // 状态栏布局
                    if(enableStatusBarLayout){
                        adjustSideContainersMargin(sbView)
                    }
                } catch (e: Exception) {
                    YLog.error(tag = TAG, msg = "Failed to setup dual rows layout: ${e.message}")
                }
            }
        }


//        val phoneStatusBarViewClz ="com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass()
//        // 再 hook updateStatusBarHeight 修改高度
//        phoneStatusBarViewClz.method {
//            name = "updateStatusBarHeight"
//        }.hook {
//            after {
//                val sbView = instance as FrameLayout
//                // 打印视图信息
//                printViewInfo(sbView)
//
//            }
//        }
        "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass().method {
            name = "updateStatusBarHeight"
        }.hook {
            after {
                val sbView = instance as FrameLayout
                // 5. 打印信息
//                printViewInfo(sbView)
            }
        }
        if(enableStatusBarLayout){
            updateStatusBarHeight() //更新状态栏高度
            updateSystemIconsContainerHeight() // 更新右侧系统图标容器宽度
        }
    }

    private fun updateSystemIconsContainerHeight() {
                "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass().method {
            name = "updateSystemIconsContainerHeight"
        }.hook {
            after {
                if(status_bar_system_icon_height ==-1.0f) {
                    YLog.debug(tag = TAG, msg = "status_bar_system_icon_height use system default")
                    return@after
                }
                val sbView = instance as FrameLayout

                // 找到 system_icons 视图
                val systemIconsId = appContext?.resources?.getIdentifier("system_icons", "id", appContext!!.packageName) ?: 0
                if (systemIconsId == 0) return@after
                val systemIcons = sbView.findViewById<View>(systemIconsId) ?: return@after
                // 获取原始高度（像素）
                val originalHeightPx = systemIcons.layoutParams?.height ?: return@after
                val metrics = sbView.resources.displayMetrics
                val originalHeightSp = originalHeightPx / metrics.scaledDensity

                // 打印原始高度
                YLog.debug(tag = TAG, msg = "Original system_icons height: ${originalHeightPx}px (${"%.2f".format(originalHeightSp)}sp)")

                // 获取自定义高度（单位 dp，可从 Preference 读取）
//                val customHeightDp = Prefs.getInt("custom_system_icons_height", 64) // 默认 48dp
                val customHeightDp = status_bar_system_icon_height// 默认 48dp
                val customHeightPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    customHeightDp.toFloat(),
                    sbView.resources.displayMetrics
                ).toInt()

                // 修改高度
                val lp = systemIcons.layoutParams
                if (lp != null && lp.height != customHeightPx) {
                    lp.height = customHeightPx
                    systemIcons.layoutParams = lp
                }

                // 可选：打印日志
                YLog.debug(tag = TAG, msg = "system_icons height set to $customHeightPx px")
            }
        }
    }

    private fun updateStatusBarHeight(){
        "com.android.internal.policy.SystemBarUtils".toClass().method {
            name = "getStatusBarHeight"
            param(Context::class.java)
        }.hook {
            after {
                val original = result<Int>() ?: return@after
                val context = args[0] as Context
                val metrics = context.resources.displayMetrics
                val originalDp = original / metrics.density
                YLog.debug(tag = TAG, msg = "Original status bar height: ${original}px (${String.format("%.2f", originalDp)}dp), density=${metrics.density}")

                if (statusBarHeight == -1.0f) {
                    YLog.debug(tag = TAG, msg = "statusBarHeight not set, using original status bar height")
                    return@after
                }
                YLog.debug(tag = TAG, msg = "Custom status bar height: $statusBarHeight dp")
                val customHeightDp = statusBarHeight
                val customHeightPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    customHeightDp.toFloat(),
                    metrics
                ).toInt()
                result = customHeightPx
            }
        }
    }
    /**
     * 调整左侧容器（status_bar_start_side_container）和右侧容器（status_bar_end_side_container）的外边距。
     * 从 Prefs 中读取对应的键值，单位 dp，分别设置四个方向的 margin。
     */
    /**
     * 调整左侧容器（status_bar_start_side_container）和右侧容器（status_bar_end_side_container）的外边距。
     * 从 Prefs 中读取对应的键值，单位 dp，分别设置四个方向的 margin。
     * 注意：配置值为 -1 时表示使用系统默认值，不进行修改；配置值为 0 时正常设置为 0。
     */
    private fun adjustSideContainersMargin(statusBarView: FrameLayout) {
        val leftContainer = statusBarView.findViewById<ViewGroup>(statusBarStartSideContainerId) ?: run {
            YLog.warn(tag = TAG, msg = "Left container not found for margin adjustment")
            return
        }
        val rightContainer = statusBarView.findViewById<ViewGroup>(statusBarEndSideContainerId) ?: run {
            YLog.warn(tag = TAG, msg = "Right container not found for margin adjustment")
            return
        }

        val metrics = statusBarView.resources.displayMetrics

        // 处理左侧容器 margin
        val leftLp = leftContainer.layoutParams as? ViewGroup.MarginLayoutParams
        if (leftLp != null) {
            // 仅当配置值不为 -1 时才修改对应方向的 margin
            if (status_bar_left_container_top_margin != -1) {
                leftLp.topMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    status_bar_left_container_top_margin.toFloat(),
                    metrics
                ).toInt()
            } else {
                YLog.debug(tag = TAG, msg = "status_bar_left_container_top_margin use system default")
            }

            if (status_bar_left_container_down_margin != -1) {
                leftLp.bottomMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    status_bar_left_container_down_margin.toFloat(),
                    metrics
                ).toInt()
            } else {
                YLog.debug(tag = TAG, msg = "status_bar_left_container_bottom_margin use system default")
            }

            if (status_bar_left_container_left_margin != -1) {
                leftLp.leftMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    status_bar_left_container_left_margin.toFloat(),
                    metrics
                ).toInt()
            } else {
                YLog.debug(tag = TAG, msg = "status_bar_left_container_left_margin use system default")
            }

            if (status_bar_left_container_right_margin != -1) {
                leftLp.rightMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    status_bar_left_container_right_margin.toFloat(),
                    metrics
                ).toInt()
            } else {
                YLog.debug(tag = TAG, msg = "status_bar_left_container_right_margin use system default")
            }

            leftContainer.layoutParams = leftLp
        }

        // 处理右侧容器 margin
        val rightLp = rightContainer.layoutParams as? ViewGroup.MarginLayoutParams
        if (rightLp != null) {
            if (status_bar_right_container_top_margin != -1) {
                rightLp.topMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    status_bar_right_container_top_margin.toFloat(),
                    metrics
                ).toInt()
            } else {
                YLog.debug(tag = TAG, msg = "status_bar_right_container_top_margin use system default")
            }

            if (status_bar_right_container_down_margin != -1) {
                rightLp.bottomMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    status_bar_right_container_down_margin.toFloat(),
                    metrics
                ).toInt()
            } else {
                YLog.debug(tag = TAG, msg = "status_bar_right_container_bottom_margin use system default")
            }

            if (status_bar_right_container_left_margin != -1) {
                rightLp.leftMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    status_bar_right_container_left_margin.toFloat(),
                    metrics
                ).toInt()
            } else {
                YLog.debug(tag = TAG, msg = "status_bar_right_container_left_margin use system default")
            }

            if (status_bar_right_container_right_margin != -1) {
                rightLp.rightMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    status_bar_right_container_right_margin.toFloat(),
                    metrics
                ).toInt()
            } else {
                YLog.debug(tag = TAG, msg = "status_bar_right_container_right_margin use system default")
            }

            rightContainer.layoutParams = rightLp
        }

        YLog.debug(tag = TAG, msg = "Side containers margins adjusted")
    }

//    private fun timeLeft(statusBarView: FrameLayout){
        // 根据时钟的id,先检查时钟是否在左侧布局状态栏容器中，如果在则直接返回
        // 如果不在左侧容器中，则检查挖孔的位置,如果在挖孔的位置，则将时钟移动到左侧状态栏容器的区域中
        // 如果不在挖孔的区域中，则检查时钟是否在右侧布局状态栏容器中，如果在，则将时钟移动到左侧状态栏容器的区域中
        // 实际上在timeCenter方法中，在设置居中之前需不需要先从左侧状态栏容器和右侧状态栏容器中根据id先查找时钟，然后备份它的布局，实际上左侧容器和右侧容器只有1个时钟

//    }

    private fun timeLeft(statusBarView: FrameLayout) {
        val clockView = statusBarView.findViewById<TextView>(clockId) ?: run {
            YLog.warn(tag = TAG, msg = "Clock view not found for left alignment")
            return
        }
        val leftContainer = statusBarView.findViewById<ViewGroup>(statusBarStartSideExceptHeadsUpId) ?: run {
            YLog.warn(tag = TAG, msg = "Left container not found for left alignment")
            return
        }

        // 如果时钟已经在左侧容器中，直接返回
        if (clockView.parent == leftContainer) {
            YLog.debug(tag = TAG, msg = "Clock already in left container, skip")
            return
        }

        val currentParent = clockView.parent as? ViewGroup
        if (currentParent == null) {
            YLog.warn(tag = TAG, msg = "Clock has no parent, cannot move")
            return
        }

        // 判断是否在挖孔区域：父容器是 cutout_space_view 的父容器且该父容器中原本有 cutout_space_view
        val cutoutSpace = statusBarView.findViewById<View>(cutoutSpaceId)
        val isInCutoutArea = if (cutoutSpace != null) {
            // 如果 cutoutSpace 还存在，说明没有执行居中，时钟不可能在挖孔区域
            false
        } else {
            // cutoutSpace 不存在，可能已被替换。检查当前父容器是否是 status_bar_contents 的子容器
            val statusBarContentsView = statusBarView.findViewById<LinearLayout>(statusBarContents)
            statusBarContentsView?.let { contents ->
                // 如果当前父容器是 status_bar_contents 的直接子视图，并且没有 ID（居中时创建的容器无 ID），则认为是挖孔容器
                currentParent.parent == contents && currentParent.id == View.NO_ID
            } ?: false
        }

        // 判断是否在右侧容器中
        val rightContainerIds = listOf(
            statusBarEndSideContainerId,
            statusBarEndSideContentId,
            systemIconsId
        ).filter { it != 0 }
        val isInRightContainer = rightContainerIds.any { id ->
            val container = statusBarView.findViewById<ViewGroup>(id)
            container != null && (currentParent == container || isChildOf(container, clockView))
        }

        when {
            isInCutoutArea -> {
                YLog.debug(tag = TAG, msg = "Clock is in cutout area, moving to left")
            }
            isInRightContainer -> {
                YLog.debug(tag = TAG, msg = "Clock is in right container, moving to left")
            }
            else -> {
                YLog.debug(tag = TAG, msg = "Clock is elsewhere, moving to left")
            }
        }

        // 从当前父容器移除时钟
        currentParent.removeView(clockView)

        // 如果当前父容器变为空且不是左侧/右侧容器，则将其从父布局移除，避免残留空视图
        if (currentParent !in listOf(leftContainer) && !rightContainerIds.contains(currentParent.id) && currentParent.childCount == 0) {
            (currentParent.parent as? ViewGroup)?.removeView(currentParent)
            YLog.debug(tag = TAG, msg = "Removed empty container: ${currentParent.javaClass.simpleName}")
        }

        // 将时钟添加到左侧容器的最前面（索引0）
        leftContainer.addView(clockView, 0)
        YLog.debug(tag = TAG, msg = "Clock moved to left container")
    }

    private fun timeRight(statusBarView: FrameLayout) {
        val clockView = statusBarView.findViewById<TextView>(clockId) ?: run {
            YLog.warn(tag = TAG, msg = "Clock view not found for right alignment")
            return
        }
        val rightContainer = statusBarView.findViewById<ViewGroup>(systemIconsId) ?: run {
            YLog.warn(tag = TAG, msg = "Right container (system_icons) not found for right alignment")
            return
        }

        // 如果时钟已经在右侧容器中，直接返回
        if (clockView.parent == rightContainer) {
            YLog.debug(tag = TAG, msg = "Clock already in right container, skip")
            return
        }

        val currentParent = clockView.parent as? ViewGroup
        if (currentParent == null) {
            YLog.warn(tag = TAG, msg = "Clock has no parent, cannot move")
            return
        }

        // 判断是否在挖孔区域：父容器是 cutout_space_view 的父容器且该父容器中原本有 cutout_space_view
        val cutoutSpace = statusBarView.findViewById<View>(cutoutSpaceId)
        val isInCutoutArea = if (cutoutSpace != null) {
            false
        } else {
            val statusBarContentsView = statusBarView.findViewById<LinearLayout>(statusBarContents)
            statusBarContentsView?.let { contents ->
                currentParent.parent == contents && currentParent.id == View.NO_ID
            } ?: false
        }

        // 判断是否在左侧容器中
        val leftContainerIds = listOf(
            statusBarStartSideContainerId,
            statusBarStartSideContentId,
            statusBarStartSideExceptHeadsUpId
        ).filter { it != 0 }
        val isInLeftContainer = leftContainerIds.any { id ->
            val container = statusBarView.findViewById<ViewGroup>(id)
            container != null && (currentParent == container || isChildOf(container, clockView))
        }

        when {
            isInCutoutArea -> {
                YLog.debug(tag = TAG, msg = "Clock is in cutout area, moving to right")
            }
            isInLeftContainer -> {
                YLog.debug(tag = TAG, msg = "Clock is in left container, moving to right")
            }
            else -> {
                YLog.debug(tag = TAG, msg = "Clock is elsewhere, moving to right")
            }
        }

        // 从当前父容器移除时钟
        currentParent.removeView(clockView)

        // 重要容器 ID 列表（左侧 + 右侧）
        val importantContainerIds = leftContainerIds + listOf(
            statusBarEndSideContainerId,
            statusBarEndSideContentId,
            systemIconsId
        ).filter { it != 0 }

        // 如果当前父容器变为空且不是重要容器，则将其从父布局移除
        if (currentParent.id !in importantContainerIds && currentParent.childCount == 0) {
            (currentParent.parent as? ViewGroup)?.removeView(currentParent)
            YLog.debug(tag = TAG, msg = "Removed empty container: ${currentParent.javaClass.simpleName}")
        }

        // 将时钟添加到右侧容器的末尾（最右边）
        rightContainer.addView(clockView, rightContainer.childCount)
        YLog.debug(tag = TAG, msg = "Clock moved to right container at index ${rightContainer.childCount - 1}")
    }


    /**
     * 判断 child 是否是 parent 的后代视图
     */
    private fun isChildOf(parent: ViewGroup, child: View): Boolean {
        var p = child.parent
        while (p is ViewGroup) {
            if (p == parent) return true
            p = p.parent
        }
        return false
    }
    /**
     * 将时钟移动到中间挖孔空间位置，实现居中显示
     */
    /**
     * 将时钟移动到中间挖孔空间位置，实现居中显示
     * 容器宽度自适应，通过 layout_gravity 在父布局中居中
     * 不使用原有的 LayoutParams，避免固定宽高
     */
    private fun timeCenter(statusBarView: FrameLayout) {
        val statusBarContentsView = statusBarView.findViewById<LinearLayout>(statusBarContents) ?: run {
            YLog.warn(tag = TAG, msg = "status_bar_contents not found for center clock")
            return
        }
        val cutoutSpace = statusBarContentsView.findViewById<View>(cutoutSpaceId) ?: run {
            YLog.warn(tag = TAG, msg = "cutout_space_view not found for center clock")
            return
        }
        val clockView = statusBarView.findViewById<TextView>(clockId) ?: run {
            YLog.warn(tag = TAG, msg = "clock view not found for center clock")
            return
        }

        try {
            val parent = cutoutSpace.parent as ViewGroup
            val index = parent.indexOfChild(cutoutSpace)

            // 创建全新的 LayoutParams，不沿用旧的，避免固定宽高
            // 父布局 status_bar_contents 是水平 LinearLayout，所以使用 LinearLayout.LayoutParams
            val containerLp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,  // 宽度自适应
                ViewGroup.LayoutParams.MATCH_PARENT    // 高度填充父容器（保证垂直居中可用）
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL    // 在水平方向上居中
            }

            // 创建容器放置时钟
            val clockContainer = FrameLayout(statusBarView.context).apply {
                layoutParams = containerLp
            }

            // 将时钟从原容器中移除
            (clockView.parent as? ViewGroup)?.removeView(clockView)

            // 将时钟添加到新容器，并在容器内居中
            clockContainer.addView(clockView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))

            // 用新容器替换原来的 Space
            parent.removeView(cutoutSpace)
            parent.addView(clockContainer, index)

            YLog.debug(tag = TAG, msg = "Clock moved to center with adaptive container (width=wrap, height=match_parent)")
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Failed to center clock: ${e.message}")
        }
    }


    /**
     * 打印视图的详细信息，包括当前视图、父类和子类视图
     */
    private fun logViewHierarchy(view: View, level: Int = 0) {
        val indent = "  ".repeat(level)
        val sb = StringBuilder()

        // 获取视图基本信息
        val viewId = view.id
        val viewIdName = if (viewId != View.NO_ID) {
            try {
                view.resources.getResourceEntryName(viewId)
            } catch (e: Exception) {
                "ID: 0x${Integer.toHexString(viewId)}"
            }
        } else {
            "NO_ID"
        }

        val className = view.javaClass.simpleName
        val visibility = when (view.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN"
        }

        // 构建当前视图信息
        sb.append("${indent}[$level] $className")
        sb.append(" (ID: $viewIdName, ")
        sb.append("Vis: $visibility, ")
        sb.append("Size: ${view.width}×${view.height}, ")
        sb.append("Pos: ${view.left},${view.top}-${view.right},${view.bottom})")

        // 添加父视图信息
        val parent = view.parent
        if (parent != null && parent is View) {
            val parentId = parent.id
            val parentIdName = if (parentId != View.NO_ID) {
                try {
                    parent.resources.getResourceEntryName(parentId)
                } catch (e: Exception) {
                    "ID: 0x${Integer.toHexString(parentId)}"
                }
            } else {
                "NO_ID"
            }
            sb.append(" <- Parent: ${parent.javaClass.simpleName}($parentIdName)")
        } else {
            sb.append(" <- Parent: null")
        }

        // 打印当前视图信息
        YLog.debug(tag = TAG, msg = sb.toString())

        // 递归打印子视图
        if (view is ViewGroup) {
            val childCount = view.childCount
            if (childCount > 0) {
                val childrenInfo = StringBuilder()
                childrenInfo.append("${indent}  └─ Children($childCount): ")
                for (i in 0 until childCount) {
                    val child = view.getChildAt(i)
                    if (child != null) {
                        val childIdName = if (child.id != View.NO_ID) {
                            try {
                                child.resources.getResourceEntryName(child.id)
                            } catch (e: Exception) {
                                "0x${Integer.toHexString(child.id)}"
                            }
                        } else {
                            "NO_ID"
                        }
                        childrenInfo.append("${child.javaClass.simpleName}($childIdName)")
                        if (i < childCount - 1) childrenInfo.append(", ")
                    }
                }
                YLog.debug(tag = TAG, msg = childrenInfo.toString())
            }

            for (i in 0 until childCount) {
                val child = view.getChildAt(i)
                if (child != null) {
                    logViewHierarchy(child, level + 1)
                }
            }
        }

        // 根节点分隔线
        if (level == 0) {
            YLog.debug(tag = TAG, msg = "====================")
        }
    }


    /**
     * 公开方法：传入视图打印其完整层级结构
     */
    fun printViewInfo(view: View) {
        YLog.debug(tag = TAG, msg = "=== 开始打印视图层级结构 ===")
        logViewHierarchy(view)
        YLog.debug(tag = TAG, msg = "=== 视图层级结构打印完成 ===")
    }


    /**
     * 设置左侧双排布局
     *  第1行，时钟+其他
     *  第2行，通知
     */
    private fun setupLeftDualRowsLayout(statusBarView: FrameLayout) {
        if (statusBarView.getTag(TAG_MODIFIED_LEFT) == true) return

        val leftContent = statusBarView.findViewById<LinearLayout>(statusBarStartSideExceptHeadsUpId) ?: run {
            YLog.warn(tag = TAG, msg = "Left content container not found")
            return
        }

        val clockView = statusBarView.findViewById<TextView>(clockId) ?: run {
            YLog.warn(tag = TAG, msg = "Clock view not found")
            return
        }

        val notificationArea = statusBarView.findViewById<FrameLayout>(notificationIconAreaId) ?: run {
            YLog.warn(tag = TAG, msg = "Notification icon area not found")
            return
        }

        val parent = leftContent.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(leftContent)
        val originalLayoutParams = leftContent.layoutParams

        // 创建双行垂直容器，保留原始 ID
        val dualRowsContainer = LinearLayout(statusBarView.context).apply {
            id = statusBarStartSideExceptHeadsUpId
            orientation = LinearLayout.VERTICAL
            layoutParams = originalLayoutParams ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setTag(TAG_MODIFIED_LEFT, true)
        }

        // 第一行和第二行的水平布局
        val firstRow = LinearLayout(statusBarView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val secondRow = LinearLayout(statusBarView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // 收集所有子视图并清空原容器
        val children = leftContent.children.toList()
        leftContent.removeAllViews()

        if (isDualClockAcross) {
            // 双时钟模式：时钟单独第一行，其他全部第二行
            for (child in children) {
                if (child == clockView) {
                    firstRow.addView(child)
                } else {
                    secondRow.addView(child)
                }
            }
        } else {
            // 普通模式：第一行 = 时钟 + 其他（除通知图标外），第二行 = 通知图标
            for (child in children) {
                if (child == notificationArea) {
                    secondRow.addView(child)
                } else {
                    firstRow.addView(child)
                }
            }
        }

        dualRowsContainer.addView(firstRow)
        dualRowsContainer.addView(secondRow)

        parent.removeView(leftContent)
        parent.addView(dualRowsContainer, index)

        YLog.debug(tag = TAG, msg = "Left dual rows layout applied (isDualClockAcross=$isDualClockAcross)")
    }


    /**
     * 设置右侧双排布局
     */
    private fun setupRightDualRowsLayout2(statusBarView: FrameLayout) {
        // 检查是否已修改（使用整数键）
        if (statusBarView.getTag(TAG_MODIFIED_RIGHT) == true) return

        val systemIcons = statusBarView.findViewById<LinearLayout>(systemIconsId) ?: run {
            YLog.warn(tag = TAG, msg = "System icons container not found")
            return
        }

        val statusIcons = statusBarView.findViewById<LinearLayout>(statusIconsId) ?: run {
            YLog.warn(tag = TAG, msg = "Status icons view not found")
            return
        }

        val batteryView = statusBarView.findViewById<LinearLayout>(batteryId) ?: run {
            YLog.warn(tag = TAG, msg = "Battery view not found")
            return
        }

        val parent = systemIcons.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(systemIcons)
        val originalLayoutParams = systemIcons.layoutParams

        // 创建双排容器，设置原始 ID
        val dualRowsContainer = LinearLayout(statusBarView.context).apply {
            id = systemIconsId // 关键：保留 ID
            orientation = LinearLayout.VERTICAL
            layoutParams = originalLayoutParams ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setTag(TAG_MODIFIED_RIGHT, true) // 使用整数键标记已修改
        }

        // 第一行：状态图标
        val firstRow = LinearLayout(statusBarView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        // 第二行：电池
        val secondRow = LinearLayout(statusBarView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        // 移动视图
        systemIcons.removeView(statusIcons)
        firstRow.addView(statusIcons)

        systemIcons.removeView(batteryView)
        secondRow.addView(batteryView)


        // 将剩余视图移到第一行
        while (systemIcons.childCount > 0) {
            val child = systemIcons.getChildAt(0)
            systemIcons.removeViewAt(0)
            firstRow.addView(child)
        }

        dualRowsContainer.addView(firstRow)
        dualRowsContainer.addView(secondRow)

        // 替换原容器
        parent.removeView(systemIcons)
        parent.addView(dualRowsContainer, index)

        YLog.debug(tag = TAG, msg = "Right dual rows layout applied")
    }
    /**
     * 设置右侧双排布局
     * 修改：将温度容器（tag = TAG_BATTERY_TEMP_CONTAINER）与电池一同放入第二行
     */
//    private fun setupRightDualRowsLayout(statusBarView: FrameLayout) {
//        // 检查是否已修改
//        if (statusBarView.getTag(TAG_MODIFIED_RIGHT) == true) return
//
//        val systemIcons = statusBarView.findViewById<LinearLayout>(systemIconsId) ?: run {
//            YLog.warn(tag = TAG, msg = "System icons container not found")
//            return
//        }
//
//        val statusIcons = statusBarView.findViewById<LinearLayout>(statusIconsId) ?: run {
//            YLog.warn(tag = TAG, msg = "Status icons view not found")
//            return
//        }
//
//        val batteryView = statusBarView.findViewById<LinearLayout>(batteryId) ?: run {
//            YLog.warn(tag = TAG, msg = "Battery view not found")
//            return
//        }
//
//        val parent = systemIcons.parent as? ViewGroup ?: return
//        val index = parent.indexOfChild(systemIcons)
//        val originalLayoutParams = systemIcons.layoutParams
//
//        // 创建双排容器，保留原始 ID
//        val dualRowsContainer = LinearLayout(statusBarView.context).apply {
//            id = systemIconsId
//            orientation = LinearLayout.VERTICAL
//            layoutParams = originalLayoutParams ?: LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//            )
//            setTag(TAG_MODIFIED_RIGHT, true)
//        }
//
//        // 第一行：状态图标及其他（除电池和温度容器外）
//        val firstRow = LinearLayout(statusBarView.context).apply {
//            orientation = LinearLayout.HORIZONTAL
//            gravity = Gravity.END or Gravity.CENTER_VERTICAL
//            layoutParams = LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                0,
//                1.0f
//            )
//        }
//
//        // 第二行：电池 + 温度容器
//        val secondRow = LinearLayout(statusBarView.context).apply {
//            orientation = LinearLayout.HORIZONTAL
//            gravity = Gravity.END or Gravity.CENTER_VERTICAL
//            layoutParams = LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                0,
//                1.0f
//            )
//        }
//
//        // 收集 systemIcons 的所有子视图
//        val children = mutableListOf<View>()
//        for (i in 0 until systemIcons.childCount) {
//            children.add(systemIcons.getChildAt(i))
//        }
//        systemIcons.removeAllViews()
//
//
//
//
//        // 分类：第一行 = statusIcons + 其他未知视图；第二行 = batteryView + 设备温度容器
////        children.forEach { child ->
////            when {
////                child == statusIcons -> firstRow.addView(child)
////                child == batteryView -> secondRow.addView(child)
////                child.tag == TAG_BATTERY_TEMP_CONTAINER -> secondRow.addView(child) // 设备温度容器放入第二行
////                else -> firstRow.addView(child) // 其他未知视图放入第一行
////            }
////
////        }
//        children.forEach { child ->
//            when {
//                child == statusIcons -> firstRow.addView(child)
//                child == batteryView -> secondRow.addView(child)
//                child.tag == TAG_BATTERY_TEMP_CONTAINER -> secondRow.addView(child)
//                child.tag == TAG_BATTERY_INFO_CONTAINER -> secondRow.addView(child)   // 新增此行
//                else -> firstRow.addView(child)
//            }
//        }
//
//        // 将两行添加到双排容器
//        dualRowsContainer.addView(firstRow)
//        dualRowsContainer.addView(secondRow)
//
//        // 替换原容器
//        parent.removeView(systemIcons)
//        parent.addView(dualRowsContainer, index)
//
//        YLog.debug(tag = TAG, msg = "Right dual rows layout applied (temperature moved to second row)")
//    }

    /**
     * 设置右侧双排布局
     * 修改：将温度容器（tag = TAG_BATTERY_TEMP_CONTAINER）与电池一同放入第二行
     * 新增：根据电池信息布局模式决定电池信息容器的放置位置
     */
    private fun setupRightDualRowsLayout(statusBarView: FrameLayout) {
        // 检查是否已修改
        if (statusBarView.getTag(TAG_MODIFIED_RIGHT) == true) return

        val systemIcons = statusBarView.findViewById<LinearLayout>(systemIconsId) ?: run {
            YLog.warn(tag = TAG, msg = "System icons container not found")
            return
        }

        val statusIcons = statusBarView.findViewById<LinearLayout>(statusIconsId) ?: run {
            YLog.warn(tag = TAG, msg = "Status icons view not found")
            return
        }

        val batteryView = statusBarView.findViewById<LinearLayout>(batteryId) ?: run {
            YLog.warn(tag = TAG, msg = "Battery view not found")
            return
        }

        val parent = systemIcons.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(systemIcons)
        val originalLayoutParams = systemIcons.layoutParams

        // 创建双排容器，保留原始 ID
        val dualRowsContainer = LinearLayout(statusBarView.context).apply {
            id = systemIconsId
            orientation = LinearLayout.VERTICAL
            layoutParams = originalLayoutParams ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setTag(TAG_MODIFIED_RIGHT, true)
        }

        // 第一行：状态图标及其他（除电池和温度容器外）
        val firstRow = LinearLayout(statusBarView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        // 第二行：电池 + 温度容器 + 电池信息容器
        val secondRow = LinearLayout(statusBarView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        // 收集 systemIcons 的所有子视图
        val children = mutableListOf<View>()
        for (i in 0 until systemIcons.childCount) {
            children.add(systemIcons.getChildAt(i))
        }
        systemIcons.removeAllViews()

        // 分类处理：第一行 = statusIcons + 其他未知视图；第二行 = batteryView + 设备温度容器 + 电池信息容器
        children.forEach { child ->
            when {
                child == statusIcons -> firstRow.addView(child)
                child == batteryView -> secondRow.addView(child)
                child.tag == TAG_BATTERY_TEMP_CONTAINER -> secondRow.addView(child)
                child.tag == TAG_BATTERY_INFO_CONTAINER -> {
                    // 根据电池信息布局模式决定放置位置
                    val batteryInfoLayout = getBatteryInfoLayoutMode()
                    if (batteryInfoLayout) {
                        // 双排模式：电池信息容器放在第二行（与电池同排）
                        secondRow.addView(child)
                        YLog.debug(tag = TAG, msg = "Battery info container (dual mode) added to second row")
                    } else {
                        // 单排模式：电池信息容器放在第二行（与电池同排）
                        secondRow.addView(child)
                        YLog.debug(tag = TAG, msg = "Battery info container (single mode) added to second row")
                    }
                }
                else -> firstRow.addView(child)
            }
        }

        // 将两行添加到双排容器
        dualRowsContainer.addView(firstRow)
        dualRowsContainer.addView(secondRow)

        // 替换原容器
        parent.removeView(systemIcons)
        parent.addView(dualRowsContainer, index)

        YLog.debug(tag = TAG, msg = "Right dual rows layout applied (battery info handled)")
    }

    /**
     * 获取电池信息布局模式
     * 注意：这个方法需要访问MonitorDeviceInfoHook中的配置
     * 由于这是不同的对象，我们需要通过Prefs重新读取配置
     */
    private fun getBatteryInfoLayoutMode(): Boolean {
        return Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_LAYOUT, false)
    }



}