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
import dev.lackluster.hyperx.compose.preference.EditTextDataType
import dev.lackluster.hyperx.compose.preference.EditTextPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Device

@Composable
fun StatusBarClockPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    // 自定义内边距:开关的值
    var spValueLayoutCustom by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_LAYOUT_CUSTOM)) }
    // 极客模式 开关的值
    var spValueGeekMode by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_GEEK)) }
    var spValueShowSecond by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_SECONDS)) }

    BasePage(
        navController,
        adjustPadding,
        // 主标题：时间
        stringResource(R.string.page_status_bar_clock),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode
    ) {
        item {
            PreferenceGroup(
                // 通用
                title = stringResource(R.string.ui_title_clock_general),
                first = true
            ) {
                // 自定义时间内边距
                SwitchPreference(
                    title = stringResource(R.string.clock_general_custom_layout),
                    // 通过共享的key,这里hook方法通过判断该值进行hook
                    key = Pref.Key.SystemUI.StatusBar.CLOCK_LAYOUT_CUSTOM
                ) {
                    spValueLayoutCustom = it
                }
                // 自定义时间内边距
                SwitchPreference(
                    title = stringResource(R.string.clock_general_custom_layout),
                    // 通过共享的key,这里hook方法通过判断该值进行hook
                    key = Pref.Key.SystemUI.StatusBar.CLOCK_LAYOUT_CUSTOM
                ) {
                    // 数据双向绑定
                    spValueLayoutCustom = it
                }
                AnimatedVisibility(
                    // 控制自定义内边距的动态的创建或者隐藏组件
                    spValueLayoutCustom
                ) {
                    Column {
                        // 左内边距
                        EditTextPreference(
                            title = stringResource(R.string.clock_general_padding_left),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_PADDING_LEFT,
                            defValue = 0.0f,
                            dataType = EditTextDataType.FLOAT,
                            dialogMessage = stringResource(R.string.clock_general_custom_layout)
                        )
                        // 右内边距
                        EditTextPreference(
                            title = stringResource(R.string.clock_general_padding_right),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_PADDING_RIGHT,
                            defValue = 0.0f,
                            dataType = EditTextDataType.FLOAT,
                            dialogMessage = stringResource(R.string.clock_general_custom_layout)
                        )
                    }
                }
                // 极客模式:数据的双向绑定？ 这里的 it 是当前组件的值
                SwitchPreference(
                    title = stringResource(R.string.clock_general_geek),
                    key = Pref.Key.SystemUI.StatusBar.CLOCK_GEEK
                ) {
                    spValueGeekMode = it
                }
                SwitchPreference(
                    title = stringResource(R.string.clock_font_tnum),
                    summary = stringResource(R.string.clock_font_tnum_tips),
                    key = Pref.Key.SystemUI.StatusBar.CLOCK_TNUM
                )
            }
        }
        // 极客模式
        item {
            PreferenceGroup(
                // 极客模式
                title = if (spValueGeekMode) {
                    stringResource(R.string.ui_title_clock_geek_mode)
                } else {
                    // 标题内容的动态控制-快速设置
                    stringResource(R.string.ui_title_clock_easy)
                },
                last = true
            ) {
                AnimatedVisibility(
                    spValueGeekMode
                ) {
                    Column {
                        // 状态栏时间格式
                        EditTextPreference(
                            title = stringResource(R.string.clock_geek_time_format_pattern),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_GEEK_FORMAT,
                            defValue = Pref.DefValue.SystemUI.CLOCK_GEEK_FORMAT,
                            dataType = EditTextDataType.STRING
                        )
                        // 如果设备是pad
                        // 状态栏日期格式
                        if (Device.isPad) {
                            EditTextPreference(
                                title = stringResource(R.string.clock_geek_time_format_pattern_pad),
                                key = Pref.Key.SystemUI.StatusBar.CLOCK_GEEK_FORMAT_PAD,
                                defValue = Pref.DefValue.SystemUI.CLOCK_GEEK_FORMAT_PAD,
                                dataType = EditTextDataType.STRING
                            )
                        }
                        // 通知中心日期格式
                        EditTextPreference(
                            title = stringResource(R.string.clock_geek_time_format_pattern_horizon),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_GEEK_FORMAT_HORIZON,
                            defValue = Pref.DefValue.SystemUI.CLOCK_GEEK_FORMAT_HORIZON,
                            dataType = EditTextDataType.STRING
                        )
                    }
                }
                AnimatedVisibility(
                    !spValueGeekMode
                ) {
                    Column {
                        // 显示月/日
                        SwitchPreference(
                            title = stringResource(R.string.clock_easy_show_month_day),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_MONTH_DAY
                        )
                        // 新增组件-显示星期
                        SwitchPreference(
                            title = stringResource(R.string.clock_easy_show_week),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_WEEK
                        )
                        // 新增组件-显示时段
                        SwitchPreference(
                            title = stringResource(R.string.clock_easy_show_period),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_PERIOD
                        )

                        // 显示上午或者下午
                        SwitchPreference(
                            title = stringResource(R.string.clock_easy_show_ampm),
                            summary = stringResource(R.string.clock_easy_show_ampm_tips),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_AMPM
                        )
                        SwitchPreference(
                            title = stringResource(R.string.clock_easy_show_leading_zero),
                            summary = stringResource(R.string.clock_easy_show_leading_zero_tips),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_LEADING_ZERO
                        )
                        SwitchPreference(
                            title = stringResource(R.string.clock_easy_show_seconds),
                            summary = stringResource(R.string.clock_easy_show_seconds_tips),
                            key = Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_SECONDS
                        ) {
                            // 如果需要动态的控制，根据值，可以使用这个
                            spValueShowSecond = it
                        }
                        AnimatedVisibility(
                            spValueShowSecond
                        ) {
                            SwitchPreference(
                                title = stringResource(R.string.clock_easy_fixed_width),
                                summary = stringResource(R.string.clock_easy_fixed_width_tips),
                                key = Pref.Key.SystemUI.StatusBar.CLOCK_FIXED_WIDTH
                            )
                        }
                    }
                }
            }
        }
    }
}