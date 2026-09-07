package dev.lackluster.mihelper.hook.rules.android.nubia

import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object RemoveAlertWindowsNotification : YukiBaseHooker() {

    override fun onHook() {
        hasEnable(Pref.Key.Android.REMOVE_ALERT_WINDOWS_NOTIFICATION) {
            YLog.debug("[RemoveAlertWindowsNotification] 开始Hook - 检测到用户启用了移除悬浮窗通知功能")

            // 尝试加载AlertWindowNotification类
            YLog.debug("[RemoveAlertWindowsNotification] 正在尝试加载类: com.android.server.wm.AlertWindowNotification")
            val alertWindowNotificationClass = "com.android.server.wm.AlertWindowNotification".toClassOrNull()

            if (alertWindowNotificationClass != null) {
                YLog.debug("[RemoveAlertWindowsNotification] 成功找到AlertWindowNotification类: ${alertWindowNotificationClass.name}")

                // 查找onPostNotification方法
                YLog.debug("[RemoveAlertWindowsNotification] 正在查找onPostNotification方法...")
                val method = alertWindowNotificationClass.method {
                    name = "onPostNotification"
                    emptyParam()
                }

                // Hook onPostNotification方法
                method.hook {
                    before {
                        YLog.debug("[RemoveAlertWindowsNotification] onPostNotification方法被调用，正在阻止悬浮窗通知发送")
                        YLog.debug("[RemoveAlertWindowsNotification] 原始方法参数: ${args?.joinToString() ?: "无参数"}")
                        YLog.debug("[RemoveAlertWindowsNotification] 原始调用栈: ${Throwable().stackTrace.take(5).joinToString("\n")}")

                        // 阻止通知的发送，将结果设为null
                        this.result = null

                        YLog.debug("[RemoveAlertWindowsNotification] 已成功阻止悬浮窗通知，方法返回null")
                        YLog.debug("[RemoveAlertWindowsNotification] Hook操作完成，用户将不会收到悬浮窗权限提示通知")
                    }

                    after {
                        YLog.debug("[RemoveAlertWindowsNotification] onPostNotification方法执行完毕，最终结果: ${result ?: "null"}")
                    }
                }

                YLog.info("[RemoveAlertWindowsNotification] Hook设置完成，等待方法被调用")
            } else {
                YLog.warn("[RemoveAlertWindowsNotification] 未找到AlertWindowNotification类，可能原因:")
                YLog.warn("[RemoveAlertWindowsNotification] 1. 系统版本不支持")
                YLog.warn("[RemoveAlertWindowsNotification] 2. 类名或路径不正确")
                YLog.warn("[RemoveAlertWindowsNotification] 3. 类已被混淆或重命名")


            }

            YLog.debug("[RemoveAlertWindowsNotification] Hook流程结束")
        }
    }


}