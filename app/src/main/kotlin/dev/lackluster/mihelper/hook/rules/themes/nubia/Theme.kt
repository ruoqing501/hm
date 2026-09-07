//package dev.lackluster.mihelper.hook.rules.themes.nubia
//
//import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
//import com.highcapable.yukihookapi.hook.factory.method
//import com.highcapable.yukihookapi.hook.log.YLog
//import com.highcapable.yukihookapi.hook.type.java.StringClass
//import dev.lackluster.mihelper.data.Pref
//import dev.lackluster.mihelper.utils.factory.hasEnable
//
//object ForceCustomTheme : YukiBaseHooker() {
//    private const val TAG = "ForceCustomTheme"
//
//    // 定制版属性值（根据提供的资料）
//    private const val CUSTOM_VARIANT_ID = "IP_MC_W_CN"               // persist.vendor.custom.variant.id 和 ro.oem.key1
//    private const val CUSTOM_DEF_THEME = "default_theme_64"        // ro.vendor.build.def_theme_name
//    private const val CUSTOM_LAUNCHER_RES = "gen_redmagic_cn_zte_nx809j_ip_mc_w_cn" // ro.vendor.feature.zte_feature_launcher_resource
//    private const val CUSTOM_THEME_RES = "nx809j_w_ip_mc_cn"        // ro.vendor.feature.zte_feature_theme_resource
//    private const val CUSTOM_MODEL = "NX809J_V4A"                    // ro.vendor.product.ztemodel
//
//    // 定制版铃声文件名（根据表格）
//    private const val RINGTONE = "Slashing_Bloom.ogg"               // ro.config.ringtone
//    private const val ALARM = "Slashing_Bloom_Alarm.ogg"            // ro.config.alarm_alert
//    private const val NOTIFICATION = "Resonance.ogg"                // ro.config.notification_sound
//    private const val MESSAGE = "Resonance.ogg"                     // ro.config.message
//    private const val MESSAGE_2 = "Convene.ogg"                     // ro.config.message_2
//    private const val RINGTONE_2 = "Meteors_Linger_in_the_Night_Sky.ogg" // ro.vendor.config.ringtone_2
//
//    override fun onHook() {
//        hasEnable(Pref.Key.Android.ANDROID_FORCE_CUSTOM_THEME) {
//            YLog.debug("$TAG 开始Hook，强制使用定制版主题")
//
//            // Hook 系统属性读取的核心方法（适用于所有进程，特别是 system_server）
//            "android.os.SystemProperties".toClass().method {
//                name = "get"
//                param(StringClass, StringClass)   // get(String key, String def)
//            }.hook {
//                before {
//                    // 安全获取参数，避免空指针
//                    val key = args.getOrNull(0) as? String ?: return@before
//                    val defVal = args.getOrNull(1) as? String ?: return@before
//                    val customValue = when (key) {
//                        // 产品标识与型号
//                        "persist.vendor.custom.variant.id" ->CUSTOM_VARIANT_ID
//                        "ro.oem.key1" -> CUSTOM_VARIANT_ID
//                        "ro.vendor.product.ztemodel" -> CUSTOM_MODEL
//                        "ro.vendor.build.def_theme_name" -> CUSTOM_DEF_THEME
//
//                        // 铃声与提示音
//                        "ro.config.ringtone" -> RINGTONE
//                        "ro.config.alarm_alert" -> ALARM
//                        "ro.config.notification_sound" -> NOTIFICATION
//                        "ro.config.message" -> MESSAGE
//                        "ro.config.message_2" -> MESSAGE_2
//                        "ro.vendor.config.ringtone_2" -> RINGTONE_2
//
//                        // 系统资源特征（launcher/theme 资源包）
//                        "ro.vendor.feature.zte_feature_launcher_resource" -> CUSTOM_LAUNCHER_RES
//                        "ro.vendor.feature.zte_feature_theme_resource" -> CUSTOM_THEME_RES
//
//                        else -> null
//                    }
//                    customValue?.let {
//                        YLog.debug("$TAG SystemProperties.get($key) -> $it (原默认值: $defVal)")
//                        result = it
//                    }
//                }
//            }
//
//            // 可选：Hook Utilities 中的包装方法（如果存在）
//            try {
//                "com.zte.beautify.view.common.tools.Utilities".toClass().method {
//                    name = "getStringSystemProperties"
//                    param(StringClass)
//                }.hook {
//                    before {
//                        val key = args.getOrNull(0) as? String ?: return@before
//                        val customValue = when (key) {
//                            "persist.vendor.custom.variant.id" ->CUSTOM_VARIANT_ID
//                            "ro.oem.key1" -> CUSTOM_VARIANT_ID
//                            "ro.vendor.build.def_theme_name" -> CUSTOM_DEF_THEME
//                            "ro.vendor.feature.zte_feature_launcher_resource" -> CUSTOM_LAUNCHER_RES
//                            "ro.vendor.feature.zte_feature_theme_resource" -> CUSTOM_THEME_RES
//                            "ro.config.ringtone" -> RINGTONE
//                            "ro.config.alarm_alert" -> ALARM
//                            "ro.config.notification_sound" -> NOTIFICATION
//                            "ro.config.message" -> MESSAGE
//                            "ro.config.message_2" -> MESSAGE_2
//                            "ro.vendor.config.ringtone_2" -> RINGTONE_2
//                            else -> null
//                        }
//                        customValue?.let {
//                            YLog.debug("$TAG Utilities.getStringSystemProperties($key) -> $it")
//                            result = it
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                YLog.error("$TAG Hook Utilities.getStringSystemProperties 失败: ${e.message}")
//            }
//
//            YLog.debug("$TAG Hook完成，系统将始终加载定制版主题")
//        }
//    }
//}