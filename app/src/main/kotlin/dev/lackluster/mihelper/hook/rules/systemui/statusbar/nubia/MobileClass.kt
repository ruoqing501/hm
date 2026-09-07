package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.highcapable.kavaref.KavaRef
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.Modifiers
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.injectModuleAppResources
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import de.robv.android.xposed.XposedHelpers
import dev.lackluster.mihelper.BuildConfig
import dev.lackluster.mihelper.R
import dev.lackluster.mihelper.utils.DexKit
import dev.lackluster.mihelper.utils.DisplayUtils
import dev.lackluster.mihelper.utils.Prefs
import io.github.kyuubiran.ezxhelper.android.util.ViewUtil.findViewByIdName
//import dev.lackluster.mihelper.R

import io.github.kyuubiran.ezxhelper.android.util.ViewUtil.getResourceIdByName
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method
import java.util.function.Consumer
import kotlin.collections.get

/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/


object MobileClass : YukiBaseHooker() {
    //    val statusBarMobileClass by lazy {
//        "com.android.systemui.statusbar.StatusBarMobileView".toClass()
//    }
    private val TAG = "MobileClass"

    val mobileIconBinder by lazy {
        "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MobileIconBinder".toClass()
    }

    val cellularIconVM by lazy {
        "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.CellularIconViewModel".toClass()
    }
    val mobileSignalController by lazy {
        "com.android.systemui.statusbar.connectivity.MobileSignalController".toClass()
    }
    val mobileUiAdapter by lazy {
        "com.android.systemui.statusbar.pipeline.mobile.ui.MobileUiAdapter".toClass()
    }
    val modernStatusBarMobileView by lazy {
        "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView".toClass()
    }
    val statusBarIconControllerImpl by lazy {
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl".toClass()
    }
    val networkController by lazy {
        "com.android.systemui.statusbar.connectivity.NetworkControllerImpl".toClass()
    }

    val systemUIApplication by lazy {
        "com.android.systemui.SystemUIApplication".toClass()
    }





    private val rightMargin by lazy {
        Prefs.getInt("system_ui_statusbar_mobile_network_icon_right_margin", 8)-8
    }
    private val leftMargin by lazy {
        Prefs.getInt("system_ui_statusbar_mobile_network_icon_left_margin", 8)-8
    }
    private val iconScale by lazy {
        Prefs.getInt("system_ui_statusbar_mobile_network_icon_size", 10)
    }
    private val verticalOffset by lazy {
        Prefs.getInt("system_ui_statusbar_mobile_network_icon_vertical_offset", 40)
    }


    private val selectedIconStyle by lazy {
        Prefs.getString("system_ui_status_mobile_network_icon_style", "")
    }



    private val selectedIconTheme by lazy {
        Prefs.getInt("system_ui_statusbar_iconmanage_mobile_network_icon_theme", 1)
    }



    private val mobileInfo = MobileInfo
    private val dualSignalResMap = HashMap<String, Int>()
    data object MobileInfo {
        private const val ID_SUB_NO_DATA_SIM = -1 // 表示当前不是数据卡 id
        var subId = ID_SUB_NO_DATA_SIM  // 表示当前默认数据卡的 id
        var dataSimLevel = 0 //开启数据时，默认数据卡的信号等级
        var noDataSimLevel = 0 // 关闭数据时，默认数据卡的信号等级

        fun reset() {
            subId = ID_SUB_NO_DATA_SIM
            dataSimLevel = 0
            noDataSimLevel = 0
        }

        override fun toString(): String {
            return "MobileInfo(subId=$subId, dataSimLevel=$dataSimLevel, noDataSimLevel=$noDataSimLevel)"
        }
    }

    // hook
    override fun onHook() {

        loadApp("com.android.systemui") {
            val modernStatusBarMobileViewKavaRef = modernStatusBarMobileView.resolve()
            val systemUIApplicationKavaRef =systemUIApplication.resolve()
            val networkControllerKavaRef = networkController.resolve()
//            loadDualSignalRes() // 加载图标资源
            loadDualSignalRes(systemUIApplicationKavaRef) // 加载图标资源
//            listenMobileSignal() // 监听移动信号变化
        }
    }

