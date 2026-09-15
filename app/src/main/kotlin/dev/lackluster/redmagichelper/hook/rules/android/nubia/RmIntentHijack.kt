package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.annotation.SuppressLint
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.nubia.Deoptimizer
import java.lang.reflect.Member

@SuppressLint("PrivateApi")
object RmIntentHijack : YukiBaseHooker() {
    private const val TAG = "[AllowUntrustedTouches]"
    

    override fun onHook() {
        // 获取 ResolveIntentHelper 类
        val resolveIntentHelperClass = "com.android.server.pm.ResolveIntentHelper".toClassOrNull()
            ?: run {
                YLog.error("$TAG 无法找到 ResolveIntentHelper 类")
                return
            }

        YLog.info("$TAG 开始 Hook ResolveIntentHelper 类")

        // Hook isCtsTesting 方法，强制返回 true
        resolveIntentHelperClass.method {
            name = "isCtsTesting"
        }.hook {
            before {
                if (!Prefs.getBoolean(Pref.Key.Android.ANDROID_REMOVE_INTENT_HIJACK_CONTENT, false)) return@before
                result = true
            }
            YLog.debug("$TAG Hook isCtsTesting 方法，强制返回 true")
        }

//            // 使用YukiHookAPI的before/after hook机制处理chooseBestActivity方法
//            resolveIntentHelperClass.method {
//                name = "chooseBestActivity"
//            }.hook {
//                // 在方法执行前可以进行一些操作
//                before {
//                    YLog.debug("$TAG chooseBestActivity 方法即将执行")
//                }
//                // 在方法执行后可以进行一些操作
//                after {
//                    YLog.debug("$TAG chooseBestActivity 方法执行完成")
//                }
//            }
        Deoptimizer.deoptimizeMethod(TAG,resolveIntentHelperClass, "chooseBestActivity")

    }
}