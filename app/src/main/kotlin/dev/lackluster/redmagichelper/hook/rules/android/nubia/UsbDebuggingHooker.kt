package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.IBinder
import android.widget.CheckBox
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object UsbDebuggingHooker : YukiBaseHooker() {
    private const val TAG = "UsbDebuggingHooker"

    @SuppressLint("PrivateApi")
    override fun onHook() {
        // 如果开关为 false，则撤销所有 USB 调试授权
        // 只会在每次重启SystemUI后生效
        // 如果勾选了一律允许后，下次链接设备的时候，它并不会撤销USB调试授权
        //if (!usbDebuggingAutoAllow) {
        //    try {
        //        val serviceManager = Class.forName("android.os.ServiceManager")
        //        val getService = serviceManager.getMethod("getService", String::class.java)
        //        val binder = getService.invoke(null, "adb") as? IBinder
        //        if (binder != null) {
        //            val stubClass = Class.forName("android.debug.IAdbManager\$Stub")
        //            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
        //            val adbManager = asInterface.invoke(null, binder)
        //            val clearMethod = adbManager.javaClass.getMethod("clearDebuggingKeys")
        //            clearMethod.invoke(adbManager)
        //            YLog.info(tag = TAG, msg = "Cleared USB debugging keys")
        //        }
        //    } catch (e: Exception) {
        //        YLog.error(tag = TAG, msg = "Failed to clear USB debugging keys", e = e)
        //    }
        //    return // 不继续 Hook 自动允许逻辑
        //}

        // 拦截 UsbDebuggingActivity 的 onCreate 方法
        "com.android.systemui.usb.UsbDebuggingActivity".toClass().method {
            name = "onCreate"
            param(Bundle::class.java)
        }.hook {
            after {
                // 开关：自动允许USB调试并勾选“一律允许”
                if (!Prefs.getBoolean(Pref.Key.NubiaSystemSettings.SYSTEM_SETTINGS_USB_DEBUGGING_AUTO_ALLOW, false)) return@after
                val activity = instance as? Activity ?: return@after
                try {
                    // 1. 将“一律允许”复选框设为选中
                    val alwaysAllowField = activity.javaClass.getDeclaredField("mAlwaysAllow")
                    alwaysAllowField.isAccessible = true
                    val checkBox = alwaysAllowField.get(activity) as CheckBox
                    checkBox.isChecked = true

                    // 2. 调用私有方法 notifyService(boolean, boolean) 执行授权
                    val notifyMethod = activity.javaClass.getDeclaredMethod(
                        "notifyService",
                        Boolean::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType
                    )
                    notifyMethod.isAccessible = true
                    notifyMethod.invoke(activity, true, true)

                    // 3. 结束 Activity，避免弹窗显示
                    activity.finish()

                    YLog.info(tag = TAG, msg = "Auto allowed USB debugging with always allow checked")
                } catch (e: Exception) {
                    YLog.error(tag = TAG, msg = "Failed to auto allow USB debugging", e = e)
                }
            }
        }

    }
}