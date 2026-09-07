package dev.lackluster.hyperx.compose.preference


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import dev.lackluster.hyperx.compose.base.DrawableResIcon
import dev.lackluster.hyperx.compose.base.ImageIcon
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults

/**
 * 图片展示项，顶部显示标题区域，下方显示 Base64 图片。
 *
 * @param icon 左侧图标（可选）
 * @param title 标题
 * @param summary 摘要（可选，显示在标题下方）
 * @param imageBase64 Base64 编码的图片字符串（支持 data URI 前缀，如 "data:image/png;base64,"）
 * @param titleColor 标题颜色
 * @param summaryColor 摘要颜色
 */
@Composable
fun ImagePreference(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    imageBase64: String,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
) {
    Column {
        // 顶部标题区域（无右侧操作，不可点击）
        BasicComponent(
            insideMargin = PaddingValues(icon?.getHorizontalPadding() ?: 16.dp, 16.dp, 16.dp, 16.dp),
            title = title,
            titleColor = titleColor,
            summary = summary,
            summaryColor = summaryColor,
            leftAction = {
                icon?.let {
                    DrawableResIcon(it)
                }
            },
            rightActions = {}, // 无右侧内容
            onClick = {},      // 无点击交互
        )

        // 显示 Base64 图片（如果解码成功）
        val imageBitmap = rememberBase64ImageBitmap(imageBase64)
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(12.dp)) // 圆角，与 MIUI 风格一致
            )
        }
        // 图片解码失败时不显示任何内容，可根据需要添加占位图
    }
}

/**
 * 将 Base64 字符串解码为 ImageBitmap，并在重组时缓存。
 */
@Composable
private fun rememberBase64ImageBitmap(base64: String): ImageBitmap? {
    return remember(base64) {
        try {
            val cleanBase64 = base64.substringAfter(",") // 移除 data URI 前缀
            val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}