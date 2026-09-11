package dev.lackluster.redmagichelper.hook.rules.recommend

import android.annotation.SuppressLint
import android.content.Intent
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.param.HookParam
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import dev.lackluster.redmagichelper.utils.nubia.Deoptimizer

/**
 * com.zte.recommend（小窗图标宿主）侧的数量上限解除（移植自 LS_Augment FreeformHook.installIconHost）。
 *
 * WindowReplyIconManager.iconShowIsError 有独立的硬限制：挂起图标达到 3 个后拒绝第 4 个。
 * 该进程在 system_server 之外，因此需要单独的包作用域 hook。
 *
 * WindowReplyService.onStartCommand 也一并处理：原厂应用 AOT 预编译后 iconShowIsError 可能
 * 被内联进调用方，此时在处理第 4 个及以后的图标请求期间，临时把多余的 mIconList 条目
 * 「寄放」出去，让原厂创建路径跑完后再还原，保留原厂图标/动画行为。
 */
@SuppressLint("PrivateApi")
object WindowReplyIconLimit : YukiBaseHooker() {
    private const val TAG = "[WindowReplyIconLimit]"
    private const val WINDOW_REPLY_ICON_MANAGER = "com.zte.wr.WindowReplyIconManager"
    private const val WINDOW_REPLY_SERVICE = "com.zte.wr.WindowReplyService"

    /** before 中寄放、after 中还原；服务请求都在同一线程同步完成。 */
    private class ParkedState(val manager: Any, val icons: MutableMap<Any, Any>, val parked: Map<Any, Any>)
    private val parkedState = ThreadLocal<ParkedState?>()

    override fun onHook() {
        hasEnable(Pref.Key.Android.REMOVE_RESTRICTIONS_WINDOW_NUMBER) {
            hookIconManager()
            hookIconService()
        }
    }

    private fun hookIconManager() {
        val managerClass = WINDOW_REPLY_ICON_MANAGER.toClassOrNull() ?: run {
            YLog.error("$TAG 无法找到 $WINDOW_REPLY_ICON_MANAGER，跳过图标上限解除")
            return
        }
        managerClass.method {
            name = "iconShowIsError"
            param(IntType)
            returnType = BooleanType
        }.ignored().give()?.hook {
            after {
                if (result == true) {
                    YLog.debug("$TAG iconShowIsError 返回 true，改写为 false")
                    result = false
                }
            }
        } ?: YLog.warn("$TAG 未找到 iconShowIsError，跳过")

        // ART 可能把这个极小的 size >= 3 判断内联进两条创建路径，反优化使 hook 确定生效
        Deoptimizer.deoptimizeMethod(TAG, managerClass, "iconShowOutScreen")
        Deoptimizer.deoptimizeMethod(TAG, managerClass, "iconInScreenCreate")
    }

    private fun hookIconService() {
        val serviceClass = WINDOW_REPLY_SERVICE.toClassOrNull() ?: run {
            YLog.warn("$TAG 无法找到 $WINDOW_REPLY_SERVICE，跳过服务入口 hook")
            return
        }
        serviceClass.method {
            name = "onStartCommand"
            param(Intent::class.java, IntType, IntType)
            returnType = IntType
        }.ignored().give()?.hook {
            before {
                runCatching { parkIconsForNewIcon(this) }
                    .onFailure { error ->
                        YLog.error("$TAG 寄放图标条目失败，回滚", error)
                        runCatching { restoreParkedIcons() }
                    }
            }
            after {
                runCatching { restoreParkedIcons() }
                    .onFailure { YLog.error("$TAG 还原寄放图标条目失败", it) }
            }
        } ?: YLog.warn("$TAG 未找到 onStartCommand，跳过")

        Deoptimizer.deoptimizeMethod(TAG, serviceClass, "onStartCommand")
    }

    private fun parkIconsForNewIcon(param: HookParam) {
        val intent = param.args[0] as? Intent ?: return
        val reason = intent.getStringExtra("reason")
        val createsIcon = reason == "create_icon" || reason == "show_icon"
        if (!createsIcon) return
        val taskId = intent.extras?.getInt("taskId", -1) ?: -1
        if (taskId < 0) return

        val manager = fieldOf(param.instanceOrNull, "mManager") ?: return
        @Suppress("UNCHECKED_CAST")
        val icons = fieldOf(manager, "mIconList") as? MutableMap<Any, Any> ?: return
        // 原厂闸门是 mIconList.size() >= 3；仅在本请求期间让少于 3 个条目对它可见。
        // 图标 View 本身在条目被寄放的几毫秒内保持存活。
        if (icons.containsKey(taskId) || icons.size < 3) return

        val parked = LinkedHashMap<Any, Any>()
        parkedState.set(ParkedState(manager, icons, parked))
        while (icons.size >= 3) {
            val key = icons.keys.first()
            val value = icons.remove(key) ?: break
            parked[key] = value
        }
        YLog.debug("$TAG 寄放 ${parked.size} 个挂起图标以创建新图标 (task=$taskId)")
    }

    private fun restoreParkedIcons() {
        val state = parkedState.get() ?: return
        parkedState.remove()
        state.icons.putAll(state.parked)
        invokeNoArg(state.manager, "iconProviderUpdate")
    }

    private fun fieldOf(owner: Any?, name: String): Any? {
        if (owner == null) return null
        var type: Class<*>? = owner.javaClass
        while (type != null) {
            try {
                val value = type.getDeclaredField(name)
                value.isAccessible = true
                return value.get(owner)
            } catch (_: NoSuchFieldException) {
                type = type.superclass
            } catch (_: Throwable) {
                return null
            }
        }
        return null
    }

    private fun invokeNoArg(owner: Any?, name: String) {
        if (owner == null) return
        runCatching {
            var type: Class<*>? = owner.javaClass
            while (type != null) {
                val clazz = type
                val method = runCatching {
                    clazz.declaredMethods.firstOrNull { it.name == name && it.parameterCount == 0 }
                }.getOrNull()
                if (method != null) {
                    method.isAccessible = true
                    method.invoke(owner)
                    return
                }
                type = clazz.superclass
            }
        }
    }
}
