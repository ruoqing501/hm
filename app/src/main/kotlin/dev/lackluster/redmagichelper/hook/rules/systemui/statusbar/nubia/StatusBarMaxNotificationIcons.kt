package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.FloatType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

// 自定义通知图标最大数量
object StatusBarMaxNotificationIcons : YukiBaseHooker() {
    private const val TAG = "StatusBarMaxNotificationIcons"

    // 状态栏通知图标数量
    private val status_bar_notification_count_icon by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT_ICON, -1)
    }

    override fun onHook() {
        // 自定义通知图标最大数量
        hasEnable(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT){
            val maxIcons = status_bar_notification_count_icon

            if (maxIcons < 0) {
                YLog.info(tag = TAG, msg = "通知图标数量设置为默认值：$maxIcons，不进行 Hook")
                return@hasEnable
            }

            YLog.info(tag = TAG, msg = "设置状态栏最大通知图标数量：$maxIcons")

            try {
                "com.android.systemui.statusbar.phone.NotificationIconContainer".toClass().apply {
                    method {
                        name = "shouldForceOverflow"
                        param(IntType, IntType, FloatType, IntType)
                    }.hook {
                        before {
                            args[3] = maxIcons
                            YLog.info(tag = TAG, msg = "shouldForceOverflow Hook: 设置最大图标数量为 $maxIcons")
                        }
                    }

                    method {
                        name = "initResources"
                        emptyParam()
                    }.hook {
                        after {
                            //instance::class.java.getDeclaredField("mMaxStaticIcons").apply {
                            //    isAccessible = true
                            //    setInt(instance, Int.MAX_VALUE)
                            //}
                            instance.current().field {
                                name = "mMaxStaticIcons"
                                superClass()  // 表示从父类开始查找
                            }.set(Int.MAX_VALUE)
                            YLog.info(tag = TAG, msg = "initResources Hook: mMaxStaticIcons 设置为： ${Int.MAX_VALUE}")
                        }
                    }
                }

                YLog.info(tag = TAG, msg = "状态栏通知图标数量限制 Hook 成功")
            } catch (t: Throwable) {
                YLog.error(tag = TAG, msg = "状态栏通知图标数量限制 Hook 失败", e = t)
            }
        }
    }
}
