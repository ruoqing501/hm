package dev.lackluster.redmagichelper.hook.rules.gameassist

import android.os.Handler
import android.os.Message
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.LongType
import dev.lackluster.redmagichelper.utils.Prefs

/**
 * AI 触发器间隔调整（移植自 LS_Augment AiTriggerSpeedHook 的 gameassist 部分）。
 *
 * GameAssist 内 YoloDataProcessor 的 YOLO 扫描循环固定 1500ms 一轮
 * （what=2）。只匹配该 Handler 类名与消息，其他 Handler 一律放行。
 * 间隔配置运行时重读，改值即时生效。
 */
object AiTriggerYoloScan : YukiBaseHooker() {

    private const val TAG = "AiTriggerYoloScan"

    private const val YOLO_HANDLER = "com.zte.aivibrate.processor.YoloDataProcessor\$1"

    private const val MIN_YOLO_SCAN_MS = 150L

    override fun onHook() {
        val handlerClass = "android.os.Handler".toClass()
        handlerClass.method {
            name = "sendEmptyMessageDelayed"
            param(IntType, LongType)
        }.hook {
            before {
                val what = (args[0] as? Int) ?: Int.MIN_VALUE
                val delay = (args[1] as? Long) ?: Long.MIN_VALUE
                val target = replacementFor(instanceOrNull, what, delay) ?: return@before
                if (alreadyQueued(instanceOrNull, what)) {
                    result = true
                    return@before
                }
                args[1] = target
            }
        }
        handlerClass.method {
            name = "sendMessageDelayed"
            param(Message::class.java, LongType)
        }.hook {
            before {
                val what = (args[0] as? Message)?.what ?: Int.MIN_VALUE
                val delay = (args[1] as? Long) ?: Long.MIN_VALUE
                val target = replacementFor(instanceOrNull, what, delay) ?: return@before
                if (alreadyQueued(instanceOrNull, what)) {
                    result = true
                    return@before
                }
                args[1] = target
            }
        }
    }

    private fun enabled() = Prefs.getBoolean(Pref.Key.GameSpace.AI_TRIGGER_SWITCH, false)

    private fun yoloScanMs() =
        Prefs.getInt(Pref.Key.GameSpace.AI_TRIGGER_YOLO_SCAN_MS, 400).coerceIn(150, 1500)

    private fun replacementFor(owner: Any?, what: Int, delay: Long): Long? {
        if (delay <= 0 || !enabled()) return null
        if (owner?.javaClass?.name != YOLO_HANDLER || what != 2 || delay != 1500L) return null
        val target = scanDelay(delay, yoloScanMs().toLong(), MIN_YOLO_SCAN_MS)
        if (target >= delay) return null
        YLog.debug(tag = TAG, msg = "yolo_scan $delay->$target")
        return target
    }

    private fun alreadyQueued(owner: Any?, what: Int): Boolean {
        if (owner !is Handler) return false
        return runCatching { owner.hasMessages(what) }.getOrDefault(false)
    }

    private fun scanDelay(original: Long, configured: Long, minimum: Long): Long {
        if (original <= 0) return original
        return minOf(original, maxOf(minimum, configured))
    }
}
