package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.hardware.display.DisplayManager
import android.os.Build
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.LongType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.factory.hasEnable

object DisableFlagSecureHookerZygoteInit : YukiBaseHooker() {


    override fun onHook() {
        // 总开关
        // 8. hook SurfaceControl.nativeSetFlags（增强开关）
        hasEnable(Pref.Key.Android.DISABLE_FLAG_SECURE_ENHANCED) {
            YLog.debug("DisableFlagSecureHookerZygoteInit")
            "android.view.SurfaceControl".toClassOrNull()?.apply {
                method {
                    name = "nativeSetFlags"
                    param(LongType, LongType, IntType, IntType)  // 明确参数类型
                }.hook {
                    before {
                        if (this.args.size > 3) {
                            val flags = this.args(2).int()
                            val mask = this.args(3).int()
                            if (mask == 64) { // SKIP_SCREENSHOT
                                this.args(2).set(0)
                            }
                        }
                    }
                }
            }
        }
    }
}

