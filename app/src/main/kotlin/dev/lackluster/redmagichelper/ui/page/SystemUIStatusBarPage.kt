package dev.lackluster.redmagichelper.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.hyperx.compose.preference.DropDownPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SeekBarPreferenceDefault3
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope

@Composable
fun SystemUIStatusBarPage(
    navController: NavController,
    adjustPadding: PaddingValues,
    mode: BasePageDefaults.Mode
) {
    // 红魔-下拉状态栏时间-选项
    val statusBarPullDownTimelEntries = listOf(
        // 不显秒
        DropDownEntry(stringResource(R.string.clock_easy_pull_down_time_hide_second)),
        // 显示秒
        DropDownEntry(stringResource(R.string.clock_easy_pull_down_time_display_second)),
    )
    // 红魔-下拉状态栏时段-选项
    val statusBarPullDownPeriodlEntries = listOf(
        // 不显秒
        DropDownEntry(stringResource(R.string.clock_easy_pull_down_period_hide)),
        // 显示秒
        DropDownEntry(stringResource(R.string.clock_easy_pull_down_period_display)),
    )

    var visibilityCustomNotifCount by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT)
        )
    }

    var visibilityStatusBarDualNetWorkSpeed by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW_NETWORK_SPEED)
        )
    }

    var visibilitySetStatusBarMaxNotificationIcons by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.SETSTATUSBARMAXNOTIFICATIONICONS_TITLE)
        )
    }

    var visibilityHideStatusBar by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.HOME_RECENT_HIDE_STATUS_BAR)
        )
    }

    BasePage(
        navController,
        adjustPadding,
        // 标题：状态栏
        stringResource(R.string.ui_title_status_bar),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            // 重启系统界面
            // appPkg 重启的对应包名：com.android.systemui
            RebootMenuItem(
                appName = stringResource(R.string.scope_systemui),
                appPkg = Scope.SYSTEM_UI
            )
        }
    ) {
        // 红魔-图标调整
        item {
            PreferenceGroup(
                stringResource(R.string.systemui_statusbar_icon),
                visible = true,
            ) {
                // 图标调整
                TextPreference(
                    title = stringResource(R.string.systemui_statusbar_icon)
                ) {
                    // 导航到图标调整界面
                    navController.navigateTo(Pages.NUBIA_ICON_TUNER)
                }
            }
        }
        // 红魔-状态栏
        item {
            PreferenceGroup(
                stringResource(R.string.ui_title_systemui_status_bar),
                visible = true // 默认显示状态栏卡片
            ) {
                // 隐藏状态栏
                SwitchPreference(
                    title = stringResource(R.string.home_recent_hide_status_bar),
                    key = Pref.Key.SystemUI.StatusBar.HOME_RECENT_HIDE_STATUS_BAR, //唯一id
                ){
                    visibilityHideStatusBar = it
                }
                AnimatedVisibility(!visibilityHideStatusBar) {
                    // 挖孔常显
                    SwitchPreference(
                        title = stringResource(R.string.holes_often_show),
                        key = Pref.Key.SystemUI.StatusBar.HOLES_OFTEN_SHOW, //唯一id
                    )
                }
                // 状态栏布局
                TextPreference(
                    title = stringResource(R.string.ui_title_status_bar_layout)
                ) {
                    // 导航
                    navController.navigateTo(Pages.STATUS_BAR_LAYOUT)
                }
                // 双排状态栏
                TextPreference(
                    title = stringResource(R.string.ui_title_status_bar_dual)
                ) {
                    // 导航
                    navController.navigateTo(Pages.STATUS_BAR_DUAL)
                }
                // 状态栏网格重排
                TextPreference(
                    title = stringResource(R.string.ui_title_status_bar_grid),
                    summary = stringResource(R.string.ui_title_status_bar_grid_summary)
                ) {
                    // 导航
                    navController.navigateTo(Pages.STATUS_BAR_GRID)
                }
                // 显示设备温度
                TextPreference(
                    title = stringResource(R.string.ui_title_status_bar_display_temp)
                ) {
                    // 导航
                    navController.navigateTo(Pages.STATUS_BAR_DISPLAY_TEMP)
                }
                // 显示电池信息
                TextPreference(
                    title = stringResource(R.string.ui_title_status_bar_display_battery_info)
                ) {
                    // 导航
                    navController.navigateTo(Pages.STATUS_BAR_DISPLAY_BATTERY_INFO)
                }
                // 时间指示器
                TextPreference(
                    title = stringResource(R.string.ui_title_time_indicator)
                ) {
                    // 导航
                    navController.navigateTo(Pages.STATUS_BAR_TIME_INDICATOR)
                }

                //// 修改状态栏最大通知图标数量
                //SwitchPreference(
                //    title = stringResource(R.string.setStatusBarMaxNotificationIcons_title),
                //    key = Pref.Key.SystemUI.StatusBar.SETSTATUSBARMAXNOTIFICATIONICONS_TITLE_SWITCH, //唯一id
                //    onCheckedChange = { newValue ->
                //        visibilitySetStatusBarMaxNotificationIcons = newValue
                //    }
                //)
                //
                //AnimatedVisibility(visibilitySetStatusBarMaxNotificationIcons) { // 确保 key 与当前开关的key 相同
                //    Column {
                //        // 固定宽度以防相邻元素左右抖动(单排dp)
                //        SeekBarPreferenceDefault3(
                //            title = stringResource(R.string.setStatusBarMaxNotificationIcons_title),
                //            key = Pref.Key.SystemUI.StatusBar.SETSTATUSBARMAXNOTIFICATIONICONS_TITLE,
                //            min = 4,
                //            max = 20,
                //        )
                //    }
                //}

                // 自定义通知的最大显示数量开关,  打开后弹出状态栏通知图标最大数量
                SwitchPreference(
                    title = stringResource(R.string.systemui_statusbar_notif_count),
                    key = Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT,
                    defValue = visibilityCustomNotifCount
                ) {
                    // 根据visibilityCustomNotifCount的值用于控制子组件的动态显示/隐藏 的开关的值
                    visibilityCustomNotifCount = it
                }
                AnimatedVisibility(
                    // key ：visibilityCustomNotifCount
                    visibilityCustomNotifCount
                ) {
                    // 自定义通知图标最大数量
                    SeekBarPreferenceDefault3(
                        title = stringResource(R.string.systemui_statusbar_notif_count_icon),
                        key = Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT_ICON,
                        min = 3,
                        max = 15
                    )
                }
                // 禁用智能深色
                SwitchPreference(
                    title = stringResource(R.string.systemui_statusbar_disable_smart_dark),
                    summary = stringResource(R.string.systemui_statusbar_disable_smart_dark_tips),
                    key = Pref.Key.SystemUI.StatusBar.DISABLE_SMART_DARK
                )
                // 双击锁定屏幕
                SwitchPreference(
                    title = stringResource(R.string.status_bar_double_clicked_locked_screen),
                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DOUBLE_CLICKED_LOCKED_SCREEN, //唯一id
                )
                // 恢复时钟日期图标字体
                SwitchPreference(
                    title = stringResource(R.string.restore_the_font_of_the_clock_date_icon),
                    key = Pref.Key.SystemUI.StatusBar.RESTORE_THE_FONT_OF_THE_CLOCK_DATE_ICON, //唯一id
                )
                // 使用原生通知图标
                SwitchPreference(
                    title = stringResource(R.string.status_bar_use_the_native_notification_icon),
                    summary = stringResource(R.string.status_bar_use_the_native_notification_icon_tips),
                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_USE_THE_NATIVE_NOTIFICATION_ICON, //唯一id
                )
                // 下拉状态栏时间
                DropDownPreference(
                    title = stringResource(R.string.clock_easy_pull_down_time),
                    entries = statusBarPullDownTimelEntries, // 选项
                    key = Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_PULL_DOWN  //唯一id
                )
                // 下拉状态栏时段
                DropDownPreference(
                    title = stringResource(R.string.clock_easy_pull_down_period),
                    entries = statusBarPullDownPeriodlEntries, // 选项
                    key = Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_PULL_DOWN_PERIOD  //唯一id
                )
            }
        }
        // 红魔-状态栏网络速度
        item {
            PreferenceGroup(
                stringResource(R.string.ui_title_systemui_status_bar_network_speed),
            ) {
                // 网络速度秒刷新
                SwitchPreference(
                    title = stringResource(R.string.status_bar_network_speed_refresh_speed),
                    summary = stringResource(R.string.status_bar_network_speed_refresh_speed_summary),
                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_NETWORK_SPEED_REFRESH_SPEED, //唯一id
                )
                // 双排网络速度 上下行网络速度显示
                SwitchPreference(
                    title = stringResource(R.string.status_bar_dual_row_network_speed),
                    summary = stringResource(R.string.status_bar_dual_row_network_speed_summary),
                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW_NETWORK_SPEED,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        // visibilityStatusBarDualNetWorkSpeed
                        visibilityStatusBarDualNetWorkSpeed = newValue
                    }
                )
                // status_bar_network_speed_dual_row_digit_len
                // visibilityStatusBarDualNetWorkSpeed key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW_NETWORK_SPEED
                AnimatedVisibility(visibilityStatusBarDualNetWorkSpeed) { // 确保 key 与当前开关的key 相同
                    Column {
                        // 隐藏速度单位中的/s
                        SwitchPreference(
                            title = stringResource(R.string.speed_unit_hide_per_second),
                            key = Pref.Key.SystemUI.StatusBar.SPEED_UNIT_HIDE_PER_SECOND,
                        )
                        // 双排大小
                        SeekBarPreference(
                            title = stringResource(R.string.status_bar_network_speed_dual_row_size),
                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_NETWORK_SPEED_DUAL_ROW_SIZE,
                            defValue = 6,
                            min = 6,
                            max = 8
                        )
                        // 双排宽度(超过38匹配内容)
                        SeekBarPreference(
                            title = stringResource(R.string.status_bar_network_speed_dual_row_width),
                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_NETWORK_SPEED_DUAL_ROW_WIDTH,
                            defValue = 35,
                            min = 28,
                            max = 39
                        )
                        // 网速的最大小数位数
                        SeekBarPreference(
                            title = stringResource(R.string.status_bar_network_speed_dual_row_digit_len),
                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_NETWORK_SPEED_DUAL_ROW_DIGIT_LEN,
                            defValue = 3,
                            min = 0,
                            max = 3
                        )
                        // 低速时隐藏网速(-1始终显示),单位KiB/s
                        SeekBarPreference(
                            title = stringResource(R.string.low_speed_hide_kilo_bytes),
                            key = Pref.Key.SystemUI.StatusBar.LOW_SPEED_HIDE_KILO_BYTES,
                            defValue = -1,
                            min = -1,
                            max = 10
                        )
                    }
                }
            }
        }
        // 红魔-状态栏图标
//        item {
//            PreferenceGroup(
//                stringResource(R.string.status_bar_icon),
//            ) {
//                // 隐藏电量百分比图标
//                SwitchPreference(
//                    title = stringResource(R.string.hide_battery_percentage_icon),
//                    summary = stringResource(R.string.hide_battery_percentage_icon_summary),
//                    key = Pref.Key.SystemUI.StatusBar.HIDE_BATTERY_PERCENTAGE_ICON, //唯一id
//                )

//            }
//        }
    }
}
