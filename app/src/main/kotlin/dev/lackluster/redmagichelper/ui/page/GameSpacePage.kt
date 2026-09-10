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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
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
import dev.lackluster.redmagichelper.hook.natives.RapidFireCompatibility
import dev.lackluster.redmagichelper.utils.ShellUtils
import dev.lackluster.redmagichelper.utils.rapidfire.RapidFireCompatTester
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

    // 肩键极速连点:兼容性测试的应用侧驱动(移植自 LS_Augment FeatureActivity 流程)
    val rapidTester = remember { RapidFireCompatTester(context.applicationContext) }
    var rapidTick by remember { mutableIntStateOf(0) }
    var rapidPanelRequested by remember { mutableStateOf(false) }
    val showRapidRebootDialog = remember { mutableStateOf(false) }
    val showRapidFuseDialog = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        rapidTester.updateListener = RapidFireCompatTester.UpdateListener { rapidTick++ }
        rapidTester.toastListener = { makeText(context, it, LENGTH_LONG).show() }
        rapidTester.refresh()
        onDispose { rapidTester.destroy() }
    }


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
        // 肩键极速连点(移植自 LS_Augment;兼容性测试通过后才解锁)
        item {
            @Suppress("UNUSED_VARIABLE")
            val rapidStateTick = rapidTick
            val session = rapidTester.session
            val fused = rapidTester.fused
            val unlocked = rapidTester.unlocked
            val fingerprintReady = rapidTester.fingerprint != null
            val now = System.currentTimeMillis()
            val expired = !unlocked && session != null && session.isExpired(now)
            val rapidState = when {
                fused -> RapidFireCompatibility.State.FUSED
                unlocked -> RapidFireCompatibility.State.PASSED
                session == null -> RapidFireCompatibility.State.UNTESTED
                expired -> RapidFireCompatibility.State.FAILED
                else -> session.state
            }
            val sideText = stringResource(
                if (rapidTester.captureLeft) R.string.tgk_rapid_fire_side_left
                else R.string.tgk_rapid_fire_side_right
            )
            val capturing = !expired && rapidTester.captureInFlight && session != null &&
                ((rapidTester.captureLeft &&
                    rapidState == RapidFireCompatibility.State.WAIT_LEFT) ||
                    (!rapidTester.captureLeft &&
                        rapidState == RapidFireCompatibility.State.WAIT_RIGHT))
            val statusText = when {
                !fingerprintReady -> stringResource(R.string.tgk_rapid_fire_status_loading)
                capturing -> {
                    val feedback = rapidTester.captureFeedback
                    when {
                        feedback != null -> feedback
                        rapidTester.captureDeadline == 0L ->
                            stringResource(R.string.tgk_rapid_fire_capturing_prepare)
                        else -> stringResource(
                            R.string.tgk_rapid_fire_capturing,
                            sideText,
                            ((rapidTester.captureDeadline - now + 999L) / 1000L).coerceAtLeast(0L)
                        )
                    }
                }
                else -> {
                    val base = when (rapidState) {
                        RapidFireCompatibility.State.PREFLIGHT ->
                            stringResource(R.string.tgk_rapid_fire_status_preflight)
                        RapidFireCompatibility.State.NEEDS_RESTART ->
                            stringResource(R.string.tgk_rapid_fire_status_needs_restart)
                        RapidFireCompatibility.State.WAIT_LEFT ->
                            stringResource(R.string.tgk_rapid_fire_status_wait_left)
                        RapidFireCompatibility.State.WAIT_RIGHT ->
                            stringResource(R.string.tgk_rapid_fire_status_wait_right)
                        RapidFireCompatibility.State.VERIFYING -> {
                            val remaining = if (session == null) 10L else
                                ((RapidFireCompatibility.STABILITY_REQUIRED_MS -
                                    (now - session.verifyingSince) + 999L) / 1000L)
                                    .coerceAtLeast(0L)
                            stringResource(R.string.tgk_rapid_fire_status_verifying, remaining)
                        }
                        RapidFireCompatibility.State.PASSED ->
                            stringResource(R.string.tgk_rapid_fire_status_passed_stale)
                        RapidFireCompatibility.State.FUSED ->
                            stringResource(R.string.tgk_rapid_fire_status_fused)
                        RapidFireCompatibility.State.FAILED ->
                            stringResource(
                                if (expired) R.string.tgk_rapid_fire_status_expired
                                else R.string.tgk_rapid_fire_status_failed
                            )
                        else -> stringResource(R.string.tgk_rapid_fire_status_untested)
                    }
                    val feedback = rapidTester.captureFeedback
                    if (feedback != null &&
                        (rapidState == RapidFireCompatibility.State.WAIT_LEFT ||
                            rapidState == RapidFireCompatibility.State.WAIT_RIGHT)
                    ) "$feedback\n\n$base" else base
                }
            }
            val actionText = when {
                capturing -> stringResource(R.string.tgk_rapid_fire_action_capturing, sideText)
                rapidState == RapidFireCompatibility.State.PREFLIGHT ->
                    stringResource(R.string.tgk_rapid_fire_action_checking)
                rapidState == RapidFireCompatibility.State.NEEDS_RESTART ->
                    stringResource(R.string.tgk_rapid_fire_action_reboot)
                rapidState == RapidFireCompatibility.State.WAIT_LEFT ->
                    stringResource(
                        if (session != null && session.physicalLeft > 0)
                            R.string.tgk_rapid_fire_action_check_left
                        else R.string.tgk_rapid_fire_action_capture_left
                    )
                rapidState == RapidFireCompatibility.State.WAIT_RIGHT ->
                    stringResource(
                        if (session != null && session.physicalRight > 0)
                            R.string.tgk_rapid_fire_action_check_right
                        else R.string.tgk_rapid_fire_action_capture_right
                    )
                rapidState == RapidFireCompatibility.State.VERIFYING ->
                    stringResource(R.string.tgk_rapid_fire_action_verify)
                rapidState == RapidFireCompatibility.State.FUSED ->
                    stringResource(R.string.tgk_rapid_fire_action_clear_fuse)
                rapidState == RapidFireCompatibility.State.PASSED ->
                    stringResource(R.string.tgk_rapid_fire_action_retest)
                rapidState == RapidFireCompatibility.State.FAILED ->
                    stringResource(R.string.tgk_rapid_fire_action_restart)
                else -> stringResource(R.string.tgk_rapid_fire_action_start)
            }
            val actionEnabled = fingerprintReady && !capturing &&
                rapidState != RapidFireCompatibility.State.PREFLIGHT
            val cancelVisible = session != null && session.active(now) && !unlocked

            PreferenceGroup(
                title = stringResource(R.string.ui_title_tgk_rapid_fire)
            ) {
                // 总开关(兼容性测试通过前禁用)
                var enableRapidFire by remember {
                    mutableStateOf(
                        SafeSP.getBoolean(Pref.Key.GameSpace.TGK_RAPID_FIRE_ENABLED, false)
                    )
                }
                SwitchPreference(
                    title = stringResource(R.string.tgk_rapid_fire_switch),
                    summary = stringResource(
                        if (unlocked) R.string.tgk_rapid_fire_unlocked_tips
                        else R.string.tgk_rapid_fire_locked_tips
                    ),
                    key = Pref.Key.GameSpace.TGK_RAPID_FIRE_ENABLED,
                    enabled = unlocked
                ) {
                    enableRapidFire = it
                }
                AnimatedVisibility(unlocked && enableRapidFire) {
                    Column {
                        SeekBarPreference(
                            title = stringResource(R.string.tgk_rapid_fire_cps),
                            key = Pref.Key.GameSpace.TGK_RAPID_FIRE_CPS,
                            defValue = 20,
                            min = 10,
                            max = 50,
                            format = "%d/s"
                        )
                        TextPreference(
                            title = stringResource(R.string.tgk_rapid_fire_cps_tips)
                        )
                    }
                }
                if (!unlocked) {
                    // 兼容性测试入口
                    TextPreference(
                        title = stringResource(R.string.tgk_rapid_fire_test_entry),
                        onClick = {
                            rapidPanelRequested = true
                            rapidTester.refresh()
                        }
                    )
                    if (rapidPanelRequested || session != null || fused) {
                        TextPreference(title = statusText)
                        TextPreference(
                            title = actionText,
                            enabled = actionEnabled,
                            onClick = {
                                when (rapidState) {
                                    RapidFireCompatibility.State.NEEDS_RESTART ->
                                        showRapidRebootDialog.value = true
                                    RapidFireCompatibility.State.FUSED ->
                                        showRapidFuseDialog.value = true
                                    else -> rapidTester.onAction()
                                }
                            }
                        )
                        if (cancelVisible) {
                            TextPreference(
                                title = stringResource(R.string.tgk_rapid_fire_test_cancel),
                                onClick = { rapidTester.cancelTest() }
                            )
                        }
                    }
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
    // 肩键连点:重启设备确认(system_server 尚未加载当前模块)
    AlertDialog(
        visibility = showRapidRebootDialog,
        title = stringResource(R.string.tgk_rapid_fire_reboot_title),
        message = stringResource(R.string.tgk_rapid_fire_reboot_message),
        mode = AlertDialogMode.NegativeAndPositive,
        negativeText = stringResource(R.string.button_cancel),
        positiveText = stringResource(R.string.tgk_rapid_fire_reboot_now)
    ) {
        showRapidRebootDialog.value = false
        rapidTester.requestReboot()
    }
    // 肩键连点:清除熔断确认
    AlertDialog(
        visibility = showRapidFuseDialog,
        title = stringResource(R.string.tgk_rapid_fire_fuse_title),
        message = stringResource(R.string.tgk_rapid_fire_fuse_message),
        mode = AlertDialogMode.NegativeAndPositive,
        negativeText = stringResource(R.string.button_cancel),
        positiveText = stringResource(R.string.tgk_rapid_fire_fuse_clear)
    ) {
        showRapidFuseDialog.value = false
        rapidTester.clearFuse()
    }
}