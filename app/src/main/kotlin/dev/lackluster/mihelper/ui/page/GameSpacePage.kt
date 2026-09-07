package dev.lackluster.mihelper.ui.page


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
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.ui.MainActivity
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.ui.component.RebootMenuItem
import dev.lackluster.mihelper.ui.component.RebootMenuItems
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.AlertDialogMode
import dev.lackluster.hyperx.compose.base.BasePage
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
import dev.lackluster.mihelper.ui.component.RebootMenuItem
import dev.lackluster.mihelper.data.Constants
import dev.lackluster.mihelper.data.Pages
import dev.lackluster.mihelper.utils.ShellUtils
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
fun GameSpacePage(
    navController: NavController,
    adjustPadding: PaddingValues,
    mode: BasePageDefaults.Mode
) {


    var enableSuperResolution by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.GameSpace.GAME_SPACE_SUPER_RESOLUTION_SWITCH)) }
    var enableDevilMode by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_SWITCH)) }
    // 添加右上角菜单状态
    val showTopPopup = remember { mutableStateOf(false) }
    val contextMenuItems = listOf(
        stringResource(R.string.ui_title_menu_reboot),
        stringResource(R.string.menu_shortcut_lsposed)
    )
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current


    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.page_game_space),
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
                                when (it) {
                                    0 -> {
                                        // 跳转到菜单页面-重启页面
                                        navController.navigateTo(Pages.GAMESPACE_MENU)
                                    }

                                    1 -> {
                                        try {
                                            ShellUtils.tryExec(
                                                Constants.CMD_LSPOSED,
                                                useRoot = true,
                                                checkSuccess = true
                                            )
                                        } catch (tout: Throwable) {
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
            //RebootMenuItems(
            //    appName = stringResource(R.string.page_game_space),
            //    appPkgs = listOf(Scope.GAME_ASSIST, Scope.GAME_FLOAT,Scope.REDMAGIC_MOMENT,Scope.GAME_SPACE,
            //        Scope.COMBO_ATTACK)
            //)
        }

    ) {

//        item {
//            // 破坏神模式
//            PreferenceGroup(
//                title = stringResource(R.string.ui_title_game_space_devil_mode)
//            ) {
//                // 破坏神模式（总开关）
//                SwitchPreference(
//                    title = stringResource(R.string.ui_title_game_space_devil_mode),
//                    key =Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_SWITCH
//                ) {
//                    enableDevilMode = it
//                }
//                AnimatedVisibility(
//                    enableDevilMode
//                ) {
//                    Column() {
//                        // 隐藏开启破坏神模式时的弹窗提示
//                        SwitchPreference(
//                            title = stringResource(R.string.ui_title_game_space_devil_mode_hide_prompt),
//                            key =Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_HIDE_PROMPT,
//                        )
//                    }
//                }
//            }
//        }

        // 解除游戏功能限制
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_game_fuction_unfreeze)
            ) {
                // 解除游戏功能限制
                SwitchPreference(
                    title = stringResource(R.string.game_fuction_unfreeze_switch),
                    summary = stringResource(R.string.game_fuction_unfreeze_switch_tips),
                    key = Pref.Key.GameSpace.GAME_FUCTION_UNFREEZE_SWITCH
                ) {

                }
            }
        }
        // 磁贴
        item {
            PreferenceGroup(
                title = stringResource(R.string.magnetic_sticker)
            ) {
                // 活跃模式
                SwitchPreference(
                    title = stringResource(R.string.ui_title_game_active_mode_title),
                    summary = stringResource(R.string.ui_title_game_active_mode_title_tips),
                    key = Pref.Key.GameSpace.ACIVE_MODE_SWITCH
                ) {
                }
            }
        }
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_game_space_super_resolution)
            ) {
                // 超竞模式（总开关）
                SwitchPreference(
                    title = stringResource(R.string.ui_title_game_space_super_resolution),
                    key = Pref.Key.GameSpace.GAME_SPACE_SUPER_RESOLUTION_SWITCH
                ) {
                    enableSuperResolution = it
                }
                AnimatedVisibility(
                    enableSuperResolution
                ) {
                    Column() {
                        // 悬浮面板-防止在点击标签的时候悬浮面板被折叠
                        SwitchPreference(
                            title = stringResource(R.string.ui_title_game_space_prevent_collapse),
                            summary = stringResource(R.string.ui_title_game_space_prevent_collapse_tips),
                            key = Pref.Key.GameSpace.GAME_SPACE_PREVENT_COLLAPSE,
                        )
                        // 隐藏开启破坏神模式时的弹窗提示
                        SwitchPreference(
                            title = stringResource(R.string.ui_title_game_space_devil_mode_hide_prompt),
                            key = Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_HIDE_PROMPT,
                        )
                        // 允许在破神模式下启用超境模式
                        SwitchPreference(
                            title = stringResource(R.string.ui_title_game_space_devil_mode_enable_super_resolution),
                            key = Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_ENABLE_SUPER_RESOLUTION,
                        )
                        // 允许在节能、均衡模式下启用超境模式
                        SwitchPreference(
                            title = stringResource(R.string.ui_title_game_space_devil_mode_enable_super_resolution_low),
                            key = Pref.Key.GameSpace.GAME_SPACE_ENABLE_SUPER_RESOLUTION_LOW,
                        )
//                        // 隐藏在节能、均衡模式下超境模式弹窗提示
//                        SwitchPreference(
//                            title = stringResource(R.string.ui_title_game_space_devil_mode_enable_super_resolution_low_prop_prompt),
//                            key =Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_ENABLE_SUPER_RESOLUTION_LOW_PROP_PROMPT,
//                        )
                    }

                }

            }
        }

    }
}