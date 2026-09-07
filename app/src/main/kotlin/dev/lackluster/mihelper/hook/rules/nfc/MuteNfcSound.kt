package dev.lackluster.mihelper.hook.rules.nfc


import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import io.github.kyuubiran.ezxhelper.core.misc.params

object MuteNfcSound: YukiBaseHooker() {
    private const val TAG = "MuteNfcSound"
    override fun onHook() {
        hasEnable(Pref.Key.Other.MUTE_NFC_SOUND){
             "com.android.nfc.NfcService".toClass().method {
                 name = "playSound"
                 params(IntType)
             }.hook(){
                 before {
                     result = null
                     YLog.debug(tag = TAG, msg = "blocked NFC Sound")
                 }
             }
        }
    }
}