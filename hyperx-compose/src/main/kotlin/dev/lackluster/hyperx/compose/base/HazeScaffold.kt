package dev.lackluster.hyperx.compose.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ==================== 液态玻璃移植（酷安同款 Backdrop 引擎） ====================
// 页面内容的液态录制层，底栏通过它采样背后的滚动内容做折射/模糊
val LocalLiquidBackdrop = compositionLocalOf<Backdrop?> { null }
// 原 Haze 模糊管线（液态不可用时的降级方案使用）
val LocalBottomBarHazeState = compositionLocalOf<HazeState?> { null }
val LocalBottomBarHazeStyle = compositionLocalOf<HazeStyle?> { null }

@Composable
fun HazeScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable ((contentPadding: PaddingValues) -> Unit)? = null,
    bottomBar: @Composable ((contentPadding: PaddingValues) -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MiuixTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Vertical),
    blurTopBar: Boolean = false,
    blurBottomBar: Boolean = false,
    hazeState: HazeState = remember { HazeState() },
    hazeStyle: HazeStyle = HazeStyle(
        blurRadius = 66.dp,
        backgroundColor = containerColor,
        tint = HazeTint(
            containerColor.copy(alpha = if (containerColor.luminance() >= 0.5) 0.85f else 0.75f),
        ),
    ),
    adjustPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (PaddingValues) -> Unit
) {
    // 液态录制层：把页面内容录制成可采样的 Backdrop
    val liquidBackdrop = rememberLayerBackdrop()
    CompositionLocalProvider(
        LocalLiquidBackdrop provides liquidBackdrop,
        LocalBottomBarHazeState provides hazeState,
        LocalBottomBarHazeStyle provides hazeStyle
    ) {
        Scaffold(
            modifier = modifier,
            topBar = @Composable {
                topBar?.let {
                    if (blurTopBar) {
                        Box(
                            modifier = Modifier.hazeEffect(state = hazeState, style = hazeStyle)
                        ) {
                            it(adjustPadding)
                        }
                    } else {
                        it(adjustPadding)
                    }
                }
            },
            bottomBar = @Composable {
                // 液态底栏自行绘制玻璃效果，这里不再统一包 hazeEffect，
                // 避免与 Backdrop 折射重复叠加；底栏内部的降级方案会自行取用
                // LocalBottomBarHazeState / LocalBottomBarHazeStyle。
                bottomBar?.let {
                    it(adjustPadding)
                }
            },
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            snackbarHost = snackbarHost,
            popupHost = {},
            containerColor = containerColor,
            contentWindowInsets = contentWindowInsets,
        ) { contentPadding ->
            Box(
                Modifier
                    .hazeSource(state = hazeState)
                    // 同时录制为液态 Backdrop（与 hazeSource 互不干扰）
                    .layerBackdrop(liquidBackdrop)
            ) {
                content(PaddingValues(
                    start = contentPadding.calculateLeftPadding(LayoutDirection.Ltr) +
                            adjustPadding.calculateLeftPadding(LayoutDirection.Ltr),
                    top = contentPadding.calculateTopPadding() +
                            adjustPadding.calculateTopPadding(),
                    end = contentPadding.calculateRightPadding(LayoutDirection.Ltr) +
                            adjustPadding.calculateRightPadding(LayoutDirection.Ltr),
                    bottom = contentPadding.calculateBottomPadding() +
                            adjustPadding.calculateBottomPadding()
                ))
            }
        }
    }
}