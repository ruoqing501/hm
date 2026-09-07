package dev.lackluster.mihelper.hook.rules.systemui.recenttasks
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import dev.lackluster.mihelper.hook.rules.desktop.RecentTasksHook.updateMemoryText


object RecentTasksHook: YukiBaseHooker() {
    private const val TAG = "NubiaRecentSystemUITasksHook"


    override fun onHook() {
        // 3. 在 TaskStackChangeListeners$Impl.onTaskRemoved 中更新内存信息
        // ==================== 3. 在 TaskStackChangeListeners$Impl.onTaskRemoved 中更新 ====================
        "com.android.systemui.shared.system.TaskStackChangeListeners\$Impl".toClass().method {
            name = "onTaskRemoved"
            param(IntType)
        }.hook {
            after {
                YLog.debug(tag = TAG, msg =  "onTaskRemoved 回调触发，准备更新内存显示")
                updateMemoryText()
            }
        }
    }
}