package dev.lackluster.redmagichelper.hook.rules.android.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

object RemoveAlertWindowsNotification : YukiBaseHooker() {
    private const val TAG = "RemoveAlertWindowsNotification"

    override fun onHook() {
        val prefValue = Prefs.getBoolean(Pref.Key.Android.REMOVE_ALERT_WINDOWS_NOTIFICATION, false)
        android.util.Log.i("RMH_DEBUG", "RemoveAlertWindowsNotification onHook pref=$prefValue")
        hasEnable(Pref.Key.Android.REMOVE_ALERT_WINDOWS_NOTIFICATION) {
            val alertWindowNotificationClass =
                "com.android.server.wm.AlertWindowNotification".toClassOrNull()

            if (alertWindowNotificationClass == null) {
                YLog.warn(tag = TAG, msg = "未找到 AlertWindowNotification 类，可能版本不兼容")
                return@hasEnable
            }

            // 按方法名 hook 所有重载，避免厂商 ROM 修改方法签名导致 hook 静默失败
            // post() 是发送入口（Session 侧调用），拦截后通知不会再进入发送流程
            alertWindowNotificationClass.method {
                name = "post"
            }.hook {
                before {
                    YLog.debug(tag = TAG, msg = "拦截 AlertWindowNotification.post()，阻止悬浮窗通知发送")
                    result = null
                }
            }

            // onPostNotification() 是实际构建并发送通知的方法，作为兜底拦截
            alertWindowNotificationClass.method {
                name = "onPostNotification"
            }.hook {
                before {
                    YLog.debug(tag = TAG, msg = "拦截 onPostNotification()，阻止悬浮窗通知发送")
                    result = null
                }
            }

            YLog.info(tag = TAG, msg = "Hook 设置完成，悬浮窗通知将被拦截")
        }
    }
}
