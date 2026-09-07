package dev.lackluster.mihelper.hook.rules.updatesystem.nubia

import android.nfc.Tag
import android.os.Build
import android.os.PowerManager
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.log.YLog
import de.robv.android.xposed.XposedBridge
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.DexKit.dexKitBridge
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.hasEnable
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method
import java.nio.ByteBuffer

object MockDeviceInfo : YukiBaseHooker() {
    private const val TAG = "MockDeviceInfo"

    // 开关：模拟设备型号
    private val system_update_mock_model by lazy {
        Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MODEL, false)
    }

    // 输入：模拟设备型号的值
    private val system_update_mock_model_update by lazy {
        Prefs.getString(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MODEL_UPDATE, "NX809J")
    }

    // 开关：模拟IMEI
    private val system_update_mock_imei_sw by lazy {
        Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_IMEI_SW, false)
    }

    // 输入：模拟IMEI的值
    private val system_update_mock_imei by lazy {
//        Prefs.getString(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_IMEI, "004400152020000")
        Prefs.getString(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_IMEI, "004400152020000")
    }

    // 开关：模拟地区
    private val system_update_mock_local_sw by lazy {
        Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_LOCAL_SW, false)
    }

    // 输入：模拟地区的值
    private val system_update_mock_local by lazy {
        Prefs.getString(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_LOCAL, "zh_CN")
    }

    // 开关：模拟签名
    private val system_update_mock_sign_sw by lazy {
        Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SIGN_SW, false)
    }

    // 输入：模拟签名的值
    private val system_update_mock_sign by lazy {
        Prefs.getString(
            Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SIGN,
            "a2e44d1795185b8e3281444007effb7f_WNJ.REDMAGIC.FOTA.16.0.000.000.2509092231"
        )
    }

    // 开关：模拟指纹
    private val system_update_mock_fingerprint_sw by lazy {
        Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_FINGERPRINT_SW, false)
    }

    // 输入：模拟指纹的值
    private val system_update_mock_fingerprint by lazy {
        Prefs.getString(
            Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_FINGERPRINT,
            "REDMAGIC/NX809J/NX809J:16/BQ2A.250705.001-BP2A.250605.031.A3/20250924.081558:user/release-keys"
        )
    }

    // 开关：模拟构建显示版本
    private val system_update_mock_build_display_sw by lazy {
        Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_BUILD_DISPLAY_SW, false)
    }

    // 输入：模拟构建显示版本的值
    private val system_update_mock_build_display by lazy {
        Prefs.getString(
            Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_BUILD_DISPLAY,
            "RedMagicOS11.0.11MR1"
        )
    }

    // 开关：模拟系统内部版本
    private val system_update_mock_system_inner_version_sw by lazy {
        Prefs.getBoolean(
            Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SYSTEM_INNER_VERSION_SW,
            false
        )
    }

    // 输入：模拟系统内部版本的值
    private val system_update_mock_system_inner_version by lazy {
        Prefs.getString(
            Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_SYSTEM_INNER_VERSION,
            "GEN_CN_NX809JV1.0.0B11MR1"
        )
    }

    // 开关：模拟变体ID
    private val system_update_mock_variant_id_sw by lazy {
        Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_VARIANT_ID_SW, false)
    }

    // 输入：模拟变体ID的值
    private val system_update_mock_variant_id by lazy {
        Prefs.getString(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_VARIANT_ID, "GEN_CN")
    }

    // 开关：模拟制造商
    private val system_update_mock_manufacture_sw by lazy {
        Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MANUFACTURE_SW, false)
    }

    // 输入：模拟制造商的值
    private val system_update_mock_manufacture by lazy {
        Prefs.getString(Pref.Key.NubiaSystemUpdate.SYSTEM_UPDATE_MOCK_MANUFACTURE, "ZTE")
    }

    override fun onHook() {
        // Hook逻辑待实现
        disguisedDeviceModel()
        disguisedImei()
        disguisedLocale()
        disguisedSign()
//        disguisedSignInfo()
        disguisedFingerprint()
//        hookFingerprintPrintInfo()
        disguisedSystemBuildVer()
//        disguisedSystemBuildVerInfo()
        disguisedInternalVersion()
//        disguisedInternalVersionInfo()
        disguisedVariantID()
//        disguisedVariantIDInfo()
        disguisedManufacture()
    }

    fun disguisedDeviceModel() {
        when (val me = findDeviceModel(dexKitBridge)) {
            null -> YLog.debug("$TAG find DeviceModel Failed")
            else -> {
                me.hook {
                    before {
                        if (system_update_mock_model) { // 如果开启了模拟设备型号

                            result = system_update_mock_model_update //将模拟设备型号的值赋给result
                            YLog.debug("$TAG update DeviceModel： $result")
                        }
                    }
                    after {
                        val original = result as String          // 获取原始值
                        YLog.debug("$TAG 原始 DeviceModel = $original")
                    }
                }
                    .also { YLog.info("$TAG find and hook DeviceModel success at ${me.declaringClass.name} -> ${me.name}") }
            }
        }

    }

    fun disguisedImei() {
        val imeiMethods = findImei(dexKitBridge)
        if (imeiMethods.isNotEmpty()) {
            imeiMethods.forEach { method ->
                method.hook {
                    before {
                        if (system_update_mock_imei_sw) { // 如果开启了模拟IMEI
                            result = system_update_mock_imei //将模拟IMEI的值赋给result
                            YLog.debug("$TAG 模拟IMEI： $result")
                        }
                    }
//                    after {
//                        val original = result as String          // 获取原始值
//                        YLog.debug("$TAG 原始 DeviceModel = $original")
//                    }
                }
                YLog.info("$TAG 成功Hook模拟IMEI: ${method.declaringClass.name} -> ${method.name}")
            }
        } else {
            YLog.error("$TAG find DeviceImei Failed")
        }
    }

    fun disguisedImeiInfo() { // ui 页面的参数-当前属性的参数必须为空
        val imeiMethods = findImei(dexKitBridge)
        if (imeiMethods.isNotEmpty()) {
            imeiMethods.forEach { method ->
                method.hook {
                    after {
                        // 打印最终返回的 IMEI 值（可能是模拟后的）
                        val finalImei = result as? String ?: "null"
                        YLog.info("$TAG 最终返回的 IMEI = $finalImei (方法: ${method.declaringClass.name}.${method.name})")
                    }
                }
                YLog.info("$TAG 成功Hook模拟IMEI: ${method.declaringClass.name} -> ${method.name}")
            }
        } else {
            YLog.error("$TAG find DeviceImei Failed")
        }
    }

    fun disguisedLocale() {
        // 模拟地区
        findLocale(dexKitBridge)?.let { method ->
            method.hook {
                before {
                    if (system_update_mock_local_sw) {
                        val byteArray = args[1] as? ByteArray
                        if (byteArray != null) {
                            system_update_mock_local?.let {
                                ByteBuffer.wrap(byteArray).put(it.toByteArray())
                            }
                        }
                        result = system_update_mock_local?.length
                        YLog.debug("$TAG 模拟地区 -> $system_update_mock_local")
                    }
                }

            }
            YLog.info("$TAG 成功Hook地区: ${method.declaringClass.name} -> ${method.name}")
        } ?: YLog.error("$TAG 未找到地区方法")
    }

    fun disguisedSign() {
        // 模拟签名+版本
        findSignandVername(dexKitBridge)?.let { method ->
            method.hook {
                before {
                    if (system_update_mock_sign_sw) {
                        result = system_update_mock_sign
                        YLog.debug("$TAG 模拟签名+版本 -> $result")
                    }
                }

            }
            YLog.info("$TAG 成功Hook签名+版本: ${method.declaringClass.name} -> ${method.name}")
        } ?: YLog.error("$TAG 未找到签名+版本方法")
    }

    fun disguisedSignInfo() {
        findSignandVername(dexKitBridge)?.let { method ->
            method.hook {
                after {
                    YLog.debug("$TAG 签名+版本信息: $result")
                }
            }
            YLog.info("$TAG 成功添加签名+版本打印hook")
        } ?: YLog.error("$TAG 未找到签名+版本方法，无法添加打印hook")
    }
    fun hookFingerprintPrintInfo() {
        findBuildFP(dexKitBridge)?.let { method ->
            method.hook {
                after {
                    val byteArray = args[1] as? ByteArray

                    if (byteArray != null) {
                        val fingerprint = String(byteArray, Charsets.UTF_8).trimEnd('\u0000')
                        YLog.debug("$TAG 原始的指纹信息数据是: $fingerprint")
                    }
                }
            }
            YLog.info("$TAG 添加指纹信息打印hook成功: ${method.declaringClass.name} -> ${method.name}")
        } ?: YLog.error("$TAG 未找到指纹方法，无法添加打印hook")
    }
    fun disguisedFingerprint() {
        // 模拟指纹
        findBuildFP(dexKitBridge)?.let { method ->
            method.hook {
                before {
                    if (system_update_mock_fingerprint_sw) {
                        val byteArray = args[1] as? ByteArray
                        if (byteArray != null) {
                            system_update_mock_fingerprint?.let {
                                ByteBuffer.wrap(byteArray).put(it.toByteArray())
                            }
                        }
                        result = system_update_mock_fingerprint?.length
                        YLog.debug("$TAG 模拟指纹 -> $system_update_mock_fingerprint")
                    }
                }
            }
            YLog.info("$TAG 成功Hook指纹: ${method.declaringClass.name} -> ${method.name}")
        } ?: YLog.error("$TAG 未找到指纹方法")
    }



    fun disguisedSystemBuildVer() {
        // 模拟系统构建版本
        findSystemBuildVer(dexKitBridge)?.let { method ->
            method.hook {
                before {
                    if (system_update_mock_build_display_sw) {
                        result = system_update_mock_build_display
                        YLog.debug("$TAG 模拟系统构建版本 -> $result")
                    }
                }
            }
            YLog.info("$TAG 成功Hook系统构建版本: ${method.declaringClass.name} -> ${method.name}")
        } ?: YLog.error("$TAG 未找到系统构建版本方法")
    }

    fun disguisedSystemBuildVerInfo() {
        // 模拟系统构建版本
        findSystemBuildVer(dexKitBridge)?.let { method ->
            method.hook {
                after {
                        YLog.debug("$TAG 模拟系统构建版本 -> $result")
                }
            }
        } ?: YLog.error("$TAG 未找到系统构建版本方法")
    }

    fun disguisedInternalVersion() {
        // 模拟内部版本
        findInnerVer(dexKitBridge)?.let { method ->
            method.hook {
                before {
                    if (system_update_mock_system_inner_version_sw) {
                        result = system_update_mock_system_inner_version
                        YLog.debug("$TAG 模拟内部版本 -> $result")
                    }
                }
            }
            YLog.info("$TAG 成功Hook内部版本: ${method.declaringClass.name} -> ${method.name}")
        } ?: YLog.error("$TAG 未找到内部版本方法")

    }

    fun disguisedInternalVersionInfo() {
        // 模拟内部版本
        findInnerVer(dexKitBridge)?.let { method ->
            method.hook {
                after {
                        YLog.debug("$TAG 原系统的内部版本 -> $result")
                }
            }
            YLog.info("$TAG 成功Hook内部版本: ${method.declaringClass.name} -> ${method.name}")
        } ?: YLog.error("$TAG 未找到内部版本方法")

    }

    fun disguisedVariantID() {
        // 模拟变体ID
        findVariantId(dexKitBridge)?.let { method ->
            method.hook {
                before {
                    if (system_update_mock_variant_id_sw) {
                        result = system_update_mock_variant_id
                        YLog.debug("$TAG 模拟变体ID -> $result")
                    }
                }
            }
            YLog.info("$TAG 成功Hook变体ID: ${method.declaringClass.name} -> ${method.name}")
        } ?: YLog.error("$TAG 未找到变体ID方法")
    }
