package dev.lackluster.redmagichelper.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import dev.chrisbanes.haze.hazeEffect
import dev.lackluster.hyperx.compose.base.LocalBottomBarHazeState
import dev.lackluster.hyperx.compose.base.LocalBottomBarHazeStyle
import dev.lackluster.hyperx.compose.base.LocalLiquidBackdrop
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.ui.MainActivity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Info
import top.yukonga.miuix.kmp.icon.icons.useful.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val tabRoutes = listOf(
    Pages.MAIN,
    Pages.MODULE_SETTINGS,
    Pages.ABOUT
)

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String) {
    val items = listOf(
        NavigationItem(stringResource(R.string.nav_home), MiuixIcons.Home),
        NavigationItem(stringResource(R.string.nav_settings), MiuixIcons.Useful.Settings),
        NavigationItem(stringResource(R.string.nav_about), MiuixIcons.Useful.Info),
    )
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route ?: currentRoute
    val selected = tabRoutes.indexOf(currentDestination).coerceAtLeast(0)
    val onClick: (Int) -> Unit = { index ->
        val target = tabRoutes[index]
        if (target != currentDestination) {
            navController.navigate(target) {
                popUpTo(Pages.MAIN) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // ==================== 液态玻璃（酷安同款） ====================
    val liquidBackdrop = LocalLiquidBackdrop.current
    val hazeState = LocalBottomBarHazeState.current
    val hazeStyle = LocalBottomBarHazeStyle.current
    val blurEnabled = MainActivity.blurEnabled.value
    val liquidEnabled = MainActivity.liquidBottomBarEnabled.value

    when {
        // 液态玻璃底栏：Backdrop 可用且用户开启了液态底栏（独立于模糊开关）
        liquidBackdrop != null && liquidEnabled -> {
            LiquidBottomNavBar(
                backdrop = liquidBackdrop,
                items = items,
                selected = selected,
                onClick = onClick
            )
        }
        // 降级 1：原 Haze 模糊底栏
        hazeState != null && hazeStyle != null && blurEnabled -> {
            Box(modifier = Modifier.hazeEffect(state = hazeState, style = hazeStyle)) {
                LegacyNavigationBar(items, selected, onClick)
            }
        }
        // 降级 2：无模糊原始底栏
        else -> {
            LegacyNavigationBar(items, selected, onClick)
        }
    }
}

/** 原始 Miuix 底栏（降级方案，保持与移植前一致） */
@Composable
private fun LegacyNavigationBar(
    items: List<NavigationItem>,
    selected: Int,
    onClick: (Int) -> Unit
) {
    NavigationBar(
        items = items,
        selected = selected,
        onClick = onClick,
        color = Color.Transparent,
        defaultWindowInsetsPadding = false
    )
}

/**
 * 液态玻璃底部菜单栏（酷安 16.6 同款效果）
 *
 * 规格对照酷安资源：
 *  - liquid_bottom_navigation_height = 56dp
 *  - liquid_bottom_navigation_content_padding = 4dp
 *  - 悬浮胶囊外形 + 边缘折射透镜 lens() + 背景模糊 blur() + 色彩提纯 vibrancy()
 *  - 选中项“水滴”透镜，弹性滑动（对应酷安 LiquidGlassTabPanel）
 *  - 按住左右拖动切换选中项（对应酷安 internal/DragGestureInspector）
 */
@Composable
private fun LiquidBottomNavBar(
    backdrop: Backdrop,
    items: List<NavigationItem>,
    selected: Int,
    onClick: (Int) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val barShape = RoundedCornerShape(32.dp)
    val dropletShape = RoundedCornerShape(26.dp)
    val dropletTint = MiuixTheme.colorScheme.primary

    // 效果参数（Backdrop effects 需要 px，先按 density 换算好）
    val barBlur = with(density) { 6.dp.toPx() }
    val barLensHeight = with(density) { 16.dp.toPx() }
    val barLensAmount = with(density) { 32.dp.toPx() }
    val dropletBlur = with(density) { 2.dp.toPx() }
    val dropletLensHeight = with(density) { 24.dp.toPx() }
    val dropletLensAmount = with(density) { 48.dp.toPx() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            // 悬浮胶囊：两侧留白，酷安同款浮起观感
            .padding(horizontal = 48.dp, vertical = 8.dp)
            .height(64.dp),
        contentAlignment = Alignment.Center
    ) {
        val itemCount = items.size
        val barWidthPx = with(density) { maxWidth.toPx() }
        val itemWidthPx = barWidthPx / itemCount
        val itemWidth = with(density) { itemWidthPx.toDp() }

        // 选中“水滴”的弹性滑动动画
        val selectionAnim = remember { Animatable(selected.toFloat()) }
        LaunchedEffect(selected) {
            selectionAnim.animateTo(
                targetValue = selected.toFloat(),
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // —— 底层玻璃体：模糊 + 边缘折射 ——
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { barShape },
                    effects = {
                        vibrancy()
                        blur(barBlur)
                        lens(barLensHeight, barLensAmount, depthEffect = true)
                    },
                    onDrawSurface = {
                        // 表面一层轻高光，增强玻璃通透感
                        drawRect(Color.White.copy(alpha = 0.06f))
                    }
                )
                // 点击切换
                .pointerInput(itemCount) {
                    detectTapGestures { offset ->
                        onClick((offset.x / itemWidthPx).toInt().coerceIn(0, itemCount - 1))
                    }
                }
                // 按住左右拖动：水滴实时跟随手指，松手后吸附到最近项并切换
                .pointerInput(itemCount) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            scope.launch { selectionAnim.stop() }
                        },
                        onDragEnd = {
                            // 无论是否触发切换（落点可能仍是当前项），都弹回目标位置
                            val target = selectionAnim.value.roundToInt().coerceIn(0, itemCount - 1)
                            scope.launch {
                                selectionAnim.animateTo(
                                    targetValue = target.toFloat(),
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f)
                                )
                            }
                            onClick(target)
                        },
                        onDragCancel = {
                            scope.launch {
                                selectionAnim.animateTo(
                                    targetValue = selectionAnim.value.roundToInt()
                                        .coerceIn(0, itemCount - 1).toFloat(),
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f)
                                )
                            }
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            scope.launch {
                                selectionAnim.snapTo(
                                    (change.position.x / itemWidthPx)
                                        .coerceIn(0f, (itemCount - 1).toFloat())
                                )
                            }
                        }
                    )
                }
        ) {
            // —— 选中项“水滴”透镜（先绘制，图标在其上方） ——
            // 弹簧欠阻尼会过冲出界，padding 为负会直接崩溃，必须钳制范围
            val dropletX = with(density) {
                (itemWidthPx * selectionAnim.value)
                    .coerceIn(0f, barWidthPx - itemWidthPx)
                    .toDp()
            }
            Box(
                modifier = Modifier
                    .padding(start = dropletX)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(dropletShape)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { dropletShape },
                        effects = {
                            vibrancy()
                            blur(dropletBlur)
                            lens(dropletLensHeight, dropletLensAmount, depthEffect = true)
                        },
                        onDrawSurface = {
                            // 主题色轻染色 + 高光，让水滴滑动清晰可见
                            drawRect(dropletTint.copy(alpha = 0.16f))
                            drawRect(Color.White.copy(alpha = 0.10f))
                        }
                    )
            )
            // —— 图标行 ——
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp),
                            tint = if (index == selected) MiuixTheme.colorScheme.primary
                                   else MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            }
        }
    }
}

val MiuixIcons.Home: ImageVector
    get() {
        if (_home != null) return _home!!
        _home = ImageVector.Builder("Home", 26.0.dp, 26.0.dp, 26.0f, 26.0f).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(12.0f, 3.594f)
                lineTo(2.167f, 11.917f)
                horizontalLineTo(4.875f)
                verticalLineTo(21.667f)
                curveTo(4.875f, 22.264f, 5.361f, 22.75f, 5.958f, 22.75f)
                horizontalLineTo(10.833f)
                verticalLineTo(16.25f)
                horizontalLineTo(15.167f)
                verticalLineTo(22.75f)
                horizontalLineTo(20.042f)
                curveTo(20.639f, 22.75f, 21.125f, 22.264f, 21.125f, 21.667f)
                verticalLineTo(11.917f)
                horizontalLineTo(23.833f)
                lineTo(12.0f, 3.594f)
                close()
            }
        }.build()
        return _home!!
    }

private var _home: ImageVector? = null
