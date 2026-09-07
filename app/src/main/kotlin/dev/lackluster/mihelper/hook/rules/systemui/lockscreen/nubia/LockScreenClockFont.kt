package dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia

import android.annotation.SuppressLint
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.hasEnable

object LockScreenClockFont : YukiBaseHooker() {




    @SuppressLint("PrivateApi")
    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.RESTORE_THE_FONT_OF_THE_CLOCK_DATE_ICON) {
            YLog.debug("[LockScreenClockFont] 开始Hook锁屏时钟字体风格修改")

            // Hook ArtFontClockView 类的 setArtFontStyle 方法
            "com.zte.mifavor.keyguard.personalclock.view.ArtFontClockView".toClassOrNull()?.apply {
                YLog.debug("[LockScreenClockFont] 找到 ArtFontClockView 类")

                // 方法1: Hook setArtFontStyle 方法，直接修改传入的风格值
                method {
                    name = "setArtFontStyle"
                    paramCount = 1
                }.hook {
                    before {
                        YLog.debug(
                            "[LockScreenClockFont] 调用 setArtFontStyle，原值: ${
                                this.args(0).int()
                            }"
                        )
                        // 修改为选择的字体风格
                        this.args(0).set(1)
                    }
                }
            }
        }
    }
}