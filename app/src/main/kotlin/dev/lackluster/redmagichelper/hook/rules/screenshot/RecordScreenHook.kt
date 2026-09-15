package dev.lackluster.redmagichelper.hook.rules.screenshot

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.core.content.ContextCompat
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.data.Constants
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object RecordScreenHook : YukiBaseHooker() {

    private const val TAG = "RecordScreenHook"

    override fun onHook() {
        val recordClass = "com.android.ztescreenshot.recordscreen.RecordScreenView".toClass()

        // Hook startRecord 方法，根据当前状态决定隐藏或显示状态栏
        recordClass.method {
            name = "startRecord"
            paramCount = 0
        }.hook {
            before {
                if (!Prefs.getBoolean(Pref.Key.Other.RERCORD_SCREEN_HIDE_STATUS_BAR, false)) return@before
                val instance = this.instance
                val curState = instance.current().field {
                    name = "curRecordState"
                    type = IntType
                }.int()
                YLog.debug(tag = TAG, msg = "startRecord before, curState=$curState")

                val context = (instance as? View)?.context ?: return@before

                when (curState) {
                    0 -> { // 停止 -> 开始录制，隐藏状态栏
                        YLog.debug(tag = TAG, msg = "即将开始录制，隐藏状态栏")
                        syncHideStatusBar(context)
                        ScreenCaptureState.isRecording.set(true)
                        YLog.debug(tag = TAG, msg = "开始；录制记录当前录制状态：isRecording = true")
                    }
                    1 -> { // 录制中 -> 暂停，隐藏状态栏
                        YLog.debug(tag = TAG, msg = "即将暂停录制，隐藏状态栏")
                        syncHideStatusBar(context)
                        // 注意：这里不改变 isRecording，仍为 true
                    }
                    2 -> { // 暂停中 -> 恢复录制，隐藏状态栏
                        YLog.debug(tag = TAG, msg = "即将恢复录制，隐藏状态栏")
                        syncHideStatusBar(context)
                        // 仍保持 isRecording = true
                    }
                }
            }
            after {
                if (!Prefs.getBoolean(Pref.Key.Other.RERCORD_SCREEN_HIDE_STATUS_BAR, false)) return@after
                val instance = this.instance
                val curState = instance.current().field {
                    name = "curRecordState"
                    type = IntType
                }.int()
                YLog.debug(tag = TAG, msg = "startRecord after, curState=$curState")
            }
        }

        recordClass.method {
            name = "stopRecord"
            paramCount = 0
        }.hook {
            after {
                if (!Prefs.getBoolean(Pref.Key.Other.RERCORD_SCREEN_HIDE_STATUS_BAR, false)) return@after
                val context = (instance as? View)?.context ?: return@after
                YLog.debug(tag = TAG, msg = "录制结束，显示状态栏")
                // 先标记录屏结束，避免截图误恢复状态栏
                ScreenCaptureState.isRecording.set(false)
                sendStatusBarBroadcast(context, true)
            }
        }
    }

    /**
     * 同步隐藏状态栏，等待确认
     * @param context 上下文
     * @return true 表示成功隐藏，false 表示超时或失败
     */
    private fun syncHideStatusBar(context: Context): Boolean {
        val latch = CountDownLatch(1)
        val success = AtomicBoolean(false)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Constants.ACTION_HIDE_STATUSBAR_DONE) {
                    success.set(intent.getBooleanExtra(Constants.EXTRA_HIDE_SUCCESS, false))
                    latch.countDown()
                }
            }
        }

        val filter = IntentFilter(Constants.ACTION_HIDE_STATUSBAR_DONE)
        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            // 发送隐藏请求
            sendStatusBarBroadcast(context, false)

            // 等待确认，超时 200ms（足够隐藏状态栏）
            val waited = latch.await(200, TimeUnit.MILLISECONDS)
            if (!waited) {
                YLog.debug(tag = TAG, msg = "Hide timeout, continue recording anyway")
            }
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Sync hide error: ${e.message}")
        } finally {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // ignore
            }
        }
        return success.get()
    }

    /**
     * 发送控制状态栏的广播
     * @param context 上下文
     * @param visible true 显示状态栏，false 隐藏状态栏
     */
    private fun sendStatusBarBroadcast(context: Context, visible: Boolean) {
        try {
            val intent = Intent(Constants.ACTION_HIDE_STATUSBAR).apply {
                setPackage("com.android.systemui")
                putExtra(Constants.EXTRA_STATUSBAR_VISIBLE, visible)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.sendBroadcast(intent)
            YLog.debug(tag = TAG, msg = "Send statusbar broadcast: visible=$visible")
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Failed to send broadcast: ${e.message}")
        }
    }
}
