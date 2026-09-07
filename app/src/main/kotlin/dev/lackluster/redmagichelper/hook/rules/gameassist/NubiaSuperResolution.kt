package dev.lackluster.redmagichelper.hook.rules.gameassist

import android.app.Dialog
import android.view.View
import android.widget.Button
import android.widget.TextView
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.android.ViewClass
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import java.util.concurrent.atomic.AtomicBoolean

//破坏神模式点击时，isOpenSuperResolution 被 Hook 返回 false。
//
//超境画质点击时，getPerformanceMode 若返回 5 则改为 3（但当前模式是3，所以没触发）。
//
//破坏神开启后，updatePerformanceMode 中自动关闭被拦截（setBiabloModeEnable(false, false) 被 Hook 阻止）。
//
//破坏神模式开启时，弹窗被跳过（setBiabloModeEnable 参数从 true,true 改为 true,false）。
object NubiaSuperResolution : YukiBaseHooker() {

    private const val TAG = "NubiaSuperResolution"

    override fun onHook() {
//        // 解除超境画质与破坏神模式的互斥
//        disableBiabloSuperResolutionCheck()
//        disableSuperResolutionBiabloCheck()
//        preventAutoDisableBiabloWhenSuperResOn()
        hasEnable(Pref.Key.GameSpace.GAME_SPACE_ENABLE_SUPER_RESOLUTION_LOW){
            // 新增：阻止超竞画质在节能/均衡模式下被自动关闭
            preventAutoCloseSuperResolutionOnPowerSaving()

            // 新增：允许在节能/均衡模式下点击开启超竞画质时不弹窗
            allowSuperResolutionInPowerSavingMode()
        }
//        //隐藏在节能、均衡模式下超境模式弹窗提示
//        hasEnable(Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_ENABLE_SUPER_RESOLUTION_LOW_PROP_PROMPT){
//            hideSuperResolutionLowPowerDialog()
//        }
    }

