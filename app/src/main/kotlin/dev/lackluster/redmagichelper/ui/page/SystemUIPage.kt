package dev.lackluster.redmagichelper.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.hyperx.compose.preference.DropDownPreference
import dev.lackluster.hyperx.compose.preference.EditTextDataType
import dev.lackluster.hyperx.compose.preference.EditTextPreference
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
fun SystemUIPage(
    navController: NavController,
    adjustPadding: PaddingValues,
    mode: BasePageDefaults.Mode
) {
    // 小米-锁屏-选项
    val lockscreenCarrierLabelEntries = listOf(
        DropDownEntry(stringResource(R.string.systemui_lock_carrier_text_default)),
        DropDownEntry(stringResource(R.string.systemui_lock_carrier_text_carrier)),
        DropDownEntry(stringResource(R.string.systemui_lock_carrier_text_clock))
    )
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
    val expandNotificationsEntries = listOf(
        DropDownEntry(
            title = stringResource(R.string.systemui_notif_expand_notif_def),
            summary = stringResource(R.string.systemui_notif_expand_notif_def_tips)
        ),
        DropDownEntry(
            title = stringResource(R.string.systemui_notif_expand_notif_first),
            summary = stringResource(R.string.systemui_notif_expand_notif_first_tips)
        ),
        DropDownEntry(
            title = stringResource(R.string.systemui_notif_expand_notif_ungrouped),
            summary = stringResource(R.string.systemui_notif_expand_notif_ungrouped_tips)
        ),
    )

    var visibilityCustomNotifCount by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT)
        )
    }
    var visibilityCCBatteryPercent by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.ControlCenter.BATTERY_PERCENTAGE)
        )
    }
    var visibilityMonetColor by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.NotifCenter.MONET_OVERLAY)
        )
    }


