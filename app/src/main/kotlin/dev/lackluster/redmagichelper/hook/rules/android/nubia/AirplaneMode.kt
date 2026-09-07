package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.annotation.SuppressLint
import android.net.MacAddress
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

object AirplaneMode : YukiBaseHooker() {
    private const val TAG = "AirplaneMode-ManagerService"

    // jar包对应的那个类。通过42、43行的注释的日志获取匹配定位
    private const val WIFI_SERVICE_CLASS_TARGET = "com.android.server.wifi.WifiService"
    private const val BLUETOOTH_SERVICE_CLASS_TARGET =
        "com.android.server.bluetooth.BluetoothService"

    // jar包内的类。通过反编译获取
    private const val WIFI_SETTINGS_STORE = "com.android.server.wifi.WifiSettingsStore"
    private const val BLUETOOTH_SERVICE_CLASS =
        "com.android.server.bluetooth.BluetoothManagerService"
    private const val WIFI_SETTINGS_TELECOM = "com.android.server.wifi.WifiCountryCode"
    private const val PIN_STA_MAC = "com.android.server.wifi.WifiNative"

    private val countryCode by lazy {
        Prefs.getString(Pref.Key.Android.TELECOM_WLAN_CC_DIALOG, "us")
    }
    private val isPinStaMac by lazy {
        Prefs.getBoolean(Pref.Key.Android.PIN_STA_MAC, false)
    }
    private val isPinStaAP by lazy {
        Prefs.getBoolean(Pref.Key.Android.PIN_AP_BSSID, false)
    }

    private val pin_sta_mac_dialog_content by lazy {
        Prefs.getString(Pref.Key.Android.PIN_STA_MAC_DIALOG, "66:31:32:35:39:75")
    }

    private val pin_ap_bssid_dialog_content by lazy {
        Prefs.getString(Pref.Key.Android.PIN_AP_BSSID_DIALOG, "b4:f3:cb:a7:b8:e7")
    }

    /**
     * 钩住 SystemServiceManager.loadClassFromLoader，支持多目标类、每个类多个处理器
     * @param targetHandlers 目标类名与该类所有处理器的映射
     */
    private fun hookMultipleSystemServices(targetHandlers: Map<String, List<(ClassLoader) -> Unit>>) {
        val systemServerManager = "com.android.server.SystemServiceManager".toClassOrNull()
            ?: return YLog.error("$TAG SystemServiceManager not found")

        // 使用可变集合跟踪尚未处理的目标类
        val pendingTargets = targetHandlers.keys.toMutableSet()

        runCatching {
            systemServerManager.method {
                name = "loadClassFromLoader"
                param(StringClass, ClassLoader::class.java)
            }.hook {
                before {
                    val className = args[0] as String
                    val classLoader = args[1] as ClassLoader

                    YLog.debug("$TAG 🔍 搜索目标类：$className")
                    YLog.debug("$TAG 🔍 Jar包路径：${classLoader}")

                    // 检查是否是目标类
                    val handlers = targetHandlers[className]
                    if (!handlers.isNullOrEmpty()) {
                        YLog.debug("$TAG 🔍 命中目标类：$className，开始执行 ${handlers.size} 个处理器")
                        handlers.forEach { handler ->
                            runCatching {
                                handler(classLoader)
                            }.onFailure { e ->
                                YLog.error("$TAG ❌ 执行处理器失败: ${e.message}", e)
                            }
                        }

                        // 移除已处理的目标
                        pendingTargets.remove(className)

                        // 如果所有目标都已处理，解除 Hook
                        if (pendingTargets.isEmpty()) {
                            YLog.debug("$TAG ✅ 所有目标类已处理，准备解除 Hook")
                        }
                    }
                }
            }
        }.onSuccess {
            YLog.info("$TAG ✅ 成功 Hook SystemServiceManager.loadClassFromLoader")
        }.onFailure { e ->
            YLog.error("$TAG ❌ Hook SystemServiceManager.loadClassFromLoader 失败", e)
        }
    }

    override fun onHook() {
        // 使用 Map 的 Value 为 List，支持同一个类注册多个处理器
        val targetHandlers = mutableMapOf<String, MutableList<(ClassLoader) -> Unit>>()

        fun addHandler(className: String, handler: (ClassLoader) -> Unit) {
            targetHandlers.getOrPut(className) { mutableListOf() }.add(handler)
        }

        // Wi-Fi 相关所有处理器（均针对 WIFI_SERVICE_CLASS_TARGET）
        addHandler(WIFI_SERVICE_CLASS_TARGET) { classLoader -> wifiAirplaneHooker(classLoader) }
        addHandler(WIFI_SERVICE_CLASS_TARGET) { classLoader -> wlanTelecomCCHooker(classLoader) }
        addHandler(WIFI_SERVICE_CLASS_TARGET) { classLoader -> wlanMacHooker(classLoader) }
        addHandler(WIFI_SERVICE_CLASS_TARGET) { classLoader -> wlanAPHooker(classLoader) }

        // 蓝牙相关处理器
        addHandler(BLUETOOTH_SERVICE_CLASS_TARGET) { classLoader -> installKeepBluetoothHook(classLoader) }

        // 单次 Hook 处理所有目标类
        hookMultipleSystemServices(targetHandlers)
    }

