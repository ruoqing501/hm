package dev.lackluster.mihelper.hook.rules.nfc



import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import io.github.kyuubiran.ezxhelper.core.misc.params

// 允许息屏状态下使用NFC
object AllowInformationScreen: YukiBaseHooker() {
    private const val TAG = "AllowInformationScreen"
    override fun onHook() {
        hasEnable(Pref.Key.Other.NFC_ALLOW_SCREEN_OFF_RECOGNITION){
            "com.android.nfc.NfcService".toClass().method {
                name = "sendMessage"
            }.hook(){
                before {
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
}