package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.net.TrafficStats
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.utils.Prefs
import java.io.File
import java.util.IdentityHashMap
import java.util.Locale
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 状态栏网格重排（移植自 LS_Augment StatusBarGridHook）。
 *
 * 接管 PhoneStatusBarView 内原生视图（时钟/通知图标/系统图标含红魔 red_magic_function_icon_container/电池）
 * 的位置：不接管视图生命周期，仅在 OnPreDraw 时按"左/中/右 × 上/下/跨排"九宫区域排序摆放，
 * 并可附加自建指标文本（CPU/GPU/电池温度、电流、功率、网速）到任意区域。
 *
 * 硬件数据读取说明：本 hook 运行在 com.android.systemui 进程，直接读取 /sys 节点；
 * 若被 SELinux 拒绝（readLong 返回 null），对应指标降级显示 "—"。电池数据亦可由
 * Intent 广播获得，此处为与 LS_Augment 行为一致沿用 sysfs，失败时已 fail-closed。
 *
 * 配置读取遵循项目约定：开关与布局配置在回调/布局阶段实时读取，切换即时生效；
 * 新增的指标文本（CPU/GPU/电池/网速）在开启开关或重建状态栏视图后挂载。
 */
@SuppressLint("DiscouragedApi")
object StatusBarGridHook : YukiBaseHooker() {
    private const val TAG = "StatusBarGridHook"
    private const val ROOT_CLASS = "com.android.systemui.statusbar.phone.PhoneStatusBarView"

    // 区域：左上/左下/左跨排/中上/中下/中跨排/右上/右下/右跨排
    private val ZONES = listOf("L1", "L2", "LS", "C1", "C2", "CS", "R1", "R2", "RS")

    private const val ID_CLOCK = "clock"
    private const val ID_NOTIFICATIONS = "notifications"
    private const val ID_SYSTEM_ICONS = "system_icons"
    private const val ID_BATTERY = "battery"
    private const val ID_CPU = "cpu"
    private const val ID_GPU = "gpu"
    private const val ID_BATTERY_TEMP = "battery_temp"
    private const val ID_CURRENT = "current"
    private const val ID_POWER = "power"
    private const val ID_NETWORK = "network"

    private val METRIC_IDS = listOf(ID_CPU, ID_GPU, ID_BATTERY_TEMP, ID_CURRENT, ID_POWER, ID_NETWORK)

    private data class ComponentDef(
        val id: String,
        val zoneKey: String, val orderKey: String, val sizeKey: String, val visibleKey: String,
        val defZone: Int, val defOrder: Int, val defSize: Int, val defVisible: Boolean
    )

    private data class GridItem(val zone: Int, val order: Int, val size: Int, val visible: Boolean)

    private val G = Pref.Key.SystemUI.StatusBarGrid

    // 默认值与 LS_Augment StatusBarGridSpec.defaults() 一致
    private val COMPONENTS = listOf(
        ComponentDef(ID_CLOCK, G.CLOCK_ZONE, G.CLOCK_ORDER, G.CLOCK_SIZE, G.CLOCK_VISIBLE, 2, 0, 13, true),
        ComponentDef(ID_NOTIFICATIONS, G.NOTIFICATIONS_ZONE, G.NOTIFICATIONS_ORDER, G.NOTIFICATIONS_SIZE, G.NOTIFICATIONS_VISIBLE, 2, 1, 13, true),
        ComponentDef(ID_SYSTEM_ICONS, G.SYSTEM_ICONS_ZONE, G.SYSTEM_ICONS_ORDER, G.SYSTEM_ICONS_SIZE, G.SYSTEM_ICONS_VISIBLE, 8, 2, 13, true),
        ComponentDef(ID_BATTERY, G.BATTERY_ZONE, G.BATTERY_ORDER, G.BATTERY_SIZE, G.BATTERY_VISIBLE, 8, 3, 13, true),
        ComponentDef(ID_CPU, G.CPU_ZONE, G.CPU_ORDER, G.CPU_SIZE, G.CPU_VISIBLE, 1, 4, 9, false),
        ComponentDef(ID_GPU, G.GPU_ZONE, G.GPU_ORDER, G.GPU_SIZE, G.GPU_VISIBLE, 1, 5, 9, false),
        ComponentDef(ID_BATTERY_TEMP, G.BATTERY_TEMP_ZONE, G.BATTERY_TEMP_ORDER, G.BATTERY_TEMP_SIZE, G.BATTERY_TEMP_VISIBLE, 4, 6, 9, false),
        ComponentDef(ID_CURRENT, G.CURRENT_ZONE, G.CURRENT_ORDER, G.CURRENT_SIZE, G.CURRENT_VISIBLE, 7, 7, 9, false),
        ComponentDef(ID_POWER, G.POWER_ZONE, G.POWER_ORDER, G.POWER_SIZE, G.POWER_VISIBLE, 7, 8, 9, false),
        ComponentDef(ID_NETWORK, G.NETWORK_ZONE, G.NETWORK_ORDER, G.NETWORK_SIZE, G.NETWORK_VISIBLE, 5, 9, 9, false),
    )

