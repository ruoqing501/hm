package dev.lackluster.redmagichelper.hook.rules.gameassist

import android.app.Dialog
import android.content.ContentResolver
import android.provider.Settings
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

            // 移植自 LS_Augment SR-01: 点击超境画质磁贴时伪造低电量检测结果
            fakeLowPowerOnSuperResolutionClick()

            // 移植自 LS_Augment SR-03: 新 OTA 混淆名 L(String,boolean)/i 的自动关闭拦截
            preventAutoCloseSuperResolutionObfuscated()
        }
        // 移植自 LS_Augment SuperMirrorDiabloHook: 超境画质与破坏神模式共存
        hasEnable(Pref.Key.GameSpace.GAME_SPACE_SUPER_RESOLUTION_DIABLO_COEXIST){
            // SR-02 共存分支: 点击超境画质时忽略破坏神模式开启状态 (mode 5 -> 3)
            allowSuperResolutionWhenDiabloOn()

            // DB-01: 破坏神开启时忽略超境画质状态 (a0 / isOpenSuperResolution)
            disableDiabloSuperResolutionGate()

            // DB-02: 阻断因超境画质开启而自动关闭破坏神 (C0 / setBiabloModeEnable)
            blockDiabloAutoReset()
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

    private fun calledFrom(className: String, vararg methodNames: String): Boolean {
        val stackTrace = Thread.currentThread().stackTrace
        return stackTrace.any { it.className == className && it.methodName in methodNames }
    }

    /**
     * 移植自 LS_Augment SR-01: 点击超境画质磁贴时, 把 Settings.Global 的
     * "low_power" 读数伪造为 0, 绕过新 OTA 里的低电量资格拦截。
     * (handleClick 为旧 OTA 名称, T 为 GameAssist 17 混淆名)
     */
    fun fakeLowPowerOnSuperResolutionClick() {
        Settings.Global::class.java.method {
            name = "getInt"
            param(ContentResolver::class.java, StringClass, IntType)
            returnType = IntType
        }.hook {
            after {
                if (args[1] != "low_power") return@after
                val isTileClick = calledFrom(
                    "cn.nubia.gameassist.plugin.tiles.SuperResolutionTile",
                    "handleClick", "T"
                )
                if (isTileClick) {
                    YLog.debug(tag = TAG, msg = "SR-01 low_power -> 0 in SuperResolutionTile click")
                    result = 0
                }
            }
        }
    }

    /**
     * 移植自 LS_Augment SR-03: GameAssist 17 混淆名下, 阻止
     * SuperResolutionViewController.L(String, boolean) 被内部方法 i 自动关闭超竞画质。
     */
    fun preventAutoCloseSuperResolutionObfuscated() {
        "cn.nubia.plugin.superresolution.SuperResolutionViewController".toClassOrNull()?.apply {
            method {
                name = "L"
                param(StringClass, BooleanType)
            }.hook {
                before {
                    val enable = args[1] as? Boolean ?: return@before
                    if (!enable && calledFrom(
                            "cn.nubia.plugin.superresolution.SuperResolutionViewController", "i"
                        )
                    ) {
                        YLog.debug(tag = TAG, msg = "SR-03 阻止来自 i 的自动关闭超竞画质")
                        result = null
                    }
                }
            }
        } ?: YLog.debug(tag = TAG, msg = "SR-03 SuperResolutionViewController 不存在，跳过混淆名拦截")
    }

    /**
     * 移植自 LS_Augment SR-02 共存分支: 点击超境画质磁贴时把破坏神模式(5)
     * 伪报为觉醒模式(3), 绕过磁贴内的破坏神互斥检查。
     */
    fun allowSuperResolutionWhenDiabloOn() {
        "cn.nubia.gameassist.performance.PerformanceModeController".toClassOrNull()?.apply {
            method {
                name = "getPerformanceMode"
                param(StringClass)
                returnType = IntType
            }.hook {
                after {
                    val isTileClick = calledFrom(
                        "cn.nubia.gameassist.plugin.tiles.SuperResolutionTile",
                        "handleClick", "T"
                    )
                    if (isTileClick && (result as? Int) == 5) {
                        YLog.debug(tag = TAG, msg = "SR-02 getPerformanceMode 5 -> 3 (coexist)")
                        result = 3
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 PerformanceModeController，共存模式拦截失败")
    }

    /**
     * 移植自 LS_Augment DB-01: 破坏神开启路径忽略超境画质状态。
     * a0()/isOpenSuperResolution() 是不同 OTA 的同一闸门, 同时 hook, 缺失的自动跳过。
     * 触发来源: 破坏神磁贴点击 (BiabloTile.handleClick / T)
     * 或性能模式更新 (PerformanceModeController.J0)。
     */
    fun disableDiabloSuperResolutionGate() {
        "cn.nubia.gameassist.performance.PerformanceModeController".toClassOrNull()?.apply {
            method {
                name("a0", "isOpenSuperResolution")
                returnType = BooleanType
            }.hook {
                after {
                    val isDiabloPath = calledFrom(
                        "cn.nubia.gameassist.plugin.tiles.BiabloTile",
                        "handleClick", "T"
                    ) || calledFrom(
                        "cn.nubia.gameassist.performance.PerformanceModeController", "J0"
                    )
                    if (isDiabloPath) {
                        YLog.debug(tag = TAG, msg = "DB-01 super resolution gate -> false")
                        result = false
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 PerformanceModeController，破坏神闸门拦截失败")
    }

    /**
     * 移植自 LS_Augment DB-02: 阻断性能模式更新时自动关闭破坏神模式,
     * 不影响用户主动关闭。C0/setBiabloModeEnable 是不同 OTA 的同名方法。
     */
    fun blockDiabloAutoReset() {
        "cn.nubia.gameassist.performance.PerformanceModeController".toClassOrNull()?.apply {
            method {
                name("C0", "setBiabloModeEnable")
                param(StringClass, BooleanType, BooleanType)
            }.hook {
                before {
                    val enable = args[1] as? Boolean ?: return@before
                    val userAction = args[2] as? Boolean ?: return@before
                    if (!enable && !userAction && calledFrom(
                            "cn.nubia.gameassist.performance.PerformanceModeController",
                            "J0", "updatePerformanceMode"
                        )
                    ) {
                        YLog.debug(tag = TAG, msg = "DB-02 阻止自动关闭破坏神模式")
                        result = null
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 PerformanceModeController，自动复位拦截失败")
    }
}