//    fun disguisedVariantIDInfo() {
//        // 模拟变体ID
//        findVariantId(dexKitBridge)?.let { method ->
//            method.hook {
//                after {
//                        YLog.debug("$TAG 变体的值ID -> $result")
//                }
//            }
//            YLog.info("$TAG 成功Hook变体ID: ${method.declaringClass.name} -> ${method.name}")
//        } ?: YLog.error("$TAG 未找到变体ID方法")
//    }

    fun disguisedManufacture() {
        // 模拟制造商
        findManu(dexKitBridge)?.let { method ->
            method.hook {
                before {
                    if (system_update_mock_manufacture_sw) {
                        val byteArray = args[1] as? ByteArray
                        if (byteArray != null) {
                            system_update_mock_manufacture?.let {
                                ByteBuffer.wrap(byteArray).put(it.toByteArray())
                            }
                        }
                        result = system_update_mock_manufacture?.length
                        YLog.debug("$TAG 模拟制造商 -> $system_update_mock_manufacture")
                    }
                }
            }
            YLog.info("$TAG 成功Hook制造商: ${method.declaringClass.name} -> ${method.name}")
        } ?: YLog.error("$TAG 未找到制造商方法")
    }




    fun findVariantId(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            searchPackages("com.zte.zdm.application.util")
            matcher {
                usingStrings(
                    listOf("ZTE_FEATURE_TRANSFORM_LEGACY", "getCurrentCustVariantId"),
                    StringMatchType.Equals
                )
            }
        }.findMethod {
            matcher {
                usingStrings(
                    listOf("com.zte.feature.Feature", "ZTE_FEATURE_TRANSFORM"),
                    StringMatchType.Equals
                )
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }

    fun findManu(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            matcher {
                className = "com.zte.zdm.mo.DevInfo"
            }
        }.findMethod {
            matcher {
                usingStrings(listOf("NUBIA", "ZTE"), StringMatchType.Equals)
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }

    fun findDeviceModel(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            matcher {
                className = "com.zte.zdm.mo.DevInfo"
            }
        }.findMethod {
            matcher {
                addUsingField {
                    name = "MODEL"
                }
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }

    fun findImei(dexKitBridge: DexKitBridge): List<Method> {
        val m = dexKitBridge.findClass {
            searchPackages("com.zte.zdm.application.util")
            matcher {
                usingStrings(listOf("cannotgetimei", "getDeviceId"), StringMatchType.Equals)
            }
        }.findMethod {
            matcher {
                returnType = "java.lang.String"
            }
        }
        val methodsList = mutableListOf<Method>()
        for (mData in m) {
            methodsList.add(mData.getMethodInstance(appClassLoader!!))
        }
        return methodsList
    }

    fun findLocale(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            matcher {
                className = "com.zte.zdm.mo.DevInfo"
            }
        }.findMethod {
            matcher {
                invokeMethods {
                    add {
                        descriptor = "Ljava/util/Locale;->getDefault()Ljava/util/Locale;"
                    }
                }
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }

    fun findSignandVername(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            matcher {
                className = "com.zte.zdm.mo.DevInfoEX"
            }
        }.findMethod {
            matcher {
                invokeMethods {
                    add {
                        descriptor =
                            "Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;"
                    }
                }
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }

    fun findBuildFP(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            matcher {
                className = "com.zte.zdm.mo.DevInfoEX"
            }
        }.findMethod {
            matcher {
                addUsingString("ro.build.fingerprint", StringMatchType.Equals)
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }

    fun findSystemBuildVer(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            searchPackages("com.zte.zdm.application.util")
            matcher {
                usingStrings(
                    listOf("ro.build.inner.version", "ro.build.sw_internal_version"),
                    StringMatchType.Equals
                )
            }
        }.findMethod {
            matcher {
                addUsingString("apps.setting.product.release", StringMatchType.Equals)
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }

    fun findInnerVer(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            searchPackages("com.zte.zdm.application.util")
            matcher {
                usingStrings(
                    listOf("ro.build.inner.version", "ro.build.sw_internal_version"),
                    StringMatchType.Equals
                )
            }
        }.findMethod {
            matcher {
                usingStrings(
                    listOf("ro.build.inner.version", "ro.build.sw_internal_version"),
                    StringMatchType.Equals
                )
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }
}