    /*
    方法作用分析
    这个方法的主要作用是获取双卡信号图标的资源ID对，用于显示两张SIM卡的信号状态。
    返回值说明
    返回一个 Pair<Int?, Int?> 类型
    第一个元素：数据卡（当前使用数据流量的SIM卡）的信号图标资源ID
    第二个元素：非数据卡（另一张SIM卡）的信号图标资源ID
    两个元素都是可空的Int类型（Int?）
    参数含义
    isUseTint: Boolean - 是否使用着色模式
    isLight: Boolean - 是否为亮色主题

    执行流程：
    数据卡图标获取：
    使用卡槽1 (slot = 1)
    信号等级为 mobileInfo.dataSimLevel（数据卡信号强度）
    结合主题参数生成资源名称
    从 dualSignalResMap 映射中查找对应的资源ID
    非数据卡图标获取：
    使用卡槽2 (slot = 2)
    信号等级为 mobileInfo.noDataSimLevel（非数据卡信号强度）
    同样结合主题参数生成资源名称
    从映射中查找资源ID
    在整个系统中的作用
    这个方法是双卡信号图标显示的核心，
    它根据当前的信号状态和主题设置
    动态获取正确的图标资源
    支持不同的显示模式（普通/暗色/着色）
    为UI层提供准确的资源引用
    使用场景推测
    当需要更新状态栏的双卡信号显示时，系统会调用此方法获取对应的图标资源，然后应用到相应的ImageView上。
    这是一个典型的资源管理+状态映射的设计模式
    * */
    private fun getDualSignalIconPairResId(
        isUseTint: Boolean,
        isLight: Boolean
    ): Pair<Int?, Int?> {
        return Pair(
            dualSignalResMap[getSignalIconResName(1, mobileInfo.dataSimLevel, isUseTint, isLight)],
            dualSignalResMap[getSignalIconResName(2, mobileInfo.noDataSimLevel, isUseTint, isLight)]
        )
    }
    private fun getSignalIconResName(
        slot: Int,
        level: Int,
        isUseTint: Boolean,
        isLight: Boolean
    ): String {
        val iconTheme = if (selectedIconTheme == 2) {
            "statusbar_signal_oa_" // drawable 自定义的图标
        } else {
            "statusbar_signal_classic_"
        }

        val iconStyle = if (selectedIconTheme != 1 && selectedIconStyle?.isNotEmpty() == true) {
            "_$selectedIconStyle"
        } else {
            ""
        }
        val colorMode = if ((!isUseTint || selectedIconStyle == "theme")) {
            if (!isLight) {
                "_dark"
            } else {
                ""
            }
        } else {
            "_tint"
        }

        return "$iconTheme${slot}_$level$colorMode$iconStyle"
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadDualSignalRes(kavaRef: KavaRef.MemberScope<Any>) = kavaRef.apply {
        firstMethod {
            modifiers(Modifiers.PUBLIC)
            name = "onCreate"
        }.hook{
            var isHooked = false
            after {
                if (!isHooked) {
                    isHooked = true

                    // 直接注入模块资源到宿主Context
                    instance<Context>().also { context ->
                        context.resources.injectModuleAppResources()

                        /*
                        三重循环结构：
                        slot: 卡槽编号 (1-2)，对应双卡
                        lvl: 信号等级 (0-5)，0表示无信号，5表示满格
                        colorMode: 颜色模式 ("", "dark", "tint")，分别代表普通、暗色、着色模式
                        isUseTint: 判断是否使用着色模式
                        isLight: 判断是否为亮色主题（非空即为亮色）
                        */
                        (1..2).forEach { slot ->
                            (0..5).forEach { lvl ->
                                arrayOf("", "dark", "tint").forEach { colorMode ->
                                    val isUseTint = colorMode == "tint"
                                    val isLight = colorMode.isNotEmpty()

                                    /*
                                    调用辅助函数生成标准化的资源名称，格式如：
                                    statusbar_signal_classic_1_3 (经典样式，卡槽1，3格信号)
                                    statusbar_signal_oa_2_0_dark (OA样式，卡槽2，0格信号，暗色)
                                    */
                                    val dualIconResName = getSignalIconResName(slot, lvl, isUseTint, isLight)

                                    // 使用反射获取对应的R.drawable资源ID
                                    try {
                                        val resId = R.drawable::class.java.getDeclaredField(dualIconResName).getInt(null)
                                        if (resId != 0) {
                                            dualSignalResMap[dualIconResName] = resId
                                            YLog.debug(tag = TAG, msg = "Loaded resource: $dualIconResName -> $resId")
                                        } else {
                                            YLog.warn(tag = TAG, msg = "Resource ID is 0: $dualIconResName")
                                        }
                                    } catch (e: Exception) {
                                        YLog.warn(tag = TAG, msg = "Resource not found: $dualIconResName, error: ${e.message}")
                                    }
                                }
                            }
                        }
                        YLog.info(tag = TAG, msg = "Dual signal resources loaded with injection, total: ${dualSignalResMap.size}")
                    }
                }
            }
        }
    }

//    private fun getStrengthId(){
//       val serviceStateClz = "android.telephony.ServiceState".toClass()
//        "com.zte.feature.signal.icons.StrengthIcon".toClass().method {
//            name = "getStrengthId"
//            param(IntType,IntType, BooleanType, BooleanType, BooleanType,serviceStateClz)
//        }.hook{
//            before {
//                YLog.debug(tag = TAG, msg = "SIM卡：${args[0]} 信号等级：${args[1]} 数据链接是否断开：${args[2]} " +
//                                 "参数3：${args[3]} 参数4：${args[4]} 参数5：${args[5]}")
//            }
//            after {
//                YLog.debug(tag = TAG, msg = "返回值：${result}")
//            }
//        }
//    }








    private fun listenMobileSignal() {
        // 标记是否有SIM卡被移除（订阅数量减少）
        var hasStopUseOneSim = false

        // Hook MobileSignalController 的 notifyListeners 方法
        // 该方法在移动信号状态变化（信号强度、数据连接等）时被调用
        mobileSignalController.method {
            name = "notifyListeners"
        }.hook {
            after { // 方法执行后，获取当前最新状态
                val signalController = this.instance // 当前 MobileSignalController 实例

                // 获取 mCurrentState 字段，该字段位于父类 SignalController 中
                // 对应 Java 源码：SignalController 中的 protected T mCurrentState;
                val currentState = signalController.current().field {
                    name = "mCurrentState"
                    superClass() // 指定在父类中查找
                }.any() // 类型为 MobileState

                // 从 MobileState 中获取 dataSim 字段，表示当前 SIM 是否是数据卡
                // 对应 Java 源码：MobileState 类的 dataSim 字段
                val dataSim = currentState?.current()?.field {
                    name = "dataSim"
                }?.boolean() ?: false

                // 获取信号强度对象 SignalStrength
                // 对应 Java 源码：MobileState 类的 signalStrength 字段
                val signalStrength = currentState?.current()?.field {
                    name = "signalStrength"
                }?.any()

                // 调用 SignalStrength.getLevel() 获取信号强度等级（0~4/5）
                val level = signalStrength?.current()?.method {
                    name = "getLevel"
                }?.int()


                var isChanged: Boolean

                if (dataSim) {
                    // 如果是数据卡，获取其 SubscriptionInfo 对象
                    // 对应 Java 源码：MobileSignalController 的 mSubscriptionInfo 字段
                    val subscriptionInfo = signalController.current().field {
                        name = "mSubscriptionInfo"
                    }.any()

                    // 获取订阅ID
                    val subscriptionId = subscriptionInfo?.current()?.method {
                        name = "getSubscriptionId"
                    }?.int() ?: -1

                    // 检查订阅ID是否改变
                    val isSubIdChanged = subscriptionId != mobileInfo.subId

                    // 状态变化条件：订阅ID改变 或 信号等级改变
                    isChanged = isSubIdChanged || level != mobileInfo.dataSimLevel

                    // 更新 MobileInfo 中数据卡的相关信息
                    mobileInfo.subId = subscriptionId
                    mobileInfo.dataSimLevel = level ?: 0


                } else {
                    // 非数据卡：仅信号等级可能变化
                    isChanged = level != mobileInfo.noDataSimLevel
                    mobileInfo.noDataSimLevel = level ?: 0
                }

                // 如果之前检测到SIM卡数量减少，强制标记变化并将非数据卡信号等级置0
                if (hasStopUseOneSim) {
                    isChanged = true
                    mobileInfo.noDataSimLevel = 0
                }

                // 如果无变化或数据卡ID无效，提前返回（不触发后续可能存在的逻辑）
                if (!isChanged || mobileInfo.subId == -1) {
                    return@after
                }
                // 注：此处可添加后续处理，例如更新UI等（原代码未体现）
            }
        }

        // Hook NetworkControllerImpl 的 setCurrentSubscriptionsLocked 方法
        // 该方法在订阅列表变化（如插拔SIM卡）时被调用
        networkController.method {
            name = "setCurrentSubscriptionsLocked"
        }.hook {
            before { // 方法执行前，获取新旧列表以检测数量变化
                val networkController = this.instance // 当前 NetworkControllerImpl 实例
                // 第一个参数：新的订阅列表 List<SubscriptionInfo>
                val subList = this.args(0).any() as List<*>

                // 获取当前存储的订阅列表 mCurrentSubscriptions
                // 对应 Java 源码：NetworkControllerImpl 的 private List<SubscriptionInfo> mCurrentSubscriptions;
                val currentSubscriptions = networkController.current().field {
                    name = "mCurrentSubscriptions"
                }.cast<List<*>>()

                // 如果旧列表大小 > 新列表大小，说明有SIM卡被移除
                hasStopUseOneSim = (currentSubscriptions?.size ?: 0) > subList.size
            }
        }
    }



    private fun loadDualSignalRes() {
        // 使用 YukiHook 的写法替代 EzxHelper
        "com.android.systemui.SystemUIApplication".toClass()
            .method {
                name = "onCreate"
            }.hook {
                var isHooked = false

                after {
                    if (!isHooked) {
                        isHooked = true
                        /*
                        this.instance<Context>() → 获取到的是 SystemUI (com.android.systemui) 的应用程序上下文
                        createPackageContext(BuildConfig.APPLICATION_ID, 0) → 用 SystemUI 的上下文创建我们模块 (dev.lackluster.mihelper) 的包上下文
                        .resources → 获取我们模块的资源对象
                        所以实际上是：
                        宿主应用：com.android.systemui
                        目标模块：dev.lackluster.mihelper (当前 Xposed 模块)
                        这种做法让我们可以在 SystemUI 进程中访问我们模块的资源文件，比如图标、字符串等资源。这是 Xposed 模块开发中的常见模式。
                        * */
                        val modRes = this.instance<Context>().createPackageContext(BuildConfig.APPLICATION_ID, 0).resources

                        (1..2).forEach { slot ->
                            (0..5).forEach { lvl ->
                                arrayOf("", "dark", "tint").forEach { colorMode ->
                                    val isUseTint = colorMode == "tint"
                                    val isLight = colorMode.isNotEmpty()
                                    val dualIconResName = getSignalIconResName(slot, lvl, isUseTint, isLight)
                                    dualSignalResMap[dualIconResName] = modRes.getIdentifier(
                                        dualIconResName,
                                        "drawable",
                                        BuildConfig.APPLICATION_ID
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }




    private fun setDualRowStyle(kavaRef: KavaRef.MemberScope<Any>) = kavaRef.apply {
        firstMethod {
            modifiers(Modifiers.PUBLIC, Modifiers.STATIC, Modifiers.FINAL)
            name = "constructAndBind"
        }.hook{
            after {
                val rootView = result as ViewGroup
                val subId = rootView.current().field { name = "subId" }.int()
//                val mobileGroup = rootView.findViewByIdName("mobile_group")
                val mobileGroup = rootView.findViewByIdName("mobile_group") as LinearLayout

//                YLog.debug(tag = TAG, msg = "mobileGroup: $mobileGroup subId: $subId mobileGroup: $mobileGroup ")

                val mobileSignal = mobileGroup.findViewByIdName("mobile_signal") as ImageView
                val mobileSignal2 = ImageView(mobileSignal.context).apply {
                    adjustViewBounds = true  // 保持图标比例，避免变形
                    tag = "mobile_signal2"
                }
                // 输出它父类的类型？
                YLog.debug(tag = TAG, msg = "mobileSignal: ${mobileSignal.parent}")

                val signalGroup = mobileSignal.parent as FrameLayout
//                val signalGroup = mobileSignal.parent as ViewGroup
                signalGroup.addView(
                    mobileSignal2,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                if (verticalOffset != 40) {
                    signalGroup.translationY = DisplayUtils.dp2px(
                        (verticalOffset - 40) * 0.1f
                    ).toFloat()
                }


                val lp = mobileSignal.layoutParams as FrameLayout.LayoutParams
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                if (iconScale != 10) {
                    lp.height = DisplayUtils.dp2px(iconScale * 2.0f)
                    lp.gravity = Gravity.CENTER
                } else {
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT
                }
                mobileSignal.layoutParams = lp
                mobileSignal2.layoutParams = lp

//                EzxHelpUtils.setAdditionalInstanceField(mobileSignal, "subId", subId)
                // 使用 YukiHook 的 instances 来替代 EzxHelpUtils.setAdditionalInstanceField
                XposedHelpers.setAdditionalInstanceField(mobileSignal, "subId", subId)

            }
        }
    }
}
