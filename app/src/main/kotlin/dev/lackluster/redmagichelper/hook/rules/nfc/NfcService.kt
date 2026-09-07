package dev.lackluster.redmagichelper.hook.rules.nfc

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia.DualRowsStatusBarHook

object NfcService: YukiBaseHooker() {
    override fun onHook() {
        // 禁用NFC提示音
        loadHooker(MuteNfcSound)
        // 允许息屏状态下使用NFC
        loadHooker(AllowInformationScreen)
    }
}