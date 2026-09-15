package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.FloatType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

// 自定义通知图标最大数量
object StatusBarMaxNotificationIcons : YukiBaseHooker() {
    private const val TAG = "StatusBarMaxNotificationIcons"

    override fun onHook() {
        // 自定义通知图标最大数量
        try {
            "com.android.systemui.statusbar.phone.NotificationIconContainer".toClass().apply {
                method {
                    name = "shouldForceOverflow"
                    param(IntType, IntType, FloatType, IntType)
                }.hook {
                    before {
                        if (!Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT, false)) return@before
                        val maxIcons = Prefs.getInt(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT_ICON, -1)
                        if (maxIcons < 0) return@before
                        args[3] = maxIcons
                        YLog.info(tag = TAG, msg = "shouldForceOverflow Hook: 设置最大图标数量为 $maxIcons")
                    }
                }

                method {
                    name = "initResources"
                    emptyParam()
                }.hook {
                    after {
                        if (!Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT, false)) return@after
                        if (Prefs.getInt(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT_ICON, -1) < 0) return@after
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
