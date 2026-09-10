package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.provider.Settings
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.ScreenOffHideExecutor
import dev.lackluster.redmagichelper.utils.factory.hasEnable

/**
 * 熄屏自动隐藏应用(移植自 LS_Augment 的 hook/ScreenOffAutomationHook.java)。
 *
 * system_server 侧事件源:监听 ACTION_SCREEN_OFF/ON,熄屏时按 Prefs 中的目标列表执行隐藏。
 * 执行语义与 LS_Augment 一致:每个熄屏周期最多执行一次(Epoch),亮屏只重置周期不自动恢复;
 * 执行失败按指数退避重试(最多 6 次,封顶 30s)。
 *
 * 跨进程执行:LS_Augment 通过 provider 回调模块进程跑 root 命令,本项目没有该 provider。
 * 这里先尝试在 system_server 进程内直接用 RootShell 执行 su;若 su 被 SELinux/Root 方案拒绝
 * (KernelSU/Magisk 默认不给非应用 uid 授权),则写入 Settings.Global 标记
 * [ScreenOffHideExecutor.PENDING_KEY],由模块 app 侧(HelperApplication)观察并代执行。
 */
object ScreenOffAutomationHook : YukiBaseHooker() {

    private const val TAG = "ScreenOffAutomationHook"

    @Volatile
    private var controller: Controller? = null

    override fun onHook() {
        hasEnable(Pref.Key.Other.SCREEN_OFF_HIDE_ENABLED) {
            attach()
        }
    }

    @Synchronized
    private fun attach() {
        if (controller != null) return
        // systemContext 在 onSystemServerStarting 阶段可能尚未就绪,交给 Controller 延迟重试获取
        val next = Controller { runCatching { systemContext }.getOrNull() }
        controller = next
        next.handler.post { next.start() }
    }

    /** 每个熄屏周期最多执行一次;亮屏或执行失败后允许再次执行。 */
    private class Epoch {
        private var handled = false

        @Synchronized
        fun screenOn() {
            handled = false
        }

        @Synchronized
        fun failed() {
            handled = false
        }

        @Synchronized
        fun claim(enabled: Boolean, screenOff: Boolean): Boolean {
            if (!screenOff) {
                handled = false
                return false
            }
            if (!enabled || handled) return false
            handled = true
            return true
        }
    }

    private class Controller(val contextProvider: () -> Context?) {
        private val thread = HandlerThread("RMH-ScreenOffHide")
        val handler: Handler
        private val epoch = Epoch()
        private var startAttempts = 0
        private var runAttempts = 0
        private var context: Context? = null
        private val retry = Runnable { refreshAndRun("retry") }

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(ignored: Context, intent: Intent) {
                if (Intent.ACTION_SCREEN_ON == intent.action) {
                    epoch.screenOn()
                    handler.removeCallbacks(retry)
                    runAttempts = 0
                } else {
                    refreshAndRun(intent.action ?: "unknown")
                }
            }
        }

        init {
            thread.start()
            handler = Handler(thread.looper)
        }

        fun start() {
            val ctx = contextProvider()
            if (ctx == null) {
                if (startAttempts++ < 60) {
                    handler.postDelayed({ start() }, 1000L)
                } else {
                    YLog.error(tag = TAG, msg = "system context unavailable, give up")
                }
                return
            }
            context = ctx
            try {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    ctx.registerReceiver(receiver, filter, null, handler, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    ctx.registerReceiver(receiver, filter, null, handler)
                }
                startAttempts = 0
                YLog.info(tag = TAG, msg = "screen-off automation listener attached")
                refreshAndRun("system_attached")
            } catch (error: Throwable) {
                YLog.error(tag = TAG, msg = "register listener failed: $error")
                runCatching { ctx.unregisterReceiver(receiver) }
                handler.postDelayed({ start() }, minOf(30000L, 1000L shl minOf(5, startAttempts++)))
            }
        }

        fun refreshAndRun(event: String) {
            val ctx = context ?: return
            var claimed = false
            try {
                // 每次事件重读配置:目标列表的修改即时生效,无需重启
                val enabled = Prefs.getBoolean(Pref.Key.Other.SCREEN_OFF_HIDE_ENABLED, false)
                val targets = Prefs.getStringSet(Pref.Key.Other.SCREEN_OFF_HIDE_TARGETS, mutableSetOf())
                val power = ctx.getSystemService(PowerManager::class.java)
                val off = power != null && !power.isInteractive
                if (!enabled || !off || targets.isEmpty()) {
                    handler.removeCallbacks(retry)
                    runAttempts = 0
                }
                if (!epoch.claim(enabled && targets.isNotEmpty(), off)) return
                claimed = true
                if (!ScreenOffHideExecutor.rootGranted()) {
                    // system_server 直接 su 通常被 SELinux/KernelSU 策略拒绝:
                    // 记录意图到 Settings.Global,交给模块 app 进程代为执行
                    runCatching {
                        Settings.Global.putInt(ctx.contentResolver, ScreenOffHideExecutor.PENDING_KEY, 1)
                    }.onFailure {
                        YLog.error(tag = TAG, msg = "mark pending failed: $it")
                    }
                    YLog.warn(tag = TAG, msg = "su unavailable in system_server, delegated to module app (event=$event)")
                    handler.removeCallbacks(retry)
                    runAttempts = 0
                    return
                }
                val outcome = ScreenOffHideExecutor.hideAll(targets)
                if (outcome.success) {
                    handler.removeCallbacks(retry)
                    runAttempts = 0
                    YLog.info(tag = TAG, msg = "screen-off hide done (event=$event): ${outcome.message}")
                } else {
                    YLog.warn(tag = TAG, msg = "screen-off hide failed (event=$event): ${outcome.message}")
                    retryFailed()
                }
            } catch (error: Throwable) {
                YLog.error(tag = TAG, msg = "event_failed: $error")
                if (claimed) retryFailed()
                else if (runAttempts++ < 6) {
                    handler.removeCallbacks(retry)
                    handler.postDelayed(retry, 5000L)
                }
            }
        }

        private fun retryFailed() {
            epoch.failed()
            handler.removeCallbacks(retry)
            if (runAttempts < 6) {
                handler.postDelayed(retry, minOf(30000L, 1000L shl runAttempts++))
            }
        }
    }
}
