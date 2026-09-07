package dev.lackluster.hyperx.compose.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
            bottomBar?.let {
                if (blurBottomBar) {
                    Box(
                        modifier = Modifier.hazeEffect(state = hazeState, style = hazeStyle),
                    ) {
                        it(adjustPadding)
                    }
                } else {
                    it(adjustPadding)
                }
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
            Modifier.hazeSource(state = hazeState)
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