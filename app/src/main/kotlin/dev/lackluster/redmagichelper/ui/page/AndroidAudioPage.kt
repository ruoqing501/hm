package dev.lackluster.redmagichelper.ui.page

import androidx.compose.animation.AnimatedVisibility
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
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.hyperx.compose.preference.ValuePosition
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem
import java.io.File

@Composable
fun AndroidAudioPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    var alarmClockVolumeLevelSwitch by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.Android.ALARM_CLOCK_VOLUME_LEVEL_SWITCH)) }
    var mediaClockVolumeLevelSwitch by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.Android.MEDIA_CLOCK_VOLUME_LEVEL_SWITCH)) }
    var notificationClockVolumeLevelSwitch by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.Android.NOTIFICATION_CLOCK_VOLUME_LEVEL_SWITCH)) }
    var ringClockVolumeLevelSwitch by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.Android.RING_CLOCK_VOLUME_LEVEL_SWITCH)) }
    var voiceClockVolumeLevelSwitch by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.Android.VOICE_CLOCK_VOLUME_LEVEL_SWITCH)) }




    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.ui_title_android_audio),
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

        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_sub_title_android_audio)
            ) {
                // 闹钟
                SwitchPreference(
                    title = stringResource(R.string.ui_title_alarm_volume_steps),
                    key =Pref.Key.Android.ALARM_CLOCK_VOLUME_LEVEL_SWITCH
                ) {
                    alarmClockVolumeLevelSwitch = it
                }
                AnimatedVisibility(
                    alarmClockVolumeLevelSwitch
                ) {
                    SeekBarPreference(
                        title = stringResource(R.string.alarm_volume_steps),
                        key = Pref.Key.Android.ALARM_CLOCK_VOLUME_LEVEL,
                        defValue = 15,
                        min = 3,
                        max = 50
                    )
                }
                // 媒体
                SwitchPreference(
                    title = stringResource(R.string.ui_title_media_volume_steps),
                    key =Pref.Key.Android.MEDIA_CLOCK_VOLUME_LEVEL_SWITCH
                ) {
                    mediaClockVolumeLevelSwitch = it
                }
                AnimatedVisibility(
                    mediaClockVolumeLevelSwitch
                ) {
                    SeekBarPreference(
                        title = stringResource(R.string.media_volume_steps),
                        key = Pref.Key.Android.MEDIA_CLOCK_VOLUME_LEVEL,
                        defValue = 15,
                        min = 3,
                        max = 50
                    )
                }
                // 通知
                SwitchPreference(
                    title = stringResource(R.string.ui_title_notify_volume_steps),
                    key =Pref.Key.Android.NOTIFICATION_CLOCK_VOLUME_LEVEL_SWITCH
                ) {
                    notificationClockVolumeLevelSwitch = it
                }
                AnimatedVisibility(
                    notificationClockVolumeLevelSwitch
                ) {
                    SeekBarPreference(
                        title = stringResource(R.string.notify_volume_steps),
                        key = Pref.Key.Android.NOTIFICATION_CLOCK_VOLUME_LEVEL,
                        defValue = 15,
                        min = 3,
                        max = 50
                    )
                }
                // 铃声
                SwitchPreference(
                    title = stringResource(R.string.ui_title_ring_volume_steps),
                    key =Pref.Key.Android.RING_CLOCK_VOLUME_LEVEL_SWITCH
                ) {
                    ringClockVolumeLevelSwitch = it
                }
                AnimatedVisibility(
                    ringClockVolumeLevelSwitch
                ) {
                    SeekBarPreference(
                        title = stringResource(R.string.ring_volume_steps),
                        key = Pref.Key.Android.RING_CLOCK_VOLUME_LEVEL,
                        defValue = 15,
                        min = 3,
                        max = 50
                    )
                }
                // 语音通话
                SwitchPreference(
                    title = stringResource(R.string.ui_title_vc_call_volume_steps),
                    key =Pref.Key.Android.VOICE_CLOCK_VOLUME_LEVEL_SWITCH
                ) {
                    voiceClockVolumeLevelSwitch = it
                }
                AnimatedVisibility(
                    voiceClockVolumeLevelSwitch
                ) {
                    SeekBarPreference(
                        title = stringResource(R.string.vc_call_volume_steps),
                        key = Pref.Key.Android.VOICE_CLOCK_VOLUME_LEVEL,
                        defValue = 15,
                        min = 3,
                        max = 50
                    )
                }

            }
        }

    }
}