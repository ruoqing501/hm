package dev.lackluster.mihelper.hook.rules.android


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.SparseArray
import android.view.View
import androidx.core.content.ContextCompat
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Constants.ACTION_SCREENSHOT
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import java.util.WeakHashMap
// 禁用每 72 小时验证锁屏密码
object DisablePinVerifyPer72h : YukiBaseHooker() {
    private const val TAG = "DisablePinVerifyPer72h"



    override fun onHook() {
        hasEnable(Pref.Key.Android.SYSTEM_FRAMEWORK_DISABLE_72H_VERIFY){
            "com.android.server.locksettings.LockSettingsStrongAuth".toClassOrNull()?.apply {
                method {
                    name = "rescheduleStrongAuthTimeoutAlarm"
                }.hook {
                    intercept() //阻止执行
                    //YLog.debug(tag = TAG, msg = "禁用每 72 小时验证锁屏密码")
                }
            }
        }
    }
}