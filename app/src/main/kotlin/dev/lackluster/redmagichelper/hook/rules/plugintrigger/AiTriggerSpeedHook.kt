package dev.lackluster.redmagichelper.hook.rules.plugintrigger

import android.os.Handler
import android.os.Message
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.LongType
import dev.lackluster.redmagichelper.utils.Prefs

/**
 * AI 触发器间隔调整（移植自 LS_Augment AiTriggerSpeedHook 的 plugintrigger 部分）。
 *
 * 只收紧厂商 AI 触发器的调度循环：模板扫描、点击事务、策略冷却。
 * 全局 Hook Handler 会加速无关工作，因此每处替换都按 Handler 类名、消息 what
 * 和原始延迟三重匹配。间隔配置运行时重读，改值即时生效。
 */
object AiTriggerSpeedHook : YukiBaseHooker() {

    private const val TAG = "AiTriggerSpeedHook"

    private const val TEMPLATE_HANDLER =
        "com.zte.game.plugintrigger.service.PluginTriggerService\$WorkHandler"
    private const val CLICK_HANDLER =
        "com.zte.game.plugintrigger.plugins.click.TouchScreenPlugin\$WorkHandler"
    private const val POLICY_INTERVAL_HANDLER =
        "com.zte.game.plugintrigger.policy.PolicyManager\$PolicyIntervalHandler"
    private const val ACTION_HANDLER =
        "com.zte.game.plugintrigger.service.PluginsController\$2"
    private const val LEGACY_ACTION_HANDLER =
        "com.zte.game.plugintrigger.service.PluginsController\$MyHandler"
    private const val POLICY_MANAGER = "com.zte.game.plugintrigger.policy.PolicyManager"

    /** autoClick 插件类型(11) + 厂商完成消息偏移 */
    private const val AUTO_CLICK_COMPLETION_MESSAGE = 1011

    /** 模板匹配低于 80ms 不予支持 */
    private const val MIN_TEMPLATE_SCAN_MS = 80L

    /** 保持事件顺序与 10ms 点击下限 */
    private const val MIN_TOUCH_DOWN_MS = 10L

    override fun onHook() {
        hookHandlerSend()
        hookPolicyCooldown()
        hookPolicyIntervalSend()
    }

    private fun enabled() = Prefs.getBoolean(Pref.Key.GameSpace.AI_TRIGGER_SWITCH, false)

    private fun templateScanMs() =
        Prefs.getInt(Pref.Key.GameSpace.AI_TRIGGER_TEMPLATE_SCAN_MS, 180).coerceIn(80, 2000)

    private fun clickDelayMs() =
        Prefs.getInt(Pref.Key.GameSpace.AI_TRIGGER_CLICK_MS, 25).coerceIn(10, 500)

    private fun cooldownMs() =
        Prefs.getInt(Pref.Key.GameSpace.AI_TRIGGER_COOLDOWN_MS, 180).coerceIn(50, 30000)

    private fun hookHandlerSend() {
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

    /**
     * 厂商把每个策略间隔钳制到 2000..30000ms。仅当区间恰好是 2000..30000
     * 时把结果换成用户配置的冷却值。
     */
    private fun hookPolicyCooldown() {
        POLICY_MANAGER.toClassOrNull()?.method {
            name = "getLegalRangeValue"
            param(LongType, LongType, LongType, LongType)
            returnType = LongType
        }?.hook {
            before {
                if (!enabled()) return@before
                val min = (args[1] as? Long) ?: return@before
                val max = (args[2] as? Long) ?: return@before
                if (min != 2000L || max != 30000L) return@before
                result = cooldownMs().toLong()
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 $POLICY_MANAGER，冷却 Hook 未安装")
    }

    /**
     * PolicyIntervalHandler 覆写了 Handler.sendMessageDelayed，只 Hook
     * android.os.Handler 观察不到这条 2 秒的策略重排，需要直接 Hook 覆写方法。
     */
    private fun hookPolicyIntervalSend() {
        POLICY_INTERVAL_HANDLER.toClassOrNull()?.method {
            name = "sendMessageDelayed"
            param(Message::class.java, LongType)
        }?.hook {
            before {
                if (!enabled()) return@before
                val delay = (args[1] as? Long) ?: return@before
                val cooldown = cooldownMs().toLong()
                if (delay < 2000L || delay > 30000L || cooldown >= delay) return@before
                YLog.debug(tag = TAG, msg = "policy_interval $delay->$cooldown")
                args[1] = cooldown
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 $POLICY_INTERVAL_HANDLER，冷却 Hook 未安装")
    }

    private fun replacementFor(owner: Any?, what: Int, delay: Long): Long? {
        if (delay <= 0 || !enabled()) return null
        val handlerName = owner?.javaClass?.name ?: return null
        var target = -1L
        var reason: String? = null
        when {
            handlerName == TEMPLATE_HANDLER && delay == 2000L -> {
                target = scanDelay(delay, templateScanMs().toLong(), MIN_TEMPLATE_SCAN_MS)
                reason = "template_scan"
            }

            handlerName == CLICK_HANDLER -> when {
                // TouchScreenPlugin 的三条延迟消息是一次点击事务而非点击间隔：
                // 102/down=50ms, 103/up=450ms, 104/disable=500ms。
                // 缩短内部时序的同时必须保持事务顺序。
                what == 102 && delay == 50L -> {
                    target = maxOf(MIN_TOUCH_DOWN_MS, minOf(clickDelayMs().toLong(), delay))
                    reason = "touch_down"
                }

                what == 103 && delay == 450L -> {
                    target = maxOf(clickDelayMs() + 25L, clickDelayMs() * 2L)
                    reason = "touch_up"
                }

                what == 104 && delay == 500L -> {
                    target = maxOf(clickDelayMs() + 50L, clickDelayMs() * 3L)
                    reason = "touch_disable"
                }
            }

            handlerName == POLICY_INTERVAL_HANDLER && delay in 2000L..30000L -> {
                target = cooldownMs().toLong()
                reason = "policy_interval"
            }

            (isHandlerType(owner, ACTION_HANDLER) || isHandlerType(owner, LEGACY_ACTION_HANDLER)) &&
                    what == AUTO_CLICK_COMPLETION_MESSAGE && delay in 2000L..30000L -> {
                // PluginsController 对 autoClick 还有一个独立的 2 秒完成门限，
                // 两处门限都要缩短，配置的极速 cadence 才会生效。
                target = cooldownMs().toLong()
                reason = "action_cooldown"
            }
        }
        if (target < 0 || target >= delay) return null
        YLog.debug(tag = TAG, msg = "replace delay reason=$reason handler=$handlerName what=$what $delay->$target")
        return target
    }

    /** 非触摸消息重复入队时直接丢弃，避免加速后消息堆积。 */
    private fun alreadyQueued(owner: Any?, what: Int): Boolean {
        if (owner !is Handler || what in 101..104) return false
        return runCatching { owner.hasMessages(what) }.getOrDefault(false)
    }

    private fun isHandlerType(owner: Any?, expectedClassName: String): Boolean {
        if (owner == null) return false
        if (owner.javaClass.name == expectedClassName) return true
        val callback = readField(owner, "mCallback")
        return callback != null && callback.javaClass.name == expectedClassName
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
}
