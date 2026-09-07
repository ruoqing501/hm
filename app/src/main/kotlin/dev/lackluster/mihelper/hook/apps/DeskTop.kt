package dev.lackluster.mihelper.hook.apps


import android.annotation.SuppressLint
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.u9521.wooboxforredmagicos.hook.app.systemui.statusbar.StatusBarSBFontRestore
import dev.lackluster.mihelper.hook.rules.desktop.CustomTextClockHook
import dev.lackluster.mihelper.hook.rules.desktop.RecentTasksHook
import dev.lackluster.mihelper.hook.rules.desktop.TaskViewHook
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
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.LockScreenAllowAdjustVolume
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.LockScreenClockFont
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.LockScreenClockPeriod
import dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia.LockScreenClockSeconds
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
import dev.lackluster.mihelper.hook.rules.systemui.screenoff.nubia.ScreenOffPeriodModifier
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.BatteryIconPercentSwapHook
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.Clock
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.DualRowsStatusBarHook
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
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.StatusBarTemperatureHook


object DeskTop : YukiBaseHooker() {
    override fun onHook() {
       loadHooker(CustomTextClockHook)
        //loadHooker(TaskViewHook)
        loadHooker(RecentTasksHook)

    }
}