package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia


import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.utils.Prefs
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("DiscouragedApi")
object Test : YukiBaseHooker() {
    private const val TAG = "DualRowsStatusBarHook"

    // 用于存储所有动态添加的组件 - key为资源ID，value为视图对象
    val dynamicComponents = ConcurrentHashMap<Int, View>()

    // 缓存常用资源ID
    private val statusBarStartSideContainerId by lazy {
        appContext!!.resources.getIdentifier("status_bar_start_side_container", "id", appContext!!.packageName)
    }
    private val statusBarStartSideExceptHeadsUpId by lazy {
        appContext!!.resources.getIdentifier("status_bar_start_side_except_heads_up", "id", appContext!!.packageName)
    }
    private val clockId by lazy {
        appContext!!.resources.getIdentifier("clock", "id", appContext!!.packageName)
    }
    private val notificationIconsAreaId by lazy {
        appContext!!.resources.getIdentifier("notification_icon_area", "id", appContext!!.packageName)
    }
    private val statusBarEndSideContainerId by lazy {
        appContext!!.resources.getIdentifier("status_bar_end_side_container", "id", appContext!!.packageName)
    }
    private val systemIconsId by lazy {
        appContext!!.resources.getIdentifier("system_icons", "id", appContext!!.packageName)
    }
    private val statusIconId by lazy {
        appContext!!.resources.getIdentifier("statusIcons", "id", appContext!!.packageName)
    }
    private val batteryId by lazy {
        appContext!!.resources.getIdentifier("battery", "id", appContext!!.packageName)
    }

    @SuppressLint("ResourceType")
    override fun onHook() {
        val phoneStatusBarViewClz = "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass()
        phoneStatusBarViewClz.method {
            name = "onFinishInflate"
        }.hook {
            after {
                val sbView = this.instance as FrameLayout

                // 打印布局结构用于调试
                logLayoutStructure(sbView)

                // 安全获取容器
                val leftContainer = findViewWithFallback<FrameLayout>(sbView, statusBarStartSideContainerId, "status_bar_start_side_container")
                val leftContainerLinearLayout = leftContainer?.let {
                    findViewWithFallback<LinearLayout>(it, statusBarStartSideExceptHeadsUpId, "status_bar_start_side_except_heads_up")
                } ?: return@after

                val clock = findViewWithFallback<TextView>(leftContainerLinearLayout, clockId, "clock") ?: return@after
                val notificationIcons = safeFindViewById<FrameLayout>(leftContainerLinearLayout, notificationIconsAreaId) ?:return@after


                // 右侧布局
                val rightContainer = findViewWithFallback<FrameLayout>(sbView, statusBarEndSideContainerId, "status_bar_end_side_container")
                val rightContainerLinearLayout = rightContainer?.let {
                    findViewWithFallback<LinearLayout>(it, systemIconsId, "system_icons")
                } ?: return@after

                // 电池容器
                val battery = safeFindViewById<View>(rightContainerLinearLayout, batteryId) ?:return@after

                // 查找 StatusIconContainer 并设置动态监听
                val statusIconContainer = findStatusIconContainer(rightContainerLinearLayout)
                if (statusIconContainer != null) { // 网速在这里获取
                    setupDynamicComponentMonitoring(statusIconContainer) // 处理动态添加/移除的组件，通过hashmap动态的获取组件
                }

                YLog.debug(tag = TAG, msg = "Hook execution completed. Current tracked dynamic components: ${dynamicComponents.size}")
            }
        }
    }

    /**
     * 设置动态组件监控（简化版）
     */
    private fun setupDynamicComponentMonitoring(statusIconContainer: View) {
        YLog.debug(tag = TAG, msg = "Setting up dynamic component monitoring")

        val statusIconContainerClass = "com.android.systemui.statusbar.phone.StatusIconContainer".toClass()

        // Hook onViewAdded 方法
        statusIconContainerClass.method {
            name = "onViewAdded"
        }.hook {
            after {
                val addedView = this.args[0] as View
                val resourceId = addedView.id

                // 只要ID不为NO_ID就添加到监控列表
                if (resourceId != View.NO_ID) {
                    YLog.debug(tag = TAG, msg = "Component added: ${addedView.javaClass.simpleName}, ID: ${getResourceName(resourceId)} ($resourceId)")
                    addDynamicComponent(resourceId, addedView)
                }
            }
        }

        // Hook onViewRemoved 方法
        statusIconContainerClass.method {
            name = "onViewRemoved"
        }.hook {
            after {
                val removedView = this.args[0] as View
                val resourceId = removedView.id

                if (resourceId != View.NO_ID) {
                    YLog.debug(tag = TAG, msg = "Component removed: ${removedView.javaClass.simpleName}, ID: ${getResourceName(resourceId)} ($resourceId)")
                    removeDynamicComponent(resourceId, removedView)
                }
            }
        }

        // 检查现有子视图
        if (statusIconContainer is ViewGroup) {
            for (i in 0 until statusIconContainer.childCount) {
                val child = statusIconContainer.getChildAt(i)
                if (child.id != View.NO_ID) {
                    YLog.debug(tag = TAG, msg = "Found existing component: ${child.javaClass.simpleName}, ID: ${getResourceName(child.id)}")
                    addDynamicComponent(child.id, child)
                }
            }
        }
    }

