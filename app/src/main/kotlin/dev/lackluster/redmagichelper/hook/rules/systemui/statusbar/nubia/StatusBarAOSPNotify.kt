package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import cn.fkj233.ui.activity.dp2px
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

object StatusBarAOSPNotify : YukiBaseHooker() {

    @SuppressLint("PrivateApi", "DiscouragedApi")
    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.STATUS_BAR_USE_THE_NATIVE_NOTIFICATION_ICON) {
            // 获取Adapt类并设置ZTE_STYLE字段
            val adaptClass = "com.zte.base.Adapt".toClassOrNull()
            if (adaptClass == null) {
                YLog.error("无法找到com.zte.base.Adapt类")
                return@hasEnable
            }

            // 获取ZTE_STYLE静态字段
            val zteStyleField = adaptClass.field {
                name = "ZTE_STYLE"
                modifiers { isStatic }
            }.get()

            // 强制所有应用使用系统图标
            val notificationUtilClass = "com.zte.feature.notification.NotificationUtil".toClassOrNull()
            if (notificationUtilClass != null) {
                // hook shouldShowAppIcon 方法，返回false
                notificationUtilClass.method {
                    name = "shouldShowAppIcon"
                    param(ApplicationInfo::class.java)
                }.hook {
                    replaceToFalse()
                }

                // hook getAppIconBackgroundDrawable 方法，返回null
                notificationUtilClass.method {
                    name = "getAppIconBackgroundDrawable"
                    param(String::class.java, BooleanType)
                }.hook {
                    replaceTo(null)
                }
            } else {
                YLog.warn("无法找到NotificationUtil类")
            }

            // 调整通知图标大小
            val iconSize = Prefs.getFloat("aosp_icon_size_dp", 16.0f)
            val notificationBackgroundClass = "com.zte.feature.notification.module.NotificationBackground".toClassOrNull()
            if (notificationBackgroundClass != null) {
                notificationBackgroundClass.method {
                    name = "adjustNotificationIcon" //不存在
                    param(View::class.java)
                }.hook {
                    before {
                        val view = this.args(0).cast<View>() ?: return@before
                        try {
                            val iconSizePx = dp2px(view.context, iconSize.toFloat())
                            val lp = FrameLayout.LayoutParams(iconSizePx, iconSizePx)
                            lp.gravity = Gravity.CENTER_HORIZONTAL
                            view.layoutParams = lp
                            this.result = null
                        } catch (e: Exception) {
                            YLog.error("调整通知图标大小失败: ${e.message}")
                        }
                    }
                }
            } else {
                YLog.warn("无法找到NotificationBackground类")
            }

            // 定义需要hook的Adapt类列表
            val adaptClasses = listOf(
                "com.zte.adapt.mifavor.notification.NotificationIconContainerAdapt",
                "com.zte.adapt.mifavor.notification.NotificationIconAreaControllerAdapt",
                "com.zte.adapt.mifavor.notification.NotificationHeaderViewWrapperAdapt",
                "com.zte.adapt.mifavor.notification.NotificationTemplateViewWrapperAdapt",
                "com.zte.adapt.mifavor.notification.ExpandableNotificationRowAdapt"
            )

            // 对每个类hook其所有方法（除了adjustNotificationIcon方法）
            adaptClasses.forEach { className ->
                val clazz = className.toClassOrNull()
                if (clazz != null) {
                    // Hook所有方法（除了adjustNotificationIcon）
                    clazz.method {}.hookAll {
                        before {
                            // 检查方法名，排除adjustNotificationIcon
                            if (this.method.name == "adjustNotificationIcon") {
                                return@before
                            }

                            // 在方法调用前设置ZTE_STYLE为false
                            try {
                                zteStyleField.setFalse()
                            } catch (e: Exception) {
                                YLog.warn("设置ZTE_STYLE为false失败: ${e.message}")
                            }
                        }

                        after {
                            // 检查方法名，排除adjustNotificationIcon
                            if (this.method.name == "adjustNotificationIcon") {
                                return@after
                            }

                            // 在方法调用后设置ZTE_STYLE为true
                            try {
                                zteStyleField.setTrue()
                            } catch (e: Exception) {
                                YLog.warn("设置ZTE_STYLE为true失败: ${e.message}")
                            }
                        }
                    }
                } else {
                    YLog.warn("无法找到类: $className")
                }
            }
        }
    }
}