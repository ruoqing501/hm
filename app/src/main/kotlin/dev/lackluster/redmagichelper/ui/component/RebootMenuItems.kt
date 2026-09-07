package dev.lackluster.redmagichelper.ui.component


import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.AlertDialogMode
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.utils.ShellUtils
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Reboot
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RebootMenuItems(
    appName: String,
    appPkgs: List<String>
) {
    val context = LocalContext.current
    val dialogVisibility = remember { mutableStateOf(false) }
    IconButton(
        modifier = Modifier.padding(end = 21.dp).size(40.dp),
        onClick = {
            dialogVisibility.value = true
        },
        holdDownState = dialogVisibility.value
    ) {
        Icon(
            modifier = Modifier.size(26.dp),
            imageVector = MiuixIcons.Useful.Reboot,
            contentDescription = "Reboot app",
            tint = MiuixTheme.colorScheme.onSurfaceSecondary,
        )
    }
    AlertDialog(
        dialogVisibility,
        stringResource(R.string.menu_reboot_common_title, appName),
        stringResource(R.string.menu_reboot_common_message, appName),
        mode = AlertDialogMode.NegativeAndPositive,
        onPositiveButton = {
            try {
                appPkgs.forEach { pkg ->
                    try {
                        if (pkg != "android") { // 排除系统框架，避免误杀
                            ShellUtils.tryExec("killall -q $pkg", useRoot = true, checkSuccess = true)
                            // 打印输出 它的命令
                            android.util.Log.d("GameSpacePage", "执行命令：killall -q $pkg")
                        }
                    } catch (t: Throwable) {
                        // 仅忽略“没有该进程”的错误，其他异常继续抛出
                        if (t.message?.contains("No such process") == false) {
                            throw t
                        }
                    }
                }
                context.let {
                    makeText(
                        it,
                        it.getString(R.string.menu_reboot_done_toast),
                        LENGTH_SHORT
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
            dialogVisibility.value = false
        }
    )
}