package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

/**
 * 状态栏视图类型枚举（基于日志中的5类场景精准划分）
 */
enum class StatusBarViewType {
    // 1. 主屏状态栏（顶部常驻状态栏）
    MAIN_STATUS_BAR,
    // 2. 锁屏状态栏（锁屏界面的状态栏）
    KEYGUARD_STATUS_BAR,
    // 3. 展开通知栏头部（split shade模式下的通知栏顶部）
    SHADE_HEADER_STATUS_BAR,
    // 4. 通知栏内控制中心头部（下拉通知栏中的CC头部）
    SHADE_CC_HEADER,
    // 5. 独立控制中心面板（单独打开的控制中心）
    INDEPENDENT_CONTROL_PANEL,
    // 未知类型
    UNKNOWN
}

object StatusBarHideStatusBarIcon : YukiBaseHooker() {
    private const val TAG = "StatusBarIconHide"
    private const val HOTSPOT_SLOT = "hotspot"
    private const val WIFI_SLOT = "wifi" // 补充WiFi的slot名称（日志中可见）
    private const val MOBILE_SLOT = "mobile" // 补充WiFi的slot名称（日志中可见）

    // 存储需要隐藏的视图
    private val hiddenWifiViews = mutableSetOf<View>()
    private val hiddenHotspotViews = mutableSetOf<View>()
    private val hiddenMobileViews = mutableSetOf<View>()

    // 读取配置（扩展配置值，覆盖5类场景）
    // 配置值映射：
    // 0: 不隐藏 | 1: 主屏状态栏 | 2: 锁屏状态栏 | 3: 通知栏头部 | 4: 通知栏内CC | 5: 独立控制中心 | 6: 全部隐藏
    private val hideWiFi = Prefs.getInt(Pref.Key.SystemUI.IconTurner.NUBIA_WIFI, 0)
    private val hideHotspot = Prefs.getInt(Pref.Key.SystemUI.IconTurner.NUBIA_HOTSPOT, 0)


    private val hideMobile = Prefs.getInt(Pref.Key.SystemUI.IconTurner.NUBIA_MOBILE, 0)

    /**
     * 各视图类型对应的「视图链特征」（从日志中提取的关键节点序列）
     * 匹配逻辑：视图链中包含所有关键节点即判定为对应类型
     */
    private val viewTypeFeatures = mapOf(
        // 主屏状态栏特征：StatusBarWindowView + PhoneStatusBarView
        StatusBarViewType.MAIN_STATUS_BAR to listOf("StatusBarWindowView", "PhoneStatusBarView"),
        // 锁屏状态栏特征：NotificationShadeWindowView + KeyguardStatusBarView
        StatusBarViewType.KEYGUARD_STATUS_BAR to listOf("NotificationShadeWindowView", "KeyguardStatusBarView"),
        // 通知栏头部特征：NotificationShadeWindowView + NoRemeasureMotionLayout (split_shade_status_bar)
        StatusBarViewType.SHADE_HEADER_STATUS_BAR to listOf("NotificationShadeWindowView", "NoRemeasureMotionLayout"),
        // 通知栏内CC特征：NotificationShadeWindowView + CCHeaderView + MfvQSContainerImpl
        StatusBarViewType.SHADE_CC_HEADER to listOf("NotificationShadeWindowView", "CCHeaderView", "MfvQSContainerImpl"),
        // 独立控制中心特征：ControlPanelWindowView + CCHeaderView
        StatusBarViewType.INDEPENDENT_CONTROL_PANEL to listOf("ControlPanelWindowView", "CCHeaderView")
    )

    override fun onHook() {
        YLog.debug("$TAG onHook, hideWiFi=$hideWiFi, hideHotspot=$hideHotspot")
        // Hook IconManager的addHolder，打印槽位信息（辅助调试）
        hookIconManagerAddHolder()

        // 无隐藏配置则直接返回
        if (hideWiFi == 0 && hideHotspot == 0 && hideMobile == 0) return

        // 核心Hook：拦截视图添加 + 拦截可见性修改
        hookViewGroupAddView()
        hookViewVisibility()
    }

