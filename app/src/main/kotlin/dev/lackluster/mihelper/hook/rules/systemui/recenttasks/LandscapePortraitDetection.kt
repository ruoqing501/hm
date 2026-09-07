package dev.lackluster.mihelper.hook.rules.systemui.recenttasks

import android.content.res.Configuration
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import dev.lackluster.mihelper.hook.rules.desktop.RecentTasksHook.updateMemoryText


// 横竖屏检测
object LandscapePortraitDetection: YukiBaseHooker() {
    private const val TAG = "LandscapePortraitDetection"


    override fun onHook() {
        // 3. 在 TaskStackChangeListeners$Impl.onTaskRemoved 中更新内存信息
        // ==================== 3. 在 TaskStackChangeListeners$Impl.onTaskRemoved 中更新 ====================
        "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass().method {
            name = "onConfigurationChanged"
            superClass()
            param(Configuration::class.java)
        }.hook {
            after {
                YLog.debug(tag = TAG, msg =  "onConfigurationChanged 回调触发，准备更新屏幕方向")
                val config =  args[0] as? Configuration
                if (config == null){
                    YLog.debug(tag = TAG, msg = "onConfigurationChanged 回调触发，参数为空")
                    return@after
                }
                when (config.orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> {
                        // 竖屏逻辑
                        YLog.debug(tag = TAG, msg =  "当前为竖屏")
                    }
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        // 横屏逻辑
                        YLog.debug(tag = TAG, msg = "当前为横屏")
                    }
                    else -> {
                        // 未知方向
                    }
                }
            }
        }
    }
}