package dev.lackluster.redmagichelper.hook.apps.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.plugintrigger.AiTriggerSpeedHook

// AI 触发器（com.zte.game.plugintrigger）专属 Hook
object NubiaPluginTrigger : YukiBaseHooker() {
    override fun onHook() {
        // AI 触发器间隔调整（模板扫描 / 点击 / 冷却）
        loadHooker(AiTriggerSpeedHook)
    }
}