    /**
     * 阻止在节能/均衡模式下点击“超境画质”时弹出确认对话框
     * 核心：拦截 SuperResolutionTile.showDialog() 方法
     */
    fun hideSuperResolutionLowPowerDialog() {
        "cn.nubia.gameassist.plugin.tiles.SuperResolutionTile".toClass()?.apply {
            method {
                name = "showDialog"
            }.hook {
                before {
                    YLog.debug(
                        tag = TAG,
                        msg = "拦截 SuperResolutionTile.showDialog()，阻止弹窗显示。调用栈：${Thread.currentThread().stackTrace.take(5).joinToString("\n")}"
                    )
                    result = null // 关键：阻止原方法执行，弹窗不会出现
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 SuperResolutionTile 类，弹窗拦截失败")
    }

    /**
     * 使 BiabloTile.handleClick() 忽略超境画质的开启状态
     * hook了isOpenSuperResolution并返回false，使得破坏神开启时不因超境开启而退出。
     */
    fun disableBiabloSuperResolutionCheck() {
        "cn.nubia.gameassist.performance.PerformanceModeController".toClass()?.apply {
            method {
                name = "isOpenSuperResolution"
                returnType = BooleanType
            }.hook {
                after {
                    // 检查调用栈：是否由 BiabloTile.handleClick 触发
                    val stackTrace = Thread.currentThread().stackTrace
                    val isCalledByBiabloHandle = stackTrace.any {
                        it.className == "cn.nubia.gameassist.plugin.tiles.BiabloTile" &&
                                it.methodName == "handleClick"
                    }
                    if (isCalledByBiabloHandle) {
                        YLog.debug(
                            tag = TAG,
                            msg = "isOpenSuperResolution in BiabloTile.handleClick returns false"
                        )
                        result = false
                    }
                }
            }
        } ?: error("PerformanceModeController class not found")
    }

    /**
     * 使 SuperResolutionTile.handleClick() 忽略破坏神模式的开启状态
     */
    fun disableSuperResolutionBiabloCheck() {
        "cn.nubia.gameassist.performance.PerformanceModeController".toClass()?.apply {
            method {
                name = "getPerformanceMode"
                param(StringClass)
                returnType = IntType
            }.hook {
                after {
                    val stackTrace = Thread.currentThread().stackTrace
                    val isCalledBySuperResHandle = stackTrace.any {
                        it.className == "cn.nubia.gameassist.plugin.tiles.SuperResolutionTile" &&
                                it.methodName == "handleClick"
                    }
                    if (isCalledBySuperResHandle) {
                        val original = result as? Int ?: 0
                        if (original == 5) {
                            YLog.debug(
                                tag = TAG,
                                msg = "getPerformanceMode in SuperResolutionTile.handleClick returns 5, modify to 3"
                            )
                            result = 3  // 临时改为觉醒模式，绕过破坏神检查
                        }
                    }
                }
            }
        } ?: error("PerformanceModeController class not found")
    }

    /**
     * 阻止 PerformanceModeController 因超境画质开启而自动关闭破坏神模式
     */
    fun preventAutoDisableBiabloWhenSuperResOn() {
        "cn.nubia.gameassist.performance.PerformanceModeController".toClass().apply {
            method {
                name = "setBiabloModeEnable"
                param(StringClass, BooleanType, BooleanType)
            }.hook {
                before {
                    val enable = args[1] as Boolean
                    val showDialog = args[2] as Boolean
                    // 拦截自动关闭（enable = false, showDialog = false）
                    if (!enable && !showDialog) {
                        val stackTrace = Thread.currentThread().stackTrace
                        val isCalledByUpdate = stackTrace.any {
                            it.className == "cn.nubia.gameassist.performance.PerformanceModeController" &&
                                    it.methodName == "updatePerformanceMode"
                        }
                        if (isCalledByUpdate) {
                            YLog.debug(
                                tag = TAG,
                                msg = "Block setBiabloModeEnable(false, false) from updatePerformanceMode"
                            )
                            result = null  // 阻止原方法执行
                        }
                    }
                }
            }
        } ?: error("PerformanceModeController class not found")
    }

    /**
     * 阻止 SuperResolutionViewController 在性能模式变为节能/均衡时自动关闭超竞画质
     */
    fun preventAutoCloseSuperResolutionOnPowerSaving() {
        "cn.nubia.plugin.superresolution.SuperResolutionViewController".toClass()?.apply {
            method {
                name = "updateEnableSwitchPkg"
                param(StringClass, BooleanType)
            }.hook {
                before {
                    val enable = args[1] as? Boolean ?: return@before
                    if (!enable) {
                        // 检查调用栈，仅当来自 checkGameMode 时才拦截
                        val stackTrace = Thread.currentThread().stackTrace
                        val isFromCheckGameMode = stackTrace.any {
                            it.className == "cn.nubia.plugin.superresolution.SuperResolutionViewController" &&
                                    it.methodName == "checkGameMode"
                        }
                        if (isFromCheckGameMode) {
                            YLog.debug(tag = TAG, msg = "阻止来自 checkGameMode 的自动关闭超竞画质")
                            result = null  // 拦截原方法
                        }
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 SuperResolutionViewController，自动关闭拦截失败")
    }

    /**
     * 修改 SuperResolutionTile.handleClick 中的性能模式判断，使节能/均衡模式下直接开启而不弹窗
     * 通过 Hook PerformanceModeController.getPerformanceMode 返回觉醒模式（3）欺骗点击逻辑
     */
    fun allowSuperResolutionInPowerSavingMode() {
        "cn.nubia.gameassist.performance.PerformanceModeController".toClass()?.apply {
            method {
                name = "getPerformanceMode"
                param(StringClass)
                returnType = IntType
            }.hook {
                after {
                    val stackTrace = Thread.currentThread().stackTrace
                    val isCalledBySuperResHandle = stackTrace.any {
                        it.className == "cn.nubia.gameassist.plugin.tiles.SuperResolutionTile" &&
                                it.methodName == "handleClick"
                    }
                    if (isCalledBySuperResHandle) {
                        val original = result as? Int ?: 0
                        if (original == 1 || original == 2) {
                            YLog.debug(
                                tag = TAG,
                                msg = "SuperResolutionTile.handleClick 中性能模式为 $original，改为 3 以绕过弹窗"
                            )
                            result = 3
                        }
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 PerformanceModeController，开启弹窗拦截失败")
    }
}