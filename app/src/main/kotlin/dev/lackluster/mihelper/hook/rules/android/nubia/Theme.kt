package dev.lackluster.mihelper.hook.rules.android.nubia

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.StringClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object ForceCustomTheme : YukiBaseHooker() {
    private const val TAG = "ForceCustomTheme"

    // 请根据您的定制版实际属性值修改以下常量
    private const val CUSTOM_DEF_THEME = "default_theme_64"
    private const val CUSTOM_OEM_KEY = "IP_MC_W_CN"
    private const val CUSTOM_MODEL = "NX809J_V4A"
    private const val CUSTOM_VARIANT_ID = "IP_MC_W_CN"   // persist.vendor.custom.variant.id 的值

    override fun onHook() {
        hasEnable(Pref.Key.Android.ANDROID_FORCE_CUSTOM_THEME) {
            YLog.debug("$TAG 开始Hook，强制使用定制版主题")

            // 1. Hook SystemProperties.get 方法（防御性编程，处理空 key）
            hookSystemProperties()

            // 2. Hook Utilities 类中的属性获取方法（确保类加载后也能拦截）
            hookUtilities()

            // 3. 可选：Hook 其他可能直接调用 SystemProperties 的关键类
            hookResourceUtils()

            YLog.debug("$TAG Hook完成，系统将始终加载定制版主题")
        }
    }

    /**
     * Hook android.os.SystemProperties 的 get 方法
     */
    private fun hookSystemProperties() {
        val systemPropertiesClass = "android.os.SystemProperties".toClassOrNull()
            ?: return YLog.error("$TAG SystemProperties类未找到")

        // Hook get(String key)
        systemPropertiesClass.method {
            name = "get"
            param(StringClass)
        }.hook {
            before {
                val key = args[0] as? String
                if (key != null) {
                    when (key) {
                        "ro.vendor.build.def_theme_name" -> result = CUSTOM_DEF_THEME
                        "ro.oem.key1" -> result = CUSTOM_OEM_KEY
                        "ro.vendor.product.ztemodel" -> result = CUSTOM_MODEL
                        "persist.vendor.custom.variant.id" -> result = CUSTOM_VARIANT_ID
                    }
                }
            }
        }

        // Hook get(String key, String def)
        systemPropertiesClass.method {
            name = "get"
            param(StringClass, StringClass)
        }.hook {
            before {
                val key = args[0] as? String
                if (key != null) {
                    when (key) {
                        "ro.vendor.build.def_theme_name" -> result = CUSTOM_DEF_THEME
                        "ro.oem.key1" -> result = CUSTOM_OEM_KEY
                        "ro.vendor.product.ztemodel" -> result = CUSTOM_MODEL
                        "persist.vendor.custom.variant.id" -> result = CUSTOM_VARIANT_ID
                    }
                }
            }
        }
    }

    /**
     * Hook Utilities 工具类中的属性读取方法
     */
    private fun hookUtilities() {
        val utilitiesClass = "com.zte.beautify.view.common.tools.Utilities".toClassOrNull()
            ?: return YLog.error("$TAG Utilities类未找到")

        // Hook getStringSystemProperties(String key)
        utilitiesClass.method {
            name = "getStringSystemProperties"
            param(StringClass)
        }.hook {
            after {
                val key = args[0] as? String ?: return@after
                when (key) {
                    "ro.vendor.build.def_theme_name" -> result = CUSTOM_DEF_THEME
                    "ro.oem.key1" -> result = CUSTOM_OEM_KEY
                    "ro.vendor.product.ztemodel" -> result = CUSTOM_MODEL
                    "persist.vendor.custom.variant.id" -> result = CUSTOM_VARIANT_ID
                }
            }
        }

        // Hook getStringSystemPropByZteFeature(String key, String def)
        utilitiesClass.method {
            name = "getStringSystemPropByZteFeature"
            param(StringClass, StringClass)
        }.hook {
            after {
                val key = args[0] as? String ?: return@after
                when (key) {
                    "ro.vendor.build.def_theme_name" -> result = CUSTOM_DEF_THEME
                    "ro.oem.key1" -> result = CUSTOM_OEM_KEY
                    "ro.vendor.product.ztemodel" -> result = CUSTOM_MODEL
                    "persist.vendor.custom.variant.id" -> result = CUSTOM_VARIANT_ID
                }
            }
        }

        // 如果还有其他类似方法，可以继续添加
    }

    /**
     * Hook ResourceUtils 中的相关方法（可选）
     */
    private fun hookResourceUtils() {
        // 例如 ResourceUtils.getVaiantFlag 间接调用了 Utilities.getStringSystemProperties，
        // 已通过 hookUtilities 覆盖，无需重复。
        // 但如果有直接调用 SystemProperties 的地方，可在此补充。
    }
}