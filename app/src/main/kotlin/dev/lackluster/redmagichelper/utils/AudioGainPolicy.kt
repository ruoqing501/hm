package dev.lackluster.redmagichelper.utils

import android.media.AudioManager
import dev.lackluster.redmagichelper.data.Pref
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 音量增益的纯逻辑策略，供 system_server 与 SystemUI 两侧的 hook 共用。
 * 移植自 LS_Augment 的 AudioGainPolicy：扩展出的音量档位代表 DSP 增益，而不是伪造的硬件档位。
 */
object AudioGainPolicy {
    const val ROUTE_SPEAKER = "speaker"
    const val ROUTE_WIRED = "wired"
    const val ROUTE_BLUETOOTH = "bluetooth"

    fun streamSupported(streamType: Int): Boolean =
        streamType == AudioManager.STREAM_MUSIC ||
                streamType == AudioManager.STREAM_RING ||
                streamType == AudioManager.STREAM_ALARM

    fun limitKey(route: String, streamType: Int): String? = when (route) {
        ROUTE_SPEAKER -> when (streamType) {
            AudioManager.STREAM_MUSIC -> Pref.Key.AudioGain.LIMIT_SPEAKER_MEDIA
            AudioManager.STREAM_RING -> Pref.Key.AudioGain.LIMIT_SPEAKER_RING
            AudioManager.STREAM_ALARM -> Pref.Key.AudioGain.LIMIT_SPEAKER_ALARM
            else -> null
        }
        ROUTE_WIRED -> when (streamType) {
            AudioManager.STREAM_MUSIC -> Pref.Key.AudioGain.LIMIT_WIRED_MEDIA
            AudioManager.STREAM_RING -> Pref.Key.AudioGain.LIMIT_WIRED_RING
            AudioManager.STREAM_ALARM -> Pref.Key.AudioGain.LIMIT_WIRED_ALARM
            else -> null
        }
        ROUTE_BLUETOOTH -> when (streamType) {
            AudioManager.STREAM_MUSIC -> Pref.Key.AudioGain.LIMIT_BLUETOOTH_MEDIA
            AudioManager.STREAM_RING -> Pref.Key.AudioGain.LIMIT_BLUETOOTH_RING
            AudioManager.STREAM_ALARM -> Pref.Key.AudioGain.LIMIT_BLUETOOTH_ALARM
            else -> null
        }
        else -> null
    }

    fun limit(route: String, streamType: Int): Int {
        val key = limitKey(route, streamType) ?: return 100
        return Prefs.getInt(key, 100).coerceIn(100, 300)
    }

    fun step(): Int = Prefs.getInt(Pref.Key.AudioGain.STEP, 5).coerceIn(1, 20)

    fun extraSteps(limit: Int, step: Int): Int =
        if (limit < 100 || limit > 300 || step < 1 || step > 20) 0
        else (limit - 100 + step - 1) / step

    fun percent(extra: Int, step: Int, limit: Int): Int =
        min(max(100, limit), 100 + max(0, extra) * max(1, step))

    fun milliBel(percent: Int): Int =
        (2000 * log10(max(100, min(300, percent)) / 100.0)).roundToInt()

    /** 将 AudioSystem 输出设备归类为输出路由。常量是安卓音频设备定义，与机型/固件无关。 */
    fun routeForDevice(device: Int): String = runCatching {
        val audioSystem = Class.forName("android.media.AudioSystem")
        for (setName in arrayOf("DEVICE_OUT_ALL_A2DP_SET", "DEVICE_OUT_ALL_BLE_SET", "DEVICE_OUT_ALL_SCO_SET")) {
            val field = audioSystem.getDeclaredField(setName)
            field.isAccessible = true
            val set = field.get(null)
            if (set is Set<*> && set.contains(device)) return@runCatching ROUTE_BLUETOOTH
        }
        // AudioSystem.DEVICE_OUT_SPEAKER
        if (device == 2) return@runCatching ROUTE_SPEAKER
        // WIRED_HEADSET / WIRED_HEADPHONE / USB_ACCESSORY / USB_DEVICE / USB_HEADSET
        if (device == 4 || device == 8 || device == 8192 || device == 16384 || device == 67108864) return@runCatching ROUTE_WIRED
        ""
    }.getOrDefault("")
}
