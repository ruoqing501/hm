package dev.lackluster.redmagichelper.hook.apps.nubia



import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.updatesystem.nubia.MockDeviceInfo
import dev.lackluster.redmagichelper.hook.rules.updatesystem.nubia.ScopeSystemUpdate
import dev.lackluster.redmagichelper.utils.DexKit

// 努比亚专属 Hook
object NubiaSystemUpdate : YukiBaseHooker(){
    override fun onHook() {
        // 时间选择器弹窗-支持多种时段
        loadHooker(ScopeSystemUpdate)


        // 模拟设备信息
        DexKit.initDexKit(packageParam)
        loadHooker(MockDeviceInfo)
        DexKit.closeDexKit()

    }
}
