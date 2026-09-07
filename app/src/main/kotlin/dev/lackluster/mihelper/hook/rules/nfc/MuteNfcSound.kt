package dev.lackluster.mihelper.hook.rules.nfc


import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object MuteNfcSound : YukiBaseHooker() {
    private const val TAG = "MuteNfcSound"
    override fun onHook() {
        hasEnable(Pref.Key.Other.MUTE_NFC_SOUND) {
            "com.android.nfc.NfcService".toClass().method {
                name = "playSound"
                param(IntType)
            }.hook {
                before {
                    result = null
                    YLog.debug(tag = TAG, msg = "blocked NFC Sound")
                }
            }
        }
    }
}
