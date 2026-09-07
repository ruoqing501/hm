package dev.lackluster.mihelper.ui.page


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
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.hyperx.compose.preference.DropDownPreference
import dev.lackluster.hyperx.compose.preference.MultDropDownPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.ui.MainActivity
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.ui.component.RebootMenuItem

@Composable
fun StatusBarDisplayTempPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {

    // 红魔-多选
    val statusBarPullDownTimelEntries = listOf(
        DropDownEntry(stringResource(R.string.clock_easy_pull_down_time_hide_second)),
        DropDownEntry(stringResource(R.string.clock_easy_pull_down_time_display_second)),
    )

    val statusBarDisplayTempPosition =  listOf(
        DropDownEntry(stringResource(R.string.status_bar_display_temp_position_left)),
        DropDownEntry(stringResource(R.string.status_bar_display_temp_position_right)),
        DropDownEntry(stringResource(R.string.status_bar_display_temp_position_center)),
    )

    val statusBarDisplayTempDigitalDisplayMode =  listOf(
        DropDownEntry(stringResource(R.string.status_bar_display_temp_digital_mode_number_int)),
        DropDownEntry(stringResource(R.string.status_bar_display_temp_digital_mode_number_float)),
    )

    var enableDisplayTemp by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMPERATURE_SWITCH))}



    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.ui_title_status_bar_display_temp),
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
                title = stringResource(R.string.ui_title_status_bar_display_temp)
            ) {
                // 启用模块
                SwitchPreference(
                    title = stringResource(R.string.ui_title_status_bar_enable_modules),
                    summary = stringResource(R.string.ui_summary_status_bar_enable_modules_summary),
                    key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMPERATURE_SWITCH,
                ) {
                    enableDisplayTemp = it
                }
                AnimatedVisibility(
                    enableDisplayTemp
                ) {
                    Column() {
                        // 显示在左侧 还是右侧
                        DropDownPreference(
                            title = stringResource(R.string.status_bar_display_temp_position),
                            entries = statusBarDisplayTempPosition, // 选项
                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_TEMPERATURE_LOCATION  //唯一id
                        )
                        DropDownPreference(
                            title = stringResource(R.string.status_bar_display_temp_display_mode),
                            entries = statusBarDisplayTempDigitalDisplayMode, // 选项
                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_DISPLAY_MODE  //唯一id
                        )

                        SwitchPreference(
                            title = stringResource(R.string.status_bar_display_temp_option1),
                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_BATTERY,
                        )
                        SwitchPreference(
                            title = stringResource(R.string.status_bar_display_temp_option2),
                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_CPU,
                        )
                        // 时钟跨双排显示
                        SwitchPreference(
                            title = stringResource(R.string.status_bar_display_temp_option3),
                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_GPU
                        )
                        // 隐藏单位
                        SwitchPreference(
                            title = stringResource(R.string.status_bar_display_temp_hide_unit),
                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_HIDE_UNIT
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.status_bar_display_temp_font_size),
                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_FONT_SIZE,
                            defValue = 8,
                            min = 6,
                            max = 12
                        )
                    }

                }


            }
        }

//        item {
////            PreferenceGroup(
////                title = stringResource(R.string.ui_title_status_bar_display_temp)
////            ) {
////                // 下拉状态栏时间
//////                MultDropDownPreference(
//////                    title = stringResource(R.string.clock_easy_pull_down_time),
//////                    entries = statusBarPullDownTimelEntries, // 选项
//////                    multiple = true,
//////                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMPERATURE  //唯一id
//////                )
////
////            }
//
//            PreferenceGroup(
//                title = stringResource(R.string.ui_title_status_bar_display_temp)
//            ) {
//                // 开启显示设备温度
//                SwitchPreference(
//                    title = stringResource(R.string.ui_title_status_bar_enable_modules),
//                    key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMPERATURE
//                ) {
//                    enableDisplayTemp = it
//                }
//                AnimatedVisibility(
//                    enableDisplayTemp
//                ) {
//                    Column() {
//                        SwitchPreference(
//                            title = stringResource(R.string.status_bar_display_temp_option1),
//                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_BATTERY,
//                        )
//                        SwitchPreference(
//                            title = stringResource(R.string.status_bar_display_temp_option2),
//                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_CPU,
//                        )
//                        SwitchPreference(
//                            title = stringResource(R.string.status_bar_display_temp_option3),
//                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DISPLAY_TEMP_GPU
//                        )
//                    }
//
//                }
//
//
//            }
//        }

    }
}