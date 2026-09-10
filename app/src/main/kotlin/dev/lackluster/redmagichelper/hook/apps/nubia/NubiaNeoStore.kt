package dev.lackluster.redmagichelper.hook.apps.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.neostore.StoreDownloadHook

// 努比亚应用中心
object NubiaNeoStore : YukiBaseHooker() {
    override fun onHook() {
        // 应用商店同时下载数量
        loadHooker(StoreDownloadHook)
    }
}