//    var visibilityScreenOffPeriodFontSize by remember { mutableStateOf(
//        SafeSP.getBoolean(Pref.Key.SystemUI.ScreenOff.SCREEN_OFF_PERIOD_FONT_SIZE)
//    ) }
    var visibilityScreenOffPeriodFontSize by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.LockScreen.SCREEN_OFF_PERIOD)
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

    var visibilityAOSPSingleHandModeAdjust by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.AOSP_SINGLEHANDMODE_ADJUST)
        )
    }


    var visibilityQsShowSearch by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.QS_SHORTCUT_REDIR_SEARCH)
        )
    }

    var visibilityQsCustomRowColumn by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.QS_CUSTOM_ROW_COLUMN_SWITCH)
        )
    }


    var visibilityHideStatusBar by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.HOME_RECENT_HIDE_STATUS_BAR)
        )
    }

    var visibilityLockScreenChargingInfo by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.LockScreen.SHOW_CHARGING_INFO)
        )
    }
    var visibilityLockScreenChargingAnimation by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_CHARGING_ANIMATION_SWITCH)
        )
    }

    var visibilityAudioGain by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.AudioGain.ENABLE)
        )
    }

    BasePage(
        navController,
        adjustPadding,
        // 标题：系统界面
        stringResource(R.string.page_systemui),
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
        // 字体
        item {
            PreferenceGroup(
                stringResource(R.string.ui_title_syetem_page_font),
                visible = true,
            ) {
                // 字体
                TextPreference(
                    title = stringResource(R.string.ui_title_syetem_page_font)
                ) {
                    // 导航到字体界面
                    navController.navigateTo(Pages.SYSTEM_PAGE_FONT)
                }
            }
        }
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
        // 一组：
        //PreferenceGroup中包含多个Preference
        item {
            // 小米-状态栏
            PreferenceGroup(
                stringResource(R.string.ui_title_systemui_status_bar),
                first = true,
                visible = false, //隐藏状态栏卡片
            ) {
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
                    SeekBarPreference(
                        title = stringResource(R.string.systemui_statusbar_notif_count_icon),
                        key = Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT_ICON,
                        defValue = 3,
                        min = 0,
                        max = 15
                    )
                }
                // 普通的开关
                SwitchPreference(
                    title = stringResource(R.string.systemui_statusbar_tap_to_sleep),
                    key = Pref.Key.SystemUI.StatusBar.DOUBLE_TAP_TO_SLEEP
                )
                // 普通的开关
                SwitchPreference(
                    title = stringResource(R.string.systemui_statusbar_disable_smart_dark),
                    summary = stringResource(R.string.systemui_statusbar_disable_smart_dark_tips),
                    key = Pref.Key.SystemUI.StatusBar.DISABLE_SMART_DARK
                )
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

        //小米-锁屏
        item {
            PreferenceGroup(
                stringResource(R.string.ui_title_systemui_lock_screen),
                visible = false
            ) {
                SwitchPreference(
                    title = stringResource(R.string.systemui_lock_hide_disturb),
                    key = Pref.Key.SystemUI.LockScreen.HIDE_DISTURB
                )
                SwitchPreference(
                    title = stringResource(R.string.systemui_lock_keep_notif),
                    summary = stringResource(R.string.systemui_lock_keep_notif_tips),
                    key = Pref.Key.SystemUI.LockScreen.KEEP_NOTIFICATION
                )
                SwitchPreference(
                    title = stringResource(R.string.systemui_lock_double_tap),
                    key = Pref.Key.SystemUI.LockScreen.DOUBLE_TAP_TO_SLEEP
                )
                SwitchPreference(
                    title = stringResource(R.string.systemui_lock_flashlight_on),
                    summary = stringResource(R.string.systemui_lock_flashlight_on_tips),
                    key = Pref.Key.SystemUI.Plugin.AUTO_FLASH_ON
                )
                DropDownPreference(
                    title = stringResource(R.string.systemui_lock_carrier_text),
                    entries = lockscreenCarrierLabelEntries,
                    key = Pref.Key.SystemUI.LockScreen.CARRIER_TEXT
                )
            }
        }
        //红魔-锁屏
        item {
            PreferenceGroup(
                stringResource(R.string.ui_title_systemui_lock_screen),
                visible = true //默认显示锁屏卡片
            ) {

                // 允许在锁屏调整音量
                // 即使没有音乐播放
                SwitchPreference(
                    title = stringResource(R.string.lock_screen_allow_adjust_volume),
                    summary = stringResource(R.string.lock_screen_allow_adjust_volume_tips),
                    key = Pref.Key.SystemUI.LockScreen.ALLOW_ADJUST_VOLUME
                )
                // 锁屏时隐藏状态栏
                SwitchPreference(
                    title = stringResource(R.string.lock_screen_hide_status_bar),
                    key = Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_HIDE_STATUS_BAR
                )
                // 充电底部显示充电信息
                SwitchPreference(
                    title = stringResource(R.string.lock_screen_display_charge_info),
                    summary = stringResource(R.string.lock_screen_display_charge_info_tips),
                    key = Pref.Key.SystemUI.LockScreen.SHOW_CHARGING_INFO,
                    onCheckedChange = { newValue ->
                        visibilityLockScreenChargingInfo = newValue
                    }
                )
                AnimatedVisibility(
                    visibilityLockScreenChargingInfo
                ) {
                    Column {
                        // 显示电池温度
                        SwitchPreference(
                            title = stringResource(R.string.lock_screen_display_battery_temperature),
                            key = Pref.Key.SystemUI.LockScreen.SHOW_BATTERY_TEMPERATURE
                        )
                        // 显示电流
                        SwitchPreference(
                            title = stringResource(R.string.lock_screen_display_charge_electricity),
                            key = Pref.Key.SystemUI.LockScreen.SHOW_CHARGING_C_MORE
                        )
                        // 显示电压
                        SwitchPreference(
                            title = stringResource(R.string.lock_screen_display_charge_voltage),
                            key = Pref.Key.SystemUI.LockScreen.SHOW_CHARGING_V_MORE
                        )
                        // 显示功率
                        SwitchPreference(
                            title = stringResource(R.string.lock_screen_display_charge_power),
                            key = Pref.Key.SystemUI.LockScreen.SHOW_CHARGING_P_MORE
                        )
                        // 刷新间隔时间(秒)
                        SeekBarPreference(
                            title = stringResource(R.string.lock_screen_display_refresh_interval_time),
                            key = Pref.Key.SystemUI.LockScreen.SHOW_REFRESH_INTERVAL_TIME,
                            defValue = 1,
                            min = 1,
                            max = 10,
                        )
                    }
                }

                // 锁屏-充电动画
                SwitchPreference(
                    title = stringResource(R.string.lock_screen_charging_animation_switch),
                    key = Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_CHARGING_ANIMATION_SWITCH,
                    onCheckedChange = { newValue ->
                        visibilityLockScreenChargingAnimation = newValue
                    }
                )
                AnimatedVisibility(
                    visibilityLockScreenChargingAnimation
                ) {
                    Column {
                        // 每次进入锁屏时都显示充电动画
                        SwitchPreference(
                            title = stringResource(R.string.lock_screen_charging_animation),
                            key = Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_CHARGING_ANIMATION
                        )
                        // 充电动画持续时间(单位：秒)
                        SeekBarPreference(
                            title = stringResource(R.string.lock_screen_display_animation_duration),
                            key = Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_DISPLAY_ANIMATION_DURATION,
                            defValue = 6,
                            min = 6,
                            max = Int.MAX_VALUE,
                        )
                        // 延迟指定秒后才开始显示充电动画
                        SeekBarPreference(
                            title = stringResource(R.string.lock_screen_display_delay_time),
                            key = Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_DISPLAY_DELAY_TIME,
                            defValue = 0,
                            min = 0,
                            max = Int.MAX_VALUE,
                        )

                    }
                }

            }
        }
        // 音量增益
        item {
            val routeSpeaker = stringResource(R.string.audio_gain_route_speaker)
            val routeWired = stringResource(R.string.audio_gain_route_wired)
            val routeBluetooth = stringResource(R.string.audio_gain_route_bluetooth)
            val streamMedia = stringResource(R.string.audio_gain_stream_media)
            val streamRing = stringResource(R.string.audio_gain_stream_ring)
            val streamAlarm = stringResource(R.string.audio_gain_stream_alarm)
            PreferenceGroup(
                stringResource(R.string.audio_gain_title),
                visible = true
            ) {
                SwitchPreference(
                    title = stringResource(R.string.audio_gain_enable),
                    summary = stringResource(R.string.audio_gain_enable_tips),
                    key = Pref.Key.AudioGain.ENABLE,
                    defValue = visibilityAudioGain
                ) {
                    visibilityAudioGain = it
                }
                AnimatedVisibility(visibilityAudioGain) {
                    Column {
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_step),
                            key = Pref.Key.AudioGain.STEP,
                            defValue = 5,
                            min = 1,
                            max = 20
                        )
                        // 扬声器
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_limit_title, routeSpeaker, streamMedia),
                            key = Pref.Key.AudioGain.LIMIT_SPEAKER_MEDIA,
                            defValue = 100,
                            min = 100,
                            max = 300
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_limit_title, routeSpeaker, streamRing),
                            key = Pref.Key.AudioGain.LIMIT_SPEAKER_RING,
                            defValue = 100,
                            min = 100,
                            max = 300
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_limit_title, routeSpeaker, streamAlarm),
                            key = Pref.Key.AudioGain.LIMIT_SPEAKER_ALARM,
                            defValue = 100,
                            min = 100,
                            max = 300
                        )
                        // 有线 / USB
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_limit_title, routeWired, streamMedia),
                            key = Pref.Key.AudioGain.LIMIT_WIRED_MEDIA,
                            defValue = 100,
                            min = 100,
                            max = 300
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_limit_title, routeWired, streamRing),
                            key = Pref.Key.AudioGain.LIMIT_WIRED_RING,
                            defValue = 100,
                            min = 100,
                            max = 300
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_limit_title, routeWired, streamAlarm),
                            key = Pref.Key.AudioGain.LIMIT_WIRED_ALARM,
                            defValue = 100,
                            min = 100,
                            max = 300
                        )
                        // 蓝牙
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_limit_title, routeBluetooth, streamMedia),
                            key = Pref.Key.AudioGain.LIMIT_BLUETOOTH_MEDIA,
                            defValue = 100,
                            min = 100,
                            max = 300
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_limit_title, routeBluetooth, streamRing),
                            key = Pref.Key.AudioGain.LIMIT_BLUETOOTH_RING,
                            defValue = 100,
                            min = 100,
                            max = 300
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.audio_gain_limit_title, routeBluetooth, streamAlarm),
                            key = Pref.Key.AudioGain.LIMIT_BLUETOOTH_ALARM,
                            defValue = 100,
                            min = 100,
                            max = 300
                        )
                    }
                }
            }
        }
        //红魔-熄屏
        item {
            PreferenceGroup(
                stringResource(R.string.ui_title_systemui_screen_off),
                visible = true //默认显示锁屏卡片
            ) {

//                // 显示时段 -打开后显示子组件
//                SwitchPreference(
//                    title = stringResource(R.string.screen_off_period),
//                    key = Pref.Key.SystemUI.LockScreen.SCREEN_OFF_PERIOD,
//                    defValue = visibilityScreenOffPeriodFontSize
//                ){
//                        // 根据visibilityScreenOffPeriodFontSize的值用于控制子组件的动态显示/隐藏 的开关的值
//                    visibilityScreenOffPeriodFontSize = it
//                }
//                AnimatedVisibility(
//                    // 控制熄屏时段字体大小的动态的创建或者隐藏组件
//                    visibilityScreenOffPeriodFontSize
//                ) {
//                    Column {
//                        // 字体大小
//                        EditTextPreference(
//                            title = stringResource(R.string.screen_off_period_font_size_settings),
//                            key = Pref.Key.SystemUI.ScreenOff.SCREEN_OFF_PERIOD_FONT_SIZE_SETTINGS,
//                            defValue = 0.6f,
//                            dataType = EditTextDataType.FLOAT,
//                        )
//                    }
//                }
                // 直接使用第一种 SwitchPreference，不要传入 checked 参数
                SwitchPreference(
                    title = stringResource(R.string.screen_off_period),
//                    key = Pref.Key.SystemUI.LockScreen.SCREEN_OFF_PERIOD,
                    key = Pref.Key.SystemUI.LockScreen.SCREEN_OFF_PERIOD,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        visibilityScreenOffPeriodFontSize = newValue
                    }
                )
                // visibilityScreenOffPeriodFontSize key = Pref.Key.SystemUI.LockScreen.SCREEN_OFF_PERIOD
                AnimatedVisibility(visibilityScreenOffPeriodFontSize) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            title = stringResource(R.string.screen_off_period_font_size_settings),
                            key = Pref.Key.SystemUI.ScreenOff.SCREEN_OFF_PERIOD_FONT_SIZE_SETTINGS,
                            defValue = 0.6f,
                            dataType = EditTextDataType.FLOAT,
                            dialogMessage = "请输入 0.3 到 1.0 之间的值",
                            isValueValid = { value ->
                                val floatValue = value as? Float
                                floatValue != null && floatValue in 0.3f..1.0f
                            }
                        )
                    }
                }
                // 暂时不支持，可能存在问题-5秒刷新一次
