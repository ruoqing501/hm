package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.SparseArray
import android.view.View
import androidx.core.content.ContextCompat
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Constants.ACTION_SCREENSHOT
import java.util.WeakHashMap

object HideStatusBarBeforeScreenshot : YukiBaseHooker() {
    private const val TAG = "HideStatusBarBeforeScreenshot"


    private const val EXTRA_IS_FINISHED: String = "IsFinished"
    private const val ACTION_SCREENSHOT = "android.intent.action.CAPTURE_SCREENSHOT"
    override fun onHook() {
        "com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment".toClass().method {
            name = "onViewCreated"
        }.hook(){
            after {
               val view =  args[0] as?  View
                if (view != null) {
                    registerScreenshotReceiver(view)
                    YLog.debug(tag = TAG, msg = "Screenshots Hide StatusBar")
                }
            }
        }
    }

    private fun registerScreenshotReceiver(view: View) {
        val context = view.context ?: return

        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent) {
                if (ACTION_SCREENSHOT != intent.action){
                    YLog.debug(tag = TAG, msg = "Screenshot action is not $ACTION_SCREENSHOT")
                    return
                }

                val finished = intent.getBooleanExtra(EXTRA_IS_FINISHED, true)
                view.visibility = if (finished) View.VISIBLE else View.INVISIBLE
                if (finished) {
                    YLog.debug(tag = TAG, msg = "Screenshot completed")
                }
            }
        }

        val filter =
            IntentFilter(ACTION_SCREENSHOT)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }
}