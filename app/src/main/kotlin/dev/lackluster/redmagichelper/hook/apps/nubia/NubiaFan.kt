package dev.lackluster.redmagichelper.hook.apps.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.nubiafan.FanControlHook

// 努比亚风扇应用(cn.nubia.fan)聚合 Hook
object NubiaFan : YukiBaseHooker() {
    override fun onHook() {
        // 风扇档位控制(固定转速/解限最高档/转速校准)
        // 安装期不做开关门控:控制器在轮询中运行时重读 Prefs,开关切换即时生效
        loadHooker(FanControlHook)
    }
}
