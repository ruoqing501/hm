package dev.lackluster.mihelper.hook.rules.gameassist

import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.StringClass
import dev.lackluster.mihelper.hook.compat.type.java.UnitType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

/**
活跃模式
唯一导致状态丢失的原因是：

每次 Tile 初始化时调用 resetActiveModeSharedPreAllKey 清空所有配置；

每次游戏退到后台时，onLauncherFirstPackage 调用 removeActiveModeSharedPreKey 删除当前游戏的配置。
Hook 精准拦截了这两个清空操作，从而确保用户手动设置的状态永久保存，并且系统能正常读取应用。
活跃模式将完全由用户手动控制：首次安装或首次进入某游戏时，默认关闭；用户点击开关后，该状态会永久记住，后续每次进入游戏都会保持用户最后的设置。
* */
object ActiveMode : YukiBaseHooker() {

    private const val TAG = "GameAssistActiveMode"

    override fun onHook() {
        hasEnable( Pref.Key.GameSpace.ACIVE_MODE_SWITCH){
            // ========== 1. 禁止重置所有保存的活跃模式配置 ==========
            "cn.nubia.gameassist.dessert.policy.ActiveModeController".toClass().method {
                name = "resetActiveModeSharedPreAllKey"
                returnType = UnitType
            }.hook {
                before {
                    YLog.debug(tag = TAG, msg = "Blocked resetActiveModeSharedPreAllKey")
                    result = null
                }
            }
            // ========== 2. 阻止 onLauncherFirstPackage 触发的配置删除 ==========
            "cn.nubia.gameassist.dessert.policy.ActiveModeController".toClass().method {
                name = "removeActiveModeSharedPreKey"
                param(StringClass)
            }.hook {
                before {
                    val stackTrace = Thread.currentThread().stackTrace
                    // 检查调用栈中是否包含 onLauncherFirstPackage（即桌面启动触发的删除）
                    if (stackTrace.any { it.methodName.contains("onLauncherFirstPackage") }) {
                        YLog.debug(tag = TAG, msg = "Block removeActiveModeSharedPreKey triggered by onLauncherFirstPackage")
                        result = null
                    }
                }
            }
            // 注意：不再添加任何自动开启逻辑，完全依赖系统原本的 updateCurrApp() 读取保存的状态
        }
    }
}