    /**
     * Hook ViewGroup.addView - 精准拦截图标视图添加
     */
    private fun hookViewGroupAddView() {
        ViewGroup::class.java.method {
            name = "addView"
            param(View::class.java, IntType, ViewGroup.LayoutParams::class.java)
        }.hook {
            after {
                val parent = this.instance as? ViewGroup ?: return@after
                val child = args[0] as? View ?: return@after

                // 精准判断当前视图类型（基于完整视图链）
                val viewType = getViewTypeByHierarchy(parent)
//                YLog.debug("$TAG addView - 视图类型：$viewType | 子视图类名：${child::class.java.simpleName}")

                // 处理WiFi图标（优先通过Slot匹配，兜底通过类名）
                if (hideWiFi != 0 && (isTargetSlotView(child, WIFI_SLOT) || isWifiIcon(child))) {
                    if (shouldHideIcon(viewType, hideWiFi)) {
//                        YLog.debug("$TAG 隐藏WiFi图标 - 视图类型：$viewType")
//                        YLog.debug("$TAG addView - 视图类型：$viewType | 子视图类名：${child::class.java.simpleName}")

                        hiddenWifiViews.add(child)
                        child.visibility = View.GONE
                    }
                }

                // 处理热点图标（仅通过Slot匹配，日志中热点是StatusBarIconView+hotspot slot）
                if (hideHotspot != 0 && isTargetSlotView(child, HOTSPOT_SLOT)) {
                    if (shouldHideIcon(viewType, hideHotspot)) {
//                        YLog.debug("$TAG 隐藏热点图标 - 视图类型：$viewType")
                        hiddenHotspotViews.add(child)
                        child.visibility = View.GONE
                    }
                }
                // 处理 移动网络 图标（通过 slot 匹配，并兼容无 slot 的视图）
                if (hideMobile != 0 && (isTargetSlotView(child, MOBILE_SLOT) || isMobileIcon(child))) {
                    if (shouldHideIcon(viewType, hideMobile)) {
//                        YLog.debug("$TAG 隐藏移动网络图标 - 视图类型：$viewType")
                        hiddenMobileViews.add(child)
                        child.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * Hook View.setVisibility - 防止被系统重新显示
     */
    private fun hookViewVisibility() {
        View::class.java.method {
            name = "setVisibility"
            param(IntType)
        }.hook {
            before {
                val view = this.instance as? View ?: return@before
                val newVisibility = args[0] as Int

                // 拦截隐藏集合中的视图，强制设为GONE
                // 拦截隐藏集合中的视图，强制设为GONE
                if ((hiddenWifiViews.contains(view) ||
                            hiddenMobileViews.contains(view) ||
                            hiddenHotspotViews.contains(view)) && newVisibility != View.GONE) {
//                    YLog.debug("$TAG 拦截视图显示 - 视图哈希：${view.hashCode()} | 原可见性：$newVisibility")
                    args[0] = View.GONE
                }
            }
        }
    }

    /**
     * 【核心】基于完整视图链匹配，判断视图类型
     * @param viewGroup 目标ViewGroup
     * @return 精准的视图类型枚举
     */
    private fun getViewTypeByHierarchy(viewGroup: ViewGroup): StatusBarViewType {
        // 1. 获取完整的视图链（从viewGroup向上遍历到ViewRootImpl）
        val hierarchy = mutableListOf<String>()
        var current: ViewParent? = viewGroup
        while (current != null) {
            hierarchy.add(current::class.java.simpleName)
            current = current.parent
        }
//        YLog.debug("$TAG 视图链：${hierarchy.joinToString(" -> ")}")

        // 2. 匹配预定义的视图链特征
        return viewTypeFeatures.entries.firstOrNull { (_, features) ->
            // 特征列表中的所有节点都必须出现在视图链中
            features.all { feature -> hierarchy.contains(feature) }
        }?.key ?: StatusBarViewType.UNKNOWN
    }

    /**
     * 判断是否是目标Slot的视图（通过反射获取slot名称）
     * @param view 目标视图
     * @param targetSlot 目标Slot名称（如hotspot/wifi）
     */
    private fun isTargetSlotView(view: View, targetSlot: String): Boolean {
        // 先过滤基础类型：必须是StatusBarIconView
        if (!view::class.java.simpleName.contains("StatusBarIconView")) return false

        return try {
            // 优先调用getSlot()方法
            val getSlotMethod = view::class.java.getMethod("getSlot")
            val slot = getSlotMethod.invoke(view) as? String
            slot == targetSlot
        } catch (e: Exception) {
            // 兜底读取mSlot字段
            try {
                val slotField = view::class.java.getDeclaredField("mSlot")
                slotField.isAccessible = true
                val slot = slotField.get(view) as? String
                slot == targetSlot
            } catch (e2: Exception) {
                YLog.error("$TAG 获取Slot失败", e2)
                false
            }
        }
    }

    /**
     * 兜底判断WiFi图标（兼容无Slot的自定义WiFi视图）
     */
    private fun isWifiIcon(view: View): Boolean {
        val className = view::class.java.name
        return className.contains("ModernStatusBarWifiView") ||
                className.contains("NubiaStatusBarWifiView")
    }
    /**
     * 兜底判断移动网络图标（兼容无Slot的自定义移动网络视图）
     */
    private fun isMobileIcon(view: View): Boolean {
        val className = view::class.java.name
        return className.contains("ModernStatusBarMobileView") ||
                className.contains("NubiaStatusBarWifiView")
    }

    /**
     * 根据视图类型和配置值，判断是否隐藏图标
     * @param viewType 精准的视图类型
     * @param config 配置值（0=不隐藏 | 1=主屏 | 2=锁屏 | 3=通知栏头部 | 4=通知栏内CC | 5=独立控制中心 | 6=全部）
     */
    private fun shouldHideIcon(viewType: StatusBarViewType, config: Int): Boolean {
        return when (config) {
            0 -> false
//            1 -> viewType == StatusBarViewType.MAIN_STATUS_BAR
//            2 -> viewType == StatusBarViewType.KEYGUARD_STATUS_BAR
//            3 -> viewType == StatusBarViewType.SHADE_HEADER_STATUS_BAR
//            4 -> viewType == StatusBarViewType.SHADE_CC_HEADER
//            5 -> viewType == StatusBarViewType.INDEPENDENT_CONTROL_PANEL
//            6 -> true // 全部隐藏


            1 -> viewType == StatusBarViewType.MAIN_STATUS_BAR
            2 -> viewType == StatusBarViewType.SHADE_HEADER_STATUS_BAR ||
                    viewType == StatusBarViewType.SHADE_CC_HEADER
            3 -> viewType == StatusBarViewType.INDEPENDENT_CONTROL_PANEL
            4 -> true // 全部隐藏
            else -> false
        }
    }

    /**
     * Hook IconManager.addHolder - 打印槽位信息（辅助调试/验证）
     */
    private fun hookIconManagerAddHolder() {
        val holderClass = "com.android.systemui.statusbar.phone.StatusBarIconHolder".toClassOrNull() ?: return

        "com.android.systemui.statusbar.phone.ui.IconManager".toClassOrNull()?.method {
            name = "addHolder"
            param(IntType, StringClass, BooleanType, holderClass)
        }?.hook {
            before {
                val slotName = args[1] as String
                val isBlocked = args[2] as Boolean
                val viewGroup = instance.current().field {
                    name = "mGroup"
                    superClass()
                }.cast<ViewGroup?>()

//                YLog.debug(
//                    tag = TAG,
//                    msg = """
//                    === Slot信息 ===
//                    SlotName: $slotName
//                    isBlocked: $isBlocked
//                    所属容器: ${viewGroup?.javaClass?.simpleName ?: "未知"}
//                    视图链: ${viewGroup?.let { getViewHierarchyStr(it) } ?: "未知"}
//                """.trimIndent()
//                )
            }
        }
    }

    /**
     * 辅助方法：获取视图链字符串（用于日志）
     */
    private fun getViewHierarchyStr(view: ViewGroup): String {
        val hierarchy = mutableListOf<String>()
        var current: ViewParent? = view
        while (current != null) {
            hierarchy.add(current::class.java.simpleName)
            current = current.parent
        }
        return hierarchy.joinToString(" -> ")
    }
}