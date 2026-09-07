package dev.lackluster.mihelper.ui.page


import android.graphics.drawable.Icon
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.AlertDialogMode
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.hyperx.compose.preference.DropDownPreference
import dev.lackluster.hyperx.compose.preference.EditTextDataType
import dev.lackluster.hyperx.compose.preference.EditTextPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.ui.MainActivity
import dev.lackluster.mihelper.ui.component.RebootMenuItem
import dev.lackluster.mihelper.data.Pages
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.utils.ShellUtils
import top.yukonga.miuix.kmp.basic.Icon


@Composable
fun SystemUpdatePage(
    navController: NavController,
    adjustPadding: PaddingValues,
    mode: BasePageDefaults.Mode
) {
    var visibilitySystemUpdateMockModel by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MODEL)
        )
    }
    var visibilitySystemUpdateMockIMEI by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_IMEI_SW)
        )
    }

    var visibilitySystemUpdateMockModelLocal by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_LOCAL_SW)
        )
    }

    var visibilitySystemUpdateMockModelSign by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SIGN_SW)
        )
    }

    var visibilitySystemUpdateMockModeFingerprint by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_FINGERPRINT_SW)
        )
    }

    var visibilitySystemUpdateMockModeBuildDisplay by remember {
        mutableStateOf(
            SafeSP.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_BUILD_DISPLAY_SW)
        )
    }

    var visibilitySystemUpdateMockModeInnerVersion by remember {
        mutableStateOf(
            SafeSP.getBoolean(
                Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SYSTEM_INNER_VERSION_SW
            )
        )
    }

    var visibilitySystemUpdateMockModeVariantId by remember {
        mutableStateOf(
            SafeSP.getBoolean(
                Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_VARIANT_ID_SW
            )
        )
    }
    var visibilitySystemUpdateMockModeManufacture by remember {
        mutableStateOf(
            SafeSP.getBoolean(
                Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MANUFACTURE_SW
            )
        )
    }



    BasePage(
        navController,
        adjustPadding,
        // 标题：系统设置
        stringResource(R.string.system_update),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            // 系统更新重启
            // appPkg 重启的对应包名：com.zte.zdm
            RebootMenuItem(
                // 弹窗标题的提示名称
                appName = stringResource(R.string.system_update),
                // 重启：com.zte.zdm
                appPkg = Scope.SYSTEM_UPDATE
                // 重启系统
//                appPkg = "android"
            )
        }
    ) {

        // 红魔-系统更新
        item {
            PreferenceGroup(
                stringResource(R.string.system_update),
                visible = true // 默认显示状态栏卡片
            ) {
                // 禁用系统更新
                SwitchPreference(
                    title = stringResource(R.string.prevent_system_update),
                    summary = stringResource(R.string.prevent_system_update_tips),
                    key = Pref.Key.NubiaSystemUpdate.DISABLE_SYSTEM_UPDATE, //唯一id
                )
                // 更新包地址
                // 系统更新点击安装后->自动复制更新包地址到剪贴板
                SwitchPreference(
                    title = stringResource(R.string.update_package_address),
                    summary = stringResource(R.string.update_package_address_tips),
                    key = Pref.Key.NubiaSystemUpdate.UPDATE_PACKAGE_ADDRESS, //唯一id
                )
            }
        }
        // 红魔-系统更新-伪装设备信息
        item {
            PreferenceGroup(
                stringResource(R.string.system_update_mock_info),
            ) {
                // model
                SwitchPreference(
                    title = stringResource(R.string.system_update_mock_model),
                    key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MODEL,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        //  visibilitySystemUpdateMockModel =  Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MODEL
                        visibilitySystemUpdateMockModel = newValue
                    }
                )
                AnimatedVisibility(visibilitySystemUpdateMockModel) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            // 修改设备型号
                            title = stringResource(R.string.system_update_mock_model_update),
                            key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MODEL_UPDATE,
                            defValue = "NX809J",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.system_update_mock_model_update_tips),
                        )
                    }
                }
                // imei
                SwitchPreference(
                    title = stringResource(R.string.system_update_mock_imei_sw),
                    key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_IMEI_SW,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        //  visibilitySystemUpdateMockModel =  Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_IMEI_SW
                        visibilitySystemUpdateMockIMEI = newValue
                    }
                )
                AnimatedVisibility(visibilitySystemUpdateMockIMEI) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            // 修改设备imei
                            title = stringResource(R.string.mock_imei),
                            key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_IMEI,
                            // "111111111111119", "000039485642710", "004400152020000"
                            defValue = "004400152020000", // 默认为空，查看打印的imei，为null的时候，它会返回1个默认的值
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.mock_imei_tips),
                        )
                    }
                }
                //local
                SwitchPreference(
                    title = stringResource(R.string.system_update_mock_local),
                    key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_LOCAL_SW,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        //  visibilitySystemUpdateMockModel =  Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_LOCAL_SW
                        visibilitySystemUpdateMockModelLocal = newValue
                    }
                )
                AnimatedVisibility(visibilitySystemUpdateMockModelLocal) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            // 修改地区
                            title = stringResource(R.string.mock_local),
                            key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_LOCAL,
                            defValue = "zh_CN",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.mock_local_tips),
                        )
                    }
                }
                //appSignatureMD5+appVersioncode
                SwitchPreference(
                    title = stringResource(R.string.system_update_mock_sign),
                    key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SIGN_SW,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        //  visibilitySystemUpdateMockModelSign =  Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SIGN_SW
                        visibilitySystemUpdateMockModelSign = newValue
                    }
                )
                AnimatedVisibility(visibilitySystemUpdateMockModelSign) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            // 修改签名
                            title = stringResource(R.string.mock_sign),
                            key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SIGN,
