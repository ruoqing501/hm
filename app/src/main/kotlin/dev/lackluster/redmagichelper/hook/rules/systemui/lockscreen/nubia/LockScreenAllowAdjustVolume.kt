package dev.lackluster.redmagichelper.hook.rules.systemui.lockscreen.nubia


import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.*
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.param.HookParam
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import java.text.SimpleDateFormat
import java.util.*

object LockScreenAllowAdjustVolume : YukiBaseHooker() {
    // 允许调节音量开关（开关的状态）
    private val allowAdjustVolume by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.ALLOW_ADJUST_VOLUME, false)
    }


    @SuppressLint("PrivateApi", "SimpleDateFormat")
    override fun onHook() {
        if (!allowAdjustVolume) return

        YLog.debug("[WooBox-LockScreenAllowAdjustVolume] 开始Hook锁屏允许调节音量功能")

        try {
            "com.zte.feature.volume.MfvVolumeDialog".toClass().apply {
                method {
                    name= "shouldKeyguardHandleVolumeKeys"
                }.hook(){
                    before {
                       this.result = false
                    }
                }
            }

        } catch (e: Exception) {
            YLog.debug("[WooBox-LockScreenAllowAdjustVolume] Hook锁屏调节音量异常: ${e.message}", e)
        }
    }

}