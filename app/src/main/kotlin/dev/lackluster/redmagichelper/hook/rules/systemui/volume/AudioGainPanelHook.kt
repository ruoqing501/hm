package dev.lackluster.redmagichelper.hook.rules.systemui.volume

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.view.View
import android.widget.TextView
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.utils.AudioGainPolicy
import dev.lackluster.redmagichelper.utils.Prefs
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * 音量面板同步（移植自 LS_Augment 的 AudioGainUiHook）：
 * 让现有音量条的量程与百分比显示和 system_server 侧扩展后的量程保持一致。
 * 所有反射失败都静默降级，不影响音量面板本身。
 */
object AudioGainPanelHook : YukiBaseHooker() {

    private const val TAG = "AudioGainPanelHook"

    private val badges = WeakHashMap<View, GainBadge>()

    /** 功能开关（回调内实时读取，切换即时生效） */
    private val enabled get() = Prefs.getBoolean(Pref.Key.AudioGain.ENABLE, false)

    override fun onHook() {
        hookPanelRange()
        hookPanelPercent()
    }

    /** 每次音量变化时，把音量条量程同步为扩展后的最大值。 */
    private fun hookPanelRange() {
        val controllerClass = "com.android.systemui.volume.VolumeDialogControllerImpl".toClassOrNull()
        if (controllerClass == null) {
            YLog.warn(tag = TAG, msg = "VolumeDialogControllerImpl not found, range sync disabled")
            return
        }
        controllerClass.method {
            name = "updateStreamLevelW"
            paramCount = 2
        }.hook {
            after {
                if (!enabled) return@after
                val streamType = args(0).int()
                if (!AudioGainPolicy.streamSupported(streamType)) return@after
                runCatching {
                    val state = invoke(instance, "streamStateW", streamType) ?: return@runCatching
                    val levelMax = state.javaClass.getDeclaredField("levelMax").apply { isAccessible = true }
                    val next = streamMaxVolume(instance, streamType) ?: return@runCatching
                    val old = levelMax.getInt(state)
                    if (old != next) {
                        levelMax.setInt(state, next)
                        if (result is Boolean) result = true
                    }
                }.onFailure { YLog.debug(tag = TAG, msg = "sync stream levelMax failed", e = it) }
            }
        }
    }

    /** 在音量条上显示换算后的百分比。 */
    private fun hookPanelPercent() {
        val dialogClass = "com.android.systemui.volume.VolumeDialogImpl".toClassOrNull()
        if (dialogClass == null) {
            YLog.warn(tag = TAG, msg = "VolumeDialogImpl not found, percent badge disabled")
            return
        }
        dialogClass.method {
            name = "updateVolumeRowH"
            paramCount = 1
        }.hook {
            after {
                runCatching {
                    val row = args(0).any() ?: return@runCatching
                    val streamType = field(row, "stream") as? Int ?: return@runCatching
                    val root = field(row, "view") as? View ?: return@runCatching
                    if (!AudioGainPolicy.streamSupported(streamType) || !Prefs.getBoolean(Pref.Key.AudioGain.ENABLE, false)) {
                        removeBadge(root)
                        return@runCatching
                    }
                    val route = currentRoute(streamType)
                    val limit = AudioGainPolicy.limit(route, streamType)
                    if (route.isEmpty() || limit <= 100) {
                        removeBadge(root)
                        return@runCatching
                    }
                    val step = AudioGainPolicy.step()
                    val context = field(instance, "mContext") as? Context ?: return@runCatching
                    val audioManager = context.getSystemService(AudioManager::class.java) ?: return@runCatching
                    // getStreamMaxVolume 已被 system_server 侧扩展，反推出原生最大值
                    val base = audioManager.getStreamMaxVolume(streamType) - AudioGainPolicy.extraSteps(limit, step)
                    if (base < 1) {
                        removeBadge(root)
                        return@runCatching
                    }
                    val state = field(row, "ss") ?: return@runCatching
                    val level = field(state, "level") as? Int ?: return@runCatching
                    val muted = (field(state, "muted") as? Boolean) == true
                    val percent = when {
                        muted -> 0
                        level <= base -> (level * 100f / base).roundToInt()
                        else -> AudioGainPolicy.percent(level - base, step, limit)
                    }
                    var badge = badges[root]
                    if (badge == null) {
                        badge = GainBadge(root, field(row, "icon") as? View)
                        badges[root] = badge
                        root.overlay.add(badge)
                    }
                    badge.text = "$percent%"
                    badge.setBounds(0, 0, maxOf(1, root.width), maxOf(1, root.height))
                    badge.invalidateSelf()
                    (field(row, "number") as? TextView)?.text = "$percent%"
                    (field(row, "header") as? TextView)?.let { header ->
                        val label = header.text.toString().replace(Regex(" · [0-9]+%$"), "")
                        header.text = "$label · $percent%"
                    }
                }.onFailure { YLog.debug(tag = TAG, msg = "update percent badge failed", e = it) }
            }
        }
    }

    private fun removeBadge(root: View) {
        badges.remove(root)?.let { root.overlay.remove(it) }
    }

    private fun streamMaxVolume(owner: Any, streamType: Int): Int? {
        runCatching { (invoke(owner, "getAudioManagerStreamMaxVolume", streamType) as? Int) }
            .getOrNull()?.let { return it }
        val audioManager = field(owner, "mAudio") as? AudioManager ?: return null
        return runCatching { audioManager.getStreamMaxVolume(streamType) }.getOrNull()
    }

    private fun currentRoute(streamType: Int): String = runCatching {
        val audioSystem = Class.forName("android.media.AudioSystem")
        val getDevice = audioSystem.getDeclaredMethod("getDeviceForStream", java.lang.Integer.TYPE)
        getDevice.isAccessible = true
        val device = (getDevice.invoke(null, streamType) as? Int) ?: 0
        AudioGainPolicy.routeForDevice(device)
    }.getOrDefault("")

    private fun field(owner: Any?, name: String): Any? {
        if (owner == null) return null
        var clazz: Class<*>? = owner.javaClass
        while (clazz != null) {
            try {
                val f = clazz.getDeclaredField(name)
                f.isAccessible = true
                return f.get(owner)
            } catch (ignored: NoSuchFieldException) {
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
                    method.isAccessible = true
                    return method.invoke(owner, *args)
                }
            }
            clazz = clazz.superclass
        }
        throw NoSuchMethodException(name)
    }

    private class GainBadge(root: View, icon: View?) : Drawable() {
        private val rootRef = WeakReference(root)
        private val iconRef = WeakReference(icon)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var text = ""

        init {
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        override fun draw(canvas: Canvas) {
            val root = rootRef.get() ?: return
            val icon = iconRef.get() ?: return
            if (text.isEmpty()) return
            val density = root.resources.displayMetrics.density
            val rootLocation = IntArray(2)
            val iconLocation = IntArray(2)
            root.getLocationOnScreen(rootLocation)
            icon.getLocationOnScreen(iconLocation)
            paint.textSize = 10 * density
            paint.color = 0xff138af0.toInt()
            canvas.drawText(
                text,
                iconLocation[0] - rootLocation[0] + icon.width / 2f,
                maxOf(12 * density, iconLocation[1] - rootLocation[1] - 3 * density),
                paint
            )
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
