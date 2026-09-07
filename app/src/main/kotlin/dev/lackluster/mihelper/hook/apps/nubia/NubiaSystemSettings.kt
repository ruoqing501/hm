package dev.lackluster.mihelper.hook.apps.nubia


import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.u9521.wooboxforredmagicos.hook.app.systemui.view.textclock.TimeTitlePeriod
import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.HidePurifySwitch
import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.HideStoreHint

import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.SkipApkScan
import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.UseCtsActivity
import dev.lackluster.mihelper.hook.rules.settings.nubia.DisableBatteryPercentageDisplayItem
import dev.lackluster.mihelper.hook.rules.settings.nubia.DisableUSBInstallVerification
import dev.lackluster.mihelper.hook.rules.settings.nubia.DisplayTimeoutHook
import dev.lackluster.mihelper.hook.rules.settings.nubia.TimePickerPeriod
import dev.lackluster.mihelper.hook.rules.settings.nubia.UsbModeChoose

// 努比亚专属 Hook
object NubiaSystemSettings : YukiBaseHooker(){
    override fun onHook() {
        // 时间选择器弹窗-支持多种时段
        loadHooker(TimePickerPeriod)
        // 时间副标题-支持多种时段
        loadHooker(TimeTitlePeriod)
        // 禁用USB安装开关账户验证
        loadHooker(DisableUSBInstallVerification)
        // 禁用电池百分比显示选项
        loadHooker(DisableBatteryPercentageDisplayItem)

        loadHooker(UsbModeChoose)

        loadHooker(DisplayTimeoutHook)
    }
}