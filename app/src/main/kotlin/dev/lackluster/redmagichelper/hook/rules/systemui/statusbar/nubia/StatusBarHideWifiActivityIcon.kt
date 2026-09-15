package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia




import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.constructor
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.HideWifiActivityAndType
import dev.lackluster.redmagichelper.utils.KotlinFlowHelper.ReadonlyStateFlow
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.getResID
import dev.lackluster.redmagichelper.utils.factory.hasEnable

object StatusBarHideWifiActivityIcon : YukiBaseHooker() {
    private const val TAG = "StatusBarHideWifiActivityIcon"
    private val hideWifiActivity get() = Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.NUBIA_HIDE_WIFI_ACTIVITY, false)
    private val hideWifiStandard get() = Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.NUBIA_ICON_TUNER_WIFI_HIDE_WIFI_TYPE, false)
    private val connectivityConstantsClass by lazy {
        "com.android.systemui.statusbar.pipeline.shared.ConnectivityConstants".toClassOrNull()
    }

    override fun onHook() {
        "com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.WifiViewModel".toClassOrNull()?.apply {
            constructor().hookAll {
                // 隐藏wifi活动图标
                before {
                    if (!hideWifiActivity) return@before
                    YLog.debug("$TAG：hideWifiActivity")
                    this.args.firstOrNull {
                        connectivityConstantsClass?.isInstance(it) == true
                    }?.current()?.field {
                        name = "shouldShowActivityConfig"
                    }?.setFalse()
                }
            }
        }
        // 隐藏wifi标准图标
        "com.zte.feature.signal.WifiUtils\$Companion".toClassOrNull()
            ?.method {
                name = "getWifiSignalStrengthIconId"
                paramCount = 6
                param(
                    IntType,   // wifiStandard
                    BooleanType, // isValidated
                    BooleanType, // metered
                    BooleanType, // mloLink
                    IntType,   // level
                    IntType    // defaultIconId
                )
                returnType = IntType
            }?.hook {
                before {
                    if (!hideWifiStandard) return@before
                    // 直接返回第六个参数 defaultIconId
                    result = args[5] as Int
                }
            }
    }

}