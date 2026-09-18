package dev.lackluster.redmagichelper.hook

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.hook.apps.Android
import dev.lackluster.redmagichelper.hook.apps.DeskTop
import dev.lackluster.redmagichelper.hook.apps.SystemUI
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaBaiduIme
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaGameAssist
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaGameAssist2
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaGameFloat
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaGameLab
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaNeoStore
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaPackageInstaller
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaPluginTrigger
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaRecommend
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaSystemSettings
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaSystemUpdate
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaThemeUpdate
import dev.lackluster.redmagichelper.hook.apps.nubia.NubiaWeather
import dev.lackluster.redmagichelper.hook.compat.XposedEnv
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.param.PackageParam
import dev.lackluster.redmagichelper.hook.rules.doubleApp.DoubleApp
import dev.lackluster.redmagichelper.hook.rules.gameheightlights.NubiaHeightLights
import dev.lackluster.redmagichelper.hook.rules.gamehelpmodule.NubiaComboAttack
import dev.lackluster.redmagichelper.hook.rules.gamehelpmodule.NubiaComboSpeed
import dev.lackluster.redmagichelper.hook.rules.gamespace.NubiaGameSpace
import dev.lackluster.redmagichelper.hook.rules.gamespace.NubiaTgkHelper
import dev.lackluster.redmagichelper.hook.rules.mtpfilebrowser.MtpFileBrowser
import dev.lackluster.redmagichelper.hook.rules.nfc.NfcService
import dev.lackluster.redmagichelper.hook.rules.permissioncontroller.PermissionController
import dev.lackluster.redmagichelper.hook.rules.screenshot.RecordScreenHook
import dev.lackluster.redmagichelper.hook.rules.screenshot.ScreenshotLoggerHook
import dev.lackluster.redmagichelper.utils.Prefs

class HookEntry : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        XposedEnv.module = this
        Prefs.initHook(this)
        android.util.Log.i("RMH_DEBUG", "onModuleLoaded process=${param.processName}")
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        XposedEnv.module = this
        Prefs.initHook(this)
        android.util.Log.i(
            "RMH_DEBUG",
            "onSystemServerStarting enabled=$moduleEnabled " +
                "rmAlert=${Prefs.getBoolean(Pref.Key.Android.REMOVE_ALERT_WINDOWS_NOTIFICATION, false)} " +
                "volSafety=${Prefs.getBoolean(Pref.Key.SystemUI.Volume.DISABLE_SAFETY_WARNING, false)}"
        )
        if (!moduleEnabled) return
        loadHookers(PackageParam(Scope.ANDROID, param.classLoader, null), Android)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        XposedEnv.module = this
        if (!param.isFirstPackage) return
        android.util.Log.i("RMH_DEBUG", "onPackageReady pkg=${param.packageName} enabled=$moduleEnabled")
        dispatchPackage(param.packageName, param.classLoader, param.applicationInfo)
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        // No module-owned threads or external callbacks to retire; old hooks are unhooked
        // in onHotReloaded before the new generation installs its own.
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        param.oldHookHandles.forEach { it.unhook() }
        XposedEnv.module = this
        Prefs.initHook(this)
        YLog.info("Hot reloaded in process ${param.processName}")
        if (param.processName == "system" || param.processName == "android") {
            if (!moduleEnabled) return
            loadHookers(PackageParam(Scope.ANDROID, ClassLoader.getSystemClassLoader(), null), Android)
            return
        }
        val app = runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
                .getMethod("currentActivityThread").invoke(null)
            activityThread?.javaClass?.getMethod("currentApplication")
                ?.invoke(activityThread) as? android.app.Application
        }.getOrNull() ?: return
        dispatchPackage(app.packageName, app.classLoader, app.applicationInfo)
    }

    private fun dispatchPackage(
        packageName: String,
        classLoader: ClassLoader,
        applicationInfo: android.content.pm.ApplicationInfo?,
    ) {
        if (!moduleEnabled) return

        val hookers = mutableListOf<YukiBaseHooker>()
        when (packageName) {
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
            Scope.PLUGIN_TRIGGER -> hookers += NubiaPluginTrigger
            Scope.GAME_LAB -> hookers += NubiaGameLab
            Scope.NFC -> hookers += NfcService
            Scope.PERMISSION_CONTROLLER -> hookers += PermissionController
            Scope.DOUBLE_APP -> hookers += DoubleApp
            Scope.NUBIA_FILE_BROWSER -> hookers += MtpFileBrowser
            Scope.ZTE_SCREENSHOT -> hookers += listOf(ScreenshotLoggerHook, RecordScreenHook)
            Scope.GAME_FLOAT -> if (gameFunctionUnfrozen) hookers += NubiaGameFloat
            Scope.REDMAGIC_MOMENT -> if (gameFunctionUnfrozen) hookers += NubiaHeightLights
            Scope.GAME_SPACE -> if (gameFunctionUnfrozen) hookers += listOf(NubiaGameSpace, NubiaTgkHelper)
            Scope.COMBO_ATTACK -> if (gameFunctionUnfrozen) hookers += listOf(NubiaComboAttack, NubiaComboSpeed)
            Scope.NEO_STORE -> hookers += NubiaNeoStore
            Scope.ZTE_RECOMMEND -> hookers += NubiaRecommend
            Scope.BAIDU_IME -> hookers += NubiaBaiduIme
        }
        if (hookers.isEmpty()) return
        loadHookers(PackageParam(packageName, classLoader, applicationInfo), *hookers.toTypedArray())
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
