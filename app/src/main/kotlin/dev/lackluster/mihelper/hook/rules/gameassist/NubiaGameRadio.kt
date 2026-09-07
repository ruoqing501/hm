package dev.lackluster.mihelper.hook.rules.gameassist

import android.content.Context
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.android.ContextClass
import dev.lackluster.mihelper.hook.compat.type.java.IntType

// 包名：cn.nubia.gameassist
object NubiaGameRadio : YukiBaseHooker() {
    private const val TAG = "NubiaGameRadio"
    override fun onHook() {
        // 目标：cn.nubia.plugin.gameratio.GameRatioSettingsPanel
        "cn.nubia.plugin.gameratio.GameRatioSettingsPanel".toClass().apply {
            method {
                name = "setData"
                param("cn.nubia.plugin.gameratio.GameRatioData".toClass())
            }.hook {
                after {
                    val instance = this.instance
                    // 获取比例选择控件
                    val sizeChoiceView = instance.current().field { name = "mSizeChoiceView" }.any() ?: return@after

                    // 正确的方式：从对象的 class 调用 method 方法
                    val context = sizeChoiceView.current().method {
                        name = "getContext"
                        superClass()
                    }.call() as? Context ?: return@after

                    val resources = context.resources
                    val packageName = "cn.nubia.gameassist" // 资源所在包



                    // 动态获取所有比例选项的资源 ID
                    val resIdOriginal = resources.getIdentifier("gameratio_size_original", "string", packageName)
                    val resId4_3 = resources.getIdentifier("gameratio_size_4_3", "string", packageName)
                    val resId16_9 = resources.getIdentifier("gameratio_size_16_9", "string", packageName)
                    val resId21_9 = resources.getIdentifier("gameratio_size_21_9", "string", packageName)
                    val resId32_9 = resources.getIdentifier("gameratio_size_32_9", "string", packageName)

                    //// 如果 32:9 资源不存在，则跳过
                    //if (resId32_9 == 0) {
                    //    YLog.debug(tag = TAG, msg = "32:9 ratio option not found")
                    //    return@after
                    //}

                    // 获取当前游戏包名
                    val gameRatioData = this.args(0).any() ?: return@after
                    val packageName_field = gameRatioData.current().method { name = "getPackageName" }.string()

                    // 构造完整选项数组（所有游戏都提供 5 个选项）
                    val valueArray = intArrayOf(0, 1, 2, 3, 4)
                    // 构造完整选项文本数组
                    val textArray = intArrayOf(resIdOriginal, resId4_3, resId16_9, resId21_9, resId32_9)

                    // 获取当前选中值
                    val checkedId = sizeChoiceView.current().method {
                        name = "getCheckedId"
                        returnType = IntType
                    }.call() as? Int ?: 0

                    // 重新设置数据（覆盖原有限制）
                    sizeChoiceView.current().method {
                        name = "setData"
                        param(IntArray::class.java, IntArray::class.java, IntType)
                    }.call( valueArray, textArray, checkedId)

                    YLog.debug(tag = TAG, msg = "Unlocked 32:9 ratio option for $packageName_field")
                }
            }
        }
    }
}
