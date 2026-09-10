package dev.lackluster.redmagichelper.hook.rules.gamespace

import android.content.ContentResolver
import android.os.BaseBundle
import android.os.Bundle
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.android.ContextClass
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.CharSequenceClass
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass

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

        // 4. 从原厂连招/连点黑名单中放行 (LS_Augment GS-04)
        hookPluginConfigBlackList()




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
     * Hook TgkMapView.getGameKeyLinkMotionState 方法:
     * - 原厂返回 Boolean false 时直接改为 true(LS_Augment GS-03 的返回值放行);
     * - 同时尽力将支持字段(mSupportedGameKeyLink 等别名)置为 true。
     */
    private fun hookSupportedGameKeyLink() {
        "cn.nubia.tgk.TgkMapView".toClass().method {
            name = "getGameKeyLinkMotionState"
            emptyParam()
        }.hook {
            after {
                if (result is Boolean && result == false) {
                    result = true
                    YLog.debug(tag = TAG, msg = "GS-03 getGameKeyLinkMotionState false-to-true")
                }
                if (forceSupportedGameKeyLink(instance)) {
                    YLog.debug(tag = TAG, msg = "强制启用 mSupportedGameKeyLink")
                }
            }
        }
    }

    /** LS_Augment GS-03 的字段置位:沿继承体系尝试多个已知字段名。 */
    private fun forceSupportedGameKeyLink(owner: Any?): Boolean {
        if (owner == null) return false
        for (name in arrayOf(
            "mSupportedGameKeyLink", "supportedGameKeyLink",
            "mGameKeyLinkSupported", "gameKeyLinkSupported"
        )) {
            var type: Class<*>? = owner.javaClass
            while (type != null) {
                try {
                    val field = type.getDeclaredField(name)
                    field.isAccessible = true
                    if (field.type != java.lang.Boolean.TYPE &&
                        field.type != java.lang.Boolean::class.java
                    ) break
                    field.set(owner, true)
                    return true
                } catch (_: NoSuchFieldException) {
                    type = type.superclass
                } catch (_: Throwable) {
                    return false
                }
            }
        }
        return false
    }

    /**
     * LS_Augment GS-04:把肩键相关插件(keylink / touch_long_keylink_point /
     * range_line)的原厂黑名单清空,仅保留 Root/模块管理器等受保护包。
     * LS_Augment 只移除用户选定且非系统的目标包;本项目与 hookTgkDisableOpt 的
     * 全量放行语义保持一致(本模块没有按游戏选择列表)。
     */
    private fun hookPluginConfigBlackList() {
        val pluginConfig =
            "cn.nubia.gamelauncher.gamecontrolpanel.config.PluginConfig".toClassOrNull() ?: return
        pluginConfig.method {
            name = "getBlackList"
            param(ContextClass, StringClass)
            modifiers { isStatic }
        }.hook {
            after {
                val plugin = args[1] as? String
                if (plugin !in setOf("keylink", "touch_long_keylink_point", "range_line")) {
                    return@after
                }
                val original = result as? Array<*> ?: return@after
                val filtered = original.filterIsInstance<String>()
                    .filterNot { isProtectedShoulderPackage(it) }
                if (filtered.size != original.size) {
                    result = filtered.toTypedArray()
                    YLog.debug(
                        tag = TAG,
                        msg = "GS-04 getBlackList plugin=$plugin removed=${original.size - filtered.size}"
                    )
                }
            }
        }
    }

    private fun isProtectedShoulderPackage(packageName: String): Boolean =
        packageName in setOf(
            "dev.lackluster.redmagichelper",
            "me.weishu.kernelsu", "me.weishu.kernelsu.debug",
            "com.rifsxd.ksunext", "org.lsposed.manager",
            "com.topjohnwu.magisk", "me.bmax.apatch"
        )
}