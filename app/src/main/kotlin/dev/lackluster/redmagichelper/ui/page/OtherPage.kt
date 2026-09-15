package dev.lackluster.redmagichelper.ui.page


import android.content.Context
import android.content.Intent
import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.BitmapFactory
import android.provider.Settings
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.base.IconSize
import dev.lackluster.hyperx.compose.base.ImageIcon
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.redmagichelper.BuildConfig.BUILD_TYPE
import dev.lackluster.redmagichelper.BuildConfig.VERSION_CODE
import dev.lackluster.redmagichelper.BuildConfig.VERSION_NAME
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.data.Contributors
import dev.lackluster.redmagichelper.data.References
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.core.net.toUri
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.Card
import dev.lackluster.hyperx.compose.base.CardDefaults
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.preference.EditTextDataType
import dev.lackluster.hyperx.compose.preference.EditTextPreference
import dev.lackluster.hyperx.compose.preference.ImageDialogWithCancel
import dev.lackluster.hyperx.compose.preference.ImagePreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.redmagichelper.data.Codes
import dev.lackluster.redmagichelper.data.Constants
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.service.RmhTileService
import dev.lackluster.redmagichelper.ui.component.RebootMenuItems
import dev.lackluster.redmagichelper.utils.BatteryLifeControl
import dev.lackluster.redmagichelper.utils.BatteryLifePolicy
import dev.lackluster.redmagichelper.utils.ScreenOffHideExecutor
import dev.lackluster.redmagichelper.utils.ShellUtils
import dev.lackluster.redmagichelper.utils.FanCalibrationChannel
import dev.lackluster.redmagichelper.utils.FanCalibrationData
import dev.lackluster.redmagichelper.utils.FanHardwareIdentity
import dev.lackluster.redmagichelper.utils.IconCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.DropdownImpl
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.ImmersionMore
import kotlin.random.Random
import java.text.DateFormat
import java.util.Date
import java.util.UUID

/** 风扇校准结果摘要:显示测量时间与各档实测转速。 */
private fun buildFanMeasurementSummary(context: Context): String {
    val data = FanCalibrationData.parse(SafeSP.getString(Pref.Key.Fan.MEASUREMENT, ""))
        ?: return context.getString(R.string.fan_calibrate_result_none)
    val time = DateFormat.getDateTimeInstance().format(Date(data.measuredAt))
    val stale = if (data.currentFor(FanHardwareIdentity.current())) ""
    else "\n" + context.getString(R.string.fan_calibrate_import_stale)
    val levels = (1..FanCalibrationData.LEVELS).joinToString("\n") { i -> "$i: ${data.rpm(i)} RPM" }
    return "$time$stale\n$levels"
}

/** 电池信息来源路径显示为短名,避免 sysfs 全路径过长。 */
private fun batterySourceName(path: String): String = when (path) {
    BatteryLifeControl.PATH_CYCLE_COUNT,
    BatteryLifeControl.PATH_CHARGE_FULL,
    BatteryLifeControl.PATH_CHARGE_FULL_DESIGN -> "power_supply/battery"
    BatteryLifeControl.PATH_BATTERY_CYCLE -> "qcom-battery"
    BatteryLifeControl.PATH_CYCLE_DAT -> "persist/zstats"
    else -> path
}

