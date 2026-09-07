package dev.lackluster.mihelper.hook.apps

import android.annotation.SuppressLint
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.u9521.wooboxforredmagicos.hook.app.systemui.statusbar.StatusBarSBFontRestore
import dev.lackluster.mihelper.hook.rules.android.AntiQues
import dev.lackluster.mihelper.hook.rules.android.nubia.UsbDebuggingHooker
import dev.lackluster.mihelper.hook.rules.screenshot.StatusBarBroadcastController
import dev.lackluster.mihelper.hook.rules.shared.RemoveFreeformRestriction
import dev.lackluster.mihelper.hook.rules.systemui.DisableSmartDark
import dev.lackluster.mihelper.hook.rules.systemui.FuckStatusBarGestures
import dev.lackluster.mihelper.hook.rules.systemui.MonetOverlay
import dev.lackluster.mihelper.hook.rules.systemui.ResourcesUtils
import dev.lackluster.mihelper.hook.rules.systemui.StatusBarActions
import dev.lackluster.mihelper.hook.rules.systemui.batteryicon.nubia.BatteryIconAdjuster
import dev.lackluster.mihelper.hook.rules.systemui.batteryicon.nubia.BatteryLevelColorController
import dev.lackluster.mihelper.hook.rules.systemui.features.AOSPSingleHandModeAdjust
import dev.lackluster.mihelper.hook.rules.systemui.features.GestureStartDefaultDigitalAssist
import dev.lackluster.mihelper.hook.rules.systemui.features.NoVibrateVolKeyLongPress
import dev.lackluster.mihelper.hook.rules.systemui.features.UnHideClipBoardOverlay
import dev.lackluster.mihelper.hook.rules.systemui.font.NubiaFont
import dev.lackluster.mihelper.hook.rules.systemui.screenoff.nubia.AodSecondUpdate
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.HideCarrierLabel
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.HideDisturbNotification
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.HideStatusBarIcon
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.IconPosition
import dev.lackluster.mihelper.hook.rules.systemui.notif.NotifFreeform
import dev.lackluster.mihelper.hook.rules.systemui.notif.NotifWhitelist
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.NotificationMaxNumber
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.StatusBarClock
import dev.lackluster.mihelper.hook.rules.systemui.freeform.HideTopBar
import dev.lackluster.mihelper.hook.rules.systemui.freeform.UnlockMultipleTask
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.CarrierTextView
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.DoubleTapToSleep
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.KeepNotification
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.LockScreenBatteryMsg
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.HideLockScreenStatusBar
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.LockScreenAllowAdjustVolume
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.LockScreenClockFont
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.LockScreenClockPeriod
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.LockScreenClockSeconds
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.ModifyChargingAnimation
import dev.lackluster.mihelper.hook.rules.systemui.media.CustomElement
import dev.lackluster.mihelper.hook.rules.systemui.media.CustomLayout
import dev.lackluster.mihelper.hook.rules.systemui.media.CustomBackground
import dev.lackluster.mihelper.hook.rules.systemui.media.UnlockCustomAction
import dev.lackluster.mihelper.hook.rules.systemui.notif.ExpandNotification
import dev.lackluster.mihelper.hook.rules.systemui.notif.MiuiXExpandButton
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.BatteryIndicator
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.ControlCenterBattery
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.ElementsFontWeight
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.HideCellularIcon
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.HideWiFiIcon
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.IgnoreSysHideIcon
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.PadClockAnim
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarDoubleTapToSleep
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarClockPeriod
import dev.lackluster.mihelper.hook.rules.systemui.nubia.StatusBarPullDownClock
import dev.lackluster.mihelper.hook.rules.systemui.qs.nubia.QSCustom
import dev.lackluster.mihelper.hook.rules.systemui.qs.nubia.QSHeaderShortcut
import dev.lackluster.mihelper.hook.rules.systemui.qs.nubia.QSHeaderShowControl
import dev.lackluster.mihelper.hook.rules.systemui.recenttasks.LandscapePortraitDetection
import dev.lackluster.mihelper.hook.rules.systemui.recenttasks.RecentTasksHook
import dev.lackluster.mihelper.hook.rules.systemui.screenoff.nubia.ScreenOffPeriodModifier
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.BatteryIconPercentSwapHook
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.Clock
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.DualRowsStatusBarHook
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.HideStatusBarBeforeScreenshot
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarAOSPNotify
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarClockHooker
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarFontRestoreHooker
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarHideCellularIcon
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarHideStatusBarIcon
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarHideWifiActivityIcon
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarIgnoreSysHideIcon
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarKSBFontRestore
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarNetworkSpeedAdjuster
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarPullDownPeriod
import dev.lackluster.mihelper.utils.DexKit
import  dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.MobileClass
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarMaxNotificationIcons
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarTemperatureHook
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.SunShineFeatureHooker
import dev.lackluster.mihelper.hook.rules.systemui.test.TextViewAnalyzer
import dev.lackluster.mihelper.hook.rules.systemui.volume.VolumeDialogHook

