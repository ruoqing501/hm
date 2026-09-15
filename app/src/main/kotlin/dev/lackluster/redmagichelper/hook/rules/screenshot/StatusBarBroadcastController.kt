package dev.lackluster.redmagichelper.hook.rules.screenshot

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Constants
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import java.lang.ref.WeakReference

object StatusBarBroadcastController : YukiBaseHooker() {
    private const val TAG = "ScreenshotLogger"
    private var statusBarReceiver: BroadcastReceiver? = null
    private var phoneStatusBarViewRef: WeakReference<View>? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onHook() {
        // 在 CollapsedStatusBarFragment 创建时注册广播并保存状态栏视图
        "com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment".toClass()
            .method { name = "onViewCreated" }
            .hook {
                after {
                    val fragmentView = args[0] as? View ?: return@after
                    findPhoneStatusBarView(fragmentView)?.let { statusBarView ->
                        phoneStatusBarViewRef = WeakReference(statusBarView)
                        registerStatusBarController(statusBarView.context)
                        YLog.debug(tag = TAG, msg = "PhoneStatusBarView saved and receiver registered")
                    } ?: YLog.debug(tag = TAG, msg = "Failed to find PhoneStatusBarView")
                }
            }

        // 在 PhoneStatusBarView 的 onFinishInflate 中更新引用，应对配置变化
        "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass()
            .method { name = "onFinishInflate" }
            .hook {
                after {
                    val statusBarView = instance as? View ?: return@after
                    phoneStatusBarViewRef = WeakReference(statusBarView)
                    YLog.debug(tag = TAG, msg = "PhoneStatusBarView reference updated")
                }
            }
    }

    // 两个截图/录屏开关都关闭时回调完全空转
    private fun featureEnabled(): Boolean =
        Prefs.getBoolean(Pref.Key.Other.SCREENSHOT_HIDE_STATUS_BAR, false) ||
            Prefs.getBoolean(Pref.Key.Other.RERCORD_SCREEN_HIDE_STATUS_BAR, false)

    private fun findPhoneStatusBarView(view: View): View? {
        if (view.javaClass.name == "com.android.systemui.statusbar.phone.PhoneStatusBarView") {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findPhoneStatusBarView(child)
                if (result != null) return result
            }
        }
        return null
    }

    private fun registerStatusBarController(context: Context) {
        if (statusBarReceiver != null) return

        statusBarReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != Constants.ACTION_HIDE_STATUSBAR) return
                if (!featureEnabled()) return
                val visible = intent.getBooleanExtra(Constants.EXTRA_STATUSBAR_VISIBLE, true)
                YLog.debug(tag = TAG, msg = "Received statusbar control: visible=$visible")
                mainHandler.post {
                    controlStatusBarVisibility(visible)
                }
            }
        }

        val filter = IntentFilter(Constants.ACTION_HIDE_STATUSBAR)
        try {
            ContextCompat.registerReceiver(
                context.applicationContext,
                statusBarReceiver!!,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            YLog.debug(tag = TAG, msg = "Receiver registered successfully")
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Failed to register receiver: ${e.message}")
        }
    }

    private fun controlStatusBarVisibility(visible: Boolean) {
        try {
            var statusBarView = phoneStatusBarViewRef?.get()
            if (statusBarView == null) {
                statusBarView = findPhoneStatusBarViewFromCurrentActivity()
                if (statusBarView != null) {
                    phoneStatusBarViewRef = WeakReference(statusBarView)
                } else {
                    YLog.debug(tag = TAG, msg = "PhoneStatusBarView not found")
                    if (!visible) sendHideDoneBroadcast(null, false)
                    return
                }
            }

            val context = statusBarView.context
            val resId = context.resources.getIdentifier("status_bar_contents", "id", context.packageName)
            val contentsView = statusBarView.findViewById<View>(resId)

            val targetVisibility = if (visible) View.VISIBLE else View.GONE
            val targetView = contentsView ?: statusBarView
            if (targetView.visibility != targetVisibility) {
                targetView.visibility = targetVisibility
                YLog.debug(tag = TAG, msg = "Visibility set to ${if (visible) "VISIBLE" else "GONE"} on ${if (contentsView != null) "status_bar_contents" else "PhoneStatusBarView"}")
            }

            // 如果是隐藏操作，等待视图重绘完成后再发送确认广播
            if (!visible) {
                // 使用 View.post 确保布局已完成
                targetView.post {
                    // 再稍微延迟一点，确保屏幕内容已更新（可选）
                    mainHandler.postDelayed({
                        sendHideDoneBroadcast(context, true)
                    }, 16) // 一帧的时间，保证绘制完成
                }
            }
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Error controlling statusbar: ${e.message}")
            if (!visible) sendHideDoneBroadcast(null, false)
        }
    }

    private fun sendHideDoneBroadcast(context: Context?, success: Boolean) {
        val ctx = context ?: phoneStatusBarViewRef?.get()?.context ?: return
        val intent = Intent(Constants.ACTION_HIDE_STATUSBAR_DONE).apply {
            setPackage("com.android.ztescreenshot")
            putExtra(Constants.EXTRA_HIDE_SUCCESS, success)
        }
        ctx.sendBroadcast(intent)
        YLog.debug(tag = TAG, msg = "Sent hide done broadcast, success=$success")
    }

    private fun findPhoneStatusBarViewFromCurrentActivity(): View? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread")
            currentActivityThreadMethod.isAccessible = true
            val activityThread = currentActivityThreadMethod.invoke(null)

            val activitiesField = activityThreadClass.getDeclaredField("mActivities")
            activitiesField.isAccessible = true
            val activities = activitiesField.get(activityThread) as? Map<*, *>

            activities?.values?.forEach { activityRecord ->
                val activityField = activityRecord?.javaClass?.getDeclaredField("activity")
                activityField?.isAccessible = true
                val activity = activityField?.get(activityRecord) as? Activity
                if (activity != null && activity.javaClass.name.contains("SystemUI")) {
                    val decorView = activity.window?.decorView
                    return findPhoneStatusBarView(decorView ?: return@forEach)
                }
            }
            null
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Fallback search failed: ${e.message}")
            null
        }
    }
}
