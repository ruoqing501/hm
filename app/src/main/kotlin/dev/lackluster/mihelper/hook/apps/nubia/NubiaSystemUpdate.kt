package dev.lackluster.mihelper.hook.apps.nubia



import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.rules.updatesystem.nubia.MockDeviceInfo
import dev.lackluster.mihelper.hook.rules.updatesystem.nubia.ScopeSystemUpdate
import dev.lackluster.mihelper.utils.DexKit

// 努比亚专属 Hook
object NubiaSystemUpdate : YukiBaseHooker(){
    override fun onHook() {
        // 时间选择器弹窗-支持多种时段
        loadHooker(ScopeSystemUpdate)


        // 模拟设备信息
        DexKit.initDexKit(this)
        loadHooker(MockDeviceInfo)
        DexKit.closeDexKit()

    }
}