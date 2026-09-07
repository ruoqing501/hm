package dev.lackluster.redmagichelper.hook.rules.android.nubia
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.android.ViewGroupClass
import dev.lackluster.redmagichelper.hook.compat.type.java.LongType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import java.io.FileInputStream
import java.util.Locale
import kotlin.math.abs
object LockScreenTimeoutHook : YukiBaseHooker() {
    private const val TAG = "LockScreenTimeoutHook"

    // 开关
    private val enable by lazy {
        Prefs.getBoolean(Pref.Key.Android.SYSTEM_FRAMEWORK_LOCK_SCREEN_TIMEOUT, false)
    }
    // 自定义超时（秒）最小10秒
    private val customTimeoutS by lazy {
        Prefs.getInt(Pref.Key.Android.SYSTEM_FRAMEWORK_LOCK_SCREEN_TIMEOUT_VALUE, 10)
    }

    // 自定义超时（毫秒）
    private val customTimeoutMs = customTimeoutS * 1000L

    override fun onHook() {
        if (!enable) return
        "com.android.server.power.PowerManagerService".toClass().method {
            name = "getScreenOffTimeoutLocked"
            param(LongType, LongType)
            returnType = LongType
        }.hook {
            after {
                val original = result as Long
                // 获取 PowerManagerService 的 Context 字段
                val context = instance.current().field {
                        name = "mContext"
                    }.any() as? Context

                val isLocked = context?.let {
                    val km = it.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    km.isKeyguardLocked
                } ?: false

                if (isLocked) {
                    // 获取系统最小超时限制，默认 10000ms（10 秒）
                    //val minTimeout = instance.current().field {
                    //    name = "mMinimumScreenOffTimeoutConfig"
                    //}.any() as? Long ?: 100000000L
                    //val minTimeout = customTimeoutMs
                    //val custom = maxOf(customTimeoutMs.toLong(), minTimeout)
                    //result = custom
                    result = customTimeoutMs
                    YLog.debug(tag = TAG, msg =  "Lock screen timeout overridden: $original -> $result")
                }
            }
        }
    }
}