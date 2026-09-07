//package dev.lackluster.redmagichelper.hook.rules.packageinstaller.nubia
//
//import android.annotation.SuppressLint
//import android.view.View
//import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
//import dev.lackluster.redmagichelper.hook.compat.factory.current
//import dev.lackluster.redmagichelper.hook.compat.factory.field
//import dev.lackluster.redmagichelper.hook.compat.factory.method
//import dev.lackluster.redmagichelper.hook.compat.log.YLog
//import dev.lackluster.redmagichelper.data.Pref
//import dev.lackluster.redmagichelper.utils.Prefs
//import dev.lackluster.redmagichelper.utils.factory.hasEnable
//import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
//import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
//
//object HideStoreHint : YukiBaseHooker() {
//
//    // 隐藏从商店安装提示开关
//    private val isHideStorePrompt by lazy {
//        Prefs.getBoolean(Pref.Key.NubiaPackageInstaller.HIDE_STORE_INSTALL_PROMPT, false)
//    }
//
//    @SuppressLint("PrivateApi")
//    override fun onHook() {
//        hasEnable(Pref.Key.NubiaPackageInstaller.HIDE_STORE_INSTALL_PROMPT) {
//            YLog.debug("[HideStoreHint] 开始执行隐藏商店安装提示钩子")
//
//            if (appClassLoader == null) {
//                YLog.error("[HideStoreHint] appClassLoader 为空，无法继续")
//                return@hasEnable
//            }
//            YLog.debug("[HideStoreHint] appClassLoader 获取成功")
//
//            // 获取 PackageInstallerActivity 类
//            val packageInstallerActivityClass = "com.android.packageinstaller.PackageInstallerActivity".toClassOrNull(appClassLoader!!)
//                ?: run {
//                    YLog.error("[HideStoreHint] 无法加载 PackageInstallerActivity 类")
//                    return@hasEnable
//                }
//            YLog.debug("[HideStoreHint] 成功加载 PackageInstallerActivity 类")
//
//            // 获取字段
//            val mMarketContainerField by lazy {
//                YLog.debug("[HideStoreHint] 获取 mMarketContainer 字段")
//                packageInstallerActivityClass.field {
//                    name = "mMarketContainer"
//                }
//            }
//
//            val mWarningUnknownField by lazy {
//                YLog.debug("[HideStoreHint] 获取 mWarningUnknown 字段")
//                packageInstallerActivityClass.field {
//                    name = "mWarningUnknown"
//                }
//            }
//
//            val mDividerField by lazy {
//                YLog.debug("[HideStoreHint] 获取 mDivider 字段")
//                packageInstallerActivityClass.field {
//                    name = "mDivider"
//                }
//            }
//
//            // Hook bindUi 方法
//            YLog.debug("[HideStoreHint] 尝试 Hook bindUi 方法")
//            packageInstallerActivityClass.method {
//                name = "bindUi"
//                param(IntType, BooleanType)
//            }.ignored().hook {
//                before {
//                    YLog.debug("[HideStoreHint] bindUi 方法被调用，参数: installFlags=${args[0]}, bindPerm=${args[1]}")
//                }
//                after {
//                    YLog.debug("[HideStoreHint] bindUi 方法执行完成，开始隐藏商店相关视图")
//                    val instance = this.instance
//
//                    val marketContainerView = mMarketContainerField.get(instance).cast<View>()
//                    if (marketContainerView != null) {
//                        marketContainerView.visibility = View.GONE
//                        YLog.debug("[HideStoreHint] mMarketContainer 视图已隐藏")
//                    } else {
//                        YLog.warn("[HideStoreHint] mMarketContainer 视图为空")
//                    }
//
//                    val warningUnknownView = mWarningUnknownField.get(instance).cast<View>()
//                    if (warningUnknownView != null) {
//                        warningUnknownView.visibility = View.GONE
//                        YLog.debug("[HideStoreHint] mWarningUnknown 视图已隐藏")
//                    } else {
//                        YLog.warn("[HideStoreHint] mWarningUnknown 视图为空")
//                    }
//
//                    val dividerView = mDividerField.get(instance).cast<View>()
//                    if (dividerView != null) {
//                        dividerView.visibility = View.GONE
//                        YLog.debug("[HideStoreHint] mDivider 视图已隐藏")
//                    } else {
//                        YLog.warn("[HideStoreHint] mDivider 视图为空")
//                    }
//
//                    YLog.debug("[HideStoreHint] bindUi 方法处理完成")
//                }
//            }
//
//            // Hook bindUiPerm 方法
//            YLog.debug("[HideStoreHint] 尝试 Hook bindUiPerm 方法")
//            packageInstallerActivityClass.method {
//                name = "bindUiPerm"
//                param(IntType, BooleanType)
//            }.ignored().hook {
//                before {
//                    YLog.debug("[HideStoreHint] bindUiPerm 方法被调用，参数: installFlags=${args[0]}, bindPerm=${args[1]}")
//                }
//                after {
//                    YLog.debug("[HideStoreHint] bindUiPerm 方法执行完成，开始隐藏商店相关视图")
//                    val instance = this.instance
//
//                    val marketContainerView = mMarketContainerField.get(instance).cast<View>()
//                    if (marketContainerView != null) {
//                        marketContainerView.visibility = View.GONE
//                        YLog.debug("[HideStoreHint] mMarketContainer 视图已隐藏")
//                    } else {
//                        YLog.warn("[HideStoreHint] mMarketContainer 视图为空")
//                    }
//
//                    val warningUnknownView = mWarningUnknownField.get(instance).cast<View>()
//                    if (warningUnknownView != null) {
//                        warningUnknownView.visibility = View.GONE
//                        YLog.debug("[HideStoreHint] mWarningUnknown 视图已隐藏")
//                    } else {
//                        YLog.warn("[HideStoreHint] mWarningUnknown 视图为空")
//                    }
//
//                    val dividerView = mDividerField.get(instance).cast<View>()
//                    if (dividerView != null) {
//                        dividerView.visibility = View.GONE
//                        YLog.debug("[HideStoreHint] mDivider 视图已隐藏")
//                    } else {
//                        YLog.warn("[HideStoreHint] mDivider 视图为空")
//                    }
//
//                    YLog.debug("[HideStoreHint] bindUiPerm 方法处理完成")
//                }
//            }
//
//            YLog.debug("[HideStoreHint] 所有钩子设置完成")
//        }
//    }
//}