/** 电池信息摘要:循环次数、满充容量估计、设计容量及来源;需在 IO 线程调用(root 读取)。 */
private fun buildBatteryInfoSummary(context: Context): String {
    val values = BatteryLifeControl.read()
    values["error"]?.let { return context.getString(R.string.battery_info_read_failed, it) }
    val unavailable = context.getString(R.string.battery_info_unavailable)
    var cycle: String? = null
    var cycleSource: String? = null
    for (path in listOf(
        BatteryLifeControl.PATH_CYCLE_COUNT,
        BatteryLifeControl.PATH_BATTERY_CYCLE,
        BatteryLifeControl.PATH_CYCLE_DAT
    )) {
        val raw = values[path] ?: continue
        val parsed = raw.trim().toLongOrNull() ?: BatteryLifePolicy.lastRecordedCycles(raw)
        if (parsed != null) {
            cycle = parsed.toString()
            cycleSource = batterySourceName(path)
            break
        }
    }
    fun capacityMah(path: String): String? =
        values[path]?.trim()?.toLongOrNull()?.takeIf { it > 0 }?.let { (it / 1000).toString() }
    val full = capacityMah(BatteryLifeControl.PATH_CHARGE_FULL)
    val design = capacityMah(BatteryLifeControl.PATH_CHARGE_FULL_DESIGN)
    return listOf(
        context.getString(R.string.battery_info_cycle, cycle ?: unavailable, cycleSource ?: unavailable),
        context.getString(
            R.string.battery_info_charge_full, full ?: unavailable,
            if (full != null) batterySourceName(BatteryLifeControl.PATH_CHARGE_FULL) else unavailable
        ),
        context.getString(R.string.battery_info_design, design ?: unavailable),
    ).joinToString("\n")
}

