package dev.lackluster.redmagichelper.hook.apps.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.themes.nubia.ScopeThemeUpdate
import dev.lackluster.redmagichelper.hook.rules.themes.nubia.ScopeThemeTrialKeep

// 努比亚专属 Hook
object NubiaThemeUpdate : YukiBaseHooker(){
    override fun onHook() {
        loadHooker(ScopeThemeUpdate)
        loadHooker(ScopeThemeTrialKeep)

    }
}
