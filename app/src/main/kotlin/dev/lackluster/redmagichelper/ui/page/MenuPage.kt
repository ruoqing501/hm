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
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.utils.ShellUtils

@Composable
fun MenuPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    val context = LocalContext.current
    
    val dialogRebootSystem = remember { mutableStateOf(false) }
    val dialogRebootScope = remember { mutableStateOf(false) }
    val dialogRebootSystemUI = remember { mutableStateOf(false) }
    val dialogRebootLauncher = remember { mutableStateOf(false) }
    
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
                TextPreference(
                    title = stringResource(R.string.menu_reboot_system)
                ) {
                    dialogRebootSystem.value = true
                }
                TextPreference(
                    title = stringResource(R.string.menu_reboot_scope)
                ) {
                    dialogRebootScope.value = true
                }
                TextPreference(
                    title = stringResource(R.string.menu_reboot_systemui)
                ) {
                    dialogRebootSystemUI.value = true
                }
                TextPreference(
                    title = stringResource(R.string.menu_reboot_launcher)
                ) {
                    dialogRebootLauncher.value = true
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
    //重启全部作用域
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
                // 重启全部作用域
//                it.resources.getStringArray(R.array.module_scope).forEach { pkg ->
//                    try {
//                        if (pkg != "android") ShellUtils.tryExec("killall -q $it", useRoot = true, checkSuccess = true)
//
//                    } catch (t: Throwable) {
//                        if (t.message?.contains("No such process") == false) {
//                            throw t
//                        }
//                    }
//                }
                // 重启全部作用域
                // 为什么pkg = dev.lackluster.redmagichelper.ui.MainActivity@8b10266
//                it.resources.getStringArray(R.array.module_scope).forEach { pkg ->
//                    try {
//                        if (pkg != "android") {
//                            val result = ShellUtils.tryExec("killall -q ${pkg.toString()}", useRoot = true, checkSuccess = true)
//                            // 打印输出 它的命令
//                            android.util.Log.d("MenuPage", "执行命令：killall -q $it")
//                            android.util.Log.d("MenuPage", "标准输出：${result.successMsg}")
//                            android.util.Log.d("MenuPage", "错误输出：${result.errorMsg}")
//                        }
//
//                    } catch (t: Throwable) {
//                        if (t.message?.contains("No such process") == false) {
//                            throw t
//                        }
//                    }
//                }
                val context = it
                val scopeArray = context.resources.getStringArray(R.array.module_scope)
                scopeArray.forEach { pkg ->
                    try {
                        if (pkg != "android") {
                            val result = ShellUtils.tryExec("killall -q $pkg", useRoot = true, checkSuccess = true)
                            // 打印输出 它的命令
                            android.util.Log.d("MenuPage", "执行命令：killall -q $pkg")
                            android.util.Log.d("MenuPage", "标准输出：${result.successMsg}")
                            android.util.Log.d("MenuPage", "错误输出：${result.errorMsg}")
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
    AlertDialog(
        visibility = dialogRebootSystemUI,
        title = stringResource(R.string.menu_reboot_systemui),
        message = stringResource(R.string.menu_reboot_systemui_tips),
        mode = AlertDialogMode.NegativeAndPositive,
        negativeText = stringResource(R.string.button_cancel),
        positiveText = stringResource(R.string.button_ok)
    ) {
        dialogRebootSystemUI.value = false
        try {
            ShellUtils.tryExec("killall com.android.systemui", useRoot = true, checkSuccess = true)
            context.let {
                makeText(
                    it,
                    it.getString(R.string.menu_reboot_done_toast),
                    LENGTH_LONG
                ).show()
            }
        } catch (tout : Throwable) {
            context.let {
                makeText(
                    it,
                    tout.message,
                    LENGTH_LONG
                ).show()
            }
        }
    }
    AlertDialog(
        visibility = dialogRebootLauncher,
        title = stringResource(R.string.menu_reboot_launcher),
        message = stringResource(R.string.menu_reboot_launcher_tips),
        mode = AlertDialogMode.NegativeAndPositive,
        negativeText = stringResource(R.string.button_cancel),
        positiveText = stringResource(R.string.button_ok)
    ) {
        dialogRebootLauncher.value = false
        try {
            ShellUtils.tryExec("killall com.zte.mifavor.launcher", useRoot = true, checkSuccess = true)
            context.let {
                makeText(
                    it,
                    it.getString(R.string.menu_reboot_done_system_desktop),
                    LENGTH_LONG
                ).show()
            }
        } catch (tout : Throwable) {
            context.let {
                makeText(
                    it,
                    tout.message,
                    LENGTH_LONG
                ).show()
            }
        }
    }
}