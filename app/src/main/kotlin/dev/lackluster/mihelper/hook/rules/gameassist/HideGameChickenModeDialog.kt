package dev.lackluster.mihelper.hook.rules.gameassist

import android.app.Dialog
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.highcapable.yukihookapi.hook.type.android.ViewClass
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.hook.rules.gameassist.NubiaSuperResolution.allowSuperResolutionInPowerSavingMode
import dev.lackluster.mihelper.hook.rules.gameassist.NubiaSuperResolution.disableBiabloSuperResolutionCheck
import dev.lackluster.mihelper.hook.rules.gameassist.NubiaSuperResolution.disableSuperResolutionBiabloCheck
import dev.lackluster.mihelper.hook.rules.gameassist.NubiaSuperResolution.preventAutoCloseSuperResolutionOnPowerSaving
import dev.lackluster.mihelper.hook.rules.gameassist.NubiaSuperResolution.preventAutoDisableBiabloWhenSuperResOn
import dev.lackluster.mihelper.utils.factory.hasEnable
import java.util.concurrent.atomic.AtomicBoolean

object HideGameChickenModeDialog : YukiBaseHooker() {

    private const val TAG = "HideGameChickenModeDialog"
    // 关键：用于通信的全局状态标志
    private val isBiabloModeChanging = AtomicBoolean(false)

    override fun onHook() {
        // 1. Hook 破坏神模式点击事件（设置防折叠标志）
        hookBiabloTileClick()
//        // 2. Hook 超竞画质点击事件（设置防折叠标志）
//        hookBiabloTileClickSuperResolution()
        // 3. 拦截因破坏神模式等标签切换而触发的面板折叠
        preventPanelCollapseOnBiabloChange()

    }


