package dev.lackluster.redmagichelper.hook.rules.settings.nubia

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.android.ContentResolverClass
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object DisableUSBInstallVerification : YukiBaseHooker() {
    private const val ADB_INSTALL_SETTING_KEY = "adb_install_enabled"

    @SuppressLint("PrivateApi")
    override fun onHook() {
        YLog.debug("[DisableUSBInstallVerification] 开始Hook USB安装验证跳过功能")

        try {
            // 1. 找到 EnableAdbInstallPreferenceController 类
            val enableAdbInstallPrefControllerClass =
                "com.zte.settings.development.EnableAdbInstallPreferenceController".toClassOrNull()
                    ?: run {
                        YLog.error("[DisableUSBInstallVerification] 无法加载 EnableAdbInstallPreferenceController 类")
                        return
                    }

            YLog.debug("[DisableUSBInstallVerification] 成功加载 EnableAdbInstallPreferenceController 类")

            // 2. Hook handlePreferenceTreeClick 方法
            enableAdbInstallPrefControllerClass.method {
                name = "handlePreferenceTreeClick"
                paramCount = 1
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.NubiaSystemSettings.DISABLE_USB_INSTALLATION_AND_SWITCH_ACCOUNT_VERIFICATION, false)) return@before
                    val preference = this.args(0).any() ?: return@before
                    val controllerInstance = this.instance

                    // 获取控制器和点击的Preference的key
                    val controllerKey = try {
                        controllerInstance.current().method {
                            name = "getPreferenceKey"
                            paramCount = 0
                        }.string()
                    } catch (e: Exception) {
                        YLog.error("[DisableUSBInstallVerification] 获取控制器key失败: ${e.message}")
                        ""
                    }

                    // 方法1：尝试直接获取 mKey 字段（最稳定）
                    val clickedKey = try {
                        // 首先尝试从 Preference 类获取 mKey 字段
                        val preferenceClass = "androidx.preference.Preference".toClassOrNull()
                        if (preferenceClass != null) {
                            val mKeyField = preferenceClass.field {
                                name = "mKey"
                            }
                            mKeyField.get(preference).string()
                        } else {
                            // 备用方案：使用 current() 并指定父类查找
                            preference.current().field {
                                name = "mKey"
                                superClass()
                            }.string()
                        }
                    } catch (e: Exception) {
                        // 方法2：如果字段获取失败，尝试调用 getKey 方法
                        try {
                            preference.current().method {
                                name = "getKey"
                                paramCount = 0
                                superClass() // 从父类查找方法
                            }.string()
                        } catch (e2: Exception) {
                            // 方法3：使用反射获取 key
                            try {
                                val getKeyMethod = preference::class.java.getMethod("getKey")
                                getKeyMethod.invoke(preference) as? String ?: ""
                            } catch (e3: Exception) {
                                YLog.error("[DisableUSBInstallVerification] 所有方法都无法获取点击key: ${e3.message}")
                                ""
                            }
                        }
                    }

                    YLog.debug("[DisableUSBInstallVerification] 控制器key: '$controllerKey', 点击key: '$clickedKey'")

                    if (TextUtils.isEmpty(clickedKey) || !TextUtils.equals(clickedKey, controllerKey)) {
                        // 点击的不是USB安装开关
                        YLog.debug("[DisableUSBInstallVerification] 点击的不是USB安装开关，跳过处理")
                        return@before
                    }

                    // 获取 ContentResolver
                    val abstractPrefControllerClass =
                        "com.android.settingslib.core.AbstractPreferenceController".toClassOrNull()
                            ?: run {
                                YLog.error("[DisableUSBInstallVerification] 无法加载 AbstractPreferenceController 类")
                                return@before
                            }

                    val context = controllerInstance.current().field {
                        name = "mContext"
                        superClass()
                    }.cast<Context>() ?: return@before

                    val contentResolver = context.contentResolver

                    // 获取 SwitchPreference 实例
                    val switchPreferenceClass = "androidx.preference.SwitchPreference".toClassOrNull()
                        ?: run {
                            YLog.error("[DisableUSBInstallVerification] 无法加载 SwitchPreference 类")
                            return@before
                        }

                    val mEnableAdbInstallField = enableAdbInstallPrefControllerClass.field {
                        name = "mEnableAdbInstall"
                    }

                    val enableAdbInstallPref = mEnableAdbInstallField.get(controllerInstance).any()

                    if (enableAdbInstallPref == null) {
                        YLog.error("[DisableUSBInstallVerification] mEnableAdbInstall 字段为空")
                        return@before
                    }

                    // 检查当前开关状态
                    val isChecked = try {
                        // 方法1：使用 TwoStatePreference 的 isChecked 方法
                        val twoStatePrefClass = "androidx.preference.TwoStatePreference".toClassOrNull()
                        if (twoStatePrefClass != null) {
                            val isCheckedMethod = twoStatePrefClass.method {
                                name = "isChecked"
                                paramCount = 0
                            }
                            isCheckedMethod.get(enableAdbInstallPref).boolean()
                        } else {
                            // 方法2：使用 SwitchPreference 的 isChecked 方法
                            enableAdbInstallPref.current().method {
                                name = "isChecked"
                                paramCount = 0
                            }.boolean()
                        }
                    } catch (e: Exception) {
                        // 方法3：直接获取 mChecked 字段
                        try {
                            val twoStatePrefClass = "androidx.preference.TwoStatePreference".toClassOrNull()
                            if (twoStatePrefClass != null) {
                                val mCheckedField = twoStatePrefClass.field {
                                    name = "mChecked"
                                }
                                mCheckedField.get(enableAdbInstallPref).boolean()
                            } else {
                                false
                            }
                        } catch (e2: Exception) {
                            YLog.error("[DisableUSBInstallVerification] 无法获取开关状态: ${e2.message}")
                            false
                        }
                    }

                    YLog.debug("[DisableUSBInstallVerification] 当前开关状态: $isChecked")

                    if (isChecked) {
                        // 打开ADB安装 - 直接设置系统设置
                        setAdbInstallSetting(contentResolver, 1)

                        // 跳过验证流程，直接返回true表示处理成功
                        this.result = true

                        YLog.debug("[DisableUSBInstallVerification] 已跳过验证，直接设置ADB安装为开启")
                    } else {
                        // 关闭ADB安装
                        setAdbInstallSetting(contentResolver, 0)

                        // 保持开关为关闭状态
                        try {
                            val setCheckedMethod = switchPreferenceClass.method {
                                name = "setChecked"
                                paramCount = 1
                                param(BooleanType)
                            }
                            setCheckedMethod.get(enableAdbInstallPref).call(false)
                        } catch (e: Exception) {
                            YLog.error("[DisableUSBInstallVerification] 设置开关状态失败: ${e.message}")
                        }

                        this.result = true
                        YLog.debug("[DisableUSBInstallVerification] 已设置ADB安装为关闭")
                    }

                    // 阻止原方法执行，因为我们已经处理了点击事件
                    this.result = true
                }
            }

            // 3. Hook onActivityResult 方法，阻止验证流程
            enableAdbInstallPrefControllerClass.method {
                name = "onActivityResult"
                paramCount = 3
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.NubiaSystemSettings.DISABLE_USB_INSTALLATION_AND_SWITCH_ACCOUNT_VERIFICATION, false)) return@before
                    YLog.debug("[DisableUSBInstallVerification] 拦截 onActivityResult 调用")
                    // 直接返回true，表示已经处理了结果，阻止验证流程
                    this.result = true
                }
            }

            // 4. Hook launchAuthCompActivity 和 launchNubiaActivity 方法
            enableAdbInstallPrefControllerClass.method {
                name = "launchAuthCompActivity"
                paramCount = 0
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.NubiaSystemSettings.DISABLE_USB_INSTALLATION_AND_SWITCH_ACCOUNT_VERIFICATION, false)) return@before
                    YLog.debug("[DisableUSBInstallVerification] 拦截 launchAuthCompActivity 调用")
                    // 直接返回，阻止启动验证Activity
                    this.result = null
                }
            }

            enableAdbInstallPrefControllerClass.method {
                name = "launchNubiaActivity"
                paramCount = 0
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.NubiaSystemSettings.DISABLE_USB_INSTALLATION_AND_SWITCH_ACCOUNT_VERIFICATION, false)) return@before
                    YLog.debug("[DisableUSBInstallVerification] 拦截 launchNubiaActivity 调用")
                    // 直接返回，阻止启动验证Activity
                    this.result = null
                }
            }

            YLog.debug("[DisableUSBInstallVerification] Hook设置完成")

        } catch (e: Exception) {
            YLog.error("[DisableUSBInstallVerification] Hook失败: ${e.message}", e)
        }
    }

    /**
     * 设置ADB安装系统设置
     */
    private fun setAdbInstallSetting(contentResolver: ContentResolver, value: Int) {
        try {
            // 使用反射调用 Settings.System.putInt
            val settingsSystemClass = "android.provider.Settings\$System".toClassOrNull()
                ?: run {
                    YLog.error("[DisableUSBInstallVerification] 无法加载 Settings.System 类")
                    return
                }

            settingsSystemClass.method {
                name = "putInt"
                paramCount = 3
                param(ContentResolverClass, StringType, IntType)
                modifiers { isStatic }
            }.get().call(contentResolver, ADB_INSTALL_SETTING_KEY, value)

            YLog.debug("[DisableUSBInstallVerification] 设置系统设置: $ADB_INSTALL_SETTING_KEY = $value")

        } catch (e: Exception) {
            YLog.error("[DisableUSBInstallVerification] 设置系统设置失败: ${e.message}")

            // 备用方案：使用常规的 Settings.System.putInt
            try {
                Settings.System.putInt(contentResolver, ADB_INSTALL_SETTING_KEY, value)
                YLog.debug("[DisableUSBInstallVerification] 使用备用方案设置系统设置成功")
            } catch (e2: Exception) {
                YLog.error("[DisableUSBInstallVerification] 备用方案也失败: ${e2.message}")
            }
        }
    }
}
