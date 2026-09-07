package dev.lackluster.redmagichelper.hook.rules.gamefloat

import android.content.Context
import android.provider.Settings
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

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