object SystemUI : YukiBaseHooker() {
    @SuppressLint("UseCompatLoadingForDrawables", "UseKtx")
    override fun onHook() {
        // 状态栏广播控制器（跨进程控制）
        loadHooker(StatusBarBroadcastController)
        // 去除音量弹窗警告
        loadHooker(VolumeDialogHook)
        // 横竖屏检测
        //loadHooker(LandscapePortraitDetection)
        // 显示内存信息
        //loadHooker(RecentTasksHook)
        loadApp(hooker = Clock())
        loadHooker(DualRowsStatusBarHook)
        loadHooker(StatusBarTemperatureHook)
        loadHooker(SunShineFeatureHooker)
        loadHooker(HideLockScreenStatusBar)
        loadHooker(LockScreenBatteryMsg)
        loadHooker(ModifyChargingAnimation)
//        loadApp(name = "com.android.systemui") {
//            resources().hook {
//                injectResource {
//                    conditions {
//                        name = "status_bar_height"
//                        dimen()
//                    }
//                    replaceTo(20.0f) // 修改为你想要的值
//                }
//                injectResource {
//                    conditions {
//                        name = "intl_status_bar_height"
//                        dimen()
//                    }
//                    replaceTo(20.0f)
//                }
//                injectResource {
//                    conditions {
//                        name = "intl_status_bar_height_default"
//                        dimen()
//                    }
//                    replaceTo(20.0f)
//                }
//            }
//        }
//        loadApp(hooker = MobileClass)
//        loadHooker(ResourcesUtils)
//
//        loadHooker(CarrierTextView)
//        loadHooker(DoubleTapToSleep)
//        loadHooker(HideDisturbNotification)
//        loadHooker(KeepNotification)
//
//        loadHooker(CustomBackground)
//        loadHooker(CustomLayout)
//        loadHooker(CustomElement)
//
//        loadHooker(MiuiXExpandButton)
//        loadHooker(NotifFreeform)
//        loadHooker(NotifWhitelist)
//        loadHooker(ExpandNotification)
//
//        loadHooker(MonetOverlay)

//        loadHooker(IgnoreSysHideIcon)

        // 状态栏忽略系统隐藏图标-Nubia
        loadHooker(StatusBarIgnoreSysHideIcon)
        // 状态栏隐藏WiFi活动图标
        loadHooker(StatusBarHideWifiActivityIcon)
        loadHooker(StatusBarHideStatusBarIcon)
        // 状态栏隐藏移动蜂窝图标
        loadHooker(StatusBarHideCellularIcon)
//        loadHooker(TextViewAnalyzer)
        loadHooker(NubiaFont)
//        loadHooker(BatteryIndicator)
//        loadHooker(ControlCenterBattery)
//        loadHooker(HideCarrierLabel)
//        loadHooker(HideCellularIcon)
//        loadHooker(HideStatusBarIcon)
//        loadHooker(HideWiFiIcon)
//        loadHooker(IconPosition)
//        loadHooker(NotificationMaxNumber)
//        loadHooker(ElementsFontWeight)
//        loadHooker(StatusBarClock)

        // 下拉状态栏时钟
        loadHooker(StatusBarPullDownClock)

        // 状态栏时钟
        loadHooker(StatusBarClockHooker)

        // 状态栏双击锁定屏幕
        loadHooker(StatusBarDoubleTapToSleep)

//        // 恢复锁屏图标与字体
//        loadHooker(StatusBarKSBFontRestore)
//
//        // 恢复状态栏 时钟 网络 电池默认字体加粗
//        loadHooker(StatusBarSBFontRestore)


        //相当于上面两个的结合

        loadHooker(StatusBarFontRestoreHooker)

        // 状态栏原生通知图标
        loadHooker(StatusBarAOSPNotify)




         // 锁屏时钟字体
//        loadHooker(LockScreenClockFont)

        // 锁屏时钟显秒
        loadHooker(LockScreenClockSeconds)

        // 锁屏时钟时段
        loadHooker(LockScreenClockPeriod)

        // 截图时隐藏状态栏
        //loadHooker(HideStatusBarBeforeScreenshot)

        // 总是允许 USB 调试授权
        loadHooker(UsbDebuggingHooker)

        // 自定义通知图标最大数量
        loadHooker(StatusBarMaxNotificationIcons)




        // 锁屏允许音量调节
        loadHooker(LockScreenAllowAdjustVolume)

        // 熄屏显示时段
        loadHooker(ScreenOffPeriodModifier)
        // 禁用长按调节音量的振动
        loadHooker(NoVibrateVolKeyLongPress)
        // 恢复显示安卓原生剪贴板浮窗
        loadHooker(UnHideClipBoardOverlay)
        // AOSP 单手模式调节
        loadHooker(AOSPSingleHandModeAdjust)
        // 快速设置面板
        loadHooker(QSHeaderShortcut)
        loadHooker(QSHeaderShowControl)
        loadHooker(QSCustom)
        // 电池图标
        loadHooker(BatteryIconAdjuster)
        // 修改电池图标颜色
        loadHooker(BatteryLevelColorController)
        // 交换电池图标百分比和图标
//        loadHooker(BatteryIconPercentSwapHook)
        // 这个hook的方法使用了dexkit，所以需要进行初始化
        DexKit.initDexKit(this)
        // 熄屏显示秒（调度-未实现，防止显示秒卡顿）
        loadHooker(AodSecondUpdate)
        loadHooker(StatusBarNetworkSpeedAdjuster)
        //特性
        loadHooker(GestureStartDefaultDigitalAssist)//手势打开默认数字助理
        DexKit.closeDexKit()





        // 状态栏时段，星期、月/日
//        loadHooker(StatusBarClockPeriod)
//         下拉状态栏时段
//        loadHooker(StatusBarPullDownPeriod)




//        loadHooker(StatusBarDoubleTapToSleep)

//        loadHooker(DisableSmartDark)
//        loadHooker(StatusBarActions)
//        loadHooker(FuckStatusBarGestures)

//        return
//        loadHooker(RemoveFreeformRestriction)
//        loadHooker(UnlockMultipleTask)
//        loadHooker(HideTopBar)


//        loadHooker(UnlockCustomAction)
//        loadHooker(PadClockAnim)
    }
}