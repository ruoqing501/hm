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
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem

@Composable
fun StatusBarDualPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {

    var enableDual by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW))}



    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.ui_title_status_bar_dual),
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
                title = stringResource(R.string.ui_title_status_bar_dual)
            ) {
                // 开启双排状态栏
                SwitchPreference(
                    title = stringResource(R.string.ui_title_status_bar_dual),
                    key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW
                ) {
                    enableDual = it
                }
                AnimatedVisibility(
                    enableDual
                ) {
                    Column() {
                        SwitchPreference(
                            title = stringResource(R.string.ui_title_status_bar_dual_left),
                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW_LEFT,
                        )
                        SwitchPreference(
                            title = stringResource(R.string.ui_title_status_bar_dual_right),
                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW_RIGHT,
                        )
                        // 时钟跨双排显示
                        SwitchPreference(
                            title = stringResource(R.string.ui_title_status_bar_dual_clock_across),
                            key =Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_CLOCK_ACROSS
                        )
                    }

                }


            }
        }

    }
}