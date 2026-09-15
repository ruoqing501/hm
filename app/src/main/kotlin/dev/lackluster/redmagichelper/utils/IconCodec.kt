package dev.lackluster.redmagichelper.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.min

/** 小型图标编解码:居中方形裁剪并缩放到 [ICON_SIZE]px,以有损 WebP + Base64 存入偏好(磁贴图片等)。 */
object IconCodec {
    const val ICON_SIZE = 192
    private const val WEBP_QUALITY = 90
    private const val MAX_ICON_BYTES = 64 * 1024

    /** 编码为 Base64 WebP。失败或超限时返回 null。 */
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

    /** 解码 Base64 WebP 图标。 */
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
