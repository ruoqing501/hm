package dev.lackluster.mihelper.hook.rules.gamefloat

import android.content.Context
import android.provider.Settings
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.hasEnable

object HideGameChickenModeDialog : YukiBaseHooker() {

    private const val TAG = "HideGameChickenModeDialog"

    override fun onHook() {
        "cn.zte.gamefloat.gamekeys.entity.GameChickenModeDialog\$ChickenModeDialog".toClassOrNull()?.method {
            name = "show"
            superClass()
        }?.hook {
            before {
                // 获取 Context
                val context = this.instance.current().field {
                    name = "mDContext"
                }.cast<Context>() ?: return@before
                // 阻止原 show() 执行，弹窗永远不会出现
                result = null
                YLog.debug(tag = TAG, msg = "已隐藏游戏破坏神模式退出按钮弹窗")
            }
        }
    }
}
