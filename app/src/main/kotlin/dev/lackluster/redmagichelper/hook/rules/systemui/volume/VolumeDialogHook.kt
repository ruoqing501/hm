package dev.lackluster.redmagichelper.hook.rules.systemui.volume

import android.view.View
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

// 屏蔽 SystemUI 的安全音量警告对话框-需要配合其他的系统框架的VolumeDialogHook
object VolumeDialogHook : YukiBaseHooker() {
    private const val TAG = "VolumeDialogHook"

    override fun onHook() {
        // 1. 屏蔽 SystemUI 的安全音量警告对话框
        "com.android.systemui.volume.VolumeDialogImpl".toClassOrNull()?.apply {
            // 按方法名 hook 所有重载，避免厂商 ROM 修改方法签名导致 hook 静默失败
            method {
                name = "showSafetyWarningH"
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.SystemUI.Volume.DISABLE_SAFETY_WARNING, false)) return@before
                    YLog.debug(tag = TAG, msg = "拦截安全音量警告对话框显示")
                    this.result = null
                }
            }
        }
    }
}
