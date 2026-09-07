package dev.lackluster.mihelper.hook

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
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
import dev.lackluster.mihelper.hook.apps.nubia.NubiaSystemUpdate
import dev.lackluster.mihelper.hook.apps.nubia.NubiaThemeUpdate
import dev.lackluster.mihelper.hook.apps.nubia.NubiaWeather
import dev.lackluster.mihelper.hook.compat.XposedEnv
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.param.PackageParam
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
import dev.lackluster.mihelper.utils.Prefs

class HookEntry : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        XposedEnv.module = this
        Prefs.initHook(this)
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        XposedEnv.module = this
        if (!moduleEnabled) return
        loadHookers(PackageParam(Scope.ANDROID, param.classLoader, null), Android)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        XposedEnv.module = this
        if (!param.isFirstPackage) return
        if (!moduleEnabled) return

        val hookers = mutableListOf<YukiBaseHooker>()
        when (param.packageName) {
            Scope.SYSTEM_UI -> hookers += SystemUI
            Scope.REDMAGIC_PACKAGE_INSTALLER -> hookers += NubiaPackageInstaller
            Scope.SYSTEM_SETTINGS -> hookers += NubiaSystemSettings
            Scope.SYSTEM_UPDATE -> hookers += NubiaSystemUpdate
            Scope.SYSTEM_THEME -> hookers += NubiaThemeUpdate
            Scope.SYSTEM_DESKTOP -> hookers += DeskTop
            Scope.SYSTEM_WEATHER -> hookers += NubiaWeather
            Scope.GAME_ASSIST -> {
                hookers += NubiaGameAssist
                if (gameFunctionUnfrozen) hookers += NubiaGameAssist2
            }
            Scope.NFC -> hookers += NfcService
            Scope.PERMISSION_CONTROLLER -> hookers += PermissionController
            Scope.DOUBLE_APP -> hookers += DoubleApp
            Scope.NUBIA_FILE_BROWSER -> hookers += MtpFileBrowser
            Scope.ZTE_SCREENSHOT -> hookers += listOf(ScreenshotLoggerHook, RecordScreenHook)
            Scope.GAME_FLOAT -> if (gameFunctionUnfrozen) hookers += NubiaGameFloat
            Scope.REDMAGIC_MOMENT -> if (gameFunctionUnfrozen) hookers += NubiaHeightLights
            Scope.GAME_SPACE -> if (gameFunctionUnfrozen) hookers += listOf(NubiaGameSpace, NubiaTgkHelper)
            Scope.COMBO_ATTACK -> if (gameFunctionUnfrozen) hookers += NubiaComboAttack
        }
        if (hookers.isEmpty()) return
        loadHookers(PackageParam(param.packageName, param.classLoader, param.applicationInfo), *hookers.toTypedArray())
    }

    private val moduleEnabled: Boolean
        get() = Prefs.getBoolean(Pref.Key.Module.ENABLED, false)

    private val gameFunctionUnfrozen: Boolean
        get() = Prefs.getBoolean(Pref.Key.GameSpace.GAME_FUCTION_UNFREEZE_SWITCH, false)

    private fun loadHookers(packageParam: PackageParam, vararg hookers: YukiBaseHooker) {
        XposedEnv.currentParam = packageParam
        for (hooker in hookers) {
            hooker.packageParam = packageParam
            runCatching { hooker.onHook() }
                .onFailure { YLog.error("Failed to load hooker ${hooker.javaClass.name}", it) }
        }
    }
}
