package dev.lackluster.redmagichelper.hook.apps.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.mihealth.MiHealthHook

// 小米运动健康(包名 com.mi.health,经应用双开/安装在红魔设备上运行)
object NubiaMiHealth : YukiBaseHooker() {
    override fun onHook() {
        // 步数增强:安装期不做 hasEnable 门控(与 FanControlHook 同理)——
        // 所有行为在 hook 内经 Prefs 运行时重读,开关修改无需重启健康应用;
        // 功能本身 fail-closed(兼容自检未通过/未绑定账户时不改写任何数据)
        loadHooker(MiHealthHook)
    }
}
