package dev.lackluster.redmagichelper.hook.apps.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.themes.nubia.ScopeThemeAdapterTrialKeep

// 努比亚主题适配服务（com.zte.beautifyadapter，独立于 com.zte.beautify 更新）
object NubiaThemeAdapter : YukiBaseHooker() {
    override fun onHook() {
        loadHooker(ScopeThemeAdapterTrialKeep)
    }
}
