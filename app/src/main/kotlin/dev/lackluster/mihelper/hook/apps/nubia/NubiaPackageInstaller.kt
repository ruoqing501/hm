package dev.lackluster.mihelper.hook.apps.nubia

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.HidePurifySwitch
import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.HideStoreHint

import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.SkipApkScan
import dev.lackluster.mihelper.hook.rules.packageinstaller.nubia.UseCtsActivity

// 努比亚专属 Hook
object NubiaPackageInstaller : YukiBaseHooker(){
    override fun onHook() {
        // 跳过APK扫描
        loadHooker(SkipApkScan)
        // 隐藏净化模式-安装界面的纯净模式的提示
        loadHooker(HidePurifySwitch)
        // 隐藏商店提示
        loadHooker(HideStoreHint)
        // 使用CTS测试的Activity
        loadHooker(UseCtsActivity)
    }
}