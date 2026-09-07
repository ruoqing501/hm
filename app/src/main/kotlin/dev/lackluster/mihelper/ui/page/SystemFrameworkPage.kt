package dev.lackluster.mihelper.ui.page

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.preference.EditTextDataType
import dev.lackluster.hyperx.compose.preference.EditTextDialog
import dev.lackluster.hyperx.compose.preference.EditTextPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.data.Pages
import dev.lackluster.mihelper.ui.MainActivity
import dev.lackluster.mihelper.ui.component.RebootMenuItem
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.utils.ShellUtils

@Composable
fun SystemFrameworkPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    val context = LocalContext.current

    var checkFontScale by remember { mutableStateOf(false) }
    var currentFontScale by remember { mutableFloatStateOf(0.0f) }
    val dialogFontScaleFailedVisibility = remember { mutableStateOf(false) }
    val dialogFontScaleVisibility = remember { mutableStateOf(false) }
    var visibilityFontScaleValue by remember { mutableStateOf(
        SafeSP.getBoolean(Pref.Key.Android.FONT_SCALE)
    ) }
    var visibilityTelecomWlanCCSWitch by remember { mutableStateOf(
        SafeSP.getBoolean(Pref.Key.Android.TELECOM_WLAN_CC)
    ) }

    var visibilityPinStaMacTips by remember { mutableStateOf(
        SafeSP.getBoolean(Pref.Key.Android.PIN_STA_MAC)
    ) }



    var visibilityPinApBssid by remember { mutableStateOf(
        SafeSP.getBoolean(Pref.Key.Android.PIN_AP_BSSID)
    ) }
    var visibilitySystemFrameworkLockScreenTimeout by remember { mutableStateOf(
        SafeSP.getBoolean(Pref.Key.Android.SYSTEM_FRAMEWORK_LOCK_SCREEN_TIMEOUT)
    ) }

    LaunchedEffect(checkFontScale) {
        try {
            ShellUtils.tryExec(
                "settings get system font_scale",
                useRoot = true,
                checkSuccess = true
            ).let { result ->
                val newScale = result.successMsg.toFloatOrNull()
                if (result.exitCode == 0 && newScale != null) {
                    currentFontScale = newScale
                } else {
                    dialogFontScaleFailedVisibility.value = true
                }
            }
        } catch (tout: Throwable) {
            Toast.makeText(
                context,
                tout.message,
                Toast.LENGTH_LONG
            ).show()
            dialogFontScaleFailedVisibility.value = true
        }
    }

    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.page_android),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            RebootMenuItem(
                appName = stringResource(R.string.scope_android),
                appPkg = Scope.ANDROID
            )
        }
    ) {
//        item {
//            PreferenceGroup(
//                title ="强制使用鸣潮主题",
//            ) {
//                SwitchPreference(
//                    title = stringResource(R.string.ui_title_theme_cancel_trial_login),
//                    summary = stringResource(R.string.ui_title_theme_cancel_trial_login_tips),
//                    key = Pref.Key.Android.ANDROID_FORCE_CUSTOM_THEME, //唯一id
//                )
//            }
//        }
        // 红魔-小窗
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_window),
                first = true
            ) {

                SwitchPreference(
                    title = stringResource(R.string.android_remove_restrictions_window),
                    key = Pref.Key.Android.REMOVE_RESTRICTIONS_WINDOW
                )
                SwitchPreference(
                    title = stringResource(R.string.android_remove_restrictions_window_number),
                    summary = stringResource(R.string.android_remove_restrictions_window_number_tips),
                    key = Pref.Key.Android.REMOVE_RESTRICTIONS_WINDOW_NUMBER
                )

            }
        }


        // 底层-禁用温控（）
        item {
            PreferenceGroup(
                title = stringResource(R.string.system_framework_core_title),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.system_framework_other_disable_thermal),
                    summary = stringResource(R.string.system_framework_other_disable_thermal_desc),
                    key = Pref.Key.Android.SYSTEM_FRAMEWORK_OTHER_DISABLE_THERMAL
                )
            }
        }
        // 红魔-锁屏
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_systemui_lock_screen),
            ) {
                // 禁用每 72 小时验证锁屏密码
                SwitchPreference(
                    title = stringResource(R.string.system_framework_disable_72h_verify),
                    key = Pref.Key.Android.SYSTEM_FRAMEWORK_DISABLE_72H_VERIFY
                )
                // 锁屏界面超时时间
                SwitchPreference(
                    title = stringResource(R.string.system_framework_lock_screen_timeout),
                    summary = stringResource(R.string.system_framework_lock_screen_timeout_tips),
                    key = Pref.Key.Android.SYSTEM_FRAMEWORK_LOCK_SCREEN_TIMEOUT,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        visibilitySystemFrameworkLockScreenTimeout = newValue
                    }
                )
                AnimatedVisibility(
                    visibilitySystemFrameworkLockScreenTimeout
                ) {
                    Column {
                        // 刷新间隔时间(秒)
                        SeekBarPreference(
                            title = stringResource(R.string.system_framework_lock_screen_timeout),
                            key = Pref.Key.Android.SYSTEM_FRAMEWORK_LOCK_SCREEN_TIMEOUT_VALUE,
                            defValue = 10,
                            min = 10,
                            max = Int.MAX_VALUE,
                        )
                    }
                }

            }
        }
        // 红魔-截图
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_screenshot),
            ) {

                SwitchPreference(
                    title = stringResource(R.string.android_display_screenshots_allowed),
                    summary = stringResource(R.string.android_display_screenshots_allowed_tips),
                    key = Pref.Key.Android.DISABLE_FLAG_SECURE_ENHANCED
                )

            }
        }
        item {
            // 红魔-声音

            PreferenceGroup(
                stringResource(R.string.ui_title_android_audio),
                first = true,
            ) {
                //TextPreference(
                //    title = stringResource(R.string.ui_title_android_audio)
                //) {
                //    // 导航到音量界面
                //    navController.navigateTo(Pages.ANDROID_AUDIO)
                //}
                SwitchPreference(
                    title = stringResource(R.string.android_mute_volume_detection),
                    summary = stringResource(R.string.android_mute_volume_detection_tips),
                    key = Pref.Key.SystemUI.Volume.DISABLE_SAFETY_WARNING
                )
            }
        }


        // 红魔-飞行模式
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_airplane_mode),
            ) {

                SwitchPreference(
                    title = stringResource(R.string.android_airplane_mode_keep_bluetooth),
                    summary = stringResource(R.string.android_airplane_mode_keep_bluetooth_tips),
                    key = Pref.Key.Android.ANDROID_AIRPLANE_MODE_KEEP_BLUETOOTH
                )
                SwitchPreference(
                    title = stringResource(R.string.airplane_mode_keep_wlan),
                    summary = stringResource(R.string.airplane_mode_keep_wlan_tips),
                    key = Pref.Key.Android.ANDROID_AIRPLANE_MODE_KEEP_WLAN
                )
            }
        }
        //红魔-WLAN服务
        item {
            PreferenceGroup(
                stringResource(R.string.ui_title_wlan_server),
                visible = true //默认显示卡片
            ) {

                // 直接使用第一种 SwitchPreference，不要传入 checked 参数
                SwitchPreference(
                    title = stringResource(R.string.telecom_wlan_cc),
                    summary = stringResource(R.string.telecom_wlan_cc_set_tips),
                    key = Pref.Key.Android.TELECOM_WLAN_CC,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        visibilityTelecomWlanCCSWitch = newValue
                    }
                )

                AnimatedVisibility(visibilityTelecomWlanCCSWitch) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            title = stringResource(R.string.telecom_wlan_cc_set),
                            key = Pref.Key.Android.TELECOM_WLAN_CC_DIALOG,
                            defValue = "US",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.telecom_wlan_cc_set_tips) ,
                            isValueValid = { value ->
                                val countryCode = value as? String
                                countryCode != null &&
                                        countryCode.length == 2 &&
                                        countryCode.all { it.isLetter() && it.isUpperCase() }
                            }
                        )
                    }
                }

                 // 固定终端MAC地址
                SwitchPreference(
                    title = stringResource(R.string.pin_sta_mac),
                    summary = stringResource(R.string.pin_sta_mac_tips),
                    key = Pref.Key.Android.PIN_STA_MAC,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        visibilityPinStaMacTips = newValue
                    }
                )
                AnimatedVisibility(visibilityPinStaMacTips) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            title = stringResource(R.string.pin_sta_mac_set),
                            key = Pref.Key.Android.PIN_STA_MAC_DIALOG,
                            defValue = "66:31:32:35:39:75", // 默认 MAC 地址示例
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.pin_sta_mac_tips),
                            isValueValid = { value ->
                                try {
                                    val mac = value as? String
                                    if (mac != null) {
                                        android.net.MacAddress.fromString(mac) // 尝试解析 MAC 地址
                                        true // 解析成功，视为有效
                                    } else {
                                        false // 输入为空或不是字符串，视为无效
                                    }
                                } catch (e: IllegalArgumentException) {
                                    false // 解析失败，视为无效
                                }
                            }
                        )
                    }
                }


                // 固定热点随机BSSID
                SwitchPreference(
                    title = stringResource(R.string.pin_ap_bssid),
                    summary = stringResource(R.string.pin_ap_bssid_tips),
                    key = Pref.Key.Android.PIN_AP_BSSID,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        visibilityPinApBssid = newValue
                    }
                )
                AnimatedVisibility(visibilityPinApBssid) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            title = stringResource(R.string.pin_sta_mac_set),
                            key = Pref.Key.Android.PIN_AP_BSSID_DIALOG,
                            defValue = "b4:f3:cb:a7:b8:e7", // 默认 MAC 地址示例
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.pin_ap_bssid_tips),
                            isValueValid = { value ->
                                try {
                                    val mac = value as? String
                                    if (mac != null) {
                                        android.net.MacAddress.fromString(mac) // 尝试解析 MAC 地址
                                        true // 解析成功，视为有效
                                    } else {
                                        false // 输入为空或不是字符串，视为无效
                                    }
                                } catch (e: IllegalArgumentException) {
                                    false // 解析失败，视为无效
                                }
                            }
                        )
                    }
                }
            }
        }
        // 红魔通知
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_notification),
            ) {
                // 移除显示上层通知
                SwitchPreference(
                    title = stringResource(R.string.android_display_remove_alert_windows_notification),
                    summary = stringResource(R.string.android_display_remove_alert_windows_notification_tips),
                    key = Pref.Key.Android.REMOVE_ALERT_WINDOWS_NOTIFICATION
                )
                // 亮屏时屏蔽通知声音和振动
                SwitchPreference(
                    title = stringResource(R.string.android_display_suppress_sound_and_vibration_for_notifications_when_screen_is_on),
                    summary = stringResource(R.string.android_display_suppress_sound_and_vibration_for_notifications_when_screen_is_on_tips),
                    key = Pref.Key.Android.DISABLE_SOUND_WHEN_UNLOCKED
                )
                //// 禁用设备名称敏感词校验
                //SwitchPreference(
                //    title = stringResource(R.string.system_settings_anti_ques),
                //    summary = stringResource(R.string.system_settings_anti_ques_desc),
                //    key = Pref.Key.Android.SYSTEM_SETTINGS_ANTI_QUES
                //)

            }

        }
        // 红魔-意图劫持
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_remove_intent_hijack),
            ) {
                // 移除显示上层通知
                SwitchPreference(
                    title = stringResource(R.string.android_remove_intent_hijack_content),
                    summary = stringResource(R.string.android_remove_intent_hijack_content_tips),
                    key = Pref.Key.Android.ANDROID_REMOVE_INTENT_HIJACK_CONTENT
                )
            }

        }


        // 红魔-允许不受信任的触摸
        item {
            PreferenceGroup(
                title = stringResource(R.string.android_title_features),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.android_disable_system_signature_verification),
                    key = Pref.Key.Android.ANDROID_DISABLE_SYSTEM_SIGNATURE_VERIFICATION
                )
                SwitchPreference(
                    title = stringResource(R.string.android_allow_untrusted_touches),
                    summary = stringResource(R.string.android_allow_untrusted_touches_tips),
                    key = Pref.Key.Android.ANDROID_ALLOW_UNTRUSTED_TOUCHES
                )
                // 长按电源键启动默认数字助理
                SwitchPreference(
                    title = stringResource(R.string.android_long_power_key_wakeup_assist),
                    summary = stringResource(R.string.android_long_power_key_wakeup_assist_tips),
                    key = Pref.Key.Android.ANDROID_LONG_POWER_KEY_WAKEUP_ASSIST
                )
                // 阻止启动遥测服务
                SwitchPreference(
                    title = stringResource(R.string.android_block_telemetry_service),
                    summary = stringResource(R.string.block_telemetry_service_tips),
                    key = Pref.Key.Android.ANDROID_BLOCK_TELEMETRY_SERVICE
                )
            }

        }

        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_display),
                first = true,
                visible = false //隐藏
            ) {
                TextPreference(
                    title = stringResource(R.string.android_display_font_scale),
                    summary = stringResource(R.string.android_display_font_scale_tips),
                    value = String.format(Locale.current.platformLocale, "%.2f", currentFontScale)
                ) {
                    dialogFontScaleVisibility.value = true
                }
                SwitchPreference(
                    title = stringResource(R.string.android_display_font_modify),
                    summary = stringResource(R.string.android_display_font_modify_tips),
                    key = Pref.Key.Android.FONT_SCALE
                ) {
                    visibilityFontScaleValue = true
                }
                AnimatedVisibility(
                    visibilityFontScaleValue
                ) {
                    EditTextPreference(
                        title = stringResource(R.string.android_display_font_modify_val),
                        key = Pref.Key.Android.FONT_SCALE_VAL,
                        defValue = 0.9f,
                        dataType = EditTextDataType.FLOAT,
                        dialogMessage = stringResource(R.string.android_display_font_modify_val_msg),
                        isValueValid = {
                            (it as? Float ?: 0.0f) in 0.5f..<1.0f
                        }
                    )
                }
            }
        }
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_freeform),
                visible = false
            ) {
                SwitchPreference(
                    title = stringResource(R.string.android_freeform_restriction),
                    summary = stringResource(R.string.android_freeform_restriction_tips),
                    key = Pref.Key.Android.DISABLE_FREEFORM_RESTRICT
                )
                SwitchPreference(
                    title = stringResource(R.string.android_freeform_allow_more),
                    summary = stringResource(R.string.android_freeform_allow_more_tips),
                    key = Pref.Key.Android.ALLOW_MORE_FREEFORM
                )
            }
        }
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_others),
                last = true,
                visible = false
            ) {
                SwitchPreference(
                    title = stringResource(R.string.android_others_force_dark),
                    summary = stringResource(R.string.android_others_force_dark_tips),
                    key = Pref.Key.Android.BLOCK_FORCE_DARK_WHITELIST
                )
            }
        }
    }
    AlertDialog(
        visibility = dialogFontScaleFailedVisibility,
        title = stringResource(R.string.dialog_error),
        message = stringResource(R.string.android_display_font_scale_fail_msg)
    )
    EditTextDialog(
        visibility = dialogFontScaleVisibility,
        title = stringResource(R.string.android_display_font_scale),
        message = stringResource(R.string.android_display_font_scale_msg),
        value = String.format(Locale.current.platformLocale, "%.2f", currentFontScale)
    ) {
        val newScale = it.toFloatOrNull()
        if (newScale != null && newScale in 0.5f..<2.0f) {
            try {
                ShellUtils.tryExec("settings put system font_scale $newScale", useRoot = true)
            } catch (tout: Throwable) {
                Toast.makeText(
                    context,
                    tout.message,
                    Toast.LENGTH_LONG
                ).show()
                dialogFontScaleFailedVisibility.value = true
            }
        } else {
            dialogFontScaleFailedVisibility.value = true
        }
    }
}