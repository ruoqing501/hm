




package dev.lackluster.redmagichelper.hook.rules.packageinstaller.nubia

import android.annotation.SuppressLint
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.*
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object HidePurifySwitch : YukiBaseHooker() {
    private val isHidePurifySwitch by lazy {
        Prefs.getBoolean(Pref.Key.NubiaPackageInstaller.HIDE_EVOLUTION_MODE_TOGGLE, false)
    }

    @SuppressLint("PrivateApi")
    override fun onHook() {
        if (!isHidePurifySwitch) {
            YLog.debug("[HidePurifySwitch] 净化模式功能未开启")
            return
        }

        try {
            // 尝试获取UICookTool类
            val uICookToolClass = runCatching {
                "com.android.packageinstaller.PackageInstallerActivity\$UICookTool".toClassOrNull()
            }.getOrNull()

            if (uICookToolClass == null) {
                YLog.error("[HidePurifySwitch] 无法加载UICookTool类")
                return
            }

            // Hook getCookUI方法 - 修正为6个参数
            "com.android.packageinstaller.PackageInstallerActivity".toClassOrNull()?.apply {
                method {
                    name = "getCookUI"
                    paramCount = 6
                    param(IntType, IntType, IntType, BooleanType, StringType, StringType)
                }.hook {
                    after {
                        YLog.debug("[HidePurifySwitch] getCookUI方法被调用，参数数量: ${this.args.size}")

                        val result = this.result ?: return@after

                        if (!uICookToolClass.isInstance(result)) {
                            YLog.error("[HidePurifySwitch] 返回的对象不是UICookTool类型")
                            return@after
                        }

                        val uICookToolOBJ = result

                        // 设置布尔字段
                        listOf(
                            "cleanBgColor",
                            "hidePureModeSwitchLayout",
                            "hideWarningLayout"
                        ).forEach { fieldName ->
                            runCatching {
                                uICookToolClass.field {
                                    name = fieldName
                                    type = BooleanType
                                }.get(uICookToolOBJ).setTrue()
                                YLog.debug("[HidePurifySwitch] 设置字段 $fieldName = true 成功")
                            }.onFailure { e ->
                                YLog.error("[HidePurifySwitch] 设置字段 $fieldName 失败: ${e.message}")
                            }
                        }

                        listOf(
                            "needIsolate",
                            "showRiskCheckbox",
                            "showSwlimitLearnMore"
                        ).forEach { fieldName ->
                            runCatching {
                                uICookToolClass.field {
                                    name = fieldName
                                    type = BooleanType
                                }.get(uICookToolOBJ).setFalse()
                                YLog.debug("[HidePurifySwitch] 设置字段 $fieldName = false 成功")
                            }.onFailure { e ->
                                YLog.error("[HidePurifySwitch] 设置字段 $fieldName 失败: ${e.message}")
                            }
                        }

                        // 设置字符串字段
                        mapOf(
                            "warningTitle" to "彩蛋",
                            "warningUnknownText" to "这是一条提醒"
                        ).forEach { (fieldName, value) ->
                            runCatching {
                                uICookToolClass.field {
                                    name = fieldName
                                    type = StringType
                                }.get(uICookToolOBJ).set(value)
                                YLog.debug("[HidePurifySwitch] 设置字段 $fieldName = $value 成功")
                            }.onFailure { e ->
                                YLog.error("[HidePurifySwitch] 设置字段 $fieldName 失败: ${e.message}")
                            }
                        }

                        this.result = uICookToolOBJ
                        YLog.debug("[HidePurifySwitch] 净化模式开关已隐藏")
                    }
                }
            } ?: YLog.error("[HidePurifySwitch] 无法加载PackageInstallerActivity类")

        } catch (e: Exception) {
            YLog.error("[HidePurifySwitch] Hook失败: ${e.message}", e)
        }
    }
}