package dev.lackluster.redmagichelper.hook.rules.packageinstaller.nubia

import android.annotation.SuppressLint
import android.view.View
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType

object HideStoreHint : YukiBaseHooker() {

    // 隐藏从商店安装提示开关
    private val isHideStorePrompt by lazy {
        Prefs.getBoolean(Pref.Key.NubiaPackageInstaller.HIDE_STORE_INSTALL_PROMPT, false)
    }

    @SuppressLint("PrivateApi")
    override fun onHook() {
        hasEnable(Pref.Key.NubiaPackageInstaller.HIDE_STORE_INSTALL_PROMPT) {
            YLog.debug("[HideStoreHint] 开始执行隐藏商店安装提示钩子")

            if (appClassLoader == null) {
                YLog.error("[HideStoreHint] appClassLoader 为空，无法继续")
                return@hasEnable
            }
            YLog.debug("[HideStoreHint] appClassLoader 获取成功")

            // 获取 PackageInstallerActivity 类
            val packageInstallerActivityClass = "com.android.packageinstaller.PackageInstallerActivity".toClassOrNull(appClassLoader!!)
                ?: run {
                    YLog.error("[HideStoreHint] 无法加载 PackageInstallerActivity 类")
                    return@hasEnable
                }
            YLog.debug("[HideStoreHint] 成功加载 PackageInstallerActivity 类")

            // Hook 三个主要的UI绑定方法
            hookUiMethod(packageInstallerActivityClass, "bindUi")
            hookUiMethod(packageInstallerActivityClass, "bindUiPerm")
            hookUiMethod(packageInstallerActivityClass, "bindUiPermRed")

            YLog.debug("[HideStoreHint] 所有钩子设置完成")
        }
    }

    private fun hookUiMethod(clazz: Class<*>, methodName: String) {
        YLog.debug("[HideStoreHint] 尝试 Hook $methodName 方法")

        try {
            // 尝试使用不同的参数类型来查找方法
            val methods = listOf(
                { clazz.getDeclaredMethod(methodName, IntType, BooleanType) },
                { clazz.getDeclaredMethod(methodName, IntType, BooleanType) }
            )

            val method = methods.firstNotNullOfOrNull {
                try { it() } catch (e: Exception) { null }
            } ?: run {
                YLog.error("[HideStoreHint] 找不到方法: $methodName")
                return
            }

            method.isAccessible = true

            // Hook 方法
            clazz.method {
                name = methodName
                param(IntType, BooleanType)
            }.ignored().hook {
                before {
                    YLog.debug("[HideStoreHint] $methodName 方法被调用，参数: installFlags=${args[0]}, bindPerm=${args[1]}")
                }
                after {
                    YLog.debug("[HideStoreHint] $methodName 方法执行完成，开始隐藏商店相关视图")

                    // 尝试隐藏所有可能的商店相关视图
                    hideStoreViews(this.instance)

                    YLog.debug("[HideStoreHint] $methodName 方法处理完成")
                }
            }

            YLog.debug("[HideStoreHint] 成功 Hook $methodName 方法")
        } catch (e: Exception) {
            YLog.error("[HideStoreHint] Hook $methodName 方法失败: ${e.message}")
        }
    }

    private fun hideStoreViews(instance: Any?) {
        if (instance == null) {
            YLog.warn("[HideStoreHint] instance 为空")
            return
        }

        try {
            // 尝试隐藏所有可能的商店相关视图字段
            val viewFields = listOf(
                "mMarketContainer",
                "mWarningUnknown",
                "mDivider",
                "mMarketGuide",
                "mMarket",
                "marketReplace",
                "mWarningLayout"
            )

            viewFields.forEach { fieldName ->
                try {
                    val field = instance.javaClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    val view = field.get(instance)
                    if (view is View) {
                        view.visibility = View.GONE
                        YLog.debug("[HideStoreHint] 隐藏视图: $fieldName")
                    }
                } catch (e: NoSuchFieldException) {
                    // 字段不存在，忽略
                } catch (e: Exception) {
                    YLog.warn("[HideStoreHint] 处理字段 $fieldName 失败: ${e.message}")
                }
            }


        } catch (e: Exception) {
            YLog.error("[HideStoreHint] 隐藏商店视图失败: ${e.message}")
        }
    }
}