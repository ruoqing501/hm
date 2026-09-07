package dev.lackluster.redmagichelper.ui.page


import android.content.Context
import android.content.Intent
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.lackluster.redmagichelper.ui.component.RebootMenuItems
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
import kotlin.random.Random

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
        // 截图
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_screenshot),
            ) {
                // 截图时隐藏状态栏
                SwitchPreference(
                    title = stringResource(R.string.lock_screen_hide_status_bar),
                    key = Pref.Key.Other.SCREENSHOT_HIDE_STATUS_BAR
                )
            }
        }

        // 录屏
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_android_rercord_screen),
            ) {
                // 录屏时隐藏状态栏
                SwitchPreference(
                    title = stringResource(R.string.lock_screen_hide_status_bar),
                    key = Pref.Key.Other.RERCORD_SCREEN_HIDE_STATUS_BAR
                )
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
}