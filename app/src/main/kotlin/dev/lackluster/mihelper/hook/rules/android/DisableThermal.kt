package dev.lackluster.mihelper.hook.rules.android


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.SparseArray
import android.view.View
import androidx.core.content.ContextCompat
import com.highcapable.kavaref.KavaRef
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Constants.ACTION_SCREENSHOT
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import java.util.WeakHashMap
// 禁用温控
object DisableThermal : YukiBaseHooker() {
    private const val TAG = "DisableThermal"
    override fun onHook() {
        hasEnable(Pref.Key.Android.SYSTEM_FRAMEWORK_OTHER_DISABLE_THERMAL){
                val kavaRef = "com.android.server.power.ThermalManagerService".toClass().resolve()
                hookOnTemperatureChanged(kavaRef)
        }
    }

    /** Hook onTemperatureChanged 方法 */
    private fun hookOnTemperatureChanged(kavaRef: KavaRef.MemberScope<Any>) = kavaRef.apply {
        firstMethod {
            name = "onTemperatureChanged"
            parameters("android.os.Temperature", Boolean::class)
        }.hook {
            before {
                // 阻止原方法执行
                intercept()
            }
        }
    }
}
