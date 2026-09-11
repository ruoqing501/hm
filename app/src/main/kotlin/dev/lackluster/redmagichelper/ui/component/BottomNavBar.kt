package dev.lackluster.redmagichelper.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pages
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
        NavigationItem(stringResource(R.string.page_main), MiuixIcons.Home),
        NavigationItem(stringResource(R.string.page_module), MiuixIcons.Useful.Settings),
        NavigationItem(stringResource(R.string.page_about), MiuixIcons.Useful.Info),
    )
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route ?: currentRoute
    val selected = tabRoutes.indexOf(currentDestination).coerceAtLeast(0)
    NavigationBar(
        items = items,
        selected = selected,
        onClick = { index ->
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
        },
        color = Color.Transparent
    )
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