    /**
     * 添加动态组件到管理集合
     */
    private fun addDynamicComponent(resourceId: Int, component: View) {
        if (!dynamicComponents.containsKey(resourceId)) {
            dynamicComponents[resourceId] = component
            YLog.debug(tag = TAG, msg = "Added component to tracking. Total tracked: ${dynamicComponents.size}")
        }
    }

    /**
     * 从管理集合中移除动态组件
     */
    private fun removeDynamicComponent(resourceId: Int, component: View) {
        if (dynamicComponents.remove(resourceId) != null) {
            YLog.debug(tag = TAG, msg = "Removed component from tracking. Remaining tracked: ${dynamicComponents.size}")
        }
    }



    /**
     * 获取特定类型的组件
     */
    inline fun <reified T : View> getComponentsOfType(): List<T> {
        return dynamicComponents.values.filterIsInstance<T>()
    }

    // ======== 辅助方法 ========

    private fun findStatusIconContainer(parent: ViewGroup): View? {
        val statusIconContainer = safeFindViewById<View>(parent, statusIconId)
        if (statusIconContainer != null) {
            YLog.debug(tag = TAG, msg = "Found StatusIconContainer by ID")
            return statusIconContainer
        }

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.javaClass.name.contains("StatusIconContainer")) {
                YLog.debug(tag = TAG, msg = "Found StatusIconContainer by class name")
                return child
            }
            if (child is ViewGroup) {
                val nestedFind = findStatusIconContainer(child)
                if (nestedFind != null) return nestedFind
            }
        }
        return null
    }

    private inline fun <reified T : View> findViewWithFallback(parent: ViewGroup, id: Int, resourceName: String): T? {
        var result = safeFindViewById<T>(parent, id)
        if (result != null) {
            YLog.debug(tag = TAG, msg = "Found $resourceName in ${parent.javaClass.simpleName}")
            return result
        }

        YLog.debug(tag = TAG, msg = "$resourceName not found in ${parent.javaClass.simpleName}, searching upward...")
        var currentParent: ViewParent? = parent
        while (currentParent is ViewGroup) {
            result = safeFindViewById<T>(currentParent, id)
            if (result != null) {
                YLog.debug(tag = TAG, msg = "Found $resourceName in ancestor ${currentParent.javaClass.simpleName}")
                return result
            }
            currentParent = currentParent.parent
        }

        YLog.debug(tag = TAG, msg = "$resourceName not found in hierarchy, trying global search...")
        return findViewByResourceName(parent.rootView as ViewGroup, resourceName) as? T
    }

    private fun logLayoutStructure(view: ViewGroup, level: Int = 0) {
        val indent = "  ".repeat(level)
        YLog.debug(tag = TAG, msg = "${indent}View: ${view.javaClass.simpleName}, ID: ${getResourceName(view.id)}, Children: ${view.childCount}")

        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is ViewGroup) {
                logLayoutStructure(child, level + 1)
            } else {
                val childIndent = "  ".repeat(level + 1)
                YLog.debug(tag = TAG, msg = "${childIndent}Leaf: ${child.javaClass.simpleName}, ID: ${getResourceName(child.id)}")
            }
        }
    }

    private fun getResourceName(id: Int): String {
        return try {
            if (id <= 0) return "NO_ID"
            appContext!!.resources.getResourceEntryName(id)
        } catch (e: Exception) {
            "ID_$id"
        }
    }

    private fun findViewByResourceName(parent: ViewGroup, resourceName: String): View? {
        val targetId = appContext!!.resources.getIdentifier(resourceName, "id", appContext!!.packageName)
        if (targetId == 0) return null

        fun traverse(view: View): View? {
            if (view.id == targetId) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    traverse(view.getChildAt(i))?.let { return it }
                }
            }
            return null
        }

        return traverse(parent)
    }

    private inline fun <reified T : View> safeFindViewById(parent: ViewGroup, id: Int): T? {
        return try {
            val view = parent.findViewById<T>(id)
            if (view != null) {
                view
            } else {
                YLog.warn(tag = TAG, msg = "View with ID $id not found in parent.")
                null
            }
        } catch (e: ClassCastException) {
            YLog.error(tag = TAG, msg = "Failed to cast view with ID $id to ${T::class.simpleName}. $e")
            null
        }
    }
}
