package dev.lackluster.mihelper.hook.rules.android.nubia




import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
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