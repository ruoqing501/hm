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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog

/**
 * 图片展示对话框，底部只有一个“取消”按钮。
 *
 * @param visibility 控制对话框显示/隐藏的状态
 * @param title 对话框标题（可选）
 * @param imageBase64 Base64 编码的图片字符串（支持 data:image/...;base64, 前缀）
 * @param onDismissRequest 对话框关闭时的额外回调（可选）
 */
//@Composable
//fun ImageDialogWithCancel(
//    visibility: MutableState<Boolean>,
//    title: String? = null,
//    imageBase64: String,
//    onDismissRequest: (() -> Unit)? = null,
//) {
//    val hapticFeedback = LocalHapticFeedback.current
//    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
//
//    // 解码图片（仅在 imageBase64 变化时执行）
//    LaunchedEffect(imageBase64) {
//        imageBitmap = try {
//            val cleanBase64 = imageBase64.substringAfter(",") // 移除 data URI 前缀
//            val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
//            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
//            bitmap?.asImageBitmap()
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    SuperDialog(
//        title = title,
//        show = visibility,
//        onDismissRequest = {
//            onDismissRequest?.invoke()
//            visibility.value = false
//        }
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // 图片区域
//            if (imageBitmap != null) {
//                Image(
//                    bitmap = imageBitmap!!,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .heightIn(max = 300.dp)
//                        .padding(16.dp)
//                        .clip(RoundedCornerShape(12.dp))
//                )
//            } else {
//                // 图片加载失败时的提示
//                Text(
////                    text = stringResource(R.string.image_load_failed), // 请确保添加该资源
//                    text = "图片资源加载失败", // 请确保添加该资源
//                    modifier = Modifier.padding(16.dp)
//                )
//            }
//
//            // 底部取消按钮（单独一行，居中或居右均可，这里使用居中）
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 16.dp),
//                horizontalArrangement = Arrangement.Center
//            ) {
//                TextButton(
//                    text = stringResource(android.R.string.cancel), // 或使用 R.string.button_cancel
//                    minHeight = 50.dp,
//                    onClick = {
//                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
//                        onDismissRequest?.invoke()
//                        visibility.value = false
//                    }
//                )
//            }
//        }
//    }
//}


/**
 * 图片展示对话框，底部只有一个“取消”按钮。
 * 无状态设计，由上层控制显示状态。
 *
 * @param visible 是否显示对话框
 * @param title 对话框标题（可选）
 * @param imageBase64 Base64 编码的图片字符串（支持 data:image/...;base64, 前缀）
 * @param onDismissRequest 对话框关闭时的回调（必需，用于更新上层状态）
 */
@Composable
fun ImageDialogWithCancel(
    visible: Boolean,
    title: String? = null,
    imageBase64: String,
    onDismissRequest: () -> Unit,  // 必选，点击取消或外部关闭时调用
) {
    val hapticFeedback = LocalHapticFeedback.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // 将外部 visible 同步为内部 MutableState（适配 SuperDialog 的 API）
    val internalVisible = remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        internalVisible.value = visible
    }

    // 解码图片（仅在 imageBase64 变化时执行）
    LaunchedEffect(imageBase64) {
        imageBitmap = try {
            val cleanBase64 = imageBase64.substringAfter(",")
            val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    SuperDialog(
        title = title,
        show = internalVisible,  // 使用内部状态
        onDismissRequest = {
            onDismissRequest()   // 通知上层关闭
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图片区域
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            else {
                Text(
//                    text = "图片资源加载失败",  // 可替换为 stringResource
                    text = stringResource(R.string.image_load_failed),  // 可替换为 stringResource
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 底部取消按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),                  // 占满整
                    text = stringResource(R.string.image_cannel),
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismissRequest()   // 通知上层关闭
                    }
                )
            }
        }
    }
}