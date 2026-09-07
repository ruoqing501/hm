package dev.lackluster.mihelper.hook.rules.android.nubia

import android.annotation.SuppressLint
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import dev.lackluster.mihelper.utils.nubia.Deoptimizer

object AllowUntrustedTouches : YukiBaseHooker() {
    private const val TAG = "[AllowUntrustedTouches]"

    @SuppressLint("PrivateApi")
    override fun onHook() {
        hasEnable(Pref.Key.Android.ANDROID_ALLOW_UNTRUSTED_TOUCHES) {
            YLog.debug("$TAG 开始Hook")
            val windowStateClass = "com.android.server.wm.WindowState".toClassOrNull()
                ?: run {
                    YLog.error("$TAG 无法找到 WindowState 类")
                    return@hasEnable
                }
            YLog.debug("$TAG 找到 WindowState 类")
            val classInputMonitor = "com.android.server.wm.InputMonitor".toClassOrNull()
                ?: run {
                    YLog.error("$TAG 无法找到 InputMonitor 类")
                    return@hasEnable
                }
            YLog.debug("$TAG 找到 InputMonitor 类")

            // Hook WindowState 的 getTouchOcclusionMode 方法
            windowStateClass.apply {
                method {
                    name = "getTouchOcclusionMode"
                }.hook {
                    replaceTo(2) // 直接返回2 (ALLOW)
                }
                YLog.debug("$TAG Hook WindowState.getTouchOcclusionMode 成功")
            }

            Deoptimizer.deoptimizeMethods(TAG,classInputMonitor, "populateOverlayInputInfo","populateInputWindowHandle")
        }
    }
}