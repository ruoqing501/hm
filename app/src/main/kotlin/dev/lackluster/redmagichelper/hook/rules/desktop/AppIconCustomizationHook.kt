package dev.lackluster.redmagichelper.hook.rules.desktop

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.View
import dev.lackluster.redmagichelper.data.LauncherIconOverrides
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap

/**
 * 桌面个性化:按应用替换桌面图标 Bitmap 与名称,支持恢复原图标及原名称。
 * 移植自 LS_Augment 的 LauncherCustomizationHook,适配本项目的 compat DSL 与 Prefs。
 *
 * 覆盖数据(JSON,含 Base64 WebP 图标)经远程偏好下发,launcher 进程只读;
 * 在每次绑定时比对原始字符串,变更则先恢复全部条目、重新解码并请求桌面重载。
 *
 * 写库保护:onAddToDatabase/writeToValues 执行前临时把条目恢复为原始标题与图标,
 * 防止自定义值污染桌面数据库,写库完成后再重新应用覆盖。
 */
object AppIconCustomizationHook : YukiBaseHooker() {

    private const val TAG = "AppIconCustomizationHook"

    override fun onHook() {
        hasEnable(Pref.Key.SystemDesktop.APP_ICON_CUSTOMIZE_SWITCH) {
            runCatching { install() }
                .onFailure { YLog.error(tag = TAG, msg = "安装桌面个性化 Hook 失败", e = it) }
        }
    }

    private fun install() {
        val infoClass = "com.android.launcher3.model.data.z".toClassOrNull()
            ?: error("未找到 com.android.launcher3.model.data.z")
        val itemClass = "com.android.launcher3.model.data.y".toClassOrNull()
            ?: error("未找到 com.android.launcher3.model.data.y")
        val bubbleClass = "com.android.launcher3.BubbleTextView".toClassOrNull()
            ?: error("未找到 com.android.launcher3.BubbleTextView")
        val controller = Controller(infoClass, appClassLoader)

        // 图标加载:替换图标 Bitmap(桌面、抽屉、文件夹预览都经过此处)
        infoClass.method {
            name = "G"
            paramCount = 2
        }.hook {
            before {
                controller.attachContext(args[0] as? Context)
                controller.apply(instance)
            }
        }

        // 名称加载:替换 BubbleTextView 读取的条目标题
        bubbleClass.method {
            name = "B"
            paramCount = 1
        }.hook {
            before {
                controller.attachContext((instance as? View)?.context)
                args[0]?.let { controller.apply(it) }
            }
        }

        // 写库保护:写库前恢复原始值,写完再应用覆盖,避免污染桌面数据库
        val persistClasses = listOfNotNull(
            itemClass,
            "com.android.launcher3.model.data.I".toClassOrNull()
        )
        for (type in persistClasses) {
            for (m in type.declaredMethods) {
                if (m.name != "onAddToDatabase" && m.name != "writeToValues") continue
                m.isAccessible = true
                m.hook {
                    before {
                        val depth = Controller.WRITING.get()
                        Controller.WRITING.set(depth + 1)
                        if (depth == 0) controller.restore(instanceOrNull)
                    }
                    after {
                        val depth = Controller.WRITING.get() - 1
                        Controller.WRITING.set(depth)
                        if (depth == 0) controller.apply(instanceOrNull)
                    }
                }
            }
        }
        YLog.info(tag = TAG, msg = "桌面个性化 Hook 已安装")
    }

    private class Controller(
        val infoClass: Class<*>,
        val loader: ClassLoader,
    ) {
        companion object {
            val WRITING: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }
        }

        private class Original {
            var title: Any? = null
            var description: Any? = null
            var icon: Any? = null
            var appliedTitle: Any? = null
            var appliedIcon: Any? = null
            var hash: String = ""
        }

        private val mainHandler = Handler(Looper.getMainLooper())
        private val originals = Collections.synchronizedMap(WeakHashMap<Any, Original>())

        @Volatile
        private var contextRef: WeakReference<Context>? = null

        @Volatile
        private var overrides: Map<String, LauncherIconOverrides.Entry> = emptyMap()

        @Volatile
        private var images: Map<String, Bitmap> = emptyMap()

        @Volatile
        private var lastRaw: String? = null

        private val titleField: Field
        private val descriptionField: Field
        private val userField: Field
        private val itemTypeField: Field
        private val iconField: Field
        private val bitmapInfoCtor: Constructor<*>
        private val getTargetPackage: Method?

        init {
            val base = infoClass.superclass
                ?: throw IllegalStateException("桌面条目缺少父类")
            titleField = base.getField("title")
            descriptionField = base.getField("contentDescription")
            userField = base.getField("user")
            itemTypeField = base.getField("itemType")
            val bitmapInfoClass = Class.forName("s1.d", false, loader)
            iconField = infoClass.getDeclaredField("g").apply { isAccessible = true }
            if (iconField.type != bitmapInfoClass) throw IllegalStateException("桌面图标类型不匹配")
            bitmapInfoCtor = bitmapInfoClass.getConstructor(Bitmap::class.java, Int::class.javaPrimitiveType)
            getTargetPackage = generateSequence(infoClass as Class<*>) { it.superclass }
                .mapNotNull { c -> c.declaredMethods.firstOrNull { it.name == "getTargetPackage" && it.parameterCount == 0 } }
                .firstOrNull()?.apply { isAccessible = true }
        }

