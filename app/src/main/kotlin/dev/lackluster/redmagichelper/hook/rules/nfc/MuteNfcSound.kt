package dev.lackluster.redmagichelper.hook.rules.nfc


import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object MuteNfcSound : YukiBaseHooker() {
    private const val TAG = "MuteNfcSound"
    override fun onHook() {
        "com.android.nfc.NfcService".toClass().method {
            name = "playSound"
            param(IntType)
        }.hook {
            before {
                if (!Prefs.getBoolean(Pref.Key.Other.MUTE_NFC_SOUND, false)) return@before
                result = null
                YLog.debug(tag = TAG, msg = "blocked NFC Sound")
            }
        }
    }
}
