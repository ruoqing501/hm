package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia


import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object StatusBarFontRestoreHooker : YukiBaseHooker() {

    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.RESTORE_THE_FONT_OF_THE_CLOCK_DATE_ICON) {
            YLog.debug("[SBFontRestore] 开始Hook状态栏字体恢复功能")

            // Hook时钟字体恢复 - 无效果（已注释）
            // hookClockFontRestore()

            // Hook网速字体恢复
            hookNetSpeedFontRestore()

            // Hook电池百分比字体恢复
            hookBatteryFontRestore()

            // Hook锁屏状态栏字体恢复
            hookKeyguardStatusBarFontRestore()

//            hookCCHeaderView()

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

    /**
     * Hook锁屏状态栏字体恢复（从第二个文件合并）
     * 将锁屏状态栏的运营商标签字体恢复为默认粗体
     */
    @SuppressLint("PrivateApi")
    private fun hookKeyguardStatusBarFontRestore() {
        YLog.debug("[SBFontRestore] 开始Hook锁屏状态栏字体恢复功能")

        try {
            // Hook KeyguardStatusBarView 类的 onFinishInflate 方法
            "com.android.systemui.statusbar.phone.KeyguardStatusBarView".toClassOrNull()?.apply {
                method {
                    name = "onFinishInflate"
                }.hook {
                    after {
                        try {
                            // 获取 mCarrierLabel 字段
                            val carrierTextView = this.instance.current().field {
                                name = "mCarrierLabel"
//                            }.cast<TextView>()
                            } as? TextView

                            if (carrierTextView != null) {
                                carrierTextView.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                YLog.debug("[SBFontRestore] 成功恢复锁屏状态栏运营商标签字体为默认粗体")
                            } else {
                                YLog.warn("[SBFontRestore] 未找到 mCarrierLabel 字段")
                            }
                        } catch (e: Exception) {
                            YLog.error("[SBFontRestore] Hook锁屏状态栏字体恢复失败: ${e.message}")
                        }
                    }
                }
            } ?: run {
                YLog.warn("[SBFontRestore] 未找到 KeyguardStatusBarView 类")
            }

            YLog.debug("[SBFontRestore] Hook锁屏状态栏字体恢复功能完成")
        } catch (e: Exception) {
            YLog.error("[SBFontRestore] Hook锁屏状态栏字体恢复失败: ${e.message}")
        }
    }
    @SuppressLint("PrivateApi")
    private fun  hookCCHeaderView(){
        val ccHeaderClazz =  "com.zte.controlcenter.widget.CCHeaderView".toClass()
        ccHeaderClazz.method {
            name = "updateHeaderResources"
        }.hook{
            after {
                YLog.debug("[SBFontRestore] 尝试HookCCHeaderView")

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