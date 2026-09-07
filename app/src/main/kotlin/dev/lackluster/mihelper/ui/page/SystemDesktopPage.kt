package dev.lackluster.mihelper.ui.page



import android.graphics.drawable.Icon
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import dev.lackluster.hyperx.compose.preference.DropDownMode
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
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ColorPalette

@Composable
fun SystemDesktopPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    var enableTImeComponentDesktop by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemDesktop.SYSTEM_TIME_COMPONENT_DESKTOP_SWITCH)) }

    BasePage(
        navController,
        adjustPadding,
        // 标题：系统设置
        stringResource(R.string.system_desktop),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            // 系统更新重启
            // appPkg 重启的对应包名：com.zte.zdm
            RebootMenuItem(
                // 弹窗标题的提示名称
                appName = stringResource(R.string.system_desktop),
                // 重启：com.zte.zdm
                appPkg = Scope.SYSTEM_DESKTOP
                // 重启系统
//                appPkg = "android"
            )
        }
    ) {
        // 红魔-桌面-时间小部件
        item {
            PreferenceGroup(
                // 桌面时间部件
                stringResource(R.string.system_time_component_desktop),
                visible = true //
            ) {
                // 时间小部件
                SwitchPreference(
                    title = stringResource(R.string.system_time_component_desktop),
                    key = Pref.Key.SystemDesktop.SYSTEM_TIME_COMPONENT_DESKTOP_SWITCH,
                ) {
                    enableTImeComponentDesktop = it
                }
                AnimatedVisibility(
                    enableTImeComponentDesktop
                ) {
                    Column() {
                        // 显示秒
                        SwitchPreference(
                            title = stringResource(R.string.system_time_component_desktop_show_seconds),
                            key = Pref.Key.SystemDesktop.SYSTEM_TIME_COMPONENT_DESKTOP_SHOW_SECONDS, //唯一id
                        )
                        // 显示时段
                        SwitchPreference(
                            title = stringResource(R.string.system_time_component_desktop_show_period),
                            summary = stringResource(R.string.system_time_component_desktop_show_period_summary),
                            key = Pref.Key.SystemDesktop.SYSTEM_TIME_COMPONENT_DESKTOP_SHOW_PERIOD, //唯一id
                        )
                    }
                }
            }
        }
        // 红魔-桌面-最近任务界面
        item {
            PreferenceGroup(
                stringResource(R.string.system_desktop_recent_task_Interface),
                visible = true,
            ) {
                // 图标调整
                TextPreference(
                    title = stringResource(R.string.system_desktop_recent_task_Interface)
                ) {
                    // 导航到最近任务界面
                    navController.navigateTo(Pages.SYSTEM_DESKTOP_RECENT_TASKS)
                }
            }
        }

        }
    }


