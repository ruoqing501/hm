//package dev.lackluster.redmagichelper.ui.page
////
////
//import android.graphics.drawable.Icon
//import android.widget.Toast.LENGTH_LONG
//import android.widget.Toast.makeText
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.stringResource
//import androidx.navigation.NavController
//import dev.lackluster.hyperx.compose.activity.SafeSP
//import dev.lackluster.hyperx.compose.base.AlertDialog
//import dev.lackluster.hyperx.compose.base.AlertDialogMode
//import dev.lackluster.hyperx.compose.base.BasePage
//import dev.lackluster.hyperx.compose.base.BasePageDefaults
//import dev.lackluster.hyperx.compose.navigation.navigateTo
//import dev.lackluster.hyperx.compose.preference.DropDownEntry
//import dev.lackluster.hyperx.compose.preference.DropDownPreference
//import dev.lackluster.hyperx.compose.preference.EditTextDataType
//import dev.lackluster.hyperx.compose.preference.EditTextPreference
//import dev.lackluster.hyperx.compose.preference.PreferenceGroup
//import dev.lackluster.hyperx.compose.preference.SeekBarPreference
//import dev.lackluster.hyperx.compose.preference.SwitchPreference
//import dev.lackluster.hyperx.compose.preference.TextPreference
//import dev.lackluster.redmagichelper.R
//import dev.lackluster.redmagichelper.ui.MainActivity
//import dev.lackluster.redmagichelper.ui.component.RebootMenuItem
//import dev.lackluster.redmagichelper.data.Pages
//import dev.lackluster.redmagichelper.data.Pref
//import dev.lackluster.redmagichelper.data.Scope
//import dev.lackluster.redmagichelper.utils.ShellUtils
//import top.yukonga.miuix.kmp.basic.Icon
//
//
//@Composable
//fun PackageInstallerPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
//
//
//    BasePage(
//        navController,
//        adjustPadding,
//        // 标题：软件包安装器
//        stringResource(R.string.package_installer),
//        MainActivity.blurEnabled,
//        MainActivity.blurTintAlphaLight,
//        MainActivity.blurTintAlphaDark,
//        mode,
//        actions = {
//            // 软件包安装器界面
//            // appPkg 重启的对应包名：com.android.systemui
//            RebootMenuItem(
//                // 弹窗标题的提示名称
//                appName = stringResource(R.string.package_installer),
//                // 对应自己的系统的安装器的包名：com.android.packageinstaller
//                appPkg = Scope.REDMAGIC_PACKAGE_INSTALLER
//            )
//        }
//    ) {
//        // 红魔-软件包安装器
//        item {
//            PreferenceGroup(
//                stringResource(R.string.package_installer),
//                visible =  true // 默认显示状态栏卡片
//            ) {
//                // 跳过安装包扫描
//                SwitchPreference(
//                    title = stringResource(R.string.skip_pkg_scan),
//                    key = Pref.Key.NubiaPackageInstaller.SKIP_PKG_INSTALLER_SCAN, //唯一id
//                )
//                // 隐藏净化模式开关
//                SwitchPreference(
//                    title = stringResource(R.string.hide_evolution_mode_toggle),
//                    summary = stringResource(R.string.hide_risk_warnings),
//                    key = Pref.Key.NubiaPackageInstaller.HIDE_EVOLUTION_MODE_TOGGLE  //唯一id
//                )
//                // 隐藏从商店安装提示
//                SwitchPreference(
//                    title = stringResource(R.string.hide_store_install_prompt),
//                    key = Pref.Key.NubiaPackageInstaller.HIDE_STORE_INSTALL_PROMPT  //唯一id
//                )
//                // 使用cts测试的安装界面
//                // 没有权限提示和商店推广，给cts测试用的
//                SwitchPreference(
//                    title = stringResource(R.string.cts_test_installer),
//                    summary = stringResource(R.string.no_permission_or_store_prompts_for_cts_testing),
//                    key = Pref.Key.NubiaPackageInstaller.CTS_TEST_INSTALLER  //唯一id
//                )
//            }
//        }
//    }
//}
//
//
//