    /**
     * Hook 破坏神模式图标的点击事件
     * 无论点击开启还是关闭，都会设置标志
     */
    private fun hookBiabloTileClick() {
        val qsTileClazz = "cn.nubia.gameassist.common.QSTile".toClassOrNull()
        if (qsTileClazz == null) {
            YLog.warn(tag = TAG, msg = "QSTile 类未找到")
            return
        }
        // Hook PluginTilesAdapter 的点击事件处理方法
        "cn.nubia.gameassist.plugin.panel.PluginTilesAdapter".toClassOrNull()?.run {
            method {
                name = "lambda\$onBindViewHolder\$0"
                param(qsTileClazz, ViewClass)
            }.hook {
                before {
                    val qsTile = args[0] ?: return@before

                    // 获取 getTileLabel() 方法的引用
                    val getTileLabelMethod = qsTileClazz.method {
                        name = "getTileLabel"
                        returnType = StringClass
                    }.get(qsTile).call()

                    // 调用该方法获取标签文本
                    val tileLabel = getTileLabelMethod as? String? ?: return@before

                    YLog.debug(tag = TAG, msg = "检测到 QS Tile 点击，标签：$tileLabel")

                    // 判断是否是破坏神模式
                    if (tileLabel.contains("破坏神模式")) {

                        hasEnable(Pref.Key.GameSpace.GAME_SPACE_PREVENT_COLLAPSE){
                            isBiabloModeChanging.set(true)

                        }
                        // 2. 拦截破坏神模式设置，阻止弹窗并模拟确认操作
                        hasEnable(Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_HIDE_PROMPT){
                            interceptBiabloModeSetting()
                        }
//                        // 3. 拦截因破坏神模式切换而触发的面板折叠
//                        preventPanelCollapseOnBiabloChange()
                        // 4. 当超竞模式开启的时候，允许破坏神模式切换
                        // 解除超境画质与破坏神模式的互斥,允许在开启破坏神模式时，允许开启超境画质
                        hasEnable(Pref.Key.GameSpace.GAME_SPACE_DEVIL_MODE_ENABLE_SUPER_RESOLUTION){
                            disableBiabloSuperResolutionCheck()
                            disableSuperResolutionBiabloCheck()
                            preventAutoDisableBiabloWhenSuperResOn()
                        }



                        YLog.debug(tag = TAG, msg = "检测到破坏神模式点击，设置防折叠标志")
                    }
                    if (tileLabel.contains("超境画质")) {
                        hasEnable(Pref.Key.GameSpace.GAME_SPACE_PREVENT_COLLAPSE){
                            isBiabloModeChanging.set(true)

                        }
                        YLog.debug(tag = TAG, msg = "检测到超境画质点击，设置防折叠标志")
                    }

                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 PluginTilesAdapter 类，点击事件Hook失败")
    }

    /**
     * 核心修改：拦截 PerformanceModeController.setBiabloModeEnable
     * 目标：当要开启Biablo模式时，阻止弹窗，直接执行开启逻辑。
     */
    private fun interceptBiabloModeSetting() {
        "cn.nubia.gameassist.performance.PerformanceModeController".toClassOrNull()?.run {
            // Hook setBiabloModeEnable(String packageName, boolean enable, boolean showDialog)
            method {
                name = "setBiabloModeEnable"
                param(StringClass, BooleanType, BooleanType) // 根据文档，第三个参数是 boolean showDialog
            }.hook {
                before {
                    val packageName = args[0] as? String
                    val enable = args[1] as? Boolean ?: false
                    val showDialog = args[2] as? Boolean ?: true

                    YLog.debug(tag = TAG, msg = "拦截 setBiabloModeEnable: pkg=$packageName, enable=$enable, showDialog=$showDialog")

                    // 核心逻辑：如果是要开启模式(enable==true)，并且需要显示弹窗(showDialog==true)，则阻止原方法，并执行我们的逻辑。
                    if (enable && showDialog) {
                        YLog.debug(tag = TAG, msg = "阻止弹窗显示，并直接执行开启逻辑")

                        // 获取当前实例
                        val controller = instance<Any>()

                        try {
                            // 查找不弹窗的版本的方法：setBiabloModeEnable(String, boolean, boolean)
                            val methodNoDialog = controller.javaClass.getMethod(
                                "setBiabloModeEnable",
                                StringClass,
                                BooleanType,
                                BooleanType
                            )

                            // 调用原方法，但第三个参数传 false，表示"不显示确认弹窗"
                            methodNoDialog.invoke(controller, packageName, true, false)
                            YLog.debug(tag = TAG, msg = "已调用不弹窗的开启方法")

                            // 阻止原始调用（即不会再去走 showBiabloModeDialog 的流程）
                            this.result = null
                            return@before
                        } catch (e: Exception) {
                            YLog.error(tag = TAG, msg = "调用不弹窗方法失败: ${e.message}")
                            // 如果调用失败，让原方法继续执行
                        }
                    }
                    // 其他情况（关闭模式，或不需弹窗的调用），让它正常执行
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 PerformanceModeController 类，弹窗拦截失败")
    }

    /**
     * 拦截面板折叠
     * 修改：当 hideWindow 被调用时，检查标志。如果是因为破坏神模式切换，则阻止本次调用。
     */
    private fun preventPanelCollapseOnBiabloChange() {
        // Hook 控制面板折叠的核心方法
        "cn.nubia.gameassist.panel.GameAssistWindowManager".toClassOrNull()?.run {
            // 根据源码，实际执行隐藏的方法是 lambda$hideWindow$9(String reason)
            method { name = "lambda\$hideWindow\$9"; paramCount = 1 }.hook {
                before {
                    val reason = args[0] as? String
                    // 检查标志：是否因为正在切换破坏神模式？
                    if (isBiabloModeChanging.get()) {
                        YLog.debug(tag = TAG, msg = "拦截因切换而触发的面板折叠，reason: $reason")
                        hasEnable(Pref.Key.GameSpace.GAME_SPACE_PREVENT_COLLAPSE){
                            isBiabloModeChanging.set(false)

                        }
                        this.result = null // 阻止原方法执行，取消折叠
                        return@before
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 GameAssistWindowManager 类，折叠拦截功能可能失效")
    }


    // 尝试处理超竞画质
    private fun hookBiabloTileClickSuperResolution() {
        val qsTileClazz = "cn.nubia.gameassist.common.QSTile".toClassOrNull()
        if (qsTileClazz == null) {
            YLog.warn(tag = TAG, msg = "QSTile 类未找到")
            return
        }
        // Hook PluginTilesAdapter 的点击事件处理方法
        "cn.nubia.gameassist.plugin.panel.PluginTilesAdapter".toClassOrNull()?.run {
            method {
                name = "lambda\$onBindViewHolder\$0"
                param(qsTileClazz, ViewClass)
            }.hook {
                before {
                    val qsTile = args[0] ?: return@before

                    // 获取 getTileLabel() 方法的引用
                    val getTileLabelMethod = qsTileClazz.method {
                        name = "getTileLabel"
                        returnType = StringClass
                    }.get(qsTile).call()

                    // 调用该方法获取标签文本
                    val tileLabel = getTileLabelMethod as? String? ?: return@before

                    YLog.debug(tag = TAG, msg = "检测到 QS Tile 点击，标签：$tileLabel")

                    // 判断是否是破坏神模式
                    if (tileLabel.contains("超境画质")) {

                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 PluginTilesAdapter 类，点击事件Hook失败")
    }




}