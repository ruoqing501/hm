package dev.lackluster.mihelper.hook.rules.gameassist

import android.content.Context
import android.os.BaseBundle
import android.os.Bundle
import android.provider.Settings
import android.view.View
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.type.java.UnitType
import dev.lackluster.mihelper.hook.rules.gamespace.NubiaTgkHelper

// 包名：cn.nubia.gameassist
object NubiaGameAssistDebug : YukiBaseHooker() {

    private const val TAG = "GameAssistDebug"

    override fun onHook() {
        // 逐个取消注释以测试对应模块
        hookSubViewController()
        hookUpdateUIPluginConfig1()
        hookUpdateUIPluginConfig2()
        hookBundleGetBoolean()
    }




    private fun hookBundleGetBoolean() {
        //Hook Bundle.getBoolean(String) - 返回 boolean
        BaseBundle::class.java.method {
            name = "getBoolean"
            param(StringClass)
        }.hook {
            after {
                val key = args[0] as? String
                // 根据需求拦截多个键
                if (key in setOf("isMacroEnable", "isPackageEnable", "is_need_show_linkview")) {
                    result = true
                    YLog.debug(tag =TAG, msg = "Bundle.getBoolean($key) → true (forced)")
                }
            }
        }

        // Hook Bundle.getBoolean(String, boolean) - 带默认值的版本
        BaseBundle::class.java.method {
            name = "getBoolean"
            param(StringClass, BooleanType)
        }.hook {
            after {
                val key = args[0] as? String
                if (key in setOf("isMacroEnable", "isPackageEnable", "is_need_show_linkview")) {
                    result = true
                    YLog.debug(tag = TAG, msg = "Bundle.getBoolean($key, ...) → true (forced)")
                }
            }
        }
    }

    private fun hookUpdateUIPluginConfig1() {
        "cn.nubia.gameassist.plugin.config.PluginConfig".toClass().method {
            name = "isPluginEnable";
            param(ContextClass,StringClass,StringClass)  // 参数顺序：Context, pluginName, packageName
            modifiers { isStatic }
            returnType = BooleanType
        }.hook(){
            before {
                result = true
                YLog.debug(tag = TAG, msg = "允许所有插件开启")
            }
        }
    }
    private fun hookUpdateUIPluginConfig2() {
        "cn.nubia.gameassist.plugin.config.PluginConfig".toClass().method {
            name = "isPluginEnable";
            param(ContextClass,StringClass)
            modifiers { isStatic }
            returnType = BooleanType
        }.hook(){
            before {
                result = true
                YLog.debug(tag = TAG, msg = "允许所有插件开启")
            }
        }
    }


    // 确保不被指定包名隐藏视图（悬浮面板顶部肩键按钮入口）
    private fun hookSubViewController() {
        val clazz = "cn.nubia.gameassist.operation.SubViewController".toClassOrNull() ?: return
        // 获取当前包名的工具方法
        val utilsClass = "cn.nubia.gameassist.utils.Utils".toClassOrNull()

        clazz.method {
            name = "initView"
            param("android.view.ViewGroup".toClass())
        }.hook {
            after {
                    // 获取当前包名
                    val getCurrentPkgMethod = utilsClass?.method {
                        name = "getCurrentPkg";
                        modifiers { isStatic }
                    }?.get()?.call()
                    // 确保不被指定包名隐藏视图（肩键）
                    val keysView = instance.current().field { name = "mKeys" }.any() as? View
                    keysView?.visibility = View.VISIBLE
                    YLog.debug(tag = TAG, msg = "Force showing mKeys for SGame:${getCurrentPkgMethod}")

                }
            }
        }
    }
