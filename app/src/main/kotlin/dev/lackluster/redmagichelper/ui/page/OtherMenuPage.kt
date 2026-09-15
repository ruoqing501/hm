package dev.lackluster.redmagichelper.ui.page



import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.AlertDialogMode
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.utils.ShellUtils

@Composable
fun OtherMenuPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    val context = LocalContext.current

    val dialogRebootSystem = remember { mutableStateOf(false) }
    val dialogRebootScope = remember { mutableStateOf(false) }

    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.page_menu),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode
    ) {
        item {
            PreferenceGroup(
                first = true,
                last = true
            ) {
                // 重启系统
                TextPreference(
                    title = stringResource(R.string.menu_reboot_system)
                ) {
                    dialogRebootSystem.value = true
                }
                // 重启有关其他界面的功能的对应hook包名的全部作用域
                TextPreference(
                    title = stringResource(R.string.menu_reboot_scope)
                ) {
                    dialogRebootScope.value = true
                }
            }
        }
    }
    // 重启系统
    AlertDialog(
        visibility = dialogRebootSystem,
        title = stringResource(R.string.menu_reboot_system),
        message = stringResource(R.string.menu_reboot_system_tips),
        mode = AlertDialogMode.NegativeAndPositive,
        negativeText = stringResource(R.string.button_cancel),
        positiveText = stringResource(R.string.button_ok)
    ) {
        dialogRebootSystem.value = false
        try {
            ShellUtils.tryExec("/system/bin/sync;/system/bin/svc power reboot || reboot", useRoot = true, checkSuccess = true)
        } catch (tout : Throwable) {
            makeText(
                context,
                tout.message,
                LENGTH_LONG
            ).show()
        }
    }
    // 重启全部作用域
    AlertDialog(
        visibility = dialogRebootScope,
        title = stringResource(R.string.menu_reboot_scope),
        message = stringResource(R.string.menu_reboot_scope_tips),
        mode = AlertDialogMode.NegativeAndPositive,
        negativeText = stringResource(R.string.button_cancel),
        positiveText = stringResource(R.string.button_ok)
    ) {
        dialogRebootScope.value = false
        context.let {
            try {
                //RebootMenuItems(
                //    appName = stringResource(R.string.menu_all_scope),
                //    appPkgs = listOf(
                //        // 权限控制器
                //        Scope.PERMISSION_CONTROLLER,
                //        // NFC 服务
                //        Scope.NFC,
                //        // 双开应用
                //        Scope.DOUBLE_APP,
                //        // 文件浏览
                //        Scope.NUBIA_FILE_BROWSER,
                //        )
                //)
                //}
                val scopeArray = listOf(
                    // 权限控制器
                    Scope.PERMISSION_CONTROLLER,
                    // NFC 服务
                    Scope.NFC,
                    // 双开应用
                    Scope.DOUBLE_APP,
                    // 文件浏览
                    Scope.NUBIA_FILE_BROWSER,
                    // 截图
                    Scope.ZTE_SCREENSHOT,
                   )
                scopeArray.forEach { pkg ->
                    try {
                        if (pkg != "android") {
                            val result = ShellUtils.tryExec("killall -q $pkg", useRoot = true, checkSuccess = true)
                            // 打印输出 它的命令
                            android.util.Log.d("OtherMenuPage", "执行命令：killall -q $pkg")
                        }

                    } catch (t: Throwable) {
                        if (t.message?.contains("No such process") == false) {
                            throw t
                        }
                    }
                }
                makeText(
                    it,
                    it.getString(R.string.menu_reboot_done_toast),
                    LENGTH_LONG
                ).show()
            } catch (tout : Throwable) {
                makeText(
                    it,
                    tout.message,
                    LENGTH_LONG
                ).show()
            }
        }
    }
}