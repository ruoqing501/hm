package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia

import android.graphics.Typeface
import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
//将锁屏状态栏的运营商标签字体恢复为默认粗体
object StatusBarKSBFontRestore : YukiBaseHooker() {
    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.RESTORE_THE_FONT_OF_THE_CLOCK_DATE_ICON) {
            YLog.debug("[StatusBarKSBFontRestore] 开始Hook锁屏状态栏字体恢复功能")

            try {
                // Hook KeyguardStatusBarView 类的 onFinishInflate 方法
                "com.android.systemui.statusbar.phone.KeyguardStatusBarView".toClass().apply {
                    method {
                        name = "onFinishInflate"
                    }.hook {
                        after {
                            // 获取 mCarrierLabel 字段
                            val carrierTextView = this.instance.current().field {
                                name = "mCarrierLabel"
                            }.cast<TextView>()

                            if (carrierTextView != null) {
                                carrierTextView.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                YLog.debug("[StatusBarKSBFontRestore] 成功恢复锁屏状态栏字体为默认粗体")
                            } else {
                                YLog.warn("[StatusBarKSBFontRestore] 未找到 mCarrierLabel 字段")
                            }
                        }
                    }
                }
                YLog.debug("[StatusBarKSBFontRestore] Hook锁屏状态栏字体恢复功能完成")
            } catch (e: Exception) {
                YLog.error("[StatusBarKSBFontRestore] Hook失败: ${e.message}")
            }
        }
    }
}