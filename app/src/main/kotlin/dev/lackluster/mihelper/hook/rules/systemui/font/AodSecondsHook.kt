//package dev.lackluster.mihelper.hook.rules.systemui.font
//
//
//import android.content.res.Resources
//import android.view.View
//import android.widget.TextClock
//import android.widget.TextView
//import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
//import com.highcapable.yukihookapi.hook.factory.method
//import com.highcapable.yukihookapi.hook.log.YLog
//import java.util.WeakHashMap
//
//object AodSeconds : YukiBaseHooker() {
//    private const val TAG = "AodSeconds"
//    private val recursiveTextClocks = WeakHashMap<TextClock, Boolean>()
//
//    override fun onHook() {
//        hookAodClockSeconds()
//    }
//
//    private fun hookAodClockSeconds() {
//        val textClockClass = "android.widget.TextClock".toClass()
//
//        // 钩住 onTimeChanged 方法（核心更新时间点）
//        textClockClass.method {
//            name = "onTimeChanged"
//            emptyParam()
//        }.hook {
//            after {
//                val textClock = this.instance as? TextClock ?: return@after
//                if (recursiveTextClocks[textClock] == true) return@after
//                recursiveTextClocks[textClock] = true
//                try {
//                    val context = textClock.context
//                    if (isAodClock(textClock)) {
//                        if (textClock::class.java.simpleName == "TextClock") {
//                            try {
//                                val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
//                                val format = if (is24Hour) "HH:mm:ss" else "h:mm:ss"
//                                val textClockClass = textClock::class.java
//                                if (is24Hour) {
//                                    textClockClass.getMethod("setFormat24Hour", CharSequence::class.java)
//                                        .invoke(textClock, format)
//                                } else {
//                                    textClockClass.getMethod("setFormat12Hour", CharSequence::class.java)
//                                        .invoke(textClock, format)
//                                }
//                                YLog.debug(tag = TAG, msg = "AOD时钟已启用秒显示: $format")
//                            } catch (e: Exception) {
//                                YLog.debug(tag = TAG, msg = "设置AOD时钟秒格式失败: ${e.message}")
//                            }
//                        }
//                    }
//                } finally {
//                    recursiveTextClocks.remove(textClock)
//                }
//            }
//        }
//
//        // 可选：钩住 refresh 方法以增加兼容性（某些版本可能使用此方法更新）
//        try {
//            textClockClass.method {
//                name = "refresh"
//                emptyParam()
//            }.hook {
//                after {
//                    val textClock = this.instance as? TextClock ?: return@after
//                    if (recursiveTextClocks[textClock] == true) return@after
//                    recursiveTextClocks[textClock] = true
//                    try {
//                        val context = textClock.context
//                        if (isAodClock(textClock)) {
//                            if (textClock::class.java.simpleName == "TextClock") {
//                                try {
//                                    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
//                                    val format = if (is24Hour) "HH:mm:ss" else "h:mm:ss"
//                                    val textClockClass = textClock::class.java
//                                    if (is24Hour) {
//                                        textClockClass.getMethod("setFormat24Hour", CharSequence::class.java)
//                                            .invoke(textClock, format)
//                                    } else {
//                                        textClockClass.getMethod("setFormat12Hour", CharSequence::class.java)
//                                            .invoke(textClock, format)
//                                    }
//                                    YLog.debug(tag = TAG, msg = "AOD时钟（refresh）已启用秒显示: $format")
//                                } catch (e: Exception) {
//                                    YLog.debug(tag = TAG, msg = "设置AOD时钟秒格式失败: ${e.message}")
//                                }
//                            }
//                        }
//                    } finally {
//                        recursiveTextClocks.remove(textClock)
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            YLog.debug(tag = TAG, msg = "refresh 方法不存在，跳过")
//        }
//    }
//
//    private fun isAodClock(view: View): Boolean {
//        try {
//            if (view !is TextView) return false
//            val viewId = getResourceIdName(view)
//            if (viewId != "screen_off_clock") return false
//
//            // 第1级父容器：LinearLayout (ID: NO_ID)
//            val parent1 = view.parent
//            if (parent1 !is View) return false
//            val parent1Id = getResourceIdName(parent1)
//            if (parent1Id != "NO_ID" && parent1Id != "UNKNOWN_ID") return false
//            if (parent1::class.java.simpleName != "LinearLayout") return false
//
//            // 第2级父容器：MaskLinearLayout (ID: mask_area)
//            val parent2 = parent1.parent
//            if (parent2 !is View) return false
//            val parent2Id = getResourceIdName(parent2)
//            if (parent2Id != "mask_area") return false
//            if (parent2::class.java.simpleName != "MaskLinearLayout") return false
//
//            // 第3级父容器：AodClockStyleHorizen (ID: single_clock)
//            val parent3 = parent2.parent
//            if (parent3 !is View) return false
//            val parent3Id = getResourceIdName(parent3)
//            if (parent3Id != "single_clock") return false
//            if (parent3::class.java.simpleName != "AodClockStyleHorizen") return false
//
//            // 第4级父容器：MaskLinearLayout (ID: double_mask_area)
//            val parent4 = parent3.parent
//            if (parent4 !is View) return false
//            val parent4Id = getResourceIdName(parent4)
//            if (parent4Id != "double_mask_area") return false
//            if (parent4::class.java.simpleName != "MaskLinearLayout") return false
//
//            // 第5级父容器：AodDualClockStyle (ID: NO_ID)
//            val parent5 = parent4.parent
//            if (parent5 !is View) return false
//            val parent5Id = getResourceIdName(parent5)
//            if (parent5Id != "NO_ID" && parent5Id != "UNKNOWN_ID") return false
//            if (parent5::class.java.simpleName != "AodDualClockStyle") return false
//
//            // 第6级父容器：AodContentView (ID: keyguard_secreen_off_area)
//            val parent6 = parent5.parent
//            if (parent6 !is View) return false
//            val parent6Id = getResourceIdName(parent6)
//            if (parent6Id != "keyguard_secreen_off_area") return false
//            if (parent6::class.java.simpleName != "AodContentView") return false
//
//            // 第7级父容器：ConstraintLayout (ID: aod_view)
//            val parent7 = parent6.parent
//            if (parent7 !is View) return false
//            val parent7Id = getResourceIdName(parent7)
//            if (parent7Id != "aod_view") return false
//            if (parent7::class.java.simpleName != "ConstraintLayout") return false
//
//            // 第8级父容器：ConstraintLayout (ID: NO_ID)
//            val parent8 = parent7.parent
//            if (parent8 !is View) return false
//            val parent8Id = getResourceIdName(parent8)
//            if (parent8Id != "NO_ID" && parent8Id != "UNKNOWN_ID") return false
//            if (parent8::class.java.simpleName != "ConstraintLayout") return false
//
//            // 第9级父容器：FrameLayout (ID: content)
//            val parent9 = parent8.parent
//            if (parent9 !is View) return false
//            val parent9Id = getResourceIdName(parent9)
//            if (parent9Id != "content") return false
//            if (parent9::class.java.simpleName != "FrameLayout") return false
//
//            return true
//        } catch (e: Exception) {
//            return false
//        }
//    }
//
//    /**
//     * 根据视图获取其资源ID的字符串名称。
//     */
//    private fun getResourceIdName(view: View): String {
//        return try {
//            val resId = view.id
//            if (resId <= 0) {
//                "NO_ID"
//            } else {
//                try {
//                    view.resources.getResourceEntryName(resId)
//                } catch (e: Resources.NotFoundException) {
//                    "0x${Integer.toHexString(resId)}"
//                }
//            }
//        } catch (e: Exception) {
//            "UNKNOWN_ID"
//        }
//    }
//}