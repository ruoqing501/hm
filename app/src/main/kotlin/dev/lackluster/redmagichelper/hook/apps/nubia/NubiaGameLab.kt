package dev.lackluster.redmagichelper.hook.apps.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.gamelab.AiTriggerSpeedHook

// GameLab（cn.nubia.gamelab）专属 Hook
object NubiaGameLab : YukiBaseHooker() {
    override fun onHook() {
        // AI 触发器间隔调整（GameLab 模板扫描）
        loadHooker(AiTriggerSpeedHook)
    }
}
