package dev.lackluster.mihelper.hook.rules.systemui.lockscreen.nubia
import android.graphics.Typeface
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs

object LockScreenClockSeconds : YukiBaseHooker() {

    private val showSeconds by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.DISPLAY_SECONDS, false)
    }

    override fun onHook() {
        if (!showSeconds) return

        // Hook第一个类
        "com.zte.mifavor.keyguard.settings.LockScreenClockDefault".toClassOrNull()?.apply {
            method { name = "refreshAmPm" }.hook {
                after {
                    val instance = this.instance
                    val mClockView = instance.current().field { name = "mClockView" }.any()

                    mClockView?.current()?.method {
                        name = "setFormat24Hour"
                        paramCount = 1
//                        param { it == String::class.java }
                    }?.call("kk:mm:ss")

                    mClockView?.current()?.method {
                        name = "setFormat12Hour"
                        paramCount = 1
//                        param { it == String::class.java }
                    }?.call("h:mm:ss")
                }
            }
        }

//        "com.zte.mifavor.keyguard.personalclock.FontResUtil".toClass().method {
//            name = "getClockTypeface"
//            param(IntType, IntType)
//        }.hook {
//            after {
//                // 获取当前应用的 AssetManager
//                val assetManager = appContext?.assets  // 需从合适地方获取 Context
//                // 从 assets 加载与原文件相同的字体（确保文件名匹配）
//                val originalFont = Typeface.createFromAsset(assetManager, "system/font/AndroidClock.ttf")
//                result = originalFont
//            }
//        }

        // Hook第二个类
        "com.zte.mifavor.keyguard.settings.LockScreenClockArtword".toClassOrNull()?.apply {
            method { name = "refreshAmPm" }.hook {
                after {
                    val instance = this.instance
                    val mClockView = instance.current().field { name = "mClockView" }.any()

                    mClockView?.current()?.method {
                        name = "setFormat24Hour"
                        paramCount = 1
//                        param { it == String::class.java }
                    }?.call("kk:mm:ss")

                    mClockView?.current()?.method {
                        name = "setFormat12Hour"
                        paramCount = 1
//                        param { it == String::class.java }
                    }?.call("h:mm:ss")
                }
            }
        }

        // Hook第三个类
        "com.zte.mifavor.keyguard.settings.LockScreenClockIncarnation".toClassOrNull()?.apply {
            method { name = "refreshAmPm" }.hook {
                after {
                    val instance = this.instance
                    val mClockView = instance.current().field { name = "mClockView" }.any()

                    mClockView?.current()?.method {
                        name = "setFormat24Hour"
                        paramCount = 1
//                        param { it == String::class.java }
                    }?.call("kk:mm:ss")

                    mClockView?.current()?.method {
                        name = "setFormat12Hour"
                        paramCount = 1
//                        param { it == String::class.java }
                    }?.call("h:mm:ss")
                }
            }
        }
    }
}