    private val gridEnabled get() = Prefs.getBoolean(G.SWITCH, false)
    private val notificationTwoRows get() = Prefs.getBoolean(G.NOTIFICATION_TWO_ROWS, true)
    private val systemTwoRows get() = Prefs.getBoolean(G.SYSTEM_TWO_ROWS, true)
    private val dualRowGapDp get() = Prefs.getInt(G.DUAL_ROW_GAP, 0).coerceIn(0, 8)
    private val marginLeftDp get() = Prefs.getInt(G.MARGIN_LEFT, 0).coerceIn(0, 40)
    private val marginRightDp get() = Prefs.getInt(G.MARGIN_RIGHT, 0).coerceIn(0, 40)
    private val marginTopDp get() = Prefs.getInt(G.MARGIN_TOP, 0).coerceIn(0, 12)
    private val marginBottomDp get() = Prefs.getInt(G.MARGIN_BOTTOM, 0).coerceIn(0, 12)
    // 0=默认(双排上下行) 1=单排仅上行 2=单排仅下载 3=单排上行+下行 4=双排上行/下行
    private val networkDisplayMode get() =
        when (Prefs.getInt(G.NETWORK_DISPLAY_MODE, 0)) {
            1, 2, 3, 4 -> Prefs.getInt(G.NETWORK_DISPLAY_MODE, 0)
            else -> 4
        }

    private val items: Map<String, GridItem> get() =
        COMPONENTS.associate { def ->
            def.id to GridItem(
                zone = Prefs.getInt(def.zoneKey, def.defZone).coerceIn(0, ZONES.size - 1),
                order = Prefs.getInt(def.orderKey, def.defOrder).coerceIn(0, 99),
                size = Prefs.getInt(def.sizeKey, def.defSize).coerceIn(6, 32),
                visible = Prefs.getBoolean(def.visibleKey, def.defVisible)
            )
        }

