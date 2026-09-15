package dev.lackluster.redmagichelper.hook.apps.nubia


import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.gameassist.ActiveMode
import dev.lackluster.redmagichelper.hook.rules.gameassist.AiTriggerYoloScan
import dev.lackluster.redmagichelper.hook.rules.gameassist.HideGameChickenModeDialog
import dev.lackluster.redmagichelper.hook.rules.gameassist.NubiaSuperResolution

// 努比亚专属 Hook
object NubiaGameAssist : YukiBaseHooker(){
    override fun onHook() {
        // 启用超境模式（总开关在各 hooker 回调内实时判断）
        loadHooker(HideGameChickenModeDialog)
        loadHooker(NubiaSuperResolution)



        // 活跃模式(根据用户设置决定，避免状态被重置，避免下次进入游戏的时候默认为关闭状态)
        loadHooker(ActiveMode)

        // AI 触发器间隔调整（YOLO 扫描）
        loadHooker(AiTriggerYoloScan)

        //
        //// 随心录制
        //loadHooker(NubiaRecordFreely)
        //// 随心显示（解锁画面比例）
        //loadHooker(NubiaGameRadio)
        //
        //// 肩键界面
        //loadHooker(NubiaGameAssistDebug)

    }
}
