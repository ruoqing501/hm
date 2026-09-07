package dev.lackluster.redmagichelper.ui.page



import android.graphics.drawable.Icon
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
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.utils.ShellUtils
import top.yukonga.miuix.kmp.basic.Icon


@Composable
fun ThemePage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {


    BasePage(
        navController,
        adjustPadding,
        // 标题：系统设置
        stringResource(R.string.system_theme),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            // 主题重启
            // appPkg 重启的对应包名：com.zte.beautify
            RebootMenuItem(
                // 弹窗标题的提示名称
                appName = stringResource(R.string.system_theme),
                // 重启：com.zte.beautify
                appPkg = Scope.SYSTEM_THEME
                // 重启系统
//                appPkg = "android"
            )
        }
    ) {
        // 红魔-主题
        item {
            PreferenceGroup(
                stringResource(R.string.system_theme),
                visible =  true // 默认显示状态栏卡片
            ) {
                // 取消试用登录
                // 试用主题，可以不登录进行下载
                SwitchPreference(
                    title = stringResource(R.string.ui_title_theme_cancel_trial_login),
                    summary = stringResource(R.string.ui_title_theme_cancel_trial_login_tips),
                    key = Pref.Key.NubiaTheme.CANCEL_TRIAL_LOGIN, //唯一id
                )


            }
        }
    }
}


