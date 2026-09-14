package dev.lackluster.redmagichelper.hook.apps.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.baiduime.ClipboardNoLimit

// 百度输入法定制版(com.baidu.input_oem)聚合 Hook
object NubiaBaiduIme : YukiBaseHooker() {
    override fun onHook() {
        // 剪贴板复制/粘贴字数与条数限制解除
        loadHooker(ClipboardNoLimit)
    }
}
