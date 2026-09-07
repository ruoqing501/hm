package dev.lackluster.redmagichelper.hook.apps.nubia



import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.gameassist.NubiaGameRadio
import dev.lackluster.redmagichelper.hook.rules.gameassist.NubiaRecordFreely
import dev.lackluster.redmagichelper.hook.rules.gameassist.NubiaGameAssistDebug


// 努比亚专属 Hook
object NubiaGameAssist2 : YukiBaseHooker(){
    override fun onHook() {
        //// 启用超境模式
        //hasEnable(Pref.Key.GameSpace.GAME_SPACE_SUPER_RESOLUTION_SWITCH){
        //    loadHooker(HideGameChickenModeDialog)
        //    loadHooker(NubiaSuperResolution)
        //}

        // 随心录制
        loadHooker(NubiaRecordFreely)
        // 随心显示（解锁画面比例）
        loadHooker(NubiaGameRadio)

        // 肩键界面
        loadHooker(NubiaGameAssistDebug)



    }
}
