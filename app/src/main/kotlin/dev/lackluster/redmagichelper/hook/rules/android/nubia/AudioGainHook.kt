package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.utils.AudioGainPolicy
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 音量增益（移植自 LS_Augment 的 AudioGainHook）：
 * 在原声道上达到系统最大音量后，按键改为 ADJUST_SAME，超出部分由 LoudnessEnhancer 施加 DSP 增益。
 * 所有原生权限与静音路径仍照常执行；反射找不到类/方法时静默降级而不是崩溃。
 */
object AudioGainHook : YukiBaseHooker() {

    private const val TAG = "AudioGainHook"

    private val internalCall = ThreadLocal.withInitial { false }
    private val pending = ThreadLocal<Adjustment?>()

    @Volatile
    private var controller: Controller? = null

    private class Adjustment {
        var stream = 0
        var requested = 0
        var rawRequested = 0
        var authorized = false
        var dndAllowed = false
        var route = ""
    }

    override fun onHook() {
        hasEnable(Pref.Key.AudioGain.ENABLE) { install() }
    }

    private fun install() {
        val serviceClass = "com.android.server.audio.AudioService".toClassOrNull()
        if (serviceClass == null) {
            YLog.error(tag = TAG, msg = "AudioService class not found")
            return
        }

        // 记录音量调整期间的权限校验结果
        serviceClass.method {
            name = "checkNoteAppOp"
            returnType = BooleanType
        }.hook {
            after {
                pending.get()?.let { it.authorized = (result as? Boolean) == true }
            }
        }
        // 记录音量调整期间的勿扰校验结果
        serviceClass.method {
            name = "volumeAdjustmentAllowedByDnd"
            paramCount = 2
        }.hook {
            after {
                pending.get()?.let { it.dndAllowed = (result as? Boolean) == true }
            }
        }

        // 播放配置变化时刷新活动播放器列表
        serviceClass.method {
            name = "onPlaybackConfigChange"
            paramCount = 1
        }.hook {
            after {
                val c = ensureController(instanceOrNull) ?: return@after
                if (internalCall.get() == true) return@after
                (args(0).any() as? List<*>)?.let { c.playback(ArrayList(it)) }
            }
        }

        // 对外暴露扩展后的量程/音量值
        for (getter in arrayOf("getStreamVolume", "getStreamMaxVolume", "getLastAudibleStreamVolume")) {
            serviceClass.method {
                name = getter
                paramCount = 1
            }.hook {
                after {
                    val c = ensureController(instanceOrNull) ?: return@after
                    if (internalCall.get() == true || pending.get() != null) return@after
                    val streamType = args(0).int()
                    if (!AudioGainPolicy.streamSupported(streamType) || !c.enabled() || !trusted(c.context)) return@after
                    val route = c.route(streamType)
                    val nativeMax = c.nativeVolume("getStreamMaxVolume", streamType)
                    if (nativeMax <= 0 || route.isEmpty()) return@after
                    val maximum = c.maxSteps(route, streamType)
                    val base = (result as? Int) ?: return@after
                    if (getter == "getStreamMaxVolume") {
                        result = base + maximum
                    } else if (base != 0) {
                        result = base + min(maximum, c.extra(route, streamType))
                    }
                }
            }
        }

        // 音量键调整：超过原生上限时改写为 ADJUST_SAME，增益由 LoudnessEnhancer 承担
        serviceClass.method {
            name = "adjustStreamVolume"
            paramCount = 10
        }.hook {
            before {
                val c = ensureController(instanceOrNull) ?: return@before
                if (internalCall.get() == true) return@before
                val streamType = args(0).int()
                if (!AudioGainPolicy.streamSupported(streamType) || !c.enabled() || !trusted(c.context)) return@before
                val route = c.route(streamType)
                val nativeMax = c.nativeVolume("getStreamMaxVolume", streamType)
                if (nativeMax <= 0 || route.isEmpty()) return@before
                val direction = args(1).int()
                if (direction != AudioManager.ADJUST_RAISE && direction != AudioManager.ADJUST_LOWER) return@before
                val nativeVolume = c.nativeVolume("getStreamVolume", streamType)
                val extra = c.extra(route, streamType)
                if (nativeVolume < nativeMax || (direction < 0 && extra == 0)) {
                    c.setExtra(route, streamType, 0)
                    return@before
                }
                val adjustment = Adjustment()
                adjustment.stream = streamType
                adjustment.route = route
                adjustment.requested = max(0, min(c.maxSteps(route, streamType), extra + direction))
                // ADJUST_SAME 仍会走 ROM 的策略与 UI 更新，但不会改变硬件档位
                args(1).set(AudioManager.ADJUST_SAME)
                pending.set(adjustment)
            }
            after {
                val adjustment = pending.get() ?: return@after
                pending.remove()
                val c = ensureController(instanceOrNull) ?: return@after
                if (adjustment.authorized && adjustment.dndAllowed &&
                    c.nativeVolume("getStreamVolume", adjustment.stream) == c.nativeVolume("getStreamMaxVolume", adjustment.stream) &&
                    adjustment.route == c.route(adjustment.stream)
                ) {
                    c.setExtra(adjustment.route, adjustment.stream, adjustment.requested)
                }
                c.notifyPanel(adjustment.stream, args(2).int())
            }
        }

        // 直接设置音量（拖动音量条）：超出原生上限的部分转为增益
        serviceClass.method {
            name = "setStreamVolume"
            paramCount = 10
        }.hook {
            before {
                val c = ensureController(instanceOrNull) ?: return@before
                if (internalCall.get() == true) return@before
                val streamType = args(0).int()
                if (!AudioGainPolicy.streamSupported(streamType) || !c.enabled() || !trusted(c.context)) return@before
                val route = c.route(streamType)
                val nativeMax = c.nativeVolume("getStreamMaxVolume", streamType)
                if (nativeMax <= 0 || route.isEmpty()) return@before
                val requested = args(1).int()
                val adjustment = Adjustment()
                adjustment.stream = streamType
                adjustment.route = route
                adjustment.rawRequested = requested
                adjustment.requested = max(0, min(c.maxSteps(route, streamType), requested - nativeMax))
                args(1).set(min(requested, nativeMax))
                pending.set(adjustment)
            }
            after {
                val adjustment = pending.get() ?: return@after
                pending.remove()
                val c = ensureController(instanceOrNull) ?: return@after
                if (adjustment.authorized && adjustment.dndAllowed &&
                    c.nativeVolume("getStreamVolume", adjustment.stream) == c.nativeVolume("getStreamMaxVolume", adjustment.stream) &&
                    adjustment.route == c.route(adjustment.stream)
                ) {
                    c.setExtra(adjustment.route, adjustment.stream, adjustment.requested)
                } else if (adjustment.authorized && adjustment.dndAllowed &&
                    adjustment.rawRequested <= c.nativeVolume("getStreamMaxVolume", adjustment.stream)
                ) {
                    c.setExtra(adjustment.route, adjustment.stream, 0)
                }
                c.notifyPanel(adjustment.stream, args(2).int())
            }
        }
        YLog.info(tag = TAG, msg = "AUDIO_GAIN_READY")
    }

