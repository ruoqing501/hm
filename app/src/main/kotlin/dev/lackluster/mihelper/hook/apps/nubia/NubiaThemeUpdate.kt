package dev.lackluster.mihelper.hook.apps.nubia

import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.rules.themes.nubia.ScopeThemeUpdate

// 努比亚专属 Hook
object NubiaThemeUpdate : YukiBaseHooker(){
    override fun onHook() {
        loadHooker(ScopeThemeUpdate)

    }
}
