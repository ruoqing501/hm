package dev.lackluster.mihelper.ui.page


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.hyperx.compose.preference.DropDownPreference
import dev.lackluster.hyperx.compose.preference.MultDropDownPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SeekBarPreferenceDefault3
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.ui.MainActivity
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.ui.component.RebootMenuItem

@Composable
fun StatusBarDisplayBatteryInfoPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {

    // 红魔-多选
    val statusBarDisplayBatteryInfoPosition =  listOf(
        DropDownEntry(stringResource(R.string.status_bar_display_temp_position_left)),
        DropDownEntry(stringResource(R.string.status_bar_display_temp_position_right)),
        DropDownEntry(stringResource(R.string.status_bar_display_temp_position_center)),
    )
    val statusBarDisplayBatteryInfoIsChargeMode =  listOf(
        DropDownEntry(stringResource(R.string.status_bar_display_battery_info_is_charge_mode_option1)),
        DropDownEntry(stringResource(R.string.status_bar_display_battery_info_is_charge_mode_option2)),
    )

    val statusBarDisplayBatteryInfoTempMode =  listOf(
        DropDownEntry(stringResource(R.string.status_bar_display_temp_digital_mode_number_int)),
        DropDownEntry(stringResource(R.string.status_bar_display_temp_digital_mode_number_float)),
    )



    val statusBarDisplayBatteryInfoMode =  listOf(
        DropDownEntry(stringResource(R.string.status_bar_display_battery_info_mode_option1)),
        DropDownEntry(stringResource(R.string.status_bar_display_battery_info_mode_option2)),
    )

    var enableDisplayBatteryInfo by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO))}
    var enableDisplayBatteryInfoTempModeSingleSwitch by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION1))}
    var enableDisplayBatteryLayout by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_LAYOUT))}

    var enableStatusBarDisplayBatteryInfoDualDisplayMode by remember { mutableIntStateOf(SafeSP.getInt( Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_DUAL_DISPLAY_MODE)) }


    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.ui_title_status_bar_display_battery_info),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            RebootMenuItem(
                appName = stringResource(R.string.scope_systemui),
                appPkg = Scope.SYSTEM_UI
            )
        }

    ) {
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_status_bar_display_battery_info)
            ) {
                // 启用模块
                SwitchPreference(
                    title = stringResource(R.string.ui_title_status_bar_enable_modules),
                    summary = stringResource(R.string.ui_summary_status_bar_enable_modules_summary),
                    key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO,
                ) {
                    enableDisplayBatteryInfo = it
                }
                AnimatedVisibility(
                    enableDisplayBatteryInfo
                ) {
                    Column() {
                        SwitchPreference(
                            title = if (enableDisplayBatteryLayout) {
                                stringResource(R.string.ui_title_status_bar_display_battery_info_dual_line)

                            } else {
                                stringResource(R.string.ui_title_status_bar_display_battery_info_single_line)
                            },
                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_LAYOUT
                        ) {
                            enableDisplayBatteryLayout = it
                        }
                        AnimatedVisibility(
                            !enableDisplayBatteryLayout
                        ) {
                            Column() {
                                // 显示在左侧 还是右侧
                                DropDownPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_position),
                                    entries = statusBarDisplayBatteryInfoPosition, // 选项
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_BATTERY_INFO_LOCATION  //唯一id
                                )
                                // 显示场景
                                DropDownPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_display_scene),
                                    entries = statusBarDisplayBatteryInfoIsChargeMode, // 选项
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_IS_CHARGE_MODE_SINGLE //唯一id
                                )
                                // 隐藏标题前缀
                                SwitchPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_hide_title),
                                    key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_HIDE_TITLE_SINGLE,
                                )

                                SwitchPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_option1),
                                    key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION1,
                                ){
                                    enableDisplayBatteryInfoTempModeSingleSwitch = it
                                }
                                AnimatedVisibility(
                                    enableDisplayBatteryInfoTempModeSingleSwitch
                                ) {
                                    Column() {
                                        // 显示模式
                                        DropDownPreference(
                                            title = stringResource(R.string.status_bar_display_battery_info_temp_mode),
                                            entries = statusBarDisplayBatteryInfoTempMode, // 选项
                                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_TEMP_MODE_SINGLE  //唯一id
                                        )
                                    }
                                }
                                SwitchPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_option2),
                                    key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION2,
                                )
                                SwitchPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_option3),
                                    key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION3
                                )
                                // 固定宽度以防相邻元素左右抖动(单排dp)
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.ui_title_status_bar_battery_info_fixed_width),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_BATTERY_INFO_FIXED_WIDTH_SINGLE,
                                    min = 12,
                                    max = 120,
                                )
                                SeekBarPreference(
                                    title = stringResource(R.string.status_bar_display_temp_font_size), //字体大小
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_FONT_SIZE,
                                    defValue = 8,
                                    min = 6,
                                    max = 12
                                )

                            }

                        }
                        AnimatedVisibility(
                            enableDisplayBatteryLayout
                        ) {
                            Column() {
                                // 显示在左侧 还是右侧 还是居中
                                DropDownPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_position),
                                    entries = statusBarDisplayBatteryInfoPosition, // 选项
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_BATTERY_INFO_LOCATION_DUAL  //唯一id
                                )
                                // 显示场景
                                DropDownPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_display_scene),
                                    entries = statusBarDisplayBatteryInfoIsChargeMode, // 选项
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_IS_CHARGE_MODE_DUAL //唯一id
                                )
                                // 隐藏标题前缀
                                SwitchPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_hide_title),
                                    key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_HIDE_TITLE_DUAL,
                                )
                                DropDownPreference(
                                    title = stringResource(R.string.status_bar_display_battery_info_dual_display_mode),
                                    entries = statusBarDisplayBatteryInfoMode, // 选项
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_DUAL_DISPLAY_MODE  //唯一id
                                ){
                                    enableStatusBarDisplayBatteryInfoDualDisplayMode = it
                                }
                                AnimatedVisibility(
                                    enableStatusBarDisplayBatteryInfoDualDisplayMode in listOf(0)
                                ) {
                                    Column() {
                                        // 显示模式
                                        DropDownPreference(
                                            title = stringResource(R.string.status_bar_display_battery_info_temp_dual_float),
                                            entries = statusBarDisplayBatteryInfoTempMode, // 选项
                                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_TEMP_MODE_DUAL  //唯一id
                                        )
                                    }
                                }
                                // 固定宽度以防相邻元素左右抖动(双排dp)
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.ui_title_status_bar_battery_info_fixed_width),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_BATTERY_INFO_FIXED_WIDTH_DUAL,
                                    min = 12,
                                    max = 120,
                                )
                                SeekBarPreference(
                                    title = stringResource(R.string.status_bar_display_temp_font_size), //字体大小
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_BATTERY_INFO_FONT_SIZE_DUAL,
                                    defValue = 8,
                                    min = 4,
                                    max = 12
                                )
                            }

                        }

                    }

                }


            }
        }

    }
}