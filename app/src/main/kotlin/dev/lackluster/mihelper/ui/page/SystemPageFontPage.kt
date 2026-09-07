package dev.lackluster.mihelper.ui.page


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
import dev.lackluster.hyperx.compose.preference.SeekBarPreferenceDefault3
import dev.lackluster.hyperx.compose.preference.SeekBarPreferenceFloatDefault3
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.hyperx.compose.preference.ValuePosition
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.ui.MainActivity
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.ui.component.RebootMenuItem
import java.io.File

@Composable
fun SystemPageFontPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    var enableLockScreenClockFont by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.FontWeight.LOCKSCREEN_CLOCK)) }




    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.ui_title_syetem_page_font),
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
                title = stringResource(R.string.ui_title_font_general),
                first = true
            ) {
                EditTextPreference(
                    title = stringResource(R.string.font_general_path),
                    key = Pref.Key.SystemUI.FontWeight.FONT_PATH,
                    defValue = "/system/fonts/AndroidClock.ttf",
                    dataType = EditTextDataType.STRING,
                    dialogMessage = stringResource(R.string.font_general_path_tips),
                    isValueValid = { path ->
                        (path as? String)?.let {
                            val file = File(it)
                            file.exists() && file.isFile
                        } ?: false
                    },
                    valuePosition = ValuePosition.SUMMARY_VIEW
                )
            }
        }
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_font_lockscreen),
                last = true
            ) {
                SwitchPreference(
                    title = stringResource(R.string.font_lockscreen_clock),
                    summary = stringResource(R.string.ui_title_font_lockscreen_summary),
                    key = Pref.Key.SystemUI.FontWeight.LOCKSCREEN_CLOCK
                ){
                    enableLockScreenClockFont = it
                }
                AnimatedVisibility(
                    enableLockScreenClockFont
                ) {
                    SeekBarPreferenceFloatDefault3(
                        title = stringResource(R.string.lock_screen_font_size_zoom),
                        key = Pref.Key.SystemUI.FontWeight.LOCK_SCREEN_FONT_SIZE_ZOOM,
                        defValue = -1f,
                        min = 0.0f,
                        max = 1.0f
                    )
                }
            }
        }

    }
}