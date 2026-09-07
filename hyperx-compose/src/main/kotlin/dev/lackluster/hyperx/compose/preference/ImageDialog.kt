package dev.lackluster.hyperx.compose.preference


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.lackluster.hyperx.compose.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog

/**
 * 图片展示对话框，底部包含取消和确定按钮。
 *
 * @param visibility 控制对话框显示/隐藏的状态
 * @param title 对话框标题
 * @param imageBase64 Base64 编码的图片字符串（支持 data URI 前缀）
 * @param onDismissRequest 对话框关闭时的回调（点击外部或取消）
 * @param onConfirm 点击确定按钮时的回调（默认关闭对话框）
 */
@Composable
fun ImageDialog(
    visibility: MutableState<Boolean>,
    title: String?,
    imageBase64: String,
    onDismissRequest: (() -> Unit)? = null,
    onConfirm: (() -> Unit)? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // 解码图片，仅在 imageBase64 变化时重新计算
    LaunchedEffect(imageBase64) {
        imageBitmap = try {
            val cleanBase64 = imageBase64.substringAfter(",") // 移除 data URI 前缀
            val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    SuperDialog(
        title = title,
        show = visibility,
        onDismissRequest = {
            onDismissRequest?.invoke()
            visibility.value = false
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)  // 限制最大高度，防止图片过大
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp)) // 圆角与 MIUI 风格一致
                )
            } else {
                // 图片加载失败时显示提示（可根据需要替换为占位图）
                Text(
                    text = "未知", // 请确保添加该字符串资源
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 底部按钮（取消 / 确定）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.button_cancel),
                    minHeight = 50.dp,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismissRequest?.invoke()
                        visibility.value = false
                    }
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.button_ok),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    minHeight = 50.dp,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        onConfirm?.invoke()
                        visibility.value = false
                    }
                )
            }
        }
    }
}