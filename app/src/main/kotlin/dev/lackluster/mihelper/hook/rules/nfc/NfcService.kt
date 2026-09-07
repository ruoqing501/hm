package dev.lackluster.mihelper.hook.rules.nfc

import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia.DualRowsStatusBarHook

object NfcService: YukiBaseHooker() {
    override fun onHook() {
        // 禁用NFC提示音
        loadHooker(MuteNfcSound)
        // 允许息屏状态下使用NFC
        loadHooker(AllowInformationScreen)
    }
}