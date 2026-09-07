package dev.lackluster.mihelper.hook.rules.android.nubia

import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.hasEnable

object VolumeDialogHook : YukiBaseHooker() {
    private const val TAG = "VolumeDialogHook"

    override fun onHook() {

        // 2. 绕过 AudioService 中的安全音量检查
        hasEnable(Pref.Key.SystemUI.Volume.DISABLE_SAFETY_WARNING) {
            // 获取 AudioService 的内部类 SoundDoseHelper
            //"com.android.server.audio.AudioService\$SoundDoseHelper".toClassOrNull()?.apply {
            "com.android.server.audio.SoundDoseHelper".toClassOrNull()?.apply {
                // hook 方法：raiseVolumeDisplaySafeMediaVolume
                method {
                    name = "raiseVolumeDisplaySafeMediaVolume"
                    paramCount = 4
                    returnType = BooleanType
                }.hook {
                    before {
                        YLog.debug(tag = TAG, msg = "绕过 raiseVolumeDisplaySafeMediaVolume 检查")
                        result = false
                    }
                }

                // hook 方法：willDisplayWarningAfterCheckVolume
                method {
                    name = "willDisplayWarningAfterCheckVolume"
                    paramCount = 4
                    returnType = BooleanType
                }.hook {
                    before {
                        YLog.debug(tag = TAG, msg = "绕过 willDisplayWarningAfterCheckVolume 检查")
                        result = false
                    }
                }
            } ?: YLog.warn(tag = TAG, msg = "未找到 SoundDoseHelper 类，可能版本不兼容")
        }
    }
}
