package dev.lackluster.redmagichelper.hook.rules.android


import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.factory.hasEnable

// 禁用温控
object DisableThermal : YukiBaseHooker() {
    private const val TAG = "DisableThermal"
    override fun onHook() {
        hasEnable(Pref.Key.Android.SYSTEM_FRAMEWORK_OTHER_DISABLE_THERMAL) {
            "com.android.server.power.ThermalManagerService".toClass().method {
                name = "onTemperatureChanged"
                param("android.os.Temperature", BooleanType)
            }.hook {
                before {
                    // 阻止原方法执行
                    result = null
                }
            }
        }
    }
}
