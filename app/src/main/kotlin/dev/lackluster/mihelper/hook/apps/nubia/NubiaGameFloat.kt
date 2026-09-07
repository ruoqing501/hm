package dev.lackluster.mihelper.hook.apps.nubia




import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.rules.gamefloat.HideGameChickenModeDialog


// 努比亚专属 Hook
object NubiaGameFloat : YukiBaseHooker(){
    override fun onHook() {
        loadHooker(HideGameChickenModeDialog)
    }
}
