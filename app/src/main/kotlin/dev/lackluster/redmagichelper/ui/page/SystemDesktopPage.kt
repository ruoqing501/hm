package dev.lackluster.redmagichelper.ui.page

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.data.Scope

@Composable
fun SystemDesktopPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
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
