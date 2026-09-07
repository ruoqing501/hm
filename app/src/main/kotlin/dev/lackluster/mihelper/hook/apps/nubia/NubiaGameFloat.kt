package dev.lackluster.mihelper.hook.apps.nubia




import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.u9521.wooboxforredmagicos.hook.app.systemui.view.textclock.TimeTitlePeriod
import dev.lackluster.mihelper.hook.rules.clockcomponent.DigitalClockAmPmHook
import dev.lackluster.mihelper.hook.rules.gamefloat.HideGameChickenModeDialog
import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.HidePurifySwitch
import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.HideStoreHint

import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.SkipApkScan
import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.UseCtsActivity
import dev.lackluster.mihelper.hook.rules.settings.nubia.DisableUSBInstallVerification
import dev.lackluster.mihelper.hook.rules.settings.nubia.TimePickerPeriod

// 努比亚专属 Hook
object NubiaGameFloat : YukiBaseHooker(){
    override fun onHook() {
        loadHooker(HideGameChickenModeDialog)
    }
}