package dev.lackluster.redmagichelper.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * 桌面图标/名称覆盖数据,在模块 app 进程与桌面(com.zte.mifavor.launcher)进程间共享。
 *
 * 整体以 JSON 字符串存储在 Prefs(见 [Pref.Key.SystemDesktop.APP_ICON_OVERRIDES]):
 * 模块 app 写入本地 SharedPreferences 后由 HelperApplication 同步到框架远程偏好,
 * launcher 进程通过 [dev.lackluster.redmagichelper.utils.Prefs.getString] 只读访问,
 * 因此不需要 ContentProvider 或跨进程文件共享。
 *
 * 大小限制说明:SharedPreferences 不适合存放大体积数据。图标统一居中裁剪为方形并缩放到
 * [ICON_SIZE]px 后以 WebP(有损)编码再 Base64 内联进 JSON,单条图标通常约 5~30KB。
 * 请勿为过多应用(建议几十个个以内)同时设置自定义图标,否则偏好文件过大可能影响读写性能。
 */
object LauncherIconOverrides {
    const val ICON_SIZE = 192
    private const val WEBP_QUALITY = 90
    private const val MAX_ENTRIES = 200
    private const val MAX_ICON_BYTES = 64 * 1024
    private const val MAX_NAME_LENGTH = 80

    data class Entry(
        val user: Int,
        val pkg: String,
        val name: String = "",
        val icon: String = "", // Base64 编码的 WebP 图标,空串表示使用原图标
    ) {
        val isEmpty: Boolean get() = name.isEmpty() && icon.isEmpty()
        val key: String get() = "$user:$pkg"
    }

    fun parse(json: String?): Map<String, Entry> {
        if (json.isNullOrEmpty()) return emptyMap()
        return runCatching {
            val root = JSONObject(json)
            if (root.optInt("v") != 1) return emptyMap()
            val items = root.optJSONArray("items") ?: return emptyMap()
            buildMap {
                for (i in 0 until min(items.length(), MAX_ENTRIES)) {
                    val obj = items.optJSONObject(i) ?: continue
                    val pkg = obj.optString("pkg")
                    if (pkg.isEmpty()) continue
                    val entry = Entry(
                        user = obj.optInt("user", 0),
                        pkg = pkg,
                        name = obj.optString("name").take(MAX_NAME_LENGTH),
                        icon = obj.optString("icon"),
                    )
                    if (!entry.isEmpty) put(entry.key, entry)
                }
            }
        }.getOrElse { emptyMap() }
    }

    fun serialize(entries: Map<String, Entry>): String {
        val items = JSONArray()
        entries.values.sortedBy { it.key }.forEach { entry ->
            if (entry.isEmpty) return@forEach
            items.put(JSONObject().apply {
                put("user", entry.user)
                put("pkg", entry.pkg)
                put("name", entry.name)
                put("icon", entry.icon)
            })
        }
        return JSONObject().apply {
            put("v", 1)
            put("items", items)
        }.toString()
    }

    /** 模块 app 侧:居中方形裁剪并缩放到 [ICON_SIZE],编码为 Base64 WebP。失败或超限时返回 null。 */
    fun encodeIcon(source: Bitmap): String? = runCatching {
        val square = centerCropSquare(source)
        val scaled = if (square.width != ICON_SIZE || square.height != ICON_SIZE) {
            Bitmap.createScaledBitmap(square, ICON_SIZE, ICON_SIZE, true)
        } else square
        val bytes = ByteArrayOutputStream().use { out ->
            if (!scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, out)) return null
            out.toByteArray()
        }
        if (bytes.size > MAX_ICON_BYTES) return null
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()

    /** launcher 进程侧:解码 Base64 WebP 图标。 */
    fun decodeIcon(base64: String): Bitmap? {
        if (base64.isEmpty() || base64.length > MAX_ICON_BYTES * 2) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.takeIf { it.width > 0 && it.height > 0 }
        }.getOrNull()
    }

    /** 参考 LS_Augment IconCropActivity 的 render():以中心为基准的方形裁剪。 */
    fun centerCropSquare(source: Bitmap): Bitmap {
        val edge = min(source.width, source.height)
        val left = (source.width - edge) / 2
        val top = (source.height - edge) / 2
        return Bitmap.createBitmap(source, left, top, edge, edge)
    }
}
