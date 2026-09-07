package com.u9521.wooboxforredmagicos.hook.app.systemui.statusbar

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import kotlin.collections.get

object StatusBarSBFontRestore : YukiBaseHooker() {

    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.RESTORE_THE_FONT_OF_THE_CLOCK_DATE_ICON) {
            YLog.debug("[SBFontRestore] 开始Hook状态栏字体恢复功能")

            // Hook时钟字体恢复 - 无效果
//            hookClockFontRestore()

            // Hook网速字体恢复
//            hookNetSpeedFontRestore()

            // Hook电池百分比字体恢复
//            hookBatteryFontRestore()

            // HookCC头部
            hookCCHeaderView()

            YLog.debug("[SBFontRestore] Hook状态栏字体恢复功能完成")
        }
    }

    /**
     * Hook时钟字体恢复
     */
    @SuppressLint("PrivateApi")
    private fun hookClockFontRestore() {
        YLog.debug("[SBFontRestore] 开始Hook时钟字体恢复")

        // 尝试不同的类名，因为系统版本可能不同
        val phoneStatusBarViewClass = "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClassOrNull()
            ?: return

        phoneStatusBarViewClass.apply {
            method {
                name = "onFinishInflate"
            }.hook {
                after {
                    try {
                        val clockTextView = this.instance.current().field {
                            name = "mClock"
                        }.cast<TextView>()

                        if (clockTextView != null) {
                            // 恢复字体为默认粗体
                            clockTextView.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            // 移除字体特性设置
                            clockTextView.fontFeatureSettings = ""
                            // 调整布局参数
                            val layoutParams = clockTextView.layoutParams
                            layoutParams.width = LayoutParams.WRAP_CONTENT
                            clockTextView.layoutParams = layoutParams

                            YLog.debug("[SBFontRestore] 成功恢复时钟字体为默认粗体")
                        } else {
                            YLog.warn("[SBFontRestore] 未找到 mClock 字段")
                        }
                    } catch (e: Exception) {
                        YLog.error("[SBFontRestore] Hook时钟字体恢复失败: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Hook网速字体恢复
     */
    @SuppressLint("PrivateApi")
    private fun hookNetSpeedFontRestore() {
        YLog.debug("[SBFontRestore] 开始Hook网速字体恢复")

        // 尝试不同的类名，因为系统版本可能不同
        val netSpeedClass = "com.zte.feature.speed.StatusBarNetSpeedMFV".toClassOrNull()
            ?: return

        netSpeedClass.apply {
            method {
                name = "init"
            }.hook {
                after {
                    try {
                        val speedTextView = this.instance.current().field {
                            name = "mSpeedText"
                        }.cast<TextView>()

                        val speedUnitView = this.instance.current().field {
                            name = "mSpeedUnit"
                        }.cast<TextView>()

                        if (speedTextView != null) {
                            // 恢复网速数字字体为默认粗体
                            speedTextView.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            YLog.debug("[SBFontRestore] 成功恢复网速数字字体为默认粗体")
                        } else {
                            YLog.warn("[SBFontRestore] 未找到 mSpeedText 字段")
                        }

                        if (speedUnitView != null) {
                            // 恢复网速单位字体为默认粗体
                            speedUnitView.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            YLog.debug("[SBFontRestore] 成功恢复网速单位字体为默认粗体")
                        } else {
                            YLog.warn("[SBFontRestore] 未找到 mSpeedUnit 字段")
                        }
                    } catch (e: Exception) {
                        YLog.error("[SBFontRestore] Hook网速字体恢复失败: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Hook电池百分比字体恢复
     */
    @SuppressLint("PrivateApi")
    private fun hookBatteryFontRestore() {
        YLog.debug("[SBFontRestore] 开始Hook电池百分比字体恢复")

        // 尝试不同的类名，因为系统版本可能不同
        val batteryViewClass = "com.zte.mifavor.views.MFVBatteryViewLayout".toClassOrNull()
            ?: return

        batteryViewClass.apply {
            method {
                name = "onFinishInflate"
            }.hook {
                after {
                    try {
                        val batteryLevelInsideView = this.instance.current().field {
                            name = "mBatteryLevelInsideView"
                        }.cast<TextView>()

                        val batteryLevelOutsideView = this.instance.current().field {
                            name = "mBatteryLevelOutsideView"
                        }.cast<TextView>()

                        if (batteryLevelInsideView != null) {
                            // 恢复内部电池百分比字体为默认粗体
                            batteryLevelInsideView.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            YLog.debug("[SBFontRestore] 成功恢复内部电池百分比字体为默认粗体")
                        } else {
                            YLog.warn("[SBFontRestore] 未找到 mBatteryLevelInsideView 字段")
                        }

                        if (batteryLevelOutsideView != null) {
                            // 恢复外部电池百分比字体为默认粗体
                            batteryLevelOutsideView.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            YLog.debug("[SBFontRestore] 成功恢复外部电池百分比字体为默认粗体")
                        } else {
                            YLog.warn("[SBFontRestore] 未找到 mBatteryLevelOutsideView 字段")
                        }
                    } catch (e: Exception) {
                        YLog.error("[SBFontRestore] Hook电池百分比字体恢复失败: ${e.message}")
                    }
                }
            }
        }
    }
    @SuppressLint("PrivateApi")
    private fun  hookCCHeaderView(){
        YLog.debug("[SBFontRestore] 尝试HookCCHeaderView")
        val ccHeaderClazz =  "com.zte.controlcenter.widget.CCHeaderView".toClass()
            ccHeaderClazz.method {
              name = "updateHeaderResources"
        }.hook{
            after {

                val dateView =  ccHeaderClazz.current().field {
                    name = "mDateView"
                } as TextView

                val clockView =  ccHeaderClazz.current().field {
                    name = "mClockView"
                } as TextView

                val carrierTextView =  ccHeaderClazz.current().field {
                    name = "mCarrierText"
                } as TextView

                YLog.debug("[SBFontRestore] $dateView $clockView $carrierTextView")

                dateView.setTypeface(Typeface.DEFAULT, Typeface.BOLD)

                clockView.setTypeface(Typeface.DEFAULT)

                clockView.fontFeatureSettings = ""

                carrierTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
    }
}