package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.os.PowerManager
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.IntClass
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

object VolumeStepHook : YukiBaseHooker() {

    private const val TAG = "VolumeStep[systemProperties]"

    // 开关
    private val alarmSwitch by lazy { Prefs.getBoolean(Pref.Key.Android.ALARM_CLOCK_VOLUME_LEVEL_SWITCH, false) }
    private val mediaSwitch by lazy { Prefs.getBoolean(Pref.Key.Android.MEDIA_CLOCK_VOLUME_LEVEL_SWITCH, false) }
    private val notifySwitch by lazy { Prefs.getBoolean(Pref.Key.Android.NOTIFICATION_CLOCK_VOLUME_LEVEL_SWITCH, false) }
    private val ringSwitch by lazy { Prefs.getBoolean(Pref.Key.Android.RING_CLOCK_VOLUME_LEVEL_SWITCH, false) }
    private val voiceSwitch by lazy { Prefs.getBoolean(Pref.Key.Android.VOICE_CLOCK_VOLUME_LEVEL_SWITCH, false) }

    // 步数数值
    private val alarmSteps by lazy { Prefs.getInt(Pref.Key.Android.ALARM_CLOCK_VOLUME_LEVEL, 15) }
    private val mediaSteps by lazy { Prefs.getInt(Pref.Key.Android.MEDIA_CLOCK_VOLUME_LEVEL, 15) }
    private val notifySteps by lazy { Prefs.getInt(Pref.Key.Android.NOTIFICATION_CLOCK_VOLUME_LEVEL, 15) }
    private val ringSteps by lazy { Prefs.getInt(Pref.Key.Android.RING_CLOCK_VOLUME_LEVEL, 15) }
    private val voiceSteps by lazy { Prefs.getInt(Pref.Key.Android.VOICE_CLOCK_VOLUME_LEVEL, 15) }

    override fun onHook() {
        // 获取 SystemProperties 类及其 getInt 方法
        val systemProperties = "android.os.SystemProperties".toClassOrNull()
        if (systemProperties == null) {
            YLog.debug("$TAG systemProperties is null")
            return
        }
        YLog.debug("$TAG found systemProperties")

        val getIntMethod = systemProperties.method {
            name = "getInt"
            param(StringClass, IntClass)
        }

        // 获取 AudioService 的 8 参数构造函数
        val audioServiceClass = "com.android.server.audio.AudioService".toClassOrNull()
        if (audioServiceClass == null) {
            YLog.debug("$TAG AudioService class not found")
            return
        }
        val audioServerConstructor = audioServiceClass.constructors.firstOrNull { it.parameterCount == 8 }
            ?: audioServiceClass.constructors.firstOrNull { it.parameterCount == 11 }
        if (audioServerConstructor == null) {
            YLog.debug("$TAG AudioService 8-param constructor not found")
            return
        }

        // Hook AudioService 构造函数，在其执行期间临时启用 SystemProperties.getInt 的 Hook
        audioServerConstructor.hook {

            before {
                YLog.debug("$TAG audioServer initializing, temporary hook SystemProperties.getInt")
                getIntMethod.hook {
                    before {
                        when (args[0] as String) {
                            "ro.config.alarm_vol_steps" -> if (alarmSwitch) result = alarmSteps
                            "ro.config.media_vol_steps" -> if (mediaSwitch) result = mediaSteps * 10  // zte specific
                            "ro.config.notify_vol_steps" -> if (notifySwitch) result = notifySteps
                            "ro.config.ring_vol_steps" -> if (ringSwitch) result = ringSteps
                            "ro.config.vc_call_vol_steps" -> if (voiceSwitch) result = voiceSteps
                        }
                    }
                }
            }

            after {
                removeSelf() // 移除临时 Hook
                YLog.debug("$TAG audioServer initialized, temporary hook removed")
            }
        }
    }
}