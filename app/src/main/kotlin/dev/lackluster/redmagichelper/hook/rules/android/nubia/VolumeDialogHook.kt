package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object VolumeDialogHook : YukiBaseHooker() {
    private const val TAG = "VolumeDialogHook"
    private const val KEY_SAFE_VOLUME_STATE = "audio_safe_volume_state"
    private const val STATE_INACTIVE = 2

    @Volatile
    private var lastEnforceMs = 0L

    private fun isHookEnabled() = Prefs.getBoolean(Pref.Key.SystemUI.Volume.DISABLE_SAFETY_WARNING, false)

    override fun onHook() {
        // 方案：等价于 settings put secure audio_safe_volume_state 2（用户实测有效），
        // 同时调用 AudioService.disableSafeMediaVolume —— 即安全音量弹窗“确定”按钮走的系统内部路径，
        // 直接把运行时状态置为未激活并应用被挂起的音量指令。
        // 不修改任何判定方法的返回值，避免各版本/厂商语义差异导致音量被锁死。
        "com.android.server.audio.AudioService".toClassOrNull()?.apply {
            // 系统就绪后先执行一次
            method {
                name = "onSystemReady"
            }.hook {
                after {
                    if (!isHookEnabled()) return@after
                    enforceInactive(instance)
                }
            }
            // 音量键/滑杆调节时兜底：覆盖开关刚打开、20 小时播放定时重新激活等场景；
            // 在 before 里先解除，原方法随后的检查就走“未激活”分支正常调音量
            method {
                name = "adjustStreamVolume"
            }.hook {
                before {
                    if (!isHookEnabled()) return@before
                    enforceInactive(instance)
                }
            }
            method {
                name = "setStreamVolume"
            }.hook {
                before {
                    if (!isHookEnabled()) return@before
                    enforceInactive(instance)
                }
            }
        } ?: YLog.warn(tag = TAG, msg = "未找到 AudioService 类，可能版本不兼容")
    }

    private fun enforceInactive(audioService: Any) {
        // 节流：音量键连按时避免反复反射/写设置；也能挡住挂起音量指令被应用时的重入
        val now = SystemClock.uptimeMillis()
        if (now - lastEnforceMs < 3000) return
        lastEnforceMs = now

        // 走系统自己的关闭路径（幂等）：状态置为未激活 + 应用挂起的音量指令 + 持久化 Global 设置
        runCatching {
            audioService.javaClass
                .getMethod("disableSafeMediaVolume", String::class.java)
                .invoke(audioService, TAG)
            YLog.debug(tag = TAG, msg = "已调用 disableSafeMediaVolume")
        }.onFailure {
            YLog.warn(tag = TAG, msg = "disableSafeMediaVolume 调用失败: $it")
        }

        // 用户实测有效的路径：直写设置项（AOSP 读 Global，部分厂商 ROM 读 Secure，两边都写）
        val context = resolveContext(audioService) ?: return
        runCatching {
            val cr = context.contentResolver
            if (Settings.Secure.getInt(cr, KEY_SAFE_VOLUME_STATE, -1) != STATE_INACTIVE) {
                Settings.Secure.putInt(cr, KEY_SAFE_VOLUME_STATE, STATE_INACTIVE)
            }
            if (Settings.Global.getInt(cr, KEY_SAFE_VOLUME_STATE, -1) != STATE_INACTIVE) {
                Settings.Global.putInt(cr, KEY_SAFE_VOLUME_STATE, STATE_INACTIVE)
            }
        }.onFailure {
            YLog.warn(tag = TAG, msg = "写入安全音量设置失败: $it")
        }
    }

    private fun resolveContext(audioService: Any): Context? {
        // AudioService.mContext
        runCatching {
            var clazz: Class<*>? = audioService.javaClass
            while (clazz != null) {
                val field = runCatching { clazz.getDeclaredField("mContext") }.getOrNull()
                if (field != null) {
                    field.isAccessible = true
                    return field.get(audioService) as? Context
                }
                clazz = clazz.superclass
            }
        }
        // 兜底：system_server 的系统上下文
        return runCatching {
            val at = Class.forName("android.app.ActivityThread")
            val thread = at.getMethod("currentActivityThread").invoke(null)
            at.getMethod("getSystemContext").invoke(thread) as? Context
        }.getOrNull()
    }
}
