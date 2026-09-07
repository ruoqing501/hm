package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia




import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.constructor
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.hook.rules.systemui.statusbar.HideWifiActivityAndType
import dev.lackluster.mihelper.utils.KotlinFlowHelper.ReadonlyStateFlow
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.getResID
import dev.lackluster.mihelper.utils.factory.hasEnable

object StatusBarHideWifiActivityIcon : YukiBaseHooker() {
    private const val TAG = "StatusBarHideWifiActivityIcon"
    private val hideWifiActivity = Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.NUBIA_HIDE_WIFI_ACTIVITY, false)
    private val hideWifiStandard = Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.NUBIA_ICON_TUNER_WIFI_HIDE_WIFI_TYPE, false)
    private val connectivityConstantsClass by lazy {
        "com.android.systemui.statusbar.pipeline.shared.ConnectivityConstants".toClassOrNull()
    }

    override fun onHook() {
        if (hideWifiActivity || hideWifiStandard) {
            "com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.WifiViewModel".toClassOrNull()?.apply {
                constructor().hookAll {
                    // 隐藏wifi活动图标
                    if (hideWifiActivity ) {
                        YLog.debug("$TAG：hideWifiActivity")
                        before {
                            this.args.firstOrNull {
                                connectivityConstantsClass?.isInstance(it) == true
                            }?.current()?.field {
                                name = "shouldShowActivityConfig"
                            }?.setFalse()
                        }
                    }
                    // 隐藏wifi标准图标
                    if (hideWifiStandard) {
//                        after {
//                            this.instance.current().field {
//                                name = "wifiStandard"
//                            }.set(
//                                ReadonlyStateFlow(0 as Int?)
//                            )
//                        }
                        "com.zte.feature.signal.WifiUtils\$Companion".toClass()
                            .method {
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
                            }.hook {
                                before {
                                    // 直接返回第六个参数 defaultIconId
                                    result = args[5] as Int
                                }
                            }
                    }
                }
            }
        }
    }

}