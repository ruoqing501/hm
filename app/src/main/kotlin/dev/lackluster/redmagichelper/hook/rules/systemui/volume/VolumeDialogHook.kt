package dev.lackluster.redmagichelper.hook.rules.systemui.volume

import android.view.View
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

// 屏蔽 SystemUI 的安全音量警告对话框-需要配合其他的系统框架的VolumeDialogHook
object VolumeDialogHook : YukiBaseHooker() {
    private const val TAG = "VolumeDialogHook"

    override fun onHook() {
        // 1. 屏蔽 SystemUI 的安全音量警告对话框
        hasEnable(Pref.Key.SystemUI.Volume.DISABLE_SAFETY_WARNING) {
            "com.android.systemui.volume.VolumeDialogImpl".toClassOrNull()?.apply {
                method {
                    name = "showSafetyWarningH"
                    paramCount = 1
                }.hook {
                    before {
                        YLog.debug(tag = TAG, msg = "拦截安全音量警告对话框显示")
                        this.result = null
                    }
                }
            }
        }
    }
}
