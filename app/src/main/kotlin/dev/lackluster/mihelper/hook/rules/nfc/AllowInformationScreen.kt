package dev.lackluster.mihelper.hook.rules.nfc



import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

// 允许息屏状态下使用NFC
object AllowInformationScreen : YukiBaseHooker() {
    private const val TAG = "AllowInformationScreen"
    override fun onHook() {
        hasEnable(Pref.Key.Other.NFC_ALLOW_SCREEN_OFF_RECOGNITION) {
            "com.android.nfc.NfcService".toClass().method {
                name = "sendMessage"
            }.hook {
                before {
                    if (args[1] == 2 || args[1] == 4) {
                        args[1] = 8
                        YLog.debug(tag = TAG, msg = "Allow NFC information screen")
                    }
                }
            }
        }
    }
}
