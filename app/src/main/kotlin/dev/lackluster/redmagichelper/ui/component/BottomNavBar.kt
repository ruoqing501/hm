package dev.lackluster.redmagichelper.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.hazeEffect
import dev.lackluster.hyperx.compose.base.LocalBottomBarHazeState
import dev.lackluster.hyperx.compose.base.LocalBottomBarHazeStyle
import dev.lackluster.hyperx.compose.base.LocalLiquidBackdrop
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.ui.component.liquid.LiquidBottomTab
import dev.lackluster.redmagichelper.ui.component.liquid.LiquidBottomTabs
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Info
import top.yukonga.miuix.kmp.icon.icons.useful.Settings

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

    // ==================== 液态玻璃（Kyant0/AndroidLiquidGlass 同款） ====================
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
 * 液态玻璃底部菜单栏，效果与 Kyant0/AndroidLiquidGlass 示例 LiquidBottomTabs 一致：
 * 胶囊玻璃体（vibrancy + blur + lens 边缘折射）、按压膨胀的“水滴”透镜
 * （色差折射 + 高光 + 外阴影 + 内阴影）、拖动阻尼跟随、松手弹性吸附、
 * 水滴区域内主题色高亮副本图标。
 */
@Composable
private fun LiquidBottomNavBar(
    backdrop: Backdrop,
    items: List<NavigationItem>,
    selected: Int,
    onClick: (Int) -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val iconColorFilter = ColorFilter.tint(contentColor)
    LiquidBottomTabs(
        selectedTabIndex = { selected },
        onTabSelected = onClick,
        backdrop = backdrop,
        tabsCount = items.size,
        // 悬浮在系统导航区上方，避免压住手势条/三大金刚键
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 36.dp, vertical = 8.dp)
    ) {
        items.forEachIndexed { index, item ->
            LiquidBottomTab(onClick = { onClick(index) }) {
                Box(
                    Modifier
                        .size(28.dp)
                        .paint(rememberVectorPainter(item.icon), colorFilter = iconColorFilter)
                )
                BasicText(
                    item.label,
                    style = TextStyle(contentColor, 12.sp)
                )
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
