package dev.lackluster.mihelper.ui.page

import android.content.Intent
import android.graphics.drawable.Icon
import android.provider.Settings
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.AlertDialogMode
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.hyperx.compose.preference.DropDownPreference
import dev.lackluster.hyperx.compose.preference.EditTextDataType
import dev.lackluster.hyperx.compose.preference.EditTextPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.ui.MainActivity
import dev.lackluster.mihelper.ui.component.RebootMenuItem
import dev.lackluster.mihelper.data.Pages
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.utils.ShellUtils
import top.yukonga.miuix.kmp.basic.Icon


@Composable
fun SystemSettingsPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {

    var visibilitySystemSettingsUsbModeNotificationShow by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.NubiaSystemSettings.SYSTEM_SETTINGS_USB_MODE)
        )
    }

    var visibilityDisplaySystemSettingsDevelop by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.NubiaSystemSettings.DISPLAY_SYSTEM_SETTINGS_DEVELOP)
        )
    }
    // usb 弹窗默认选项
    val usbOption = listOf(
        // 默认
        DropDownEntry(stringResource(R.string.system_settings_usb_default)),
        // 仅限充电
        DropDownEntry(stringResource(R.string.system_usb_use_charging_only)),
        // 传输文件
        DropDownEntry(stringResource(R.string.system_usb_use_file_transfers)),
        // 传输照片
        DropDownEntry(stringResource(R.string.system_usb_use_photo_transfers)),
        // 多屏投屏
        DropDownEntry(stringResource(R.string.system_usb_use_multi_screen_projection)),
    )

    BasePage(
        navController,
        adjustPadding,
        // 标题：系统设置
        stringResource(R.string.system_settings),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            // 系统设置重启
            // appPkg 重启的对应包名：com.android.settings
            RebootMenuItem(
                // 弹窗标题的提示名称
                appName = stringResource(R.string.system_settings),
                // 重启：com.android.settings
                appPkg = Scope.SYSTEM_SETTINGS
                // 重启系统
//                appPkg = "android"
            )
        }
    ) {
        // 开发者选项
        item {
            val context = LocalContext.current
            PreferenceGroup(
                stringResource(R.string.system_settings_develop_title),
            ) {
                // 显示开发者选项设置
                //# 开启开发者选项
                //settings put global development_settings_enabled 1
                //# 关闭开发者选项
                //settings put global development_settings_enabled 0

                // 显示开发者选项
                SwitchPreference(
                    title = stringResource(R.string.display_system_settings_develop),
                    summary = if (visibilityDisplaySystemSettingsDevelop) {
                        stringResource(R.string.display_system_settings_develop_tips1)
                    } else {
                        stringResource(R.string.display_system_settings_develop_tips2)

                    },
                    key = Pref.Key.NubiaSystemSettings.DISPLAY_SYSTEM_SETTINGS_DEVELOP
                ) { value ->
                    visibilityDisplaySystemSettingsDevelop = value
                    // 根据开关状态执行命令：开启 (1) 或关闭 (0) 开发者选项
                    ShellUtils.tryExec("settings put global development_settings_enabled ${if (value) 1 else 0}", useRoot = true)
                }
                AnimatedVisibility(visibilityDisplaySystemSettingsDevelop) {
                    // 进入开发者选项
                    TextPreference(
                        title = stringResource(R.string.system_settings_develop_title),
                        summary = stringResource(R.string.system_settings_develop_title_tips)
                    ) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                        context.startActivity(intent)
                    }

                }

                // 开启USB调试 settings put global adb_enabled 1
                // 关闭USB调试 settings put global adb_enabled 0
                // USB调试
                SwitchPreference(
                    title = stringResource(R.string.usb_debugging),
                    summary = stringResource(R.string.usb_debugging_tips),
                    key = Pref.Key.NubiaSystemSettings.USB_DEBUGGING
                ){value ->
                    ShellUtils.tryExec("settings put global adb_enabled ${if (value) 1 else 0}", useRoot = true)
                }
                // USB安装
                // 开启USB安装 settings put global adb_install 1
                // 关闭USB安装 settings put global adb_install 0
                SwitchPreference(
                    title = stringResource(R.string.usb_install),
                    key = Pref.Key.NubiaSystemSettings.USB_INSTALL
                ){value ->
                    ShellUtils.tryExec("settings put system adb_install_enabled ${if (value) 1 else 0}", useRoot = true)
                }
                // 禁用USB安装开关账号验证
                SwitchPreference(
                    title = stringResource(R.string.disable_usb_installation_and_switch_account_verification),
                    key = Pref.Key.NubiaSystemSettings.DISABLE_USB_INSTALLATION_AND_SWITCH_ACCOUNT_VERIFICATION, //唯一id
                )
                // 锁定屏幕刷新率
                // 开启 settings put system lock_refresh_rate 1
                // 关闭 settings put system lock_refresh_rate 0

                SwitchPreference(
                    title = stringResource(R.string.lock_refresh_rate),
                    key = Pref.Key.NubiaSystemSettings.LOCK_REFRESH_RATE
                ){value ->
                    ShellUtils.tryExec("settings put system lock_refresh_rate ${if (value) 1 else 0}", useRoot = true)
                }

                // 接入 USB时不弹窗
                SwitchPreference(
                    title = stringResource(R.string.system_settings_usb_mode),
                    key = Pref.Key.NubiaSystemSettings.SYSTEM_SETTINGS_USB_MODE
                ) {
                    visibilitySystemSettingsUsbModeNotificationShow= it
                }
                // USB 弹窗默认选项
                DropDownPreference(
                    title = stringResource(R.string.system_settings_usb_mode_choose),
                    entries = usbOption, // 选项
                    key = Pref.Key.NubiaSystemSettings.SYSTEM_SETTINGS_USB_MODE_CHOOSE  //唯一id
                )

                AnimatedVisibility(
                    visibilitySystemSettingsUsbModeNotificationShow
                ) {
                    SwitchPreference(
                        //title = stringResource(R.string.system_settings_usb_mode),
                        title = "通知栏点击 USB 通知时，正常显示弹窗",
                        key = Pref.Key.NubiaSystemSettings.SYSTEM_SETTINGS_USB_MODE_NOTIFICATION_SHOW

                    )
                }


            }
        }
        // 红魔-显示
        item {
            PreferenceGroup(
                stringResource(R.string.ui_title_android_display),
                visible =  true // 默认显示状态栏卡片
            ) {
                // 自动熄屏
                SwitchPreference(
                    title = stringResource(R.string.automatic_screen_off),
                    // 设置-显示-自动熄屏（解锁选项：30分钟、从不）
                    summary = stringResource(R.string.automatic_screen_off_tips),
                    key = Pref.Key.NubiaSystemSettings.AUTOMATIC_SCREEN_OFF
                )
            }
        }
        //// 红魔-刷新率
        //item {
        //    PreferenceGroup(
        //        stringResource(R.string.refresh_rate),
        //        visible =  true // 默认显示状态栏卡片
        //    ) {
        //        // 解锁165hz选项
        //        SwitchPreference(
        //            title = stringResource(R.string.refresh_rate_unlock_165hz),
        //            // 实际刷新率还是144hz，只是多个选项
        //            summary = stringResource(R.string.refresh_rate_unlock_165hz_tips),
        //            key = Pref.Key.NubiaSystemSettings.UNLOCK_165HZ
        //        )
        //    }
        //}
        // 红魔-系统设置
        item {
            PreferenceGroup(
                stringResource(R.string.system_settings),
                visible =  true // 默认显示状态栏卡片
            ) {
                // 禁用电量百分比显示选项
                SwitchPreference(
                    title = stringResource(R.string.disable_attery_percentage_display_setting_option),
                    key = Pref.Key.SystemUI.IconTurner.DISABLE_ATTERY_PERCENTAGE_DISPLAY_SETTING_OPTION
                )
                // 时间选择器支持多种时段选择
                SwitchPreference(
                    title = stringResource(R.string.time_picker_display_period),
                    summary = stringResource(R.string.time_picker_display_period_summary),
                    key = Pref.Key.NubiaSystemSettings.TIME_PICKER_PERIOD, //唯一id
                )
            }
        }
    }
}