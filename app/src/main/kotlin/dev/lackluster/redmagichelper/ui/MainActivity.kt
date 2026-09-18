package dev.lackluster.redmagichelper.ui

import android.os.Bundle
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import dev.lackluster.hyperx.compose.activity.HyperXActivity
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.HyperXApp
import dev.lackluster.hyperx.compose.navigation.miuixComposable
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.page.AboutDonorsPage
import dev.lackluster.redmagichelper.ui.page.AboutPage
import dev.lackluster.redmagichelper.ui.page.MainPage
import dev.lackluster.redmagichelper.ui.page.MediaControlPage
import dev.lackluster.redmagichelper.ui.page.MenuPage
import dev.lackluster.redmagichelper.ui.page.ModuleSettingsPage
import dev.lackluster.redmagichelper.ui.page.SearchPage
import dev.lackluster.redmagichelper.ui.page.ScreenOffHideAppsPage
import dev.lackluster.redmagichelper.ui.page.SystemFrameworkPage
import dev.lackluster.redmagichelper.ui.page.SystemUIPage
import dev.lackluster.redmagichelper.ui.page.UITestPage
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.ui.dialog.MarketFilterTabDialog
import dev.lackluster.redmagichelper.ui.dialog.FreeformExcludedAppsDialog
import dev.lackluster.redmagichelper.ui.page.GameSpaceMenuPage
import dev.lackluster.redmagichelper.ui.page.GameSpacePage
import dev.lackluster.redmagichelper.ui.page.MediaActionResizePage
import dev.lackluster.redmagichelper.ui.page.NubiaIconTunerPage
import dev.lackluster.redmagichelper.ui.page.OtherMenuPage
import dev.lackluster.redmagichelper.ui.page.OtherPage
import dev.lackluster.redmagichelper.ui.page.PackageInstallerPage
import dev.lackluster.redmagichelper.ui.page.StatusBaLayoutPage
import dev.lackluster.redmagichelper.ui.page.StatusBarDisplayBatteryInfoPage
import dev.lackluster.redmagichelper.ui.page.StatusBarDisplayTempPage
import dev.lackluster.redmagichelper.ui.page.StatusBarDualPage
import dev.lackluster.redmagichelper.ui.page.StatusBarGridPage
import dev.lackluster.redmagichelper.ui.page.StatusBarTimeIndicatorPage
import dev.lackluster.redmagichelper.ui.page.SystemDesktopPage
import dev.lackluster.redmagichelper.ui.page.SystemDesktopRecentTasksPage
import dev.lackluster.redmagichelper.ui.page.SystemPageFontPage
import dev.lackluster.redmagichelper.ui.page.SystemUIStatusBarPage
import dev.lackluster.redmagichelper.ui.page.SystemSettingsPage
import dev.lackluster.redmagichelper.ui.page.SystemUpdatePage
import dev.lackluster.redmagichelper.ui.page.ThemePage
import dev.lackluster.redmagichelper.utils.Device
import dev.lackluster.redmagichelper.utils.ShellUtils
import dev.lackluster.redmagichelper.utils.factory.getSP
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : HyperXActivity() {
    companion object {
        val moduleActive: MutableState<Boolean> = mutableStateOf(false)
        val moduleEnabled: MutableState<Boolean> = mutableStateOf(false)
        val blurEnabled: MutableState<Boolean> = mutableStateOf(true)
        val liquidBottomBarEnabled: MutableState<Boolean> = mutableStateOf(true)
        val blurTintAlphaLight: MutableFloatState = mutableFloatStateOf(0.6f)
        val blurTintAlphaDark: MutableFloatState = mutableFloatStateOf(0.5f)
        val splitEnabled: MutableState<Boolean> = mutableStateOf(Device.isPad)
        val rootGranted: MutableState<Boolean> = mutableStateOf(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAndCheck()
    }
    // 尝试修复黑屏问题
    override fun onResume() {
        super.onResume()
        // 确保返回时重新初始化状态
        //initAndCheck()
    }

    @Composable
    override fun AppContent() {
        HyperXApp(
            autoSplitView = splitEnabled,
            mainPageContent = { navController, adjustPadding, mode ->
                MainPage(navController, adjustPadding, mode)
            },
            emptyPageContent = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val foregroundColor: Color
                    val backgroundColor: Color
                    MiuixTheme.colorScheme.onBackground.let {
                        if (it.luminance() >= 0.5f) {
                            foregroundColor = it.copy(alpha = 0.2f)
                            backgroundColor = it.copy(alpha = 0.12f)
                        }
                        else {
                            foregroundColor = it.copy(alpha = 0.1f)
                            backgroundColor = it.copy(alpha = 0.06f)
                        }
                    }
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.empty_page_background),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(backgroundColor),
                        contentScale = ContentScale.Crop
                    )
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.empty_page_foreground),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(foregroundColor),
                        contentScale = ContentScale.Inside
                    )
                }
            },
            otherPageBuilder = { navController, adjustPadding, mode ->
                miuixComposable(
                    Pages.MODULE_SETTINGS,
                    enterTransition = { fadeIn(animationSpec = tween(200)) },
                    exitTransition = { fadeOut(animationSpec = tween(200)) }
                ) { ModuleSettingsPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SEARCH) { SearchPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SYSTEM_UI) { SystemUIPage(navController, adjustPadding, mode)}
                miuixComposable(Pages.PACKAGE_INSTALLER) { PackageInstallerPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SYSTEM_SETTINGS) { SystemSettingsPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SYSTEM_UPDATE) { SystemUpdatePage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SYSTEM_DESKTOP) { SystemDesktopPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SYSTEM_THEME) { ThemePage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SYSTEM_FRAMEWORK) { SystemFrameworkPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.OTHER) { OtherPage (navController, adjustPadding, mode) }
                miuixComposable(
                    Pages.ABOUT,
                    enterTransition = { fadeIn(animationSpec = tween(200)) },
                    exitTransition = { fadeOut(animationSpec = tween(200)) }
                ) { AboutPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.STATUS_BAR_TIME_INDICATOR) { StatusBarTimeIndicatorPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.STATUS_BAR_DUAL) { StatusBarDualPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.STATUS_BAR_GRID) { StatusBarGridPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.STATUS_BAR_DISPLAY_TEMP) { StatusBarDisplayTempPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.STATUS_BAR_DISPLAY_BATTERY_INFO) { StatusBarDisplayBatteryInfoPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.STATUS_BAR_LAYOUT) { StatusBaLayoutPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SYSTEM_PAGE_FONT) { SystemPageFontPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.STATUS_BAR) { SystemUIStatusBarPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.ABOUT_DONORS) { AboutDonorsPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.GAME_SPACE) { GameSpacePage(navController, adjustPadding, mode) }

                miuixComposable(Pages.SYSTEM_DESKTOP_RECENT_TASKS) { SystemDesktopRecentTasksPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SCREEN_OFF_HIDE_APPS) { ScreenOffHideAppsPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.OTHER_MENU) { OtherMenuPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.MENU) { MenuPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.GAMESPACE_MENU) { GameSpaceMenuPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.DEV_UI_TEST) { UITestPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.MEDIA_CONTROL) { MediaControlPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.NUBIA_ICON_TUNER) { NubiaIconTunerPage(navController, adjustPadding, mode) }

                miuixComposable(Pages.DIALOG_MARKET_FILTER_TAB) { MarketFilterTabDialog(navController, adjustPadding, mode) }
                miuixComposable(Pages.DIALOG_FREEFORM_EXCLUDED_APPS) { FreeformExcludedAppsDialog(navController, adjustPadding, mode) }
                miuixComposable(Pages.DEV_UI_TEST2) { MediaActionResizePage(navController, adjustPadding, "MediaActionResizePage", mode = mode) }
            }
        )
    }

    private fun initAndCheck() {
        try {
            SafeSP.setSP(getSP(this))
            versionCompatible()
            moduleActive.value = true
            moduleEnabled.value = SafeSP.getBoolean(Pref.Key.Module.ENABLED, false)
            blurEnabled.value = SafeSP.getBoolean(Pref.Key.App.HAZE_BLUR, true)
            liquidBottomBarEnabled.value = SafeSP.getBoolean(Pref.Key.App.LIQUID_BOTTOM_BAR, true)
            blurTintAlphaLight.floatValue =
                SafeSP.getInt(Pref.Key.App.HAZE_TINT_ALPHA_LIGHT, 60) / 100f
            blurTintAlphaDark.floatValue =
                SafeSP.getInt(Pref.Key.App.HAZE_TINT_ALPHA_LIGHT, 50) / 100f
            splitEnabled.value = SafeSP.getBoolean(Pref.Key.App.SPLIT_VIEW, Device.isPad)
            rootGranted.value = if (!SafeSP.getBoolean(Pref.Key.App.SKIP_ROOT_CHECK, false)) {
                try {
                    ShellUtils.tryExec(
                        "whoami",
                        useRoot = true,
                        checkSuccess = true
                    ).successMsg.trim().contentEquals("root")
                } catch (_: Exception) {
                    false
                }
            } else {
                true
            }
        } catch (_: SecurityException) {
            moduleActive.value = false
            moduleEnabled.value = false
            blurEnabled.value = true
            liquidBottomBarEnabled.value = true
            blurTintAlphaLight.floatValue = 0.6f
            blurTintAlphaDark.floatValue = 0.5f
            splitEnabled.value = Device.isPad
        }
    }

    private fun versionCompatible() {
        val spVersion = SafeSP.getInt(Pref.Key.Module.SP_VERSION, 0)
        if (spVersion < 2) {
            if (SafeSP.getFloat(Pref.Key.SystemUI.IconTurner.BATTERY_PADDING_LEFT, -1f) == -1f) {
                val oldValue = SafeSP.getInt(Pref.OldKey.SystemUI.IconTurner.BATTERY_PADDING_LEFT, -1)
                if (oldValue != -1) {
                    SafeSP.putAny(Pref.Key.SystemUI.IconTurner.BATTERY_PADDING_LEFT, oldValue.toFloat())
                }
            }
            if (SafeSP.getFloat(Pref.Key.SystemUI.IconTurner.BATTERY_PADDING_RIGHT, -1f) == -1f) {
                val oldValue = SafeSP.getInt(Pref.OldKey.SystemUI.IconTurner.BATTERY_PADDING_RIGHT, -1)
                if (oldValue != -1) {
                    SafeSP.putAny(Pref.Key.SystemUI.IconTurner.BATTERY_PADDING_RIGHT, oldValue.toFloat())
                }
            }
            if (SafeSP.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_PERCENTAGE_SYMBOL_STYLE, -1) == -1) {
                val hidePercentageSymbol = SafeSP.getBoolean(Pref.OldKey.SystemUI.IconTurner.HIDE_BATTERY_PERCENT_SYMBOL, false)
                val uniPercentageSymbolSize = SafeSP.getBoolean(Pref.OldKey.SystemUI.IconTurner.CHANGE_BATTERY_PERCENT_SYMBOL, false)
                val newValue =
                    if (hidePercentageSymbol)  2
                    else if (uniPercentageSymbolSize) 1
                    else 0
                SafeSP.putAny(Pref.Key.SystemUI.IconTurner.BATTERY_PERCENTAGE_SYMBOL_STYLE, newValue)
            }
        }
        if (spVersion < 4) {
            if (SafeSP.getInt(Pref.Key.PackageInstaller.INSTALL_SOURCE, -1) == -1) {
                val oldValue = SafeSP.getBoolean(Pref.OldKey.PackageInstaller.UPDATE_SYSTEM_APP, false)
                val newValue = if (oldValue) 1 else 0
                SafeSP.putAny(Pref.Key.PackageInstaller.INSTALL_SOURCE, newValue)
            }
            if (SafeSP.getInt(Pref.Key.SecurityCenter.LINK_START, -1) == -1) {
                val oldValue = SafeSP.getBoolean(Pref.OldKey.SecurityCenter.SKIP_WARNING, false)
                val newValue = if (oldValue) 1 else 0
                SafeSP.putAny(Pref.Key.SecurityCenter.LINK_START, newValue)
            }
        }
        if (spVersion < 5) {
            if (SafeSP.getInt(Pref.Key.SystemUI.MediaControl.LYT_ALBUM, -1) == -1) {
                val oldValue = SafeSP.getBoolean(Pref.OldKey.SystemUI.MediaControl.HIDE_APP_ICON, false)
                val newValue = if (oldValue) 1 else 0
                SafeSP.putAny(Pref.Key.SystemUI.MediaControl.LYT_ALBUM, newValue)
            }
            if (SafeSP.getInt(Pref.Key.SystemUI.MediaControl.ELM_PROGRESS_STYLE, -1) == -1) {
                val oldValue = SafeSP.getBoolean(Pref.OldKey.SystemUI.MediaControl.SQUIGGLY_PROGRESS, false)
                val newValue = if (oldValue) 2 else 0
                SafeSP.putAny(Pref.Key.SystemUI.MediaControl.ELM_PROGRESS_STYLE, newValue)
            }
        }
        if (spVersion < 6) {
            // 熄屏显秒的旧 key 值误写为 show_period,迁移到新值
            if (!SafeSP.getBoolean(Pref.Key.SystemUI.LockScreen.SCREEN_OFF_SHOW_SECONDS, false)) {
                val oldValue = SafeSP.getBoolean(Pref.OldKey.SystemUI.LockScreen.SCREEN_OFF_SHOW_SECONDS, false)
                if (oldValue) {
                    SafeSP.putAny(Pref.Key.SystemUI.LockScreen.SCREEN_OFF_SHOW_SECONDS, true)
                }
            }
        }
        SafeSP.putAny(Pref.Key.Module.SP_VERSION, Pref.VERSION)
    }
}