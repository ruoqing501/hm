package dev.lackluster.mihelper.hook.rules.clockcomponent

import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object DigitalClockAmPmHook : YukiBaseHooker() {
    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_PERIOD) {
            YLog.debug("[DigitalClockAmPmHook] 开始Hook时钟AM/PM显示功能")

            // 1. Hook 水平时钟的 DigitalClock$DigitalClockAmPm 类
            YLog.debug("[DigitalClockAmPmHook] 尝试Hook水平时钟类: DigitalClock\$DigitalClockAmPm")
            "zte.com.cn.alarmclock.DigitalClock\$DigitalClockAmPm".toClassOrNull()?.apply {
                YLog.debug("[DigitalClockAmPmHook] 找到水平时钟类，开始Hook setIsMorning 方法")

                // Hook setIsMorning 方法
                method {
                    name = "setIsMorning"
                    paramCount = 1
                }.hook {
                    before {
                        YLog.debug("[DigitalClockAmPmHook] 水平时钟 setIsMorning 被调用，参数 z = ${this.args[0]}")
                    }

                    after {
                        YLog.debug("[DigitalClockAmPmHook] 水平时钟 setIsMorning 方法执行完成")
                    }
                }

                // Hook setShowAmPm 方法，强制显示AM/PM
                method {
                    name = "setShowAmPm"
                    paramCount = 1
                }.hook {
                    before {
                        YLog.debug("[DigitalClockAmPmHook] 水平时钟 setShowAmPm 被调用，参数 z = ${this.args[0]}")

                        // 强制设置为true，始终显示AM/PM
                        this.args[0] = true
                        YLog.debug("[DigitalClockAmPmHook] 已修改参数为true，强制显示水平时钟AM/PM")
                    }

                    after {
                        YLog.debug("[DigitalClockAmPmHook] 水平时钟 setShowAmPm 方法执行完成")

                        try {
                            // 获取 TextView 实例并验证
                            val mAmPm = this.instance.current().field {
                                name = "mAmPm"
                            }.cast<TextView>()

                            if (mAmPm != null) {
                                YLog.debug("[DigitalClockAmPmHook] 水平时钟AM/PM可见性已设置为: ${mAmPm.visibility}")
                            }
                        } catch (e: Exception) {
                            YLog.error("[DigitalClockAmPmHook] 验证水平时钟AM/PM可见性时出错: ${e.message}")
                        }
                    }
                }

                YLog.debug("[DigitalClockAmPmHook] 水平时钟Hook设置完成")
            } ?: run {
                // 如果找不到类，记录日志
                YLog.warn("[DigitalClockAmPmHook] 未找到 DigitalClock\$DigitalClockAmPm 类")
            }

            // 2. Hook 垂直时钟的 DigitalClockVertical$DigitalClockAmPm 类
            YLog.debug("[DigitalClockAmPmHook] 尝试Hook垂直时钟类: DigitalClockVertical\$DigitalClockAmPm")
            "zte.com.cn.alarmclock.DigitalClockVertical\$DigitalClockAmPm".toClassOrNull()?.apply {
                YLog.debug("[DigitalClockAmPmHook] 找到垂直时钟类，开始Hook setIsMorning 方法")

                // Hook setIsMorning 方法
                method {
                    name = "setIsMorning"
                    paramCount = 1
                }.hook {
                    before {
                        YLog.debug("[DigitalClockAmPmHook] 垂直时钟 setIsMorning 被调用，参数 z = ${this.args[0]}")
                    }

                    after {
                        YLog.debug("[DigitalClockAmPmHook] 垂直时钟 setIsMorning 方法执行完成")
                    }
                }

                // Hook setShowAmPm 方法，强制显示AM/PM
                method {
                    name = "setShowAmPm"
                    paramCount = 1
                }.hook {
                    before {
                        YLog.debug("[DigitalClockAmPmHook] 垂直时钟 setShowAmPm 被调用，参数 z = ${this.args[0]}")

                        // 强制设置为true，始终显示AM/PM
                        this.args[0] = true
                        YLog.debug("[DigitalClockAmPmHook] 已修改参数为true，强制显示垂直时钟AM/PM")
                    }

                    after {
                        YLog.debug("[DigitalClockAmPmHook] 垂直时钟 setShowAmPm 方法执行完成")

                        try {
                            // 获取 TextView 实例并验证
                            val mAmPm = this.instance.current().field {
                                name = "mAmPm"
                            }.cast<TextView>()

                            if (mAmPm != null) {
                                YLog.debug("[DigitalClockAmPmHook] 垂直时钟AM/PM可见性已设置为: ${mAmPm.visibility}")
                            }
                        } catch (e: Exception) {
                            YLog.error("[DigitalClockAmPmHook] 验证垂直时钟AM/PM可见性时出错: ${e.message}")
                        }
                    }
                }

                YLog.debug("[DigitalClockAmPmHook] 垂直时钟Hook设置完成")
            } ?: run {
                // 如果找不到类，记录日志
                YLog.warn("[DigitalClockAmPmHook] 未找到 DigitalClockVertical\$DigitalClockAmPm 类")
            }

            // 3. Hook DigitalClock 的 setDateFormat 方法，确保AM/PM始终显示
            YLog.debug("[DigitalClockAmPmHook] 尝试Hook DigitalClock 的 setDateFormat 方法")
            "zte.com.cn.alarmclock.DigitalClock".toClassOrNull()?.apply {
                method {
                    name = "setDateFormat"
                }.hook {
                    after {
                        YLog.debug("[DigitalClockAmPmHook] DigitalClock setDateFormat 方法执行完成")

                        try {
                            // 获取 mFormat 字段
                            val mFormat = this.instance.current().field {
                                name = "mFormat"
                            }.string()

                            YLog.debug("[DigitalClockAmPmHook] 当前时间格式: $mFormat")

                            // 获取 mAmPmDis
                            val mAmPmDis = this.instance.current().field {
                                name = "mAmPmDis"
                            }.any()

                            if (mAmPmDis != null) {
                                // 强制调用 setShowAmPm(true)
                                mAmPmDis.current()?.method {
                                    name = "setShowAmPm"
                                    paramCount = 1
                                }?.call(true)
                                YLog.debug("[DigitalClockAmPmHook] 已强制调用 setShowAmPm(true) 确保显示AM/PM")
                            }
                        } catch (e: Exception) {
                            YLog.error("[DigitalClockAmPmHook] 处理DigitalClock setDateFormat时出错: ${e.message}")
                        }
                    }
                }
            }

            // 4. Hook DigitalClockVertical 的 setDateFormat 方法，确保AM/PM始终显示
            YLog.debug("[DigitalClockAmPmHook] 尝试Hook DigitalClockVertical 的 setDateFormat 方法")
            "zte.com.cn.alarmclock.DigitalClockVertical".toClassOrNull()?.apply {
                method {
                    name = "setDateFormat"
                }.hook {
                    after {
                        YLog.debug("[DigitalClockAmPmHook] DigitalClockVertical setDateFormat 方法执行完成")

                        try {
                            // 获取 mFormat 字段
                            val mFormat = this.instance.current().field {
                                name = "mFormat"
                            }.string()

                            YLog.debug("[DigitalClockAmPmHook] 当前时间格式: $mFormat")

                            // 获取 mAmPmDis
                            val mAmPmDis = this.instance.current().field {
                                name = "mAmPmDis"
                            }.any()

                            if (mAmPmDis != null) {
                                // 强制调用 setShowAmPm(true)
                                mAmPmDis.current()?.method {
                                    name = "setShowAmPm"
                                    paramCount = 1
                                }?.call(true)
                                YLog.debug("[DigitalClockAmPmHook] 已强制调用 setShowAmPm(true) 确保显示AM/PM")
                            }
                        } catch (e: Exception) {
                            YLog.error("[DigitalClockAmPmHook] 处理DigitalClockVertical setDateFormat时出错: ${e.message}")
                        }
                    }
                }
            }

            // 5. Hook DigitalClock 和 DigitalClockVertical 的 updateTime 方法，观察其行为
            listOf(
                "zte.com.cn.alarmclock.DigitalClock",
                "zte.com.cn.alarmclock.DigitalClockVertical"
            ).forEach { className ->
                className.toClassOrNull()?.apply {
                    YLog.debug("[DigitalClockAmPmHook] 找到 $className 类，开始Hook updateTime 方法")

                    method {
                        name = "updateTime"
                    }.hook {
                        after {
                            YLog.debug("[DigitalClockAmPmHook] $className updateTime 方法执行后")

                            try {
                                // 确保AM/PM显示
                                val mAmPmDis = this.instance.current().field {
                                    name = "mAmPmDis"
                                }.any()

                                if (mAmPmDis != null) {
                                    // 强制设置AM/PM可见
                                    mAmPmDis.current()?.method {
                                        name = "setShowAmPm"
                                        paramCount = 1
                                    }?.call(true)

                                    // 获取mAmPm字段并检查
                                    val mAmPm = mAmPmDis.current()?.field {
                                        name = "mAmPm"
                                    }?.cast<TextView>()

                                    if (mAmPm != null) {
                                        YLog.debug("[DigitalClockAmPmHook] $className AM/PM文本: ${mAmPm.text}, 可见性: ${mAmPm.visibility}")
                                    }
                                }
                            } catch (e: Exception) {
                                YLog.error("[DigitalClockAmPmHook] 处理$className updateTime时出错: ${e.message}")
                            }
                        }
                    }

                    YLog.debug("[DigitalClockAmPmHook] $className Hook设置完成")
                } ?: run {
                    YLog.warn("[DigitalClockAmPmHook] 未找到 $className 类")
                }
            }

            YLog.debug("[DigitalClockAmPmHook] 所有Hook设置完成，现在24小时制下也会显示AM/PM")
        }
    }
}