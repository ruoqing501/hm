package dev.lackluster.redmagichelper.ui.page

import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.AlertDialogMode
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.base.ImageIcon
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.navigation.navigateWithPopup
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.data.Constants
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.ui.component.BottomNavBar
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
import top.yukonga.miuix.kmp.icon.icons.useful.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MainPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    val context = LocalContext.current

    val showTopPopup = remember { mutableStateOf(false) }
     // 右上角菜单-点击后，重启/lsposed
    val contextMenuItems = listOf(
        stringResource(R.string.ui_title_menu_reboot),
        stringResource(R.string.menu_shortcut_lsposed)
    )

    BasePage(
        navController,
        adjustPadding,
        // 顶部：主标题
        stringResource(R.string.page_main),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        bottomBar = if (mode == BasePageDefaults.Mode.FULL) {
            { BottomNavBar(navController, Pages.MAIN) }
        } else {
            null
        },
        navigationIcon = {},
        actions = { padding ->
            val hapticFeedback = LocalHapticFeedback.current
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
                                        navController.navigateWithPopup(Pages.MENU)
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
            IconButton(
                modifier = Modifier.padding(padding).padding(end = 21.dp).size(40.dp),
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
        }
    ) {

        // 功能搜索入口
        item {
            PreferenceGroup(first = true) {
                TextPreference(
                    icon = ImageIcon(iconVector = MiuixIcons.Useful.Search),
                    title = stringResource(R.string.search_features_hint)
                ) {
                    navController.navigateWithPopup(Pages.SEARCH)
                }
            }
        }
        // 主页面
        item {
            PreferenceGroup(last = true) {
                // 游戏空间
                TextPreference(
                    icon = ImageIcon(iconRes = R.drawable.ic_game_space),
                    title = stringResource(R.string.page_game_space)
                ) {
                    // 路由导航到系统界面页面
                    navController.navigateWithPopup(Pages.GAME_SPACE)
                }
                // 系统界面
                TextPreference(
                    icon = ImageIcon(iconRes = R.drawable.ic_header_systemui),
                    title = stringResource(R.string.page_systemui)
                ) {
                    // 路由导航到系统界面页面
                    navController.navigateWithPopup(Pages.SYSTEM_UI)
                }
                // 系统框架
                TextPreference(
                    icon = ImageIcon(iconRes = R.drawable.ic_header_android_green),
                    title = stringResource(R.string.page_android)
                ) {
                    navController.navigateWithPopup(Pages.SYSTEM_FRAMEWORK)
                }
                // 软件安装包
                TextPreference(
                    icon = ImageIcon(iconRes = R.drawable.ic_header_android_purple),
                    title = stringResource(R.string.package_installer)
                ) {
                    navController.navigateWithPopup(Pages.PACKAGE_INSTALLER)
                }
                // 桌面
                TextPreference(
                    icon = ImageIcon(iconRes = R.drawable.ic_header_home),
                    title = stringResource(R.string.system_desktop)
                ) {
                    navController.navigateWithPopup(Pages.SYSTEM_DESKTOP)
                }
                // 主题
                TextPreference(
                    icon = ImageIcon(iconRes = R.drawable.ic_android_system_theme),
                    title = stringResource(R.string.system_theme)
                ) {
                    navController.navigateWithPopup(Pages.SYSTEM_THEME)
                }
                // 系统设置
                TextPreference(
                    icon = ImageIcon(iconRes = R.drawable.ic_system_app_settings),
                    title = stringResource(R.string.system_settings)
                ) {
                    // 路由导航到系统界面页面
                    navController.navigateWithPopup(Pages.SYSTEM_SETTINGS)
                }
                // 系统更新
                TextPreference(
                    icon = ImageIcon(iconRes = R.drawable.ic_android_system_update),
                    title = stringResource(R.string.system_update)
                ) {
                    // 路由导航到系统界面页面
                    navController.navigateWithPopup(Pages.SYSTEM_UPDATE)
                }
                TextPreference(
                    icon = ImageIcon(iconRes = R.drawable.ic_header_others),
                    title = stringResource(R.string.page_other)
                ) {
                    navController.navigateWithPopup(Pages.OTHER)
                }
            }
        }
    }

    val dialogInactiveVisibility = remember { mutableStateOf(false) }
    val dialogDisabledVisibility = remember { mutableStateOf(false) }
    val dialogRootRequiredVisibility = remember { mutableStateOf(false) }
    if (MainActivity.moduleActive.value) {
        if (!MainActivity.moduleEnabled.value) {
            dialogDisabledVisibility.value = true
        } else if (!MainActivity.rootGranted.value) {
            dialogRootRequiredVisibility.value = true
        }
    } else {
        dialogInactiveVisibility.value = true
    }
    AlertDialog(
        visibility = dialogDisabledVisibility,
        title = stringResource(R.string.dialog_warning),
        message = stringResource(R.string.main_module_disabled_tips),
        mode = AlertDialogMode.NegativeAndPositive,
        negativeText = stringResource(R.string.button_ignore),
        positiveText = stringResource(R.string.button_enable)
    ) {
        dialogDisabledVisibility.value = false
        navController.navigateTo(Pages.MODULE_SETTINGS)
    }
    AlertDialog(
        visibility = dialogInactiveVisibility,
        title = stringResource(R.string.dialog_error),
        message = stringResource(R.string.main_module_inactive_tips),
        cancelable = false,
        mode = AlertDialogMode.NegativeAndPositive,
        negativeText = stringResource(R.string.button_ignore),
        positiveText = stringResource(R.string.main_module_inactive_dialog_lsposed)
    ) {
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
        dialogInactiveVisibility.value = false
    }
    AlertDialog(
        visibility = dialogRootRequiredVisibility,
        title = stringResource(R.string.dialog_warning),
        message = stringResource(R.string.main_app_root_tips),
        cancelable = false
    )
}