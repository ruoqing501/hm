package dev.lackluster.mihelper.hook

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.hook.apps.Android
import dev.lackluster.mihelper.hook.apps.DeskTop
import dev.lackluster.mihelper.hook.apps.SystemUI
import dev.lackluster.mihelper.hook.apps.nubia.NubiaGameAssist
import dev.lackluster.mihelper.hook.apps.nubia.NubiaGameAssist2
import dev.lackluster.mihelper.hook.apps.nubia.NubiaGameFloat
import dev.lackluster.mihelper.hook.apps.nubia.NubiaPackageInstaller
import dev.lackluster.mihelper.hook.apps.nubia.NubiaSystemSettings
import dev.lackluster.mihelper.utils.factory.hasEnable
import dev.lackluster.mihelper.hook.apps.nubia.NubiaSystemUpdate
import dev.lackluster.mihelper.hook.apps.nubia.NubiaThemeUpdate
import dev.lackluster.mihelper.hook.apps.nubia.NubiaWeather
import dev.lackluster.mihelper.hook.rules.doubleApp.DoubleApp
import dev.lackluster.mihelper.hook.rules.gameheightlights.NubiaHeightLights
import dev.lackluster.mihelper.hook.rules.gamehelpmodule.NubiaComboAttack
import dev.lackluster.mihelper.hook.rules.gamespace.NubiaGameSpace
import dev.lackluster.mihelper.hook.rules.gamespace.NubiaTgkHelper
import dev.lackluster.mihelper.hook.rules.mtpfilebrowser.MtpFileBrowser
import dev.lackluster.mihelper.hook.rules.nfc.NfcService
import dev.lackluster.mihelper.hook.rules.permissioncontroller.PermissionController
import dev.lackluster.mihelper.hook.rules.screenshot.RecordScreenHook
import dev.lackluster.mihelper.hook.rules.screenshot.ScreenshotLoggerHook

@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        debugLog {
            tag = "RedMagicHelper"
        }
        isDebug = false
    }

    override fun onHook() = encase {
        hasEnable(Pref.Key.Module.ENABLED) {
            loadSystem(Android)
//            loadApp(Scope.BROWSER, Browser)
//            loadApp(Scope.DOWNLOAD, Download)
//            loadApp(Scope.DOWNLOAD_UI, DownloadUI)
//            loadApp(Scope.GUARD_PROVIDER, GuardProvider)
//            loadApp(Scope.IN_CALL_UI, InCallUI)
//            loadApp(Scope.LBE, LBE)
//            loadApp(Scope.MARKET, Market)
//            loadApp(Scope.MI_AI, MiAi)
//            loadApp(Scope.MI_LINK, MiLink)
//            loadApp(Scope.MI_SETTINGS, MiSettings)
//            loadApp(Scope.MI_MIRROR, MiMirror)
//            loadApp(Scope.MI_TRUST, MiTrust)
//            loadApp(Scope.MIUI_HOME, MiuiHome)
//            loadApp(Scope.MMS, Mms)
//            loadApp(Scope.MUSIC, Music)
//            loadApp(Scope.PACKAGE_INSTALLER, PackageInstaller)
//            loadApp(Scope.PERSONAL_ASSIST, PersonalAssist)
//            loadApp(Scope.POWER_KEEPER, PowerKeeper)
//            loadApp(Scope.REMOTE_CONTROLLER, RemoteController)
//            loadApp(Scope.SEARCH, Search)
//            loadApp(Scope.SECURITY_CENTER, SecurityCenter)
//            loadApp(Scope.SETTINGS, Settings)
            // 添加 nubia 专有
            loadApp(Scope.SYSTEM_UI, SystemUI)
//            loadApp(Scope.CLOCK_COMPONENT, NubiaClockComponent)
            loadApp(Scope.REDMAGIC_PACKAGE_INSTALLER, NubiaPackageInstaller)
            loadApp(Scope.SYSTEM_SETTINGS, NubiaSystemSettings)
            loadApp(Scope.SYSTEM_UPDATE,NubiaSystemUpdate)
            loadApp(Scope.SYSTEM_THEME, NubiaThemeUpdate)
            loadApp(Scope.SYSTEM_DESKTOP, DeskTop)
            loadApp(Scope.SYSTEM_WEATHER, NubiaWeather)
            // 游戏助手
            loadApp(Scope.GAME_ASSIST, NubiaGameAssist)
            // NFC服务
            loadApp(Scope.NFC, NfcService)
            // 权限控制器
            loadApp(Scope.PERMISSION_CONTROLLER, PermissionController)
            // 应用双开
            loadApp(Scope.DOUBLE_APP, DoubleApp)
            // MTP浏览
            loadApp(Scope.NUBIA_FILE_BROWSER, MtpFileBrowser)

            // 截图和录屏
            loadApp(Scope.ZTE_SCREENSHOT, ScreenshotLoggerHook)
            loadApp(Scope.ZTE_SCREENSHOT, RecordScreenHook)



           // 游戏空间解除各种功能限制
            hasEnable(Pref.Key.GameSpace.GAME_FUCTION_UNFREEZE_SWITCH){
                loadApp(Scope.GAME_ASSIST, NubiaGameAssist2)
                loadApp(Scope.GAME_FLOAT, NubiaGameFloat)
                loadApp(Scope.REDMAGIC_MOMENT, NubiaHeightLights)
                loadApp(Scope.GAME_SPACE, NubiaGameSpace)
                loadApp(Scope.GAME_SPACE, NubiaTgkHelper)
                loadApp(Scope.COMBO_ATTACK, NubiaComboAttack)
            }

//            loadApp(Scope.TAPLUS, Taplus)
//            loadApp(Scope.THEMES, Themes)
//            loadApp(Scope.UPDATER, Updater)
//            loadApp(Scope.WEATHER, Weather)
//            loadApp(Scope.SYSTEM_UI_PLUGIN, SystemUIPlugin)
        }
    }
}