    /*
     * 阻止蓝牙服务在飞行模式下关闭
     * param classLoader: 类加载器
     */
    @SuppressLint("PrivateApi")
    private fun installKeepBluetoothHook(classLoader: ClassLoader) {
        hasEnable(Pref.Key.Android.ANDROID_AIRPLANE_MODE_KEEP_BLUETOOTH) {
            YLog.debug("$TAG 🔍 蓝牙服务类功能开关已开启")
            val serviceBluetooth =
                classLoader.loadClass(BLUETOOTH_SERVICE_CLASS) ?: return@hasEnable
            YLog.debug("$TAG 🔍 找到蓝牙服务类：$serviceBluetooth")

            serviceBluetooth.method {
                name = "onAirplaneModeChanged"
            }.hook {
                before {
                    val isAirplaneOn = args[0] as Boolean
                    YLog.debug("$TAG ✈️ 飞行模式状态：$isAirplaneOn")
                    if (isAirplaneOn) {
                        YLog.debug("$TAG ✈️ 飞行模式开启，阻止蓝牙关闭")
                        result = null   // 完全跳过原方法
                    }
                }
            }
        }
    }

    /*
     * 阻止Wi-Fi/热点服务在飞行模式下关闭
     * param classLoader: 类加载器
     */
    @SuppressLint("PrivateApi")
    private fun wifiAirplaneHooker(classLoader: ClassLoader) {
        hasEnable(Pref.Key.Android.ANDROID_AIRPLANE_MODE_KEEP_WLAN) {
            YLog.debug("$TAG 🔍 Wi-Fi服务类功能开关已开启")
            val wifiSettingsStore = classLoader.loadClass(WIFI_SETTINGS_STORE) ?: return@hasEnable
            YLog.debug("$TAG 🔍 找到Wi-Fi设置存储类：$wifiSettingsStore")

            wifiSettingsStore.method {
                name = "isAirplaneSensitive"
            }.hook {
                before {
                    result = false
                    YLog.debug("$TAG ✈️ isAirplaneSensitive 被调用，阻止Wi-Fi/热点关闭")
                }
            }
            YLog.debug("$TAG ✅ hook isAirplaneSensitive success")
        }
    }

    /*
     * 拦截WIFI国家码设置
     * param classLoader: 类加载器
     */
    @SuppressLint("PrivateApi")
    private fun wlanTelecomCCHooker(classLoader: ClassLoader) {
        hasEnable(Pref.Key.Android.TELECOM_WLAN_CC) {
            YLog.debug("$TAG 🔍 设置WIFI国家码开关已开启")
            val wifiSettingsTelecom =
                classLoader.loadClass(WIFI_SETTINGS_TELECOM) ?: return@hasEnable
            YLog.debug("$TAG 🔍 找到WIFI国家码类：$wifiSettingsTelecom")

            wifiSettingsTelecom.method {
                name = "setTelephonyCountryCode"
                param(StringClass)
            }.hook {
                before {
                    if (isValidCC(countryCode)) {
                        YLog.debug("$TAG 🌍 set country Code ${args[0] as String?} to $countryCode")
                        args[0] = countryCode
                        YLog.debug("$TAG 🌍 设置WIFI国家码为：$countryCode")
                    } else {
                        YLog.info("$TAG Invalid countryCode:$countryCode,please check it")
                    }
                }
            }
        }
    }

    /*
     * 拦截WIFI Native层MAC地址设置
     * param classLoader: 类加载器
     */
    @SuppressLint("PrivateApi")
    private fun wlanMacHooker(classLoader: ClassLoader) {
        YLog.debug("$TAG 🔍 设置固定终端MAC地址开关已开启")
        val pinStaMacClazz = classLoader.loadClass(PIN_STA_MAC) ?: run {
            YLog.debug("$TAG ❌ 固定终端MAC地址类：$PIN_STA_MAC 未找到")
            return
        }
        YLog.debug("$TAG 🔍 找到固定终端MAC地址类：$pinStaMacClazz")
        pinStaMacClazz.method {
            name = "setStaMacAddress"
            param(StringClass, MacAddress::class.java)
        }.hook {
            before {
                if (isPinStaMac) {
                    validMAC(pin_sta_mac_dialog_content)?.let { mac ->
                        args[1] = mac
                        YLog.debug("$TAG 🌍 override sta mac address to $mac")
                    }
                }
            }
        }
    }

    /*
     * 拦截WIFI热点MAC地址设置
     * param classLoader: 类加载器
     */
    @SuppressLint("PrivateApi")
    private fun wlanAPHooker(classLoader: ClassLoader) {
        YLog.debug("$TAG 🔍 设置固定热点MAC地址开关已开启")
        val pinStaMacClazz = classLoader.loadClass(PIN_STA_MAC) ?: run {
            YLog.debug("$TAG ❌ 固定热点MAC地址类：$PIN_STA_MAC 未找到")
            return
        }
        YLog.debug("$TAG 🔍 找到固定热点MAC地址类：$pinStaMacClazz")
        pinStaMacClazz.method {
            name = "setApMacAddress"
            param(StringClass, MacAddress::class.java)
        }.hook {
            before {
                if (isPinStaAP) {
                    YLog.debug("$TAG setApMacAddress:${args[0] as String},${args[1] as MacAddress}")
                    validMAC(pin_ap_bssid_dialog_content)?.let { mac ->
                        args[1] = mac
                        YLog.debug("$TAG 🌍 override AP mac address to $mac")
                    }
                }
            }
        }
    }

    private fun validMAC(macAddress: String?): MacAddress? {
        return try {
            MacAddress.fromString(macAddress as String)
        } catch (e: IllegalArgumentException) {
            YLog.error("macAddress: $macAddress is invalid：${e}")
            null
        }
    }

    private fun isValidCC(countryCode: String?): Boolean {
        if (countryCode.isNullOrEmpty() || countryCode.length != 2) {
            return false
        }
        return countryCode.all { it.isLetterOrDigit() }
    }
}