//                            defValue = "a2e44d1795185b8e3281444007effb7f_UNJ.REDMAGIC.FOTA.14.0.0.2409191549",
                            // a2e44d1795185b8e3281444007effb7f_WNJ.REDMAGIC.FOTA.16.0.000.000.2509092231
                            defValue = "a2e44d1795185b8e3281444007effb7f_WNJ.REDMAGIC.FOTA.16.0.000.000.2509092231",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.mock_sign_tips),
                        )
                    }
                }
                //build fingerprint
                SwitchPreference(
                    title = stringResource(R.string.system_update_mock_fingerprint),
                    key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_FINGERPRINT_SW,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        //  visibilitySystemUpdateMockModeFingerprint =  Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_FINGERPRINT_SW
                        visibilitySystemUpdateMockModeFingerprint = newValue
                    }
                )
                AnimatedVisibility(visibilitySystemUpdateMockModeFingerprint) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            // 修改指纹
                            title = stringResource(R.string.mock_fingerprint),
                            key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_FINGERPRINT,
//                            defValue = "nubia/NX769J/NX769J:14/UKQ1.230917.001/20250310.231626:user/release-keys",
                             // REDMAGIC/NX809J/NX809J:16/BQ2A.250705.001-BP2A.250605.031.A3/20250924.081558:user/release-keys
                            defValue = "REDMAGIC/NX809J/NX809J:16/BQ2A.250705.001-BP2A.250605.031.A3/20250924.081558:user/release-keys",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.mock_fingerprint_tips),
                        )
                    }
                }
                //system version
                SwitchPreference(
                    title = stringResource(R.string.system_update_mock_build_display),
                    key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_BUILD_DISPLAY_SW,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        //  visibilitySystemUpdateMockModeBuildDisplay = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_BUILD_DISPLAY_SW
                        visibilitySystemUpdateMockModeBuildDisplay = newValue
                    }
                )
                AnimatedVisibility(visibilitySystemUpdateMockModeBuildDisplay) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            // 修改版本
                            title = stringResource(R.string.mock_build_display),
                            key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_BUILD_DISPLAY,
//                            defValue = "RedMagicOS9.5.25_NX769J_NY",
                            defValue = "RedMagicOS11.0.11MR1",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.mock_build_display_tips),
                        )
                    }
                }
                //inner version
                SwitchPreference(
                    title = stringResource(R.string.system_update_mock_system_inner_version),
                    key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SYSTEM_INNER_VERSION_SW,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        //  visibilitySystemUpdateMockModeInnerVersion = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SYSTEM_INNER_VERSION_SW
                        visibilitySystemUpdateMockModeInnerVersion = newValue
                    }
                )
                AnimatedVisibility(visibilitySystemUpdateMockModeInnerVersion) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            // 内部版本
                            title = stringResource(R.string.mock_system_inner_version),
                            key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SYSTEM_INNER_VERSION,
//                            defValue = "GEN_CN_NX769SV1.5.0B25",
                            defValue = "GEN_CN_NX809JV1.0.0B11MR1",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.mock_system_inner_version_tips),
                        )
                    }
                }
                //VariantId
                SwitchPreference(
                    title = stringResource(R.string.system_update_mock_variant_id),
                    key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_VARIANT_ID_SW,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        //  visibilitySystemUpdateMockModeVariantId = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_VARIANT_ID_SW
                        visibilitySystemUpdateMockModeVariantId = newValue
                    }
                )
                AnimatedVisibility(visibilitySystemUpdateMockModeVariantId) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            // 设备版本
                            title = stringResource(R.string.mock_variant_id),
                            key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_VARIANT_ID,
//                            defValue = "GEN_NY_CN",
                            defValue = "GEN_CN",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.mock_variant_id_tips),
                        )
                    }
                }
                // mock manufacture
                SwitchPreference(
                    title = stringResource(R.string.system_update_mock_manufacture),
                    key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MANUFACTURE_SW,
                    defValue = false,
                    onCheckedChange = { newValue ->
                        //  visibilitySystemUpdateMockModeManufacture = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MANUFACTURE_SW
                        visibilitySystemUpdateMockModeManufacture = newValue
                    }
                )
                AnimatedVisibility(visibilitySystemUpdateMockModeManufacture) { // 确保 key 与当前开关的key 相同
                    Column {
                        EditTextPreference(
                            // 设备版本
                            title = stringResource(R.string.mock_manufacture),
                            key = Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MANUFACTURE,
                            defValue = "ZTE",
                            dataType = EditTextDataType.STRING,
                            dialogMessage = stringResource(R.string.mock_manufacture_tips),
                            isValueValid = { value ->
                                val stringValue = value as? String // 将 value 转换为 String 类型
                                val isValid = stringValue != null && (stringValue == "NUBIA" || stringValue == "ZTE")
                                isValid // 返回校验结果
                            }
                        )
                    }
                }




            }
        }
    }
}


