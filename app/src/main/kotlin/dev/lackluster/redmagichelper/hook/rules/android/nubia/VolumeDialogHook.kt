package dev.lackluster.redmagichelper.hook.rules.android.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object VolumeDialogHook : YukiBaseHooker() {
    private const val TAG = "VolumeDialogHook"

    private fun isHookEnabled() = Prefs.getBoolean(Pref.Key.SystemUI.Volume.DISABLE_SAFETY_WARNING, false)

    override fun onHook() {
        android.util.Log.i(
            "RMH_DEBUG",
            "VolumeDialogHook(android) onHook pref=${Prefs.getBoolean(Pref.Key.SystemUI.Volume.DISABLE_SAFETY_WARNING, false)}"
        )

        // 2. 绕过 AudioService 中的安全音量检查
        // Android 14+ 是独立类 SoundDoseHelper，更早版本是 AudioService 的内部类
        val soundDoseHelper = "com.android.server.audio.SoundDoseHelper".toClassOrNull()
            ?: "com.android.server.audio.AudioService\$SoundDoseHelper".toClassOrNull()

        if (soundDoseHelper != null) {
            // 按方法名 hook 所有重载，避免厂商 ROM 修改方法签名导致 hook 静默失败
            // 根因检查：返回 false 表示“未超过安全音量”，两条音量路径都依赖它
            soundDoseHelper.method {
                name = "checkSafeMediaVolume"
            }.hook {
                before {
                    if (!isHookEnabled()) return@before
                    YLog.debug(tag = TAG, msg = "绕过 checkSafeMediaVolume 检查")
                    result = false
                }
            }

            // hook 方法：raiseVolumeDisplaySafeMediaVolume
            soundDoseHelper.method {
                name = "raiseVolumeDisplaySafeMediaVolume"
            }.hook {
                before {
                    if (!isHookEnabled()) return@before
                    YLog.debug(tag = TAG, msg = "绕过 raiseVolumeDisplaySafeMediaVolume 检查")
                    result = false
                }
            }

            // hook 方法：willDisplayWarningAfterCheckVolume
            soundDoseHelper.method {
                name = "willDisplayWarningAfterCheckVolume"
            }.hook {
                before {
                    if (!isHookEnabled()) return@before
                    YLog.debug(tag = TAG, msg = "绕过 willDisplayWarningAfterCheckVolume 检查")
                    result = false
                }
            }
        } else {
            // Android 12 及以下：安全音量检查逻辑在 AudioService 内部
            // checkSafeMediaVolume 返回 true 表示“安全，可以继续调整”
            "com.android.server.audio.AudioService".toClassOrNull()?.apply {
                method {
                    name = "checkSafeMediaVolume"
                }.hook {
                    before {
                        if (!isHookEnabled()) return@before
                        YLog.debug(tag = TAG, msg = "绕过 AudioService.checkSafeMediaVolume 检查")
                        result = true
                    }
                }
            } ?: YLog.warn(tag = TAG, msg = "未找到 SoundDoseHelper / AudioService 类，可能版本不兼容")
        }
    }
}