package dev.lackluster.redmagichelper.ui.page

import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.AlertDialogMode
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.navigation.navigateWithPopup
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
import dev.lackluster.redmagichelper.data.Constants
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.utils.ShellUtils
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.extra.DropdownImpl
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.ImmersionMore
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PackageInstallerPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    val context = LocalContext.current

    // 添加右上角菜单状态
    val showTopPopup = remember { mutableStateOf(false) }
    val contextMenuItems = listOf(
        stringResource(R.string.ui_title_menu_reboot),
        stringResource(R.string.menu_shortcut_lsposed)
    )
    val hapticFeedback = LocalHapticFeedback.current

    BasePage(
        navController,
        adjustPadding,
        // 标题：软件包安装器
        stringResource(R.string.package_installer),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = { padding ->
            // 右上角菜单弹窗
            ListPopup(
                show = showTopPopup,
                popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
                alignment = PopupPositionProvider.Align.TopRight,
                onDismissRequest = {
                    showTopPopup.value = false
                }
            ) {
                ListPopupColumn {
                    contextMenuItems.forEachIndexed { index, string ->
                        DropdownImpl(
                            text = string,
                            optionSize = contextMenuItems.size,
                            isSelected = false,
                            onSelectedIndexChange = {
                                when(it) {
                                    0 -> {
                                        // 跳转到菜单页面-重启页面
//                                        navController.navigateWithPopup(Pages.MENU)
                                        navController.navigateTo(Pages.MENU)
                                    }
                                    1 -> {
                                        try {
                                            ShellUtils.tryExec(
                                                Constants.CMD_LSPOSED,
                                                useRoot = true,
                                                checkSuccess = true
                                            )
                                        } catch (tout : Throwable) {
                                            makeText(
                                                context,
                                                tout.message,
                                                LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                                showTopPopup.value = false
                            },
                            index = index
                        )
                    }
                }
            }

            // 菜单按钮
            IconButton(
                modifier = Modifier
                    .padding(padding)
                    .padding(end = 21.dp)
                    .size(40.dp),
                onClick = {
                    showTopPopup.value = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                holdDownState = showTopPopup.value
            ) {
                Icon(
                    modifier = Modifier.size(26.dp),
                    imageVector = MiuixIcons.Useful.ImmersionMore,
                    contentDescription = "Menu",
                    tint = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }

//            // 原有的重启按钮
//            RebootMenuItem(
//                // 弹窗标题的提示名称
//                appName = stringResource(R.string.package_installer),
//                // 对应自己的系统的安装器的包名：com.android.packageinstaller
//                appPkg = Scope.REDMAGIC_PACKAGE_INSTALLER
//            )
        }
    ) {
        // 红魔-软件包安装器
        item {
            PreferenceGroup(
                stringResource(R.string.package_installer),
                visible = true // 默认显示状态栏卡片
            ) {
                // 跳过安装包扫描
                SwitchPreference(
                    title = stringResource(R.string.skip_pkg_scan),
                    key = Pref.Key.NubiaPackageInstaller.SKIP_PKG_INSTALLER_SCAN, //唯一id
                )
                // 隐藏净化模式开关
                SwitchPreference(
                    title = stringResource(R.string.hide_evolution_mode_toggle),
                    summary = stringResource(R.string.hide_risk_warnings),
                    key = Pref.Key.NubiaPackageInstaller.HIDE_EVOLUTION_MODE_TOGGLE  //唯一id
                )
                // 隐藏从商店安装提示
                SwitchPreference(
                    title = stringResource(R.string.hide_store_install_prompt),
                    key = Pref.Key.NubiaPackageInstaller.HIDE_STORE_INSTALL_PROMPT  //唯一id
                )
                // 使用cts测试的安装界面
                // 没有权限提示和商店推广，给cts测试用的
                SwitchPreference(
                    title = stringResource(R.string.cts_test_installer),
                    summary = stringResource(R.string.no_permission_or_store_prompts_for_cts_testing),
                    key = Pref.Key.NubiaPackageInstaller.CTS_TEST_INSTALLER  //唯一id
                )
            }
        }
    }
}
