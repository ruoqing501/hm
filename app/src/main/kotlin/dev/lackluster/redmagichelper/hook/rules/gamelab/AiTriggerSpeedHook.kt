package dev.lackluster.redmagichelper.hook.rules.gamelab

import android.os.Handler
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.LongType
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import java.util.concurrent.ThreadPoolExecutor

/**
 * AI 触发器间隔调整（移植自 LS_Augment AiTriggerSpeedHook 的 gamelab 部分）。
 *
 * GameLab 的模板匹配循环默认 2000ms 一轮。王者荣耀在当前 Nubia GameLab
 * 版本走 SGameToy，CommonToy 作为兼容回退，两者都 Hook。
 * 间隔配置运行时重读，改值即时生效。
 */
object AiTriggerSpeedHook : YukiBaseHooker() {

    private const val TAG = "GameLabAiTriggerSpeed"

    private const val COMMON_TOY = "cn.nubia.gamelab.toy.CommonToy"
    private const val SGAME_TOY = "cn.nubia.gamelab.toy.SGameToy"

    /** 模板匹配低于 80ms 不予支持 */
    private const val MIN_TEMPLATE_SCAN_MS = 80L

    override fun onHook() {
        hasEnable(Pref.Key.GameSpace.AI_TRIGGER_SWITCH) {
            hookPostDelayed()
            listOf(SGAME_TOY, COMMON_TOY).forEach { hookToy(it) }
        }
    }

    private fun enabled() = Prefs.getBoolean(Pref.Key.GameSpace.AI_TRIGGER_SWITCH, false)

    private fun templateScanMs() =
        Prefs.getInt(Pref.Key.GameSpace.AI_TRIGGER_TEMPLATE_SCAN_MS, 180).coerceIn(80, 2000)

    /**
     * 常规路径：检测 Runnable 通过 Handler.postDelayed 以 2000ms 重排。
     * 只替换可识别的检测 Runnable，其他 postDelayed 一律放行。
     */
    private fun hookPostDelayed() {
        "android.os.Handler".toClass().method {
            name = "postDelayed"
            param(Runnable::class.java, LongType)
        }.hook {
            before {
                if (!enabled()) return@before
                val callback = args[0]
                val delay = (args[1] as? Long) ?: return@before
                if (delay != 2000L || !isDetectRunnable(callback)) return@before
                val target = scanDelay(delay, templateScanMs().toLong(), MIN_TEMPLATE_SCAN_MS)
                if (target >= delay) return@before
                YLog.debug(tag = TAG, msg = "scan_post ${callback?.javaClass?.name} $delay->$target")
                args[1] = target
            }
        }
    }

    /**
     * 王者荣耀路径从检测 Runnable 内部直接调用 scheduleDetectRunnable()，
     * Handler.postDelayed 的 Hook 观察不到，厂商的 2000ms 会原样保留。
     * 直接替换调度方法：先移除厂商已入队的回调，再按配置的 cadence 重排。
     */
    private fun hookToy(className: String) {
        val toy = className.toClassOrNull() ?: run {
            YLog.error(tag = TAG, msg = "未找到 $className，跳过 GameLab 扫描调度 Hook")
            return
        }
        toy.method {
            name = "scheduleDetectRunnable"
            emptyParam()
        }.hook {
            before {
                if (!enabled()) return@before
                val handler = readField(instance, "mHandler") as? Handler ?: return@before
                val runnable = readField(instance, "mDetectRunnable") as? Runnable ?: return@before
                val original = intField(instance, "MSG_DELAY_TIME", 2000).toLong()
                val target = scanDelay(original, templateScanMs().toLong(), MIN_TEMPLATE_SCAN_MS)
                YLog.debug(tag = TAG, msg = "scan_schedule ${instance.javaClass.name} $original->$target")
                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, target)
                intercept()
            }
        }
        // 上一轮检测还在执行时跳过本轮采集，防止 executor 积压；
        // CommonToy$2 会在本方法返回后继续排下一轮，厂商的工作状态不受影响。
        toy.method {
            name = "startDetectBitmap"
            emptyParam()
        }.hook {
            before {
                if (!enabled() || !isBusy(instance)) return@before
                YLog.debug(tag = TAG, msg = "scan_skip_busy ${instance.javaClass.name}")
                intercept()
            }
        }
    }

    private fun isDetectRunnable(value: Any?): Boolean {
        val name = value?.javaClass?.name ?: return false
        return name == "$SGAME_TOY\$1" || name == "$COMMON_TOY\$2"
    }

    private fun isBusy(toy: Any): Boolean {
        val executor = readField(toy, "mExecutor") as? ThreadPoolExecutor ?: return false
        return executor.activeCount > 0 || !executor.queue.isEmpty()
    }

    private fun scanDelay(original: Long, configured: Long, minimum: Long): Long {
        if (original <= 0) return original
        return minOf(original, maxOf(minimum, configured))
    }

    private fun readField(owner: Any, name: String): Any? {
        var current: Class<*>? = owner.javaClass
        while (current != null) {
            try {
                val field = current.getDeclaredField(name)
                field.isAccessible = true
                return field.get(owner)
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            } catch (_: Throwable) {
                return null
            }
        }
        return null
    }

    private fun intField(owner: Any, name: String, fallback: Int): Int =
        (readField(owner, name) as? Number)?.toInt() ?: fallback
}