    @Synchronized
    private fun ensureController(owner: Any?): Controller? {
        controller?.let { return it }
        val context = field(owner, "mContext") as? Context ?: return null
        if (owner == null) return null
        return Controller(owner, context).also { controller = it }
    }

    private fun trusted(context: Context): Boolean {
        val uid = Binder.getCallingUid()
        if (uid == 0 || uid == Process.SYSTEM_UID) return true
        val names = runCatching { context.packageManager.getPackagesForUid(uid) }.getOrNull() ?: return false
        return names.contains("dev.lackluster.redmagichelper") || names.contains("com.android.systemui")
    }

    private fun field(owner: Any?, name: String): Any? {
        if (owner == null) return null
        var clazz: Class<*>? = owner.javaClass
        while (clazz != null) {
            try {
                val f = clazz.getDeclaredField(name)
                f.isAccessible = true
                return f.get(owner)
            } catch (ignored: Throwable) {
            }
            clazz = clazz.superclass
        }
        return null
    }

    @Throws(ReflectiveOperationException::class)
    private fun invoke(owner: Any, name: String, vararg args: Any?): Any? {
        var clazz: Class<*>? = owner.javaClass
        while (clazz != null) {
            for (method in clazz.declaredMethods) {
                if (method.name == name && method.parameterCount == args.size) {
                    try {
                        method.isAccessible = true
                        return method.invoke(owner, *args)
                    } catch (wrongOverload: IllegalArgumentException) {
                    }
                }
            }
            clazz = clazz.superclass
        }
        throw NoSuchMethodException(name)
    }

