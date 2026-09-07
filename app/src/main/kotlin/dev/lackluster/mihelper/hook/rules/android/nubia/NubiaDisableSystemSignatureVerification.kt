package dev.lackluster.mihelper.hook.rules.android.nubia




import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object NubiaDisableSystemSignatureVerification : YukiBaseHooker() {
    private const val TAG = "NubiaDisableSystemSignatureVerification"


    override fun onHook() {
        hasEnable(Pref.Key.Android.ANDROID_DISABLE_SYSTEM_SIGNATURE_VERIFICATION) {
            YLog.debug("$TAG start")
            "android.util.apk.ApkSignatureVerifier".toClassOrNull()?.method {
                name = "getMinimumSignatureSchemeVersionForTargetSdk"
                param(IntType)
            }?.hook{
                replaceTo(1)
                YLog.debug("$TAG success")
            }
        }
    }
}