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
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.hyperx.compose.preference.EditTextDataType
import dev.lackluster.hyperx.compose.preference.EditTextPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.hyperx.compose.preference.ValuePosition
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.ui.MainActivity
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.ui.component.RebootMenuItem
import dev.lackluster.mihelper.utils.Device
import java.io.File
import dev.lackluster.hyperx.compose.preference.DropDownPreference

@Composable
fun StatusBarTimeIndicatorPage(
    navController: NavController,
    adjustPadding: PaddingValues,
    mode: BasePageDefaults.Mode
) {
    var timeIndicator by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.TIME_INDICATOR)) }
    // 极客模式 开关的值
    var spValueGeekMode by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_GEEK)) }


    // 对齐方式
    val alignmentDropdownEntry = listOf(
        DropDownEntry(stringResource(R.string.status_bar_time_gravity_center)),
        DropDownEntry(stringResource(R.string.status_bar_time_gravity_top)),
        DropDownEntry(stringResource(R.string.status_bar_time_gravity_bottom)),
        DropDownEntry(stringResource(R.string.status_bar_time_gravity_end)),
        DropDownEntry(stringResource(R.string.status_bar_time_gravity_center_horizontal)),
        DropDownEntry(stringResource(R.string.status_bar_time_gravity_center_vertical)),
        DropDownEntry(stringResource(R.string.status_bar_time_gravity_fill)),
        DropDownEntry(stringResource(R.string.status_bar_time_gravity_fill_horizontal)),
        DropDownEntry(stringResource(R.string.status_bar_time_gravity_fill_vertical)),
    )
    val statusBarClockHorizontalAlignment = listOf(
        DropDownEntry(stringResource(R.string.horizontal_alignment_option1)),
        DropDownEntry(stringResource(R.string.horizontal_alignment_option2)),
        DropDownEntry(stringResource(R.string.horizontal_alignment_option3)),
        DropDownEntry(stringResource(R.string.horizontal_alignment_option4)),
    )
    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.ui_title_time_indicator),
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
                // 状态栏
                title = stringResource(R.string.ui_title_systemui_status_bar),
                first = true
            ) {
                SwitchPreference(
                    title = stringResource(R.string.ui_title_time_indicator),
                    key = Pref.Key.SystemUI.StatusBar.TIME_INDICATOR
                ) {
                    timeIndicator = it
                }
                AnimatedVisibility(
                    timeIndicator
                ) {
                    // 极客模式:数据的双向绑定？ 这里的 it 是当前组件的值
                    SwitchPreference(

//                        title = stringResource(R.string.clock_general_geek),
                        // 极客模式
                        title = if (spValueGeekMode && timeIndicator) {
                            stringResource(R.string.ui_title_clock_geek_mode)
                        } else {

                            // 标题内容的动态控制-快速设置
                            stringResource(R.string.ui_title_clock_easy)
                        },
                        key = Pref.Key.SystemUI.StatusBar.CLOCK_GEEK
                    ) {
                        spValueGeekMode = it
                    }

                }
            }

        }
        // 极客模式
        item {
//            AnimatedVisibility(
//                timeIndicator
//            ) {


                PreferenceGroup(
                    // 极客模式
                    title = if (spValueGeekMode && timeIndicator) {
                        stringResource(R.string.ui_title_clock_geek_mode)
                    } else {
                        if(!timeIndicator) return@item
                        // 标题内容的动态控制-快速设置
                        stringResource(R.string.ui_title_clock_easy)
                    },
                ) {
                    AnimatedVisibility(
                        timeIndicator
                    ) {
                        AnimatedVisibility(
                            spValueGeekMode
                        ) {
                            Column {
                                // 对齐方式
                                DropDownPreference(
                                    title = stringResource(R.string.alignment),
                                    entries =alignmentDropdownEntry, // 选项
                                    key = Pref.Key.SystemUI.StatusBar.ALIGNMENT  //唯一id
                                )
                                // 状态栏时间格式
                                EditTextPreference(
                                    title = stringResource(R.string.clock_geek_time_format_pattern),
                                    key = Pref.Key.SystemUI.StatusBar.CLOCK_GEEK_FORMAT,
                                    defValue = Pref.DefValue.SystemUI.CLOCK_GEEK_FORMAT,
                                    dataType = EditTextDataType.STRING
                                )
                                // 时钟大小
                                SeekBarPreference(
                                    title = stringResource(R.string.clock_size),
                                    key = Pref.Key.SystemUI.StatusBar.CLOCK_SIZE,
                                    defValue = 0,
                                    min = 0,
                                    max = 30
                                )
                                // 时钟边距

                                SeekBarPreference(
                                    title = stringResource(R.string.clock_top_margin),
                                    key = Pref.Key.SystemUI.StatusBar.NUBIA_CLOCK_PADDING_TOP,
                                    defValue = 0,
                                    min = 0,
                                    max = 30
                                )
                                SeekBarPreference(
                                    title = stringResource(R.string.clock_bottom_margin),
                                    key = Pref.Key.SystemUI.StatusBar.NUBIA_CLOCK_PADDING_DOWN,
                                    defValue = 0,
                                    min = 0,
                                    max = 30
                                )
                                SeekBarPreference(
                                    title = stringResource(R.string.clock_left_margin),
                                    key = Pref.Key.SystemUI.StatusBar.NUBIA_CLOCK_PADDING_LEFT,
                                    defValue = 0,
                                    min = 0,
                                    max = 30
                                )
                                SeekBarPreference(
                                    title = stringResource(R.string.clock_right_margin),
                                    key = Pref.Key.SystemUI.StatusBar.NUBIA_CLOCK_PADDING_RIGHT,
                                    defValue = 0,
                                    min = 0,
                                    max = 30
                                )
                            }
                        }
                        AnimatedVisibility(
                            !spValueGeekMode
                        ) {
                            Column {
                                // 水平对齐
                                DropDownPreference(
                                    title = stringResource(R.string.horizontal_alignment),
                                    entries =statusBarClockHorizontalAlignment, // 选项
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_HORIZONTAL_ALIGNMENT  //唯一id
                                )
                                // 显示年份
                                SwitchPreference(
                                    title = stringResource(R.string.show_years_title),
                                    summary = stringResource(R.string.show_years_summary),
                                    key = Pref.Key.SystemUI.StatusBar.SHOW_YEARS
                                )
                                // 显示月份
                                SwitchPreference(
                                    title = stringResource(R.string.show_month_title),
                                    summary = stringResource(R.string.show_month_summary),
                                    key = Pref.Key.SystemUI.StatusBar.SHOW_MONTH
                                )
                                // 显示日期
                                SwitchPreference(
                                    title = stringResource(R.string.show_day_title),
                                    summary = stringResource(R.string.show_day_summary),
                                    key = Pref.Key.SystemUI.StatusBar.SHOW_DAY
                                )
                                // 显示星期
                                SwitchPreference(
                                    title = stringResource(R.string.show_week_title),
                                    summary = stringResource(R.string.show_week_summary),
                                    key = Pref.Key.SystemUI.StatusBar.SHOW_WEEK
                                )
                                // 显示时辰
                                SwitchPreference(
                                    title = stringResource(R.string.show_cn_hour_title),
                                    summary = stringResource(R.string.show_cn_hour_summary),
                                    key = Pref.Key.SystemUI.StatusBar.SHOW_CN_HOUR
                                )
                                // 显示时段
                                SwitchPreference(
                                    title = stringResource(R.string.showtime_period_title),
                                    summary = stringResource(R.string.showtime_period_summary),
                                    key = Pref.Key.SystemUI.StatusBar.SHOW_PERIOD
                                )
                                // 显示秒数
                                SwitchPreference(
                                    title = stringResource(R.string.show_seconds_title),
                                    summary = stringResource(R.string.show_seconds_summary),
                                    key = Pref.Key.SystemUI.StatusBar.SHOW_SECONDS
                                )
                                // 显示毫秒数
                                SwitchPreference(
                                    title = stringResource(R.string.show_millisecond_title),
                                    summary = stringResource(R.string.show_millisecond_summary),
                                    key = Pref.Key.SystemUI.StatusBar.SHOW_MILLISECOND
                                )
                                // 隐藏间隔
                                SwitchPreference(
                                    title = stringResource(R.string.hide_space_title),
                                    summary = stringResource(R.string.hide_space_summary),
                                    key = Pref.Key.SystemUI.StatusBar.HIDE_SPACE
                                )
                                // 双排显示时间
                                SwitchPreference(
                                    title = stringResource(R.string.dual_row_title),
                                    summary = stringResource(R.string.dual_row_summary),
                                    key = Pref.Key.SystemUI.StatusBar.DUAL_ROW
                                )

                            }
                        }
                    }

                }
//            }
        }
    }
}
