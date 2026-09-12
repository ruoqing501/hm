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
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem
import dev.lackluster.redmagichelper.ui.component.RebootMenuItems
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
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem
import dev.lackluster.redmagichelper.data.Constants
import dev.lackluster.redmagichelper.data.Pages
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
fun GameSpacePage(
    navController: NavController,
    adjustPadding: PaddingValues,
    mode: BasePageDefaults.Mode
) {


    var enableSuperResolution by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.GameSpace.GAME_SPACE_SUPER_RESOLUTION_SWITCH)) }
    var enableDevilMode by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_SWITCH)) }
    var enableAiTrigger by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.GameSpace.AI_TRIGGER_SWITCH)) }
    var enableComboSpeed by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.GameSpace.GAME_SPACE_COMBO_SPEED_ENABLED)) }
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
                        // 允许超境模式与破坏神模式共存
                        SwitchPreference(
                            title = stringResource(R.string.ui_title_game_space_super_resolution_diablo_coexist),
                            summary = stringResource(R.string.ui_title_game_space_super_resolution_diablo_coexist_tips),
                            key = Pref.Key.GameSpace.GAME_SPACE_SUPER_RESOLUTION_DIABLO_COEXIST,
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
        // 一键连招倍速
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_game_combo_speed)
            ) {
                // 一键连招倍速（总开关）
                SwitchPreference(
                    title = stringResource(R.string.game_combo_speed_switch),
                    summary = stringResource(R.string.game_combo_speed_switch_tips),
                    key = Pref.Key.GameSpace.GAME_SPACE_COMBO_SPEED_ENABLED
                ) {
                    enableComboSpeed = it
                }
                AnimatedVisibility(
                    enableComboSpeed
                ) {
                    // 播放倍率 1~10
                    SeekBarPreference(
                        title = stringResource(R.string.game_combo_speed_rate),
                        key = Pref.Key.GameSpace.GAME_SPACE_COMBO_SPEED_RATE,
                        defValue = 1,
                        min = 1,
                        max = 10,
                        format = "%dx"
                    )
                }
            }
        }
        // AI 触发器
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_ai_trigger)
            ) {
                // AI 触发器间隔调整（总开关）
                SwitchPreference(
                    title = stringResource(R.string.ai_trigger_switch),
                    summary = stringResource(R.string.ai_trigger_switch_tips),
                    key = Pref.Key.GameSpace.AI_TRIGGER_SWITCH
                ) {
                    enableAiTrigger = it
                }
                AnimatedVisibility(
                    enableAiTrigger
                ) {
                    Column() {
                        // 模板扫描间隔
                        SeekBarPreference(
                            title = stringResource(R.string.ai_trigger_template_scan_ms),
                            key = Pref.Key.GameSpace.AI_TRIGGER_TEMPLATE_SCAN_MS,
                            defValue = 180,
                            min = 80,
                            max = 2000,
                            format = "%d ms"
                        )
                        // 点击延迟
                        SeekBarPreference(
                            title = stringResource(R.string.ai_trigger_click_ms),
                            key = Pref.Key.GameSpace.AI_TRIGGER_CLICK_MS,
                            defValue = 25,
                            min = 10,
                            max = 500,
                            format = "%d ms"
                        )
                        // 触发冷却
                        SeekBarPreference(
                            title = stringResource(R.string.ai_trigger_cooldown_ms),
                            key = Pref.Key.GameSpace.AI_TRIGGER_COOLDOWN_MS,
                            defValue = 180,
                            min = 50,
                            max = 30000,
                            format = "%d ms"
                        )
                        // YOLO 扫描间隔
                        SeekBarPreference(
                            title = stringResource(R.string.ai_trigger_yolo_scan_ms),
                            key = Pref.Key.GameSpace.AI_TRIGGER_YOLO_SCAN_MS,
                            defValue = 400,
                            min = 150,
                            max = 1500,
                            format = "%d ms"
                        )
                    }

                }

            }
        }

    }
}
