package dev.lackluster.redmagichelper.hook.rules.screenshot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.data.Constants
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object ScreenshotLoggerHook : YukiBaseHooker() {

    private const val TAG = "ScreenshotLogger"

    override fun onHook() {
        "com.android.ztescreenshot.cropimage.CropImageService".toClass().apply {
            method {
                name = "onStartCommand"
                param(Intent::class.java, IntType, IntType)
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.Other.SCREENSHOT_HIDE_STATUS_BAR, false)) return@before
                    YLog.debug(tag = TAG, msg = "Screenshot started")
                    val context = instance as? Context ?: return@before
                    // 同步隐藏状态栏，等待确认
                    val hidden = syncHideStatusBar(context)
                    YLog.debug(tag = TAG, msg = "Status bar hidden: $hidden")
                    // 继续原始截图逻辑
                }
            }

            method {
                name = "onDestroy"
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.Other.SCREENSHOT_HIDE_STATUS_BAR, false)) return@before
                    YLog.debug(tag = TAG, msg = "Screenshot finished (service destroyed)")
                    val context = instance as? Context ?: return@before
                    // 如果正在录屏，则在截图结束后不显示状态栏
                    if (ScreenCaptureState.isRecording.get()) {
                        YLog.debug(tag = TAG, msg = "Screen recording active, skip showing status bar")
                        return@before
                    }
                    sendStatusBarBroadcast(context, true)
                }
            }

            method {
                name = "saveAndExit"
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.Other.SCREENSHOT_HIDE_STATUS_BAR, false)) return@before
                    YLog.debug(tag = TAG, msg = "Screenshot saved and exit")
                    val context = instance as? Context ?: return@before
                    sendStatusBarBroadcast(context, true)
                }
            }
        }
    }

    /**
     * 同步隐藏状态栏，等待确认
     * @return true 表示成功隐藏，false 表示超时或失败
     */
    private fun syncHideStatusBar(context: Context): Boolean {
        val latch = CountDownLatch(1)
        val success = AtomicBoolean(false)
        val receiver = object : BroadcastReceiver() {
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
                YLog.debug(tag = TAG, msg = "Hide timeout, continue screenshot anyway")
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