    // 沿用现有"通知图标最大数量"设置；0 表示不限制
    private val notificationMax get() =
        if (Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT, false)) {
            Prefs.getInt(Pref.Key.SystemUI.StatusBar.NOTIFICATION_COUNT_ICON, 3).coerceIn(0, 20)
        } else 0

    private val states = WeakHashMap<ViewGroup, State>()

    override fun onHook() {
        val rootClass = ROOT_CLASS.toClassOrNull()
        if (rootClass == null) {
            YLog.warn(tag = TAG, msg = "PhoneStatusBarView not found, grid disabled")
            return
        }
        rootClass.method { name = "onFinishInflate" }.hook {
            after {
                val owner = instance as? ViewGroup ?: return@after
                if (owner.javaClass.name != ROOT_CLASS) return@after
                synchronized(states) {
                    states.getOrPut(owner) { State(owner) }
                }.attach()
            }
        }
        // 仅在该类自身声明时 hook（避免 hook 到 View 全局实现）；缺省时由采样线程自检兜底
        rootClass.method { name = "onDetachedFromWindow" }.ignored().hook {
            after {
                val owner = instance as? ViewGroup ?: return@after
                if (owner.javaClass.name != ROOT_CLASS) return@after
                synchronized(states) { states.remove(owner) }?.detach()
            }
        }
        YLog.debug(tag = TAG, msg = "Status bar grid hook installed")
    }

    private fun px(context: Context, dp: Float): Int =
        (dp * context.resources.displayMetrics.density).roundToInt()

    private fun baseId(id: String): String = id.substringBefore('#')

    private fun resourceName(view: View): String = try {
        view.resources.getResourceEntryName(view.id)
    } catch (_: Throwable) {
        ""
    }

    private fun findViewByNames(root: View, vararg names: String): View? {
        if (names.contains(resourceName(root))) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findViewByNames(root.getChildAt(i), *names)?.let { return it }
            }
        }
        return null
    }

    private fun findViewByClass(root: View, simpleName: String): View? {
        if (root.javaClass.simpleName == simpleName) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findViewByClass(root.getChildAt(i), simpleName)?.let { return it }
            }
        }
        return null
    }

    /** 提取图标槽位名（用于识别原生网速视图），失败返回 null */
    private fun slotOf(view: View): String? {
        val className = view.javaClass.name
        if (!className.contains("StatusBar") && !className.contains("StatusIcon") &&
            !className.contains("IconView")
        ) return null
        var type: Class<*>? = view.javaClass
        while (type != null) {
            try {
                val method = type.getDeclaredMethod("getSlot")
                method.isAccessible = true
                val value = method.invoke(view)
                if (value is String) return value.trim()
            } catch (_: NoSuchMethodException) {
                // 继续向父类查找
            } catch (_: Throwable) {
                break
            }
            type = type.superclass
        }
        return null
    }

    private fun isNativeNetSpeed(view: View): Boolean {
        val slot = slotOf(view)
        if ("NET_SPEED".equals(slot, ignoreCase = true)) return true
        return view.javaClass.simpleName.lowercase(Locale.ROOT).contains("netspeed")
    }

    private fun formatNetwork(mode: Int, up: String, down: String): String = when (mode) {
        1 -> "↑$up"
        2 -> "↓$down"
        4 -> "↑$up\n↓$down"
        else -> "↑$up ↓$down"
    }

    // ========================= 打包布局（纯函数，移植自 StatusBarGridLayout） =========================

    private class Node(val id: String, val zone: String, val order: Int, width: Float, height: Float) {
        val width: Float = max(1f, width)
        val height: Float = max(1f, height)
    }

    private class Box(val x: Float, val y: Float, val width: Float, val height: Float, val scale: Float)

    private fun pack(
        nodes: List<Node>, width: Float, height: Float,
        left: Float, right: Float, top: Float, bottom: Float, gap: Float,
        cutoutLeft: Float, cutoutRight: Float
    ): Map<String, Box> {
        val result = LinkedHashMap<String, Box>()
        val usableW = max(0f, width - left - right)
        val usableH = max(0f, height - top - bottom)
        if (usableW < 3 || usableH < 2) return result
        val rowGap = min(max(0f, gap), usableH / 4)
        val rowH = (usableH - rowGap) / 2
        for (col in 0 until 3) {
            val region = "LCR"[col]
            var start = left + usableW * col / 3
            var end = left + usableW * (col + 1) / 3
            // 与挖孔相交的区域使用其更大的避让区间
            if (cutoutRight > cutoutLeft && cutoutLeft < end && cutoutRight > start) {
                val before = max(0f, cutoutLeft - start)
                val afterCut = max(0f, end - cutoutRight)
                if (before >= afterCut) end = max(start, cutoutLeft) else start = min(end, cutoutRight)
            }
            val available = max(0f, end - start)
            val group = nodes.filter { it.zone[0] == region }.sortedBy { it.order }
            val cursor = floatArrayOf(0f, 0f)
            var notificationTop = false
            var notificationBottom = false
            for (n in group) {
                if (n.id == "notifications#0" && n.zone[1] == '1') notificationTop = true
                if (n.id == "notifications#1" && n.zone[1] == '2') notificationBottom = true
            }
            var notificationsAligned = false
            val raw = LinkedHashMap<String, FloatArray>()
            for (n in group) {
                // 通知两排共享同一左缘，避免与前排内容重叠
                if (notificationTop && notificationBottom && !notificationsAligned &&
                    (n.id == "notifications#0" || n.id == "notifications#1")
                ) {
                    cursor[0] = max(cursor[0], cursor[1])
                    cursor[1] = cursor[0]
                    notificationsAligned = true
                }
                val span = n.zone[1] == 'S'
                val row = if (n.zone[1] == '2') 1 else 0
                val h = if (span) usableH else rowH
                val scale = min(1f, h / n.height)
                val w = n.width * scale
                val itemH = n.height * scale
                val x = if (span) max(cursor[0], cursor[1]) else cursor[row]
                raw[n.id] = floatArrayOf(x, 0f, w, itemH, scale, if (span) -1f else row.toFloat())
                if (span) {
                    cursor[0] = x + w + 2
                    cursor[1] = cursor[0]
                } else {
                    cursor[row] = x + w + 2
                }
            }
            val used = max(0f, max(cursor[0], cursor[1]) - 2)
            val fit = if (used == 0f) 1f else min(1f, available / used)
            val origin = when (col) {
                2 -> end - used * fit
                1 -> start + (available - used * fit) / 2
                else -> start
            }
            for ((id, a) in raw) {
                val scaledH = a[3] * fit
                // 两排共享内侧边缘，配置的 gap 即真实行间距
                val middle = top + usableH / 2
                val y = when {
                    a[5] < 0 -> middle - scaledH / 2
                    a[5] == 0f -> middle - rowGap / 2 - scaledH
                    else -> middle + rowGap / 2
                }
                result[id] = Box(origin + a[0] * fit, y, a[2] * fit, scaledH, a[4] * fit)
            }
        }
        return result
    }

    // ========================= 几何状态保存/恢复 =========================

    private class Geometry(view: View) {
        var x = 0f; var y = 0f; var sx = 0f; var sy = 0f; var alpha = 0f
        var lastX = 0f; var lastY = 0f; var lastSx = 0f; var lastSy = 0f; var lastAlpha = 0f
        var applied = false

        init {
            capture(view)
        }

        fun capture(v: View) {
            x = v.translationX; y = v.translationY
            sx = v.scaleX; sy = v.scaleY; alpha = v.alpha
        }

        fun observe(v: View) {
            if (!applied) {
                capture(v)
                return
            }
            if (v.translationX != lastX) x = v.translationX
            if (v.translationY != lastY) y = v.translationY
            if (v.scaleX != lastSx) sx = v.scaleX
            if (v.scaleY != lastSy) sy = v.scaleY
            if (v.alpha != lastAlpha) alpha = v.alpha
        }

        fun mark(v: View) {
            lastX = v.translationX; lastY = v.translationY
            lastSx = v.scaleX; lastSy = v.scaleY; lastAlpha = v.alpha
            applied = true
        }

        fun restore(v: View) {
            observe(v)
            v.translationX = x; v.translationY = y
            v.scaleX = sx; v.scaleY = sy; v.alpha = alpha
            applied = false
        }
    }

    // ========================= 每个状态栏实例的状态 =========================

    private class State(val root: ViewGroup) {
        val context: Context = root.context
        val main = Handler(Looper.getMainLooper())
        val geometry = IdentityHashMap<View, Geometry>()
        val clips = IdentityHashMap<ViewGroup, BooleanArray>()
        val metrics = LinkedHashMap<String, TextView>()
        val contentBounds = IdentityHashMap<View, Rect>()
        val groups = HashMap<String, List<View>>()
        val thermalPaths = HashMap<String, String>()
        var batteryBitmap: Bitmap? = null
        var batteryPixels: IntArray? = null
        var batteryInk: Rect? = null
        var batteryInkAt = 0L
        var batteryInkWidth = 0
        var batteryInkHeight = 0
        var overlay: FrameLayout? = null
        var attached = false
        var applying = false
        var broken = false
        // 是否正在应用网格布局（开关关闭时还原并停止指标线程）
        var gridActive = false
        var thread: HandlerThread? = null

        @Volatile
        var worker: Handler? = null

        @Volatile
        var metricsGeneration = 0L

        val preDrawListener = ViewTreeObserver.OnPreDrawListener {
            layout()
            true
        }

        fun attach() {
            if (attached) return
            attached = true
            root.viewTreeObserver.addOnPreDrawListener(preDrawListener)
            root.requestLayout()
            root.invalidate()
        }

        fun detach() {
            attached = false
            gridActive = false
            main.removeCallbacksAndMessages(null)
            stopMetrics()
            restore()
            batteryBitmap?.recycle()
            batteryBitmap = null
            batteryPixels = null
            batteryInk = null
            if (root.viewTreeObserver.isAlive) {
                root.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
            }
        }

        fun own(view: View): Geometry = geometry.getOrPut(view) { Geometry(view) }

        fun visibleInRoot(view: View): Boolean {
            var current: View? = view
            while (current != null) {
                if (current.visibility != View.VISIBLE) return false
                if (current == root) return true
                current = current.parent as? View
            }
            return false
        }

        fun two(id: String): Boolean =
            if (id == ID_NOTIFICATIONS) notificationTwoRows else systemTwoRows

        fun layout() {
            if (!gridEnabled) {
                // 开关已关闭：还原几何并停止指标线程，等待再次开启
                if (gridActive) {
                    gridActive = false
                    stopMetrics()
                    restore()
                }
                return
            }
            if (!gridActive) {
                gridActive = true
                startMetrics()
            }
            if (broken || applying || root.width == 0 || root.height == 0) return
            applying = true
            try {
                for ((v, g) in geometry) g.observe(v)
                val views = LinkedHashMap<String, View>()
                contentBounds.clear()
                groups.clear()
                val clock = findViewByNames(root, "clock", "status_bar_clock")
                val notifications = findViewByClass(root, "NotificationIconContainer")
                var systems = findViewByClass(root, "StatusIconContainer")
                if (systems == null) systems = findViewByNames(root, "statusIcons", "status_icons")
                val battery = findViewByNames(root, "battery", "battery_view")
                val nodes = ArrayList<Node>()
                add(nodes, views, ID_CLOCK, clock)
                addGroup(nodes, views, ID_NOTIFICATIONS, notifications)
                // 红魔风扇/刷新率/散热图标位于 StatusIconContainer 之外，一并收入系统图标组
                addGroup(nodes, views, ID_SYSTEM_ICONS, systems,
                    findViewByNames(root, "red_magic_function_icon_container"))
                add(nodes, views, ID_BATTERY, battery)
                // 运营商/常驻通知等复合指示器是图标列表的兄弟节点，整体保留并跟随对应组
                addCompanion(nodes, views, "clock.operator", ID_CLOCK,
                    findViewByNames(root, "operator_name_frame"), -10, false)
                addCompanion(nodes, views, "notifications.ongoing_primary", ID_NOTIFICATIONS,
                    findViewByNames(root, "ongoing_activity_chip_primary"), -9, false)
                addCompanion(nodes, views, "notifications.ongoing_secondary", ID_NOTIFICATIONS,
                    findViewByNames(root, "ongoing_activity_chip_secondary"), -8, false)
                addCompanion(nodes, views, "notifications.heads_up", ID_NOTIFICATIONS,
                    findViewByNames(root, "heads_up_status_bar_view"), -7, false)
                addCompanion(nodes, views, "notifications.join", ID_NOTIFICATIONS,
                    findViewByNames(root, "ll_notification_icon_join"), -1, true)
                addCompanion(nodes, views, "system_icons.user", ID_SYSTEM_ICONS,
                    findViewByNames(root, "user_switcher_container"), 10, false)
                for ((id, tv) in metrics) {
                    (clock as? TextView)?.let { tv.setTextColor(it.textColors) }
                    add(nodes, views, id, tv)
                }
                var cutLeft = 0f
                var cutRight = 0f
                val insets = root.rootWindowInsets
                val cutout = insets?.displayCutout
                if (cutout != null) {
                    val rp = IntArray(2)
                    root.getLocationOnScreen(rp)
                    for (r in cutout.boundingRects) {
                        if (r.top < rp[1] + root.height && r.bottom > rp[1]) {
                            cutLeft = r.left - rp[0] - px(context, 2f).toFloat()
                            cutRight = r.right - rp[0] + px(context, 2f).toFloat()
                        }
                    }
                }
                val boxes = pack(
                    nodes, root.width.toFloat(), root.height.toFloat(),
                    px(context, 4f + marginLeftDp).toFloat(),
                    px(context, 4f + marginRightDp).toFloat(),
                    px(context, marginTopDp.toFloat()).toFloat(),
                    px(context, marginBottomDp.toFloat()).toFloat(),
                    px(context, dualRowGapDp.toFloat()).toFloat(),
                    cutLeft, cutRight
                )
                for ((id, box) in boxes) place(id, views[id], box)
            } catch (t: Throwable) {
                YLog.error(tag = TAG, msg = "grid layout failed: $t")
                restore()
                broken = true
            } finally {
                applying = false
            }
        }

        fun add(nodes: MutableList<Node>, views: MutableMap<String, View>, id: String, view: View?) {
            val item = items[baseId(id)]
            if (view == null || item == null || !visibleInRoot(view)) return
            val state = own(view)
            if (!item.visible) {
                view.alpha = 0f
                state.mark(view)
                return
            }
            view.alpha = state.alpha
            if (view is TextView && metrics.containsKey(id)) {
                // setMaxLines 即使值不变也会触发 layout；仅在未完成布局时手动测量
                if (view.isLayoutRequested || view.width == 0 || view.height == 0) {
                    view.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    )
                    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                }
            }
            var h = view.height.toFloat()
            var w = view.width.toFloat()
            if (h <= 0 || w <= 0) return
            if (id == ID_BATTERY && view is ViewGroup) {
                val bounds = batteryBounds(view)
                if (!bounds.isEmpty) {
                    contentBounds[view] = bounds
                    h = bounds.height().toFloat()
                    w = bounds.width().toFloat()
                }
            }
            var scale = px(context, item.size.toFloat()) / h
            if (view is TextView) {
                val lineHeight = view.textSize
                if (lineHeight > 0) scale = px(context, item.size.toFloat()) / lineHeight
            }
            val zone = if (id.contains("#")) {
                item.zone.let { ZONES[it].substring(0, 1) + (if (id.endsWith("#0")) "1" else "2") }
            } else ZONES[item.zone]
            nodes.add(Node(id, zone, item.order * 100, w * scale, h * scale))
            views[id] = view
        }

        fun addGroup(
            nodes: MutableList<Node>, views: MutableMap<String, View>,
            id: String, parent: View?, vararg additionalParents: View?
        ) {
            val sources = ArrayList<ViewGroup>()
            if (parent is ViewGroup && visibleInRoot(parent)) sources.add(parent)
            for (extra in additionalParents) {
                if (extra is ViewGroup && visibleInRoot(extra) && extra != parent) sources.add(extra)
            }
            if (sources.isEmpty()) return
            val anchorView = parent ?: sources[0]
            val item = items[id] ?: return
            var n = 0
            val maxCount = if (id == ID_NOTIFICATIONS) notificationMax else 0
            val size = px(context, item.size.toFloat()).toFloat()
            val children = ArrayList<View>()
            for (group in sources) {
                for (i in 0 until group.childCount) {
                    val child = group.getChildAt(i)
                    if (child.visibility != View.VISIBLE || child.height == 0 || child.width == 0) continue
                    val g = own(child)
                    // 网格自绘网速时隐藏原生网速视图
                    if ((items[ID_NETWORK]?.visible == true) && isNativeNetSpeed(child)) {
                        child.alpha = 0f
                        g.mark(child)
                        continue
                    }
                    // 原生圆点/隐藏状态不能恢复成图标
                    if (g.alpha == 0f) continue
                    if (!item.visible || (maxCount > 0 && n >= maxCount)) {
                        child.alpha = 0f
                        g.mark(child)
                        continue
                    }
                    child.alpha = g.alpha
                    children.add(child)
                    n++
                }
            }
            val rows = if (two(id)) 2 else 1
            for (row in 0 until rows) {
                val members = ArrayList<View>()
                var width = 0f
                var i = row
                while (i < children.size) {
                    val child = children[i]
                    members.add(child)
                    width += child.width * size / child.height + px(context, 1f)
                    i += rows
                }
                if (members.isEmpty()) continue
                val part = if (rows == 2) "$id#$row" else id
                val zone = if (rows == 2) ZONES[item.zone].substring(0, 1) + (row + 1) else ZONES[item.zone]
                nodes.add(Node(part, zone, item.order * 100, max(1f, width - px(context, 1f)), size))
                views[part] = anchorView
                groups[part] = members
            }
        }

        fun addCompanion(
            nodes: MutableList<Node>, views: MutableMap<String, View>,
            id: String, style: String, view: View?, offset: Int, followsVisibility: Boolean
        ) {
            if (view == null || !visibleInRoot(view) || view.width <= 0 || view.height <= 0) return
            val item = items[style] ?: return
            val nativeGeometry = own(view)
            if (followsVisibility && !item.visible) {
                view.alpha = 0f
                nativeGeometry.mark(view)
                return
            }
            view.alpha = nativeGeometry.alpha
            if (nativeGeometry.alpha == 0f) return
            val height = px(context, item.size.toFloat()).toFloat()
            val width = view.width * height / view.height
            nodes.add(Node(id, ZONES[item.zone], item.order * 100 + offset, width, height))
            views[id] = view
        }

        /** 测量电池视图的实际绘制范围（原生百分比/矢量图带大量透明内边距） */
        fun batteryBounds(view: View): Rect {
            val now = SystemClock.elapsedRealtime()
            val w = view.width
            val h = view.height
            batteryInk?.let {
                if (batteryInkWidth == w && batteryInkHeight == h && now - batteryInkAt < 1000) return it
            }
            val padding = px(context, 12f)
            val bw = w + padding * 2
            val bh = h + padding * 2
            val result = Rect(0, 0, w, h)
            if (bw > 0 && bh > 0 && bw <= 768 && bh <= 768) {
                try {
                    var bitmap = batteryBitmap
                    if (bitmap == null || bitmap.width != bw || bitmap.height != bh) {
                        bitmap?.recycle()
                        bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                        batteryBitmap = bitmap
                        batteryPixels = IntArray(bw * bh)
                    }
                    bitmap.eraseColor(Color.TRANSPARENT)
                    val canvas = Canvas(bitmap)
                    canvas.translate(padding.toFloat(), padding.toFloat())
                    view.draw(canvas)
                    val pixels = batteryPixels!!
                    bitmap.getPixels(pixels, 0, bw, 0, 0, bw, bh)
                    var left = bw
                    var top = bh
                    var right = 0
                    var bottom = 0
                    for (y in 0 until bh) {
                        for (x in 0 until bw) {
                            if ((pixels[y * bw + x] ushr 24) > 16) {
                                left = min(left, x)
                                top = min(top, y)
                                right = max(right, x + 1)
                                bottom = max(bottom, y + 1)
                            }
                        }
                    }
                    if (right > left && bottom > top) {
                        result.set(left - padding, top - padding, right - padding, bottom - padding)
                    }
                } catch (_: RuntimeException) {
                }
            }
            batteryInk = result
            batteryInkAt = now
            batteryInkWidth = w
            batteryInkHeight = h
            return result
        }

        fun place(id: String, view: View?, box: Box) {
            if (view == null) return
            val children = groups[id]
            if (children != null) {
                var x = box.x
                val gap = px(context, 1f) * box.scale
                for (child in children) {
                    val h = box.height
                    val w = child.width * h / max(1, child.height)
                    move(child, x, box.y, w, h)
                    x += w + gap
                }
                return
            }
            move(view, box.x, box.y, box.width, box.height)
        }

        fun move(v: View, x: Float, y: Float, width: Float, height: Float) {
            val g = own(v)
            val bounds = contentBounds[v]
            val sx = width / max(1, bounds?.width() ?: v.width)
            val sy = height / max(1, bounds?.height() ?: v.height)
            v.scaleX = sx
            v.scaleY = sy
            // 保持亚像素精度：整数窗口坐标会把取整误差反馈进位移，导致静态文本抖动
            val parentToRoot = Matrix()
            var child: View? = v
            while (child != root && child?.parent is View) {
                val parent = child.parent as View
                parentToRoot.postTranslate(
                    (child.left - parent.scrollX).toFloat(),
                    (child.top - parent.scrollY).toFloat()
                )
                if (parent == root) break
                parentToRoot.postConcat(parent.matrix)
                child = parent
            }
            val rootToParent = Matrix()
            if (parentToRoot.invert(rootToParent)) {
                val target = floatArrayOf(x, y)
                rootToParent.mapPoints(target)
                val origin = floatArrayOf(
                    (bounds?.left ?: 0).toFloat(),
                    (bounds?.top ?: 0).toFloat()
                )
                v.matrix.mapPoints(origin)
                val dx = target[0] - origin[0]
                val dy = target[1] - origin[1]
                if (abs(dx) > .001f) v.translationX = v.translationX + dx
                if (abs(dy) > .001f) v.translationY = v.translationY + dy
            }
            g.mark(v)
            var parent = v.parent
            while (parent is ViewGroup) {
                val p = parent
                if (!clips.containsKey(p)) clips[p] = booleanArrayOf(p.clipChildren, p.clipToPadding)
                p.clipChildren = false
                p.clipToPadding = false
                if (p == root) break
                parent = p.parent
            }
        }

        fun restore() {
            for ((v, g) in geometry) g.restore(v)
            geometry.clear()
            groups.clear()
            for ((p, saved) in clips) {
                p.clipChildren = saved[0]
                p.clipToPadding = saved[1]
            }
            clips.clear()
        }

        // ========================= 指标文本（温度/电流/功率/网速） =========================

        fun startMetrics() {
            stopMetrics()
            if (METRIC_IDS.none { items[it]?.visible == true }) return
            val overlayView = FrameLayout(context).apply {
                clipChildren = false
                isClickable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
            overlay = overlayView
            root.addView(overlayView, ViewGroup.LayoutParams(-1, -1))
            for (id in METRIC_IDS) {
                val item = items[id] ?: continue
                if (!item.visible) continue
                val rows = if (id == ID_NETWORK && networkDisplayMode == 4) 2 else 1
                for (row in 0 until rows) {
                    val tv = TextView(context).apply {
                        includeFontPadding = false
                        gravity = Gravity.CENTER
                        text = "—"
                        setTextSize(item.size.toFloat())
                        setSingleLine(true)
                        ellipsize = null
                    }
                    overlayView.addView(tv, FrameLayout.LayoutParams(-2, -2))
                    metrics[if (rows == 2) "$id#$row" else id] = tv
                }
            }
            val newThread = HandlerThread("RedMagic-status-metrics", android.os.Process.THREAD_PRIORITY_BACKGROUND)
            newThread.start()
            thread = newThread
            val sessionWorker = Handler(newThread.looper)
            worker = sessionWorker
            val generation = metricsGeneration
            sessionWorker.post(object : Runnable {
                var previousRx = -1L
                var previousTx = -1L
                var previousTime = 0L

                override fun run() {
                    if (worker !== sessionWorker || metricsGeneration != generation) return
                    if (!root.isAttachedToWindow) {
                        // 视图已销毁（未 hook 到 onDetachedFromWindow 时的兜底）
                        main.post { detach() }
                        return
                    }
                    val values = LinkedHashMap<String, String>()
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    if (powerManager != null && !powerManager.isInteractive) {
                        sessionWorker.postDelayed(this, 2000)
                        return
                    }
                    if (items[ID_CPU]?.visible == true || items[ID_GPU]?.visible == true) {
                        values[ID_CPU] = temperature(ID_CPU)
                        values[ID_GPU] = temperature(ID_GPU)
                    }
                    val temp = readLong("/sys/class/power_supply/battery/temp")
                    values[ID_BATTERY_TEMP] =
                        if (temp == null) "B:—" else String.format(Locale.ROOT, "B:%.1f°", temp / 10.0)
                    val current = readLong("/sys/class/power_supply/battery/current_now")
                    val voltage = readLong("/sys/class/power_supply/battery/voltage_now")
                    values[ID_CURRENT] =
                        if (current == null) "I:—" else String.format(Locale.ROOT, "I:%.0fmA", current / 1000.0)
                    values[ID_POWER] =
                        if (current == null || voltage == null) "P:—"
                        else String.format(Locale.ROOT, "P:%.1fW", abs(current.toDouble() * voltage / 1e12))
                    val rx = TrafficStats.getTotalRxBytes()
                    val tx = TrafficStats.getTotalTxBytes()
                    val now = SystemClock.elapsedRealtime()
                    val down = if (previousRx < 0 || rx < previousRx || now <= previousTime) 0
                    else (rx - previousRx) * 1000 / (now - previousTime)
                    val up = if (previousTx < 0 || tx < previousTx || now <= previousTime) 0
                    else (tx - previousTx) * 1000 / (now - previousTime)
                    previousRx = rx
                    previousTx = tx
                    previousTime = now
                    values[ID_NETWORK] = formatNetwork(networkDisplayMode, rate(up), rate(down))
                    main.post {
                        if (!attached || metricsGeneration != generation) return@post
                        for ((id, tv) in metrics) {
                            var value = values[baseId(id)] ?: continue
                            if (id.contains("#")) {
                                val lines = value.split("\n")
                                value = lines.getOrElse(if (id.endsWith("#0")) 0 else 1) { "" }
                            }
                            if (tv.text.contentEquals(value).not()) tv.text = value
                        }
                    }
                    if (worker === sessionWorker && metricsGeneration == generation) {
                        sessionWorker.postDelayed(this, 1000)
                    }
                }
            })
        }

        fun temperature(kind: String): String {
            var path = thermalPaths[kind]
            if (path == null) {
                val zones = File("/sys/class/thermal").listFiles()
                if (zones != null) {
                    for (zone in zones) {
                        val type = readText(File(zone, "type").path).lowercase(Locale.ROOT)
                        if (type.contains(kind)) {
                            path = File(zone, "temp").path
                            thermalPaths[kind] = path
                            break
                        }
                    }
                }
            }
            val value = if (path == null) null else readLong(path)
            val c = if (value == null) Double.NaN else if (abs(value) > 300) value / 1000.0 else value.toDouble()
            val label = if (kind == ID_GPU) "G:" else "C:"
            return label + if (c.isFinite() && c > -20 && c < 150) {
                String.format(Locale.ROOT, "%.0f°", c)
            } else "—"
        }

        fun rate(value: Long): String =
            if (value >= 1024 * 1024) String.format(Locale.ROOT, "%.1fM", value / 1048576.0)
            else String.format(Locale.ROOT, "%.0fK", value / 1024.0)

        fun stopMetrics() {
            metricsGeneration++
            worker?.removeCallbacksAndMessages(null)
            worker = null
            thread?.quitSafely()
            thread = null
            overlay?.let { root.removeView(it) }
            overlay = null
            metrics.clear()
        }
    }

    private fun readText(path: String): String = try {
        File(path).bufferedReader().use { it.readLine()?.trim() ?: "" }
    } catch (_: Throwable) {
        ""
    }

    private fun readLong(path: String): Long? = try {
        readText(path).toLong()
    } catch (_: Throwable) {
        null
    }
}
