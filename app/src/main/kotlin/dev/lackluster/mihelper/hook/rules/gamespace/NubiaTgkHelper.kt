package dev.lackluster.mihelper.hook.rules.gamespace

import android.content.ContentResolver
import android.os.BaseBundle
import android.os.Bundle
import android.view.View
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.android.ContextClass
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType
import dev.lackluster.mihelper.hook.compat.type.java.CharSequenceClass
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.hook.compat.type.java.StringClass
import dev.lackluster.mihelper.hook.compat.type.java.UnitType

// 包名：cn.nubia.gamelauncher 游戏空间
object NubiaTgkHelper : YukiBaseHooker() {

    private const val TAG = "NubiaTgkHelper"

    override fun onHook() {

        // 允许使用肩键菜单弹窗功能(需要重启手机)
        hookTgkHelperDisableTgkFunction()
        // 2. 强制启用一键连招功能 (解决 Link 选项不可用)
        hookSupportedGameKeyLink()


        // 1. 解除特定游戏的禁用选项掩码 (解决连点/滑动等被禁用)
        hookTgkDisableOpt()




        // 允许所有的插件开启（磁贴）
        //hookUpdateUIPluginConfig()
        hookBundleGetBoolean() //兼容旧版本
        //hookStringContainsForGame()


    }




    //private fun hookUpdateUIPluginConfig() {
    //    "cn.nubia.gamelauncher.gamecontrolpanel.config.PluginConfig".toClass().method {
    //        name = "isPluginEnable";
    //        param(ContextClass,StringClass,StringClass, BooleanType)
    //        modifiers { isStatic }
    //        returnType = BooleanType
    //    }.hook(){
    //        before {
    //            result = true
    //            YLog.debug(tag = TAG, msg = "允许所有插件开启")
    //        }
    //    }
    //}


    // 4. 允许使用 TgkHelper.disableTgkFunction 方法-开启后，会弹出提示框
    private fun hookTgkHelperDisableTgkFunction() {
        val tgkHelperClass = "cn.nubia.tgk.TgkHelper".toClassOrNull() ?: return
        tgkHelperClass.method {
            name = "disableTgkFunction"
            param(StringClass)
            returnType = BooleanType
            modifiers { isStatic }
        }.hook {
            before {
                val pkg = args[0] as? String
                YLog.debug(tag =TAG, msg = "disableTgkFunction called with pkg = $pkg")
            }
            after {
                result = false
            }
            //replaceToFalse()

        }
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
                    //YLog.debug(tag = TAG, msg = "修改自动连招成功")
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

    //private fun hookBundleGetBoolean() {
    //    //Hook Bundle.getBoolean(String) - 返回 boolean
    //    BaseBundle::class.java.method {
    //        name = "getBoolean"
    //        param(StringClass)
    //    }.hook {
    //        after {
    //           if( args[0].toString().equals("isMacroEnable")||
    //               args[0].toString().equals("isPackageEnable")||
    //               args[0].toString().equals("is_need_show_linkview")){
    //               result = true
    //               YLog.debug(tag = TAG, msg = "修改自动连招成功")
    //
    //           }
    //
    //        }
    //    }
    //
    //    // Hook Bundle.getBoolean(String, boolean) - 带默认值的版本
    //    BaseBundle::class.java.method {
    //        name = "getBoolean"
    //        param(StringClass,BooleanType)
    //    }.hook {
    //        after {
    //            if( args[0].toString().equals("isMacroEnable")||
    //                args[0].toString().equals("isPackageEnable")||
    //                args[0].toString().equals("is_need_show_linkview")){
    //                result = true
    //                YLog.debug(tag = TAG, msg = "修改自动连招成功")
    //
    //            }
    //
    //        }
    //    }
    //}

    //private fun hookStringContainsForGame() {
    //    StringClass.method {
    //        name = "contains"
    //        param(CharSequenceClass)
    //    }.hook {
    //        after {
    //            val target = args[0] as? CharSequence
    //            if (target == "com.tencent.tmgp.sgame") {
    //                YLog.debug(tag = TAG, msg = "Detected 王者荣耀包名，修改 contains 返回 false")
    //                result = false
    //            }
    //        }
    //    }
    //}


    /**
     * Hook TgkHelper.getTgkDisableOpt 方法，对指定游戏返回 0，解锁所有选项。
     */
    private fun hookTgkDisableOpt() {
        "cn.nubia.tgk.TgkHelper".toClass().method {
            name = "getTgkDisableOpt"
            param(ContentResolver::class.java, StringClass)
            returnType = IntType
            modifiers { isStatic }
        }.hook {
            after {
                val packageName = args[1] as String
                result = 0
                YLog.debug(tag = TAG, msg = "解除 $packageName 的选项禁用掩码")
            }
        }
    }

    /**
     * Hook TgkMapView.getGameKeyLinkMotionState 方法后的 mSupportedGameKeyLink 字段，
     * 强制设置 mSupportedGameKeyLink = true，确保一键连招选项始终可用。
     */
    private fun hookSupportedGameKeyLink() {
        "cn.nubia.tgk.TgkMapView".toClass().method {
            name = "getGameKeyLinkMotionState"
            emptyParam()
            returnType = UnitType
        }.hook {
            after {
                instance.current().field {
                    name = "mSupportedGameKeyLink"
                    type = BooleanType
                }.set(true)

                YLog.debug(tag = TAG, msg = "强制启用 mSupportedGameKeyLink")
            }
        }
    }
}