        fun attachContext(context: Context?) {
            if (context != null && contextRef?.get() == null) {
                contextRef = WeakReference(context.applicationContext)
            }
        }

        /** 远程偏好变更时重新加载覆盖数据,并请求桌面模型重载使改动立即生效。 */
        @Synchronized
        private fun reloadIfChanged() {
            val raw = Prefs.getString(Pref.Key.SystemDesktop.APP_ICON_OVERRIDES, "") ?: ""
            if (raw == lastRaw) return
            val parsed = LauncherIconOverrides.parse(raw)
            val decoded = HashMap<String, Bitmap>()
            var failed = 0
            for (entry in parsed.values) {
                if (entry.icon.isEmpty() || decoded.containsKey(entry.icon)) continue
                val bitmap = images[entry.icon] ?: LauncherIconOverrides.decodeIcon(entry.icon)
                if (bitmap != null) decoded[entry.icon] = bitmap else failed++
            }
            synchronized(originals) {
                for (value in ArrayList(originals.keys)) restore(value)
                originals.clear()
            }
            overrides = parsed
            images = Collections.unmodifiableMap(decoded)
            lastRaw = raw
            YLog.info(tag = TAG, msg = "已载入 ${parsed.size} 项自定义" + if (failed > 0) ",$failed 张图标解码失败" else "")
            requestModelRefresh()
        }

        /** 参考 LS_Augment:调用 LauncherApps 状态持有者触发模型重载(尽力而为)。 */
        private fun requestModelRefresh() {
            val context = contextRef?.get() ?: return
            mainHandler.post {
                runCatching {
                    val stateClass = Class.forName("com.android.launcher3.P2", false, loader)
                    val state = stateClass.getMethod("h", Context::class.java).invoke(null, context)
                    val model = state.javaClass.getMethod("j").invoke(state)
                    model.javaClass.getMethod("a0").invoke(model)
                    YLog.debug(tag = TAG, msg = "已请求桌面模型重载")
                }.onFailure {
                    YLog.warn(tag = TAG, msg = "自动刷新桌面失败,重新打开桌面后生效: ${it.message}")
                }
            }
        }

        fun apply(value: Any?) {
            if (value == null || WRITING.get() > 0 || !infoClass.isInstance(value)) return
            runCatching {
                reloadIfChanged()
                if (itemTypeField.getInt(value) != 0) return
                val pkg = getTargetPackage?.invoke(value) as? String ?: return
                val userHandle = userField.get(value) as? UserHandle ?: return
                val userId = runCatching {
                    UserHandle::class.java.getMethod("getIdentifier").invoke(userHandle) as Int
                }.getOrElse { userHandle.hashCode() }
                val entry = overrides["$userId:$pkg"]
                if (entry == null) {
                    restore(value)
                    return
                }
                synchronized(originals) {
                    val original = originals.getOrPut(value) { Original() }
                    val currentTitle = titleField.get(value)
                    val currentIcon = iconField.get(value)
                    if (original.appliedTitle == null || original.appliedTitle != currentTitle) {
                        original.title = currentTitle
                        original.description = descriptionField.get(value)
                    }
                    if (original.appliedIcon == null || original.appliedIcon !== currentIcon) {
                        original.icon = currentIcon
                        original.appliedIcon = null
                        original.hash = ""
                    }
                    if (entry.name.isNotEmpty()) {
                        original.appliedTitle = entry.name
                        titleField.set(value, entry.name)
                        contextRef?.get()?.packageManager?.let { pm ->
                            descriptionField.set(value, pm.getUserBadgedLabel(entry.name, userHandle))
                        }
                    }
                    val custom = images[entry.icon]
                    if (custom != null) {
                        if (original.appliedIcon == null || original.hash != entry.icon) {
                            // 保留工作资料/分身角标标志,仅替换为所选全彩图片
                            val copy = bitmapInfoCtor.newInstance(custom, 0)
                            for (flag in arrayOf("d", "f")) {
                                runCatching {
                                    val f = bitmapInfoCtor.declaringClass.getDeclaredField(flag)
                                    f.isAccessible = true
                                    f.set(copy, f.get(original.icon))
                                }
                            }
                            original.appliedIcon = copy
                            original.hash = entry.icon
                        }
                        iconField.set(value, original.appliedIcon)
                    }
                }
            }.onFailure { YLog.error(tag = TAG, msg = "应用桌面条目覆盖失败", e = it) }
        }

        fun restore(value: Any?) {
            if (value == null) return
            synchronized(originals) {
                val original = originals[value] ?: return
                runCatching {
                    if (original.appliedTitle != null && titleField.get(value) == original.appliedTitle) {
                        titleField.set(value, original.title)
                        descriptionField.set(value, original.description)
                    }
                    if (original.appliedIcon != null && iconField.get(value) === original.appliedIcon) {
                        iconField.set(value, original.icon)
                    }
                    original.appliedTitle = null
                    original.appliedIcon = null
                    original.hash = ""
                }
            }
        }
    }
}