    private class Controller(val service: Any, val context: Context) {
        private val handler: Handler
        private val extras = ConcurrentHashMap<String, Int>()
        private val effects = HashMap<Int, LoudnessEnhancer>()
        private var players: List<*> = emptyList<Any?>()
        private var lastError = ""

        private val tick = object : Runnable {
            override fun run() {
                apply()
                handler.postDelayed(this, 1000)
            }
        }

        init {
            val thread = HandlerThread("RMH-audio-gain", Process.THREAD_PRIORITY_BACKGROUND)
            thread.start()
            handler = Handler(thread.looper)
            handler.postDelayed(tick, 1000)
        }

        fun enabled(): Boolean = Prefs.getBoolean(Pref.Key.AudioGain.ENABLE, false)
        private fun step(): Int = AudioGainPolicy.step()
        private fun limit(route: String, streamType: Int): Int = AudioGainPolicy.limit(route, streamType)
        fun maxSteps(route: String, streamType: Int): Int = AudioGainPolicy.extraSteps(limit(route, streamType), step())

        private fun id(route: String, streamType: Int): String = "$route:$streamType"
        fun extra(route: String, streamType: Int): Int = min(extras[id(route, streamType)] ?: 0, maxSteps(route, streamType))
        fun setExtra(route: String, streamType: Int, value: Int) {
            extras[id(route, streamType)] = value
            handler.post { apply() }
        }

        fun nativeVolume(method: String, streamType: Int): Int {
            val old = internalCall.get()
            internalCall.set(true)
            return try {
                invoke(service, method, streamType) as? Int ?: -1
            } catch (t: Throwable) {
                -1
            } finally {
                internalCall.set(old)
            }
        }

        fun route(streamType: Int): String = try {
            val device = invoke(service, "getDeviceForStream", streamType) as? Int ?: return ""
            AudioGainPolicy.routeForDevice(device)
        } catch (t: Throwable) {
            ""
        }

        fun notifyPanel(streamType: Int, flags: Int) {
            handler.post {
                runCatching {
                    val volumeController = field(service, "mVolumeController") ?: return@runCatching
                    invoke(volumeController, "postVolumeChanged", streamType, flags)
                }
            }
        }

        fun playback(configs: List<*>) {
            handler.post {
                players = configs
                apply()
            }
        }

        fun apply() {
            if (!enabled()) extras.clear()
            val wanted = HashSet<Int>()
            if (enabled()) for (player in players) {
                if (player == null) continue
                try {
                    if (invoke(player, "isActive") != true) continue
                    val attributes = invoke(player, "getAudioAttributes") as? AudioAttributes ?: continue
                    val streamType = when (attributes.usage) {
                        AudioAttributes.USAGE_ALARM -> AudioManager.STREAM_ALARM
                        AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> AudioManager.STREAM_RING
                        AudioAttributes.USAGE_MEDIA, AudioAttributes.USAGE_GAME, AudioAttributes.USAGE_UNKNOWN -> AudioManager.STREAM_MUSIC
                        else -> -1
                    }
                    if (streamType < 0) continue
                    val route = route(streamType)
                    if (route.isEmpty()) continue
                    val current = nativeVolume("getStreamVolume", streamType)
                    val maximum = nativeVolume("getStreamMaxVolume", streamType)
                    if (current in 1 until maximum) extras.remove(id(route, streamType))
                    val extra = extra(route, streamType)
                    if (extra == 0 || current <= 0) continue
                    val session = invoke(player, "getSessionId") as? Int ?: continue
                    if (session <= 0) continue
                    val gain = AudioGainPolicy.milliBel(AudioGainPolicy.percent(extra, step(), limit(route, streamType)))
                    var effect = effects[session]
                    if (effect == null) {
                        effect = LoudnessEnhancer(session)
                        effects[session] = effect
                    }
                    if (!effect.hasControl()) throw IllegalStateException("gain effect occupied by another app")
                    effect.setTargetGain(gain)
                    effect.enabled = true
                    if (!effect.enabled || abs(effect.targetGain - gain) > 1) throw IllegalStateException("audio session rejected the gain")
                    wanted.add(session)
                } catch (t: Throwable) {
                    val error = t.javaClass.simpleName + ":" + t.message
                    if (error != lastError) {
                        lastError = error
                        YLog.warn(tag = TAG, msg = "apply gain failed: $error")
                    }
                }
            }
            val iterator = effects.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key !in wanted) {
                    entry.value.release()
                    iterator.remove()
                }
            }
        }
    }
}
