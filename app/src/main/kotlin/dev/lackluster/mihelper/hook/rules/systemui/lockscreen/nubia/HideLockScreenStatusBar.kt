package dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia

import android.view.View
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object HideLockScreenStatusBar : YukiBaseHooker() {
    private const val TAG = "HideLockScreenStatusBar"

    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_HIDE_STATUS_BAR) {
            YLog.debug(tag = TAG, msg =" 开始 Hook 锁屏状态栏隐藏")

            // 根据系统版本确定目标类
            val statusBarClass =
                "com.android.systemui.statusbar.phone.CentralSurfacesImpl"


            // 查找 makeStatusBarView 方法并使用 YukiHook 语法注入钩子
            try {
                statusBarClass.toClass().method {
                    name = "makeStatusBarView"
                }.hook {
                    after {
                        try {
                            val instance = this.instance

                            // 获取 mShadeSurface 字段
                            var shadeSurface: Any? = null
                            try {
                                shadeSurface = instance.current().field {
                                    name = "mShadeSurface"
                                }.any()
                            } catch (e: Exception) {
                                // 尝试 mNotificationPanelViewController
                                shadeSurface = instance.current().field {
                                    name = "mNotificationPanelViewController"
                                }.any()
                            }

                            if (shadeSurface == null) {
                                YLog.debug(tag = TAG, msg = "无法获取 mShadeSurface 或 mNotificationPanelViewController")
                                return@after
                            }

                            // 获取 KeyguardStatusBarView 视图
                            val keyguardStatusBar = shadeSurface.current().field {
                                name = "mKeyguardStatusBar"
                                superClass()
                            }.any() as? View

                            if (keyguardStatusBar == null) {
                                YLog.debug(tag = TAG, msg ="无法获取 mKeyguardStatusBar")
                                return@after
                            }

                            // 移出屏幕实现隐藏
                            keyguardStatusBar.translationY = -999f
                            YLog.debug(tag = TAG, msg ="锁屏状态栏已隐藏")
                        } catch (e: Exception) {
                            YLog.debug(tag = TAG, msg ="反射操作失败：${e.message}", e=e)
                        }
                    }
                }
            } catch (e: NoSuchMethodException) {
                YLog.debug(tag = TAG, msg ="找不到方法 makeStatusBarView")
            }
        }
    }
}