//                 显示秒
                SwitchPreference(
                    title = stringResource(R.string.screen_off_show_seconds),
                    summary = stringResource(R.string.screen_off_show_seconds_summary),
                    key = Pref.Key.SystemUI.LockScreen.SCREEN_OFF_SHOW_SECONDS
                )

            }
        }
        // 红魔-开发者选项
        item {
            PreferenceGroup(stringResource(R.string.system_settings_develop_title)) {
                // USB调试始终允许授权 USB debugging always enabled (authorized users)
                SwitchPreference(
                    title = stringResource(R.string.usb_debugging_always_enabled_authorized_users),
                    key = Pref.Key.NubiaSystemSettings.SYSTEM_SETTINGS_USB_DEBUGGING_AUTO_ALLOW
                )
            }
        }
        // 特色功能
        item {
            PreferenceGroup(
                stringResource(R.string.android_title_features),
                visible = true // 默认显示状态栏卡片
            ) {
                // 手势默认打开数字助理
                SwitchPreference(
                    title = stringResource(R.string.gesture_use_default_digital_assist),
                    summary = stringResource(R.string.gesture_use_default_digital_assist_tips),
                    key = Pref.Key.SystemUI.StatusBar.GESTURE_USE_DEFAULT_DIGITAL_ASSIST
                )
                // 禁用音量键长按振动
                SwitchPreference(
                    title = stringResource(R.string.no_vibrate_volKey_Long_press),
                    summary = stringResource(R.string.no_vibrate_volKey_Long_press_tips),
                    key = Pref.Key.SystemUI.StatusBar.NO_VIBRATE_VOLKEY_LONG_PRESS
                )
                // 恢复显示安卓原生剪贴板浮窗
                SwitchPreference(
                    title = stringResource(R.string.unhide_clipboard_overlay),
                    key = Pref.Key.SystemUI.StatusBar.UNHIDE_CLIPBOARD_OVERLAY
                )
                // AOSP 单手模式调节
                SwitchPreference(
                    title = stringResource(R.string.aosp_singlehandmode_adjust),
                    key = Pref.Key.SystemUI.StatusBar.AOSP_SINGLEHANDMODE_ADJUST,
                    onCheckedChange = { newValue ->
                        visibilityAOSPSingleHandModeAdjust = newValue
                    }

                )
                AnimatedVisibility(visibilityAOSPSingleHandModeAdjust) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            title = stringResource(R.string.input_int),
                            key = Pref.Key.SystemUI.StatusBar.INPUT_INT,
                            defValue = 0,
                            dataType = EditTextDataType.INT,
                            dialogMessage = stringResource(R.string.aosp_singlehandmode_offest),
                        )
                    }
                }

            }
        }
        // 快速设置面板
        item {
            PreferenceGroup(
                stringResource(R.string.quick_settings_panel),
                visible = true // 默认显示状态栏卡片
            ) {
                // 点击日期可跳转第三方日历
                SwitchPreference(
                    title = stringResource(R.string.qs_shortcut_redir_calendar),
                    key = Pref.Key.SystemUI.StatusBar.QS_SHORTCUT_REDIR_CALENDAR
                )
                // 点击搜索打开浏览器
                SwitchPreference(
                    title = stringResource(R.string.qs_shortcut_redir_search),
                    key = Pref.Key.SystemUI.StatusBar.QS_SHORTCUT_REDIR_SEARCH,
                    onCheckedChange = { newValue ->
                        visibilityQsShowSearch = newValue
                    }

                )
                AnimatedVisibility(visibilityQsShowSearch) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            title = stringResource(R.string.custom_browser_package),
                            key = Pref.Key.SystemUI.StatusBar.QS_CUSTOM_BROWSER_PACKAGE,
                            defValue = "cn.nubia.browser",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.custom_browser_package_tips),
                        )
                    }
                }
                // 快速设置显示运营商
                SwitchPreference(
                    title = stringResource(R.string.qs_show_carrier),
                    key = Pref.Key.SystemUI.StatusBar.QS_SHOW_CARRIER
                )
                // 快速设置显示搜索按钮
                SwitchPreference(
                    title = stringResource(R.string.qs_show_search),
                    key = Pref.Key.SystemUI.StatusBar.QS_SHOW_SEARCH,

                )
                // 磁贴自定义行列数
                SwitchPreference(
                    title = stringResource(R.string.qs_custom_row_column_switch),
                    summary = stringResource(R.string.qs_custom_row_column_switch_tips),
                    key = Pref.Key.SystemUI.StatusBar.QS_CUSTOM_ROW_COLUMN_SWITCH,
                    onCheckedChange = { newValue ->
                        visibilityQsCustomRowColumn = newValue
                    }

                )
                AnimatedVisibility(visibilityQsCustomRowColumn) { // 确保 key 与当前开关的key 相同
                    Column {
                        SeekBarPreference(
                            title = stringResource(R.string.qs_custom_row),
                            key = Pref.Key.SystemUI.StatusBar.QS_CUSTOM_ROW,
                            defValue = 4,
                            min = 1,
                            max = 9
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.qs_custom_row_landscape),
                            key = Pref.Key.SystemUI.StatusBar.QS_CUSTOM_ROW_LANDSCAPE,
                            defValue = 1,
                            min = 1,
                            max = 4
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.qs_custom_column),
                            key = Pref.Key.SystemUI.StatusBar.QS_CUSTOM_COLUMN,
                            defValue = 5,
                            min = 1,
                            max = 9
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.qs_custom_column_landscape),
                            key = Pref.Key.SystemUI.StatusBar.QS_CUSTOM_COLUMN_LANDSCAPE,
                            defValue = 7,
                            min = 1,
                            max = 10
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.qs_custom_column_edit),
                            key = Pref.Key.SystemUI.StatusBar.QS_CUSTOM_COLUMN_EDIT,
                            defValue = 5,
                            min = 1,
                            max = 10
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.qs_custom_column_edit_landscape),
                            key = Pref.Key.SystemUI.StatusBar.QS_CUSTOM_COLUMN_EDIT_LANDSCAPE,
                            defValue = 6,
                            min = 1,
                            max = 10
                        )
                    }
                }
            }
        }


        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_systemui_notification_center),
                visible = false // 默认不显示通知卡片
            ) {
                SwitchPreference(
                    title = stringResource(R.string.systemui_notif_freeform),
                    summary = stringResource(R.string.systemui_notif_freeform_tips),
                    key = Pref.Key.SystemUI.NotifCenter.NOTIF_FREEFORM
                )
                SwitchPreference(
                    title = stringResource(R.string.systemui_notif_disable_whitelist),
                    key = Pref.Key.SystemUI.NotifCenter.NOTIF_NO_WHITELIST
                )
                DropDownPreference(
                    title = stringResource(R.string.systemui_notif_expand_notif),
                    summary = stringResource(R.string.systemui_notif_expand_notif_tips),
                    entries = expandNotificationsEntries,
                    key = Pref.Key.SystemUI.NotifCenter.EXPAND_NOTIFICATION
                )
                SwitchPreference(
                    title = stringResource(R.string.systemui_notif_miuix_expand_btn),
                    summary = stringResource(R.string.systemui_notif_miuix_expand_btn_tips),
                    key = Pref.Key.SystemUI.NotifCenter.MIUIX_EXPAND_BUTTON
                )
                TextPreference(
                    title = stringResource(R.string.systemui_notif_media_control_style),
                    summary = stringResource(R.string.systemui_notif_media_control_style_tips)
                ) {
                    navController.navigateTo(Pages.MEDIA_CONTROL)
                }
            }
        }
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_systemui_control_center),
                visible = false // 默认不显示通知卡片
            ) {
                SwitchPreference(
                    title = stringResource(R.string.systemui_control_hide_carrier_one),
                    key = Pref.Key.SystemUI.ControlCenter.HIDE_CARRIER_ONE
                )
                SwitchPreference(
                    title = stringResource(R.string.systemui_control_hide_carrier_two),
                    key = Pref.Key.SystemUI.ControlCenter.HIDE_CARRIER_TWO
                )
                SwitchPreference(
                    title = stringResource(R.string.systemui_control_hide_carrier_hd),
                    summary = stringResource(R.string.systemui_control_hide_carrier_hd_tips),
                    key = Pref.Key.SystemUI.ControlCenter.HIDE_CARRIER_HD
                )
                SwitchPreference(
                    title = stringResource(R.string.systemui_control_battery_percent),
                    summary = stringResource(R.string.systemui_control_battery_percent_tips),
                    key = Pref.Key.SystemUI.ControlCenter.BATTERY_PERCENTAGE
                ) {
                    visibilityCCBatteryPercent = it
                }
                AnimatedVisibility(
                    visibilityCCBatteryPercent
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.systemui_control_battery_percent_anim),
                        summary = stringResource(R.string.systemui_control_battery_percent_anim_tips),
                        key = Pref.Key.SystemUI.ControlCenter.BATTERY_PERCENTAGE_ANIM
                    )
                }
            }
        }
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_systemui_others),
                last = true,
                visible = false // 默认不显示其他卡片
            ) {
                SwitchPreference(
                    title = stringResource(R.string.systemui_others_monet_overlay),
                    summary = stringResource(R.string.systemui_others_monet_overlay_tips),
                    key = Pref.Key.SystemUI.NotifCenter.MONET_OVERLAY
                ) {
                    visibilityMonetColor = it
                }
                AnimatedVisibility(
                    visibilityMonetColor
                ) {
                    EditTextPreference(
                        title = stringResource(R.string.systemui_others_monet_color),
                        summary = stringResource(R.string.systemui_others_monet_color_tips),
                        key = Pref.Key.SystemUI.NotifCenter.MONET_OVERLAY_COLOR,
                        defValue = "#FF3482FF",
                        dataType = EditTextDataType.STRING,
                        dialogMessage = stringResource(R.string.systemui_others_monet_color_msg),
                        isValueValid = { color ->
                            (color as? String)?.let {
                                color.matches("#[0-9a-fA-f]{8}".toRegex()) || color.matches("#[0-9a-fA-f]{6}".toRegex())
                            } == true
                        }
                    )
                }
            }
        }
    }
}