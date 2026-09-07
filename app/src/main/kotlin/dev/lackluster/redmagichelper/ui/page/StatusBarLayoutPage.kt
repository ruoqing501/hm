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
import dev.lackluster.hyperx.compose.preference.SeekBarPreferenceDefault2
import dev.lackluster.hyperx.compose.preference.SeekBarPreferenceDefault3
import dev.lackluster.hyperx.compose.preference.SeekBarPreferenceFloatDefault
import dev.lackluster.hyperx.compose.preference.SeekBarPreferenceFloatDefault2
import dev.lackluster.hyperx.compose.preference.SeekBarPreferenceFloatDefault3
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem

@Composable
fun StatusBaLayoutPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    var enableStatusBarLayout by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_LAYOUT_SWITCH)) }




    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.ui_title_status_bar_layout),
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
                title = stringResource(R.string.ui_title_status_bar_layout)
            ) {
                // 启用模块
                SwitchPreference(
                    title = stringResource(R.string.ui_title_status_bar_enable_modules),
                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_LAYOUT_SWITCH,
                ) {
                    enableStatusBarLayout = it
                }
                AnimatedVisibility(
                    enableStatusBarLayout
                ) {
                    Column() {
                        SeekBarPreferenceFloatDefault3(
                            title = stringResource(R.string.ui_title_status_bar_height), //状态栏高度
                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_HEIGHT,
//                            defValue = -1.0f,
                            min = 24.0f,
                            max = 64.0f
                        )

                        SeekBarPreferenceFloatDefault3(
                            title = stringResource(R.string.ui_title_status_bar_system_icon_height), //状态栏右侧系统图标高度
                            key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_SYSTEM_ICON_HEIGHT,
//                            defValue = -1.0f,
                            min = 24.0f,
                            max = 64.0f
                        )

                        //  状态栏左侧容器上边距
                        PreferenceGroup(
                            title = stringResource(R.string.ui_title_status_bar_left_container)
                        ) {
                            Column() {
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.top_margin),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_LEFT_CONTAINER_TOP_MARGIN,
                                    min = 0,
                                    max = 40,
                                )
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.down_margin),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_LEFT_CONTAINER_DOWN_MARGIN,
                                    min = 0,
                                    max = 40,
                                )
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.left_margin),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_LEFT_CONTAINER_LEFT_MARGIN,
                                    min = 0,
                                    max = 40,
                                )
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.right_margin),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_LEFT_CONTAINER_RIGHT_MARGIN,
                                    min = 0,
                                    max = 40,
                                )
                            }

                        }
                        //  状态栏左侧容器上边距
                        PreferenceGroup(
                            title = stringResource(R.string.ui_title_status_bar_right_container)
                        ) {
                            Column() {
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.top_margin),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_RIGHT_CONTAINER_TOP_MARGIN,
                                    min = 0,
                                    max = 40,
                                )
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.down_margin),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_RIGHT_CONTAINER_DOWN_MARGIN,
                                    min = 0,
                                    max = 40,
                                )
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.left_margin),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_RIGHT_CONTAINER_LEFT_MARGIN,
                                    min = 0,
                                    max = 40,
                                )
                                SeekBarPreferenceDefault3(
                                    title = stringResource(R.string.right_margin),
                                    key = Pref.Key.SystemUI.StatusBar.STATUS_BAR_RIGHT_CONTAINER_RIGHT_MARGIN,
                                    min = 0,
                                    max = 40,
                                )
                            }

                        }
                    }

                }
            }

        }
    }
}