@Composable
fun OtherPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    val context = LocalContext.current
    val showTopPopup = remember { mutableStateOf(false) }
    val contextMenuItems = listOf(
        stringResource(R.string.ui_title_menu_reboot),
    )
    val hapticFeedback = LocalHapticFeedback.current
    var mtpRenameRootNameSwitchStatus by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.Other.MTP_RENAME_ROOT_NAME_SWITCH)
        )
    }
    var neoStoreDownloadEnabled by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.NeoStore.STORE_DOWNLOAD_ENABLED)
        )
    }
    var fanFixedEnabled by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.Fan.FIXED_ENABLED)
        )
    }
    var fanMeasurementSummary by remember {
        mutableStateOf(buildFanMeasurementSummary(context))
    }
    var fanManualEntryEnabled by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.Fan.MANUAL_ENTRY_ENABLED)
        )
    }
    val rootGranted = MainActivity.rootGranted.value
    var batteryInfoSummary by remember {
        mutableStateOf(context.getString(R.string.battery_info_refresh))
    }
    val batteryDisableAgeReduction = remember {
        mutableStateOf(SafeSP.getBoolean(Pref.Key.Battery.DISABLE_AGE_REDUCTION, false))
    }
    var batteryBusy by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val showTileImageDialog = remember { mutableStateOf(false) }

    LaunchedEffect(rootGranted) {
        if (rootGranted) {
            batteryInfoSummary = withContext(Dispatchers.IO) { buildBatteryInfoSummary(context) }
        }
    }

    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.page_others),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        //actions = {
        //
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
                                        navController.navigateTo(Pages.OTHER_MENU)
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
        }
    ) {
        // MTP浏览
        item {
            PreferenceGroup(
                title = stringResource(R.string.mtp_service),
                first = true
            ) {
                //隐藏MTP中分类浏览
                SwitchPreference(
                    title = stringResource(R.string.hide_mtp_category_browse),
                    summary = stringResource(R.string.hide_mtp_category_browse_tips),
                    key = Pref.Key.Other.HIDE_MTP_CATEGORY_BROWSE
                )
                // 重命名MTP存储设备名
                SwitchPreference(
                    title = stringResource(R.string.rename_mtp_storage_root_name),
                    summary = stringResource(R.string.rename_mtp_storage_root_name_tips),
                    key = Pref.Key.Other.MTP_RENAME_ROOT_NAME_SWITCH,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        mtpRenameRootNameSwitchStatus = newValue
                    }
                )
                // 确保 key 与当前开关的key 相同
                AnimatedVisibility(mtpRenameRootNameSwitchStatus) {
                    Column {
                        EditTextPreference(
                            title = stringResource(R.string.mtp_storage_root_name),
                            //summary = stringResource(R.string.rename_mtp_storage_root_name_tips),
                            key = Pref.Key.Other.MTP_RENAME_ROOT_NAME,
                            defValue = stringResource(R.string.mtp_storage_root_name_Internal_storage_device),
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.rename_mtp_storage_root_name_tips)
                        )
                    }
                }
            }
        }
        // NFC 服务
        item {
            PreferenceGroup(
                title = stringResource(R.string.nfc_service),
            ) {
                // 禁用NFC提示音
                SwitchPreference(
                    title = stringResource(R.string.mute_nfc_sound),
                    key = Pref.Key.Other.MUTE_NFC_SOUND
                )
                // 允许息屏时识别NFC
                SwitchPreference(
                    title = stringResource(R.string.nfc_allow_screen_off_recognition),
                    key = Pref.Key.Other.NFC_ALLOW_SCREEN_OFF_RECOGNITION
                )
            }
        }
        // 权限控制器
        item {
            PreferenceGroup(
                title = stringResource(R.string.scope_permission_controller),
            ) {
                // 允许设置默认第三方桌面
                SwitchPreference(
                    title = stringResource(R.string.allow_thirdparty_launcher),
                    key = Pref.Key.Other.ALLOW_THIRDPARTY_LAUNCHER
                )
            }
        }
        // 百度输入法定制版
        item {
            PreferenceGroup(
                title = stringResource(R.string.scope_baidu_ime),
            ) {
                // 复制粘贴字数无限制
                SwitchPreference(
                    title = stringResource(R.string.baidu_ime_no_clipboard_limit),
                    summary = stringResource(R.string.baidu_ime_no_clipboard_limit_tips),
                    key = Pref.Key.Other.BAIDU_IME_NO_CLIPBOARD_LIMIT
                )
            }
        }
        // 应用中心
        item {
            PreferenceGroup(
                title = stringResource(R.string.scope_neo_store),
            ) {
                // 自定义应用商店同时下载数量
                SwitchPreference(
                    title = stringResource(R.string.neo_store_download_enabled),
                    summary = stringResource(R.string.neo_store_download_enabled_tips),
                    key = Pref.Key.NeoStore.STORE_DOWNLOAD_ENABLED,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        neoStoreDownloadEnabled = newValue
                    }
                )
                AnimatedVisibility(neoStoreDownloadEnabled) {
                    Column {
                        // 同时下载数量(1~50)
                        SeekBarPreference(
                            title = stringResource(R.string.neo_store_download_count),
                            key = Pref.Key.NeoStore.STORE_DOWNLOAD_COUNT,
                            defValue = 5,
                            min = 1,
                            max = 50
                        )
                    }
                }
            }
        }
        // 熄屏自动隐藏
        item {
            PreferenceGroup(
                title = stringResource(R.string.screen_off_hide_group),
            ) {
                // 熄屏时自动隐藏所选应用
                SwitchPreference(
                    title = stringResource(R.string.screen_off_hide_switch),
                    summary = stringResource(R.string.screen_off_hide_switch_tips),
                    key = Pref.Key.Other.SCREEN_OFF_HIDE_ENABLED
                )
                // 选择要隐藏的应用
                TextPreference(
                    title = stringResource(R.string.screen_off_hide_pick_apps),
                    summary = stringResource(R.string.screen_off_hide_pick_apps_summary),
                    onClick = { navController.navigateTo(Pages.SCREEN_OFF_HIDE_APPS) }
                )
                // 立即恢复全部(手动恢复入口,与 LS_Augment 的"全部显示"对应)
                TextPreference(
                    title = stringResource(R.string.screen_off_hide_restore_all),
                    summary = stringResource(R.string.screen_off_hide_restore_all_summary),
                    onClick = {
                        coroutineScope.launch {
                            val outcome = withContext(Dispatchers.IO) {
                                ScreenOffHideExecutor.restoreAll(
                                    SafeSP.getStringSet(Pref.Key.Other.SCREEN_OFF_HIDE_TARGETS, mutableSetOf())
                                )
                            }
                            makeText(
                                context,
                                if (outcome.success) R.string.screen_off_hide_restore_success
                                else R.string.screen_off_hide_restore_failed,
                                LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }
        // 快捷设置磁贴(移植自 LS_Augment 的磁贴自定义;点击行为为「恢复全部隐藏应用」)
        item {
            PreferenceGroup(
                title = stringResource(R.string.tile_group),
            ) {
                // 启用磁贴
                SwitchPreference(
                    title = stringResource(R.string.tile_switch),
                    summary = stringResource(R.string.tile_switch_tips),
                    key = Pref.Key.Module.TILE_ENABLED,
                    defValue = true,
                    onCheckedChange = {
                        RmhTileService.requestRefresh(context)
                    }
                )
                // 磁贴名称
                EditTextPreference(
                    title = stringResource(R.string.tile_label),
                    summary = stringResource(R.string.tile_label_summary),
                    key = Pref.Key.Module.TILE_LABEL,
                    defValue = stringResource(R.string.tile_default_label),
                    dataType = EditTextDataType.STRING,
                    onValueChange = { _, _ -> RmhTileService.requestRefresh(context) }
                )
                // 磁贴描述(副标题)
                EditTextPreference(
                    title = stringResource(R.string.tile_description),
                    summary = stringResource(R.string.tile_description_summary),
                    key = Pref.Key.Module.TILE_DESCRIPTION,
                    defValue = stringResource(R.string.tile_default_description),
                    dataType = EditTextDataType.STRING,
                    onValueChange = { _, _ -> RmhTileService.requestRefresh(context) }
                )
                // 磁贴图片
                TextPreference(
                    title = stringResource(R.string.tile_image),
                    summary = stringResource(R.string.tile_image_summary),
                    onClick = { showTileImageDialog.value = true }
                )
                // 添加磁贴到快捷设置(Android 13+ 的系统添加请求)
                TextPreference(
                    title = stringResource(R.string.tile_add),
                    summary = stringResource(R.string.tile_add_summary),
                    onClick = {
                        val statusBar = context.getSystemService(StatusBarManager::class.java)
                        if (statusBar == null) {
                            makeText(context, R.string.tile_add_failed, LENGTH_LONG).show()
                        } else runCatching {
                            statusBar.requestAddTileService(
                                ComponentName(context, RmhTileService::class.java),
                                SafeSP.getString(Pref.Key.Module.TILE_LABEL, "")
                                    .ifEmpty { context.getString(R.string.tile_default_label) },
                                RmhTileService.resolveIcon(context),
                                context.mainExecutor
                            ) { result ->
                                makeText(
                                    context,
                                    when (result) {
                                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> R.string.tile_add_added
                                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> R.string.tile_add_exists
                                        else -> R.string.tile_add_not_added
                                    },
                                    LENGTH_SHORT
                                ).show()
                            }
                        }.onFailure {
                            makeText(context, R.string.tile_add_failed, LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
        // 风扇控制
        item {
            PreferenceGroup(
                title = stringResource(R.string.fan_control_group),
            ) {
                // 固定风扇转速
                SwitchPreference(
                    title = stringResource(R.string.fan_fixed_enabled),
                    summary = stringResource(R.string.fan_fixed_enabled_tips),
                    key = Pref.Key.Fan.FIXED_ENABLED,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        fanFixedEnabled = newValue
                    }
                )
                AnimatedVisibility(fanFixedEnabled) {
                    Column {
                        // 目标转速
                        SeekBarPreference(
                            title = stringResource(R.string.fan_target_rpm),
                            key = Pref.Key.Fan.TARGET_RPM,
                            defValue = 12000,
                            min = 500,
                            max = 30000
                        )
                    }
                }
                // 解除原厂最高档限制(第 5 档)
                SwitchPreference(
                    title = stringResource(R.string.fan_unlock_max),
                    summary = stringResource(R.string.fan_unlock_max_tips),
                    key = Pref.Key.Fan.UNLOCK_MAX
                )
                // 发起转速测量(经远程配置下发请求,风扇应用进程消费)
                TextPreference(
                    title = stringResource(R.string.fan_calibrate_start),
                    summary = stringResource(R.string.fan_calibrate_start_tips),
                    onClick = {
                        val request = System.currentTimeMillis().toString() + ":" +
                            UUID.randomUUID().toString().replace("-", "")
                        SafeSP.putAny(Pref.Key.Fan.CALIBRATION_REQUEST, request)
                        makeText(context, R.string.fan_calibrate_requested, LENGTH_LONG).show()
                    }
                )
                // 导入测量结果(hook 侧经 Settings.System 回写)
                TextPreference(
                    title = stringResource(R.string.fan_calibrate_import),
                    summary = fanMeasurementSummary,
                    onClick = {
                        val raw = runCatching {
                            Settings.System.getString(
                                context.contentResolver,
                                FanCalibrationChannel.MEASUREMENT_KEY
                            )
                        }.getOrNull()
                        val data = FanCalibrationData.parse(raw)
                        if (data == null || raw == null) {
                            makeText(context, R.string.fan_calibrate_import_none, LENGTH_SHORT).show()
                        } else {
                            SafeSP.putAny(Pref.Key.Fan.MEASUREMENT, raw)
                            makeText(
                                context,
                                if (data.currentFor(FanHardwareIdentity.current()))
                                    R.string.fan_calibrate_import_ok
                                else R.string.fan_calibrate_import_stale,
                                LENGTH_LONG
                            ).show()
                        }
                        fanMeasurementSummary = buildFanMeasurementSummary(context)
                    }
                )
                // 手动输入校准数据(自动回写不可用时的退化方案)
                SwitchPreference(
                    title = stringResource(R.string.fan_manual_entry),
                    summary = stringResource(R.string.fan_manual_entry_tips),
                    key = Pref.Key.Fan.MANUAL_ENTRY_ENABLED,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        fanManualEntryEnabled = newValue
                    }
                )
                AnimatedVisibility(fanManualEntryEnabled) {
                    Column {
                        val manualKeys = listOf(
                            Pref.Key.Fan.MANUAL_RPM_1,
                            Pref.Key.Fan.MANUAL_RPM_2,
                            Pref.Key.Fan.MANUAL_RPM_3,
                            Pref.Key.Fan.MANUAL_RPM_4,
                            Pref.Key.Fan.MANUAL_RPM_5,
                        )
                        manualKeys.forEachIndexed { index, manualKey ->
                            EditTextPreference(
                                title = stringResource(R.string.fan_manual_rpm, index + 1),
                                key = manualKey,
                                defValue = 0,
                                dataType = EditTextDataType.INT
                            )
                        }
                        TextPreference(
                            title = stringResource(R.string.fan_manual_apply),
                            onClick = {
                                val rpm = manualKeys.map { SafeSP.getInt(it, 0) }.toIntArray()
                                val data = FanCalibrationData.fromManual(
                                    FanHardwareIdentity.current(),
                                    System.currentTimeMillis(),
                                    rpm
                                )
                                if (data == null) {
                                    makeText(context, R.string.fan_manual_apply_invalid, LENGTH_LONG).show()
                                } else {
                                    SafeSP.putAny(Pref.Key.Fan.MEASUREMENT, data.serialize())
                                    fanMeasurementSummary = buildFanMeasurementSummary(context)
                                    makeText(context, R.string.fan_manual_apply_ok, LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
        // 电池
        item {
            PreferenceGroup(
                title = stringResource(R.string.battery_group),
            ) {
                if (!rootGranted) {
                    // Root 不可用:功能静默停用,仅给出提示
                    TextPreference(
                        title = stringResource(R.string.battery_info),
                        summary = stringResource(R.string.battery_info_root_unavailable)
                    )
                } else {
                    // 循环次数与容量(点击刷新)
                    TextPreference(
                        title = stringResource(R.string.battery_info),
                        summary = batteryInfoSummary,
                        onClick = {
                            if (!batteryBusy) {
                                batteryBusy = true
                                coroutineScope.launch {
                                    batteryInfoSummary = withContext(Dispatchers.IO) {
                                        buildBatteryInfoSummary(context)
                                    }
                                    batteryBusy = false
                                }
                            }
                        }
                    )
                    // 关闭按循环降压策略(可逆 bind 覆盖;失败时回滚开关与状态)
                    SwitchPreference(
                        title = stringResource(R.string.battery_disable_age_reduction),
                        summary = stringResource(R.string.battery_disable_age_reduction_tips),
                        key = Pref.Key.Battery.DISABLE_AGE_REDUCTION,
                        defValue = false,
                        enabled = !batteryBusy,
                        checked = batteryDisableAgeReduction,
                        onCheckedChange = { newValue ->
                            batteryBusy = true
                            coroutineScope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    BatteryLifeControl.reconcile(newValue)
                                }
                                batteryBusy = false
                                if (result.isSuccess) {
                                    batteryDisableAgeReduction.value = newValue
                                    makeText(
                                        context,
                                        context.getString(R.string.battery_apply_success, result.output),
                                        LENGTH_LONG
                                    ).show()
                                } else {
                                    SafeSP.putAny(Pref.Key.Battery.DISABLE_AGE_REDUCTION, !newValue)
                                    batteryDisableAgeReduction.value = !newValue
                                    makeText(
                                        context,
                                        context.getString(
                                            R.string.battery_apply_failed,
                                            result.publicError()
                                        ),
                                        LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
        //// 应用双开
        //item {
        //// 应用双开
        //item {
        //    PreferenceGroup(
        //        title = stringResource(R.string.scope_double_app),
        //        first = true
        //    ) {
        //        //去除低内存设备两个双开应用限制
        //        SwitchPreference(
        //            title = stringResource(R.string.rm_double_low_memory_limit),
        //            summary = stringResource(R.string.rm_double_low_memory_limit_tips),
        //            key = Pref.Key.Other.RM_LOW_MEMORY_LIMIT
        //        )
        //        // 双开任意应用
        //        SwitchPreference(
        //            title = stringResource(R.string.double_any_app),
        //            summary = stringResource(R.string.double_any_app_tips),
        //            key = Pref.Key.Other.DOUBLE_ANY_APP
        //        )
        //    }
        //}
    }

    if (showTileImageDialog.value) {
        TileImageDialog(show = showTileImageDialog)
    }
}

/**
 * 磁贴图片选择对话框:GetContent 选图 → [IconCodec.encodeIcon] 居中方形裁剪缩放为
 * 192px WebP → Base64 存入 Prefs。保存后立即请求系统刷新磁贴监听状态,使新图片即时生效。
 */
@Composable
private fun TileImageDialog(show: MutableState<Boolean>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var iconBase64 by remember { mutableStateOf(SafeSP.getString(Pref.Key.Module.TILE_ICON, "")) }

    val preview by produceState<ImageBitmap?>(initialValue = null, iconBase64) {
        value = withContext(Dispatchers.IO) {
            iconBase64.takeIf { it.isNotEmpty() }
                ?.let { IconCodec.decodeIcon(it) }
                ?.asImageBitmap()
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val encoded = withContext(Dispatchers.IO) {
                runCatching {
                    val source = context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    } ?: return@runCatching null
                    IconCodec.encodeIcon(source)
                }.getOrNull()
            }
            if (encoded != null) {
                iconBase64 = encoded
                SafeSP.putAny(Pref.Key.Module.TILE_ICON, encoded)
                RmhTileService.requestRefresh(context)
                makeText(context, R.string.tile_image_saved, LENGTH_SHORT).show()
            } else {
                makeText(context, R.string.tile_image_failed, LENGTH_LONG).show()
            }
        }
    }

    SuperDialog(
        title = stringResource(R.string.tile_image),
        summary = stringResource(R.string.tile_image_summary),
        show = show,
        onDismissRequest = { show.value = false }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            preview?.let {
                Image(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(72.dp),
                    bitmap = it,
                    contentDescription = stringResource(R.string.tile_image)
                )
            } ?: Image(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(72.dp),
                painter = painterResource(R.drawable.ic_tile_restore),
                contentDescription = stringResource(R.string.tile_image)
            )
            Row {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.tile_image_choose),
                    onClick = { pickImage.launch("image/*") }
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.tile_image_reset),
                    enabled = iconBase64.isNotEmpty(),
                    onClick = {
                        iconBase64 = ""
                        SafeSP.putAny(Pref.Key.Module.TILE_ICON, "")
                        RmhTileService.requestRefresh(context)
                        makeText(context, R.string.tile_image_saved, LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}