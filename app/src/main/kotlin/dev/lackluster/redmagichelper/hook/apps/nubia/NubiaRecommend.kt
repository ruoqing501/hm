package dev.lackluster.redmagichelper.hook.apps.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.recommend.WindowReplyIconLimit

// com.zte.recommend（小窗图标宿主）
object NubiaRecommend : YukiBaseHooker() {
    override fun onHook() {
        // 解除挂起小窗图标的数量上限
        loadHooker(WindowReplyIconLimit)
    }
}
