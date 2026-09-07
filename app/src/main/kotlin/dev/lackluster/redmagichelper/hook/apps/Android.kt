package dev.lackluster.redmagichelper.hook.apps

import androidx.compose.remote.creation.log
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.android.DisablePinVerifyPer72h
import dev.lackluster.redmagichelper.hook.rules.android.DisableThermal
import dev.lackluster.redmagichelper.hook.rules.android.nubia.AirplaneMode
import dev.lackluster.redmagichelper.hook.rules.android.nubia.AllowUntrustedTouches
import dev.lackluster.redmagichelper.hook.rules.android.nubia.BlockScreenOnNotificationSound
import dev.lackluster.redmagichelper.hook.rules.android.nubia.BlockTelemetryService
//import dev.lackluster.redmagichelper.hook.rules.android.nubia.CpuFreezerHook
import dev.lackluster.redmagichelper.hook.rules.android.nubia.DisableFlagSecureHooker
import dev.lackluster.redmagichelper.hook.rules.android.nubia.DisableFlagSecureHookerZygoteInit
import dev.lackluster.redmagichelper.hook.rules.android.nubia.LockScreenTimeoutHook
import dev.lackluster.redmagichelper.hook.rules.android.nubia.NubiaDisableSystemSignatureVerification
import dev.lackluster.redmagichelper.hook.rules.android.nubia.PowerWakeupAssist
import dev.lackluster.redmagichelper.hook.rules.android.nubia.RemoveAlertWindowsNotification
import dev.lackluster.redmagichelper.hook.rules.android.nubia.RmIntentHijack
import dev.lackluster.redmagichelper.hook.rules.android.nubia.RmWindowReplyLimits
import dev.lackluster.redmagichelper.hook.rules.android.nubia.VolumeDialogHook
import dev.lackluster.redmagichelper.hook.rules.android.nubia.VolumeStepHook

object Android : YukiBaseHooker() {
    override fun onHook() {
        // 飞行模式


//        loadHooker(DarkModeForAll)
//        loadHooker(RemoveFreeformRestriction)
//        loadHooker(AllowMoreFreeform)
//        loadHooker(FontScale)
//        return
//        loadHooker(DisableFixedOrientation)
//        loadHooker(WallpaperScaleRatio)
        // 禁用FLAG_SECURE 启用截图
        loadHooker(DisableFlagSecureHookerZygoteInit)
        loadHooker(DisableFlagSecureHooker)
//        loadHooker(Debugger)
//        loadHooker(CpuFreezerHook)

        // 小窗限制
        loadHooker(RmWindowReplyLimits)

        // 锁屏-禁用每 72 小时验证锁屏密码
        loadHooker(DisablePinVerifyPer72h)
        // 锁屏界面-屏幕超时时间
        loadHooker(LockScreenTimeoutHook)
        // 禁用温控
        loadHooker(DisableThermal)

        loadHooker(VolumeDialogHook)

        //禁用设备名称敏感词校验
        //loadHooker(AntiQues)

        // 移除上层通知显示
        loadHooker(RemoveAlertWindowsNotification)
        //亮屏时屏蔽通知声音和振动
        loadHooker(BlockScreenOnNotificationSound)

        // 去除意图劫持
        loadHooker(RmIntentHijack)

        // 允许不受信任的触摸
        loadHooker(AllowUntrustedTouches)
        // 长按电源键启动默认数字助理
        loadHooker(PowerWakeupAssist)
        // 禁用遥测服务
        loadHooker(BlockTelemetryService)
        // 飞行模式、WIFI、热点、蓝牙
        loadHooker(AirplaneMode)
        // 禁用系统签名验证
        loadHooker(NubiaDisableSystemSignatureVerification)

        // 声音步进调节
        loadHooker(VolumeStepHook)



    }
}
