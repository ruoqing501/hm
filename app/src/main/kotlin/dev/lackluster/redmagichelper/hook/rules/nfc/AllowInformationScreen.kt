package dev.lackluster.redmagichelper.hook.rules.nfc



import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

// 允许息屏状态下使用NFC
object AllowInformationScreen : YukiBaseHooker() {
    private const val TAG = "AllowInformationScreen"
    override fun onHook() {
        "com.android.nfc.NfcService".toClass().method {
            name = "sendMessage"
        }.hook {
            before {
                if (!Prefs.getBoolean(Pref.Key.Other.NFC_ALLOW_SCREEN_OFF_RECOGNITION, false)) return@before
                if (args[1] == 2 || args[1] == 4) {
                    args[1] = 8
                    YLog.debug(tag = TAG, msg = "Allow NFC information screen")
                }
            }
        }
    }
}
