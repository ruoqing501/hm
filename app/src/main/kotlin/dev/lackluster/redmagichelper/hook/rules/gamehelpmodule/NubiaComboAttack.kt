package dev.lackluster.redmagichelper.hook.rules.gamehelpmodule


import android.content.ContentResolver
import android.os.BaseBundle
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.CharSequenceClass
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.hook.compat.type.java.UnitType
import dev.lackluster.redmagichelper.hook.rules.gameassist.NubiaGameAssistDebug
import dev.lackluster.redmagichelper.hook.rules.gamespace.NubiaTgkHelper

// 包名： cn.nubia.gamehelpmodule

// 一键连招 : 包名： cn.nubia.gamehelpmodule
object NubiaComboAttack : YukiBaseHooker() {

    private const val TAG = "NubiaComboAttack"

    private val edt_loop_count by lazy {
        appContext!!.resources.getIdentifier("edt_loop_count", "id", appContext!!.packageName)
    }

    private val seekbar_loop_delay by lazy {
        appContext!!.resources.getIdentifier("seekbar_loop_delay", "id", appContext!!.packageName)
    }


    override fun onHook() {
        "cn.nubia.gamehelper.utils.PackageUtils".toClass().method {
            name = "isBlackPackageName"
        }.hook{
            after {
                result = false
                YLog.debug(tag = TAG, msg = "PackageUtils.isBlackPackageName called")
            }
        }

        // 在 NubiaGameAssistDebug.kt 的 onHook() 中添加
        // edt_loop_count  0x7f08005e       2131230814
        // seekbar_loop_delay 0x7f0800be    2131230910
        View::class.java.method {
            name = "findViewById"
            param(IntType)  // 参数类型：int
        }.hook {
            after {
                val id = args[0] as Int
                when (id) {
                    //2131230814 -> {
                    edt_loop_count -> {
                        val editText = result as? EditText
                        editText?.filters = arrayOf(InputFilter.LengthFilter(6))
                        YLog.debug(tag = TAG, msg = "Nubia: 修改最大循环次数成功")
                    }
                    //2131230910 -> {
                    seekbar_loop_delay -> {
                        val seekBar = result as? SeekBar
                        seekBar?.max = 3600
                        YLog.debug(tag = TAG, msg = "Nubia: 修改最大间隔时间成功")
                    }
                }
            }
        }

        // 检测游戏包名
        //hookStringContainsForGame()
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
}