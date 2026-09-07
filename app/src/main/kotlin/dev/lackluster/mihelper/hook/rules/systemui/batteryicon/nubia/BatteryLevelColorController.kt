package dev.lackluster.mihelper.hook.rules.systemui.batteryicon.nubia

import android.R
import android.content.Context
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.widget.ImageView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntType
import de.robv.android.xposed.XposedHelpers
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.hasEnable

object BatteryLevelColorController : YukiBaseHooker() {
    private const val TAG = "BatteryLevelColorController"
    private val battery_style by lazy {
        Prefs.getInt(Pref.Key.SystemUI.IconTurner.BATTERY_STYLE, 0)
    }

    private val battery_alpha by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.BATTERY_STYLE_ALPHA, 100)
    }

    private val battery_color_switch by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.BATTERY_ICON_COLOR_SWITCH, false)
    }

    // 值为0 -> 0xFF388E3C
    // 值为 1 -> 0xFF4CAF50
    // 值为 2 -> 0xFFFF9500
    // 值为 3 -> 0xFFFF3B30

    private val battery_color_phase1 by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.BATTERY_COLOR_Phase1, 0)
    }

    private val battery_color_phase2 by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.BATTERY_COLOR_Phase2, 1)
    }
    private val battery_color_phase3 by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.BATTERY_COLOR_Phase3, 2)
    }
    private val battery_color_phase4 by lazy {
        Prefs.getInt( Pref.Key.SystemUI.StatusBar.BATTERY_COLOR_Phase4, 3)
    }



    // 值为0 -> 0xFF388E3C
    // 值为 1 -> 0xFF4CAF50
    // 值为 2 -> 0xFFFF9500
    // 值为 3 -> 0xFFFF3B30
    private fun  getBatteryColor(phase: Int): Long {
        return when (phase) {
            0 -> 0xFF388E3C
            1 -> 0xFF4CAF50
            2 -> 0xFFFF9500
            3 -> 0xFFFF3B30
            else -> 0xFFFFFF
        }
    }



    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.IconTurner.BATTERY_ICON_COLOR_SWITCH) {
            val batteryViewClz = "com.zte.mifavor.views.MFVBatteryMeterView".toClass()
            val systemUIApplication = "com.android.systemui.SystemUIApplication".toClass()
            val setImageDrawable = batteryViewClz.method {
                name = "setImageDrawable"
                superClass()
            }

            // Hook updateBattery 方法，在原始逻辑完成后修改颜色
            batteryViewClz.method {
                name = "updateBattery"
            }.hook {
                after {
                    try {
                        // 直接转换为 ImageView 获取当前显示的 Drawable（已通过 mutate 隔离）
                        val currentDrawable = (instance as? ImageView)?.drawable ?: return@after

//                        if(battery_style ==0){ //电池内部，修改透明度为255
//                            currentDrawable.alpha = 255
//                            currentDrawable.alpha = (255 * 0.3).toInt()
//                            currentDrawable.alpha =  (battery_alpha / 100.0 * 255).toInt()
//                            YLog.debug(tag = TAG, msg = "update battery alpha ${currentDrawable.alpha}")
//                        }

                        // 获取电量
                        val batteryLevel = instance.current().field { name = "mBateryLevel" }.int()

                        // 获取充电状态
                        val isCharging = instance.current().field { name = "mIsCharing" }.boolean()

                        // 根据电量计算颜色（ARGB 格式）
//                        val batteryColor = when (batteryLevel) {
//                            in 0..19 -> 0xFFFF3B30.toInt()  // 红
//                            in 20..50 -> 0xFFFF9500.toInt() // 橙
//                            in 51..79 -> 0xFF4CAF50.toInt() // 浅绿
//                            in 80..100 -> 0xFF388E3C.toInt() // 深绿
//                            else -> 0xFFFFFFFF.toInt()       // 默认白
//                        }

                        val batteryColor = when (batteryLevel) {
                            in 0..19 -> getBatteryColor(battery_color_phase4).toInt()  // 红
                            in 20..50 -> getBatteryColor(battery_color_phase3).toInt() // 橙
                            in 51..79 -> getBatteryColor(battery_color_phase2).toInt() // 浅绿
                            in 80..100 -> getBatteryColor(battery_color_phase1).toInt() // 深绿
                            else -> 0xFFFFFFFF.toInt()       // 默认白
                        }
                        // 充电状态使用特殊颜色


                        val finalColor = if (isCharging) 0xFF34C759.toInt() else batteryColor
//                        val  finalColor = if(isCharging && batteryLevel >= 20){
//                            0xFF34C759.toInt()
//                        }else{
//                            batteryColor
//                        }
//                        val finalColor =  batteryColor

                        // 设置 tint（保留原始透明度）
//                        currentDrawable.setTint(finalColor)


                        // 获取视图类型

                        val mType = instance.current().field { name = "mType" }.any()

                        // isStatusBar isQuickStatusBar isPhoneCallView isKeyguardDoze
                        if (mType != null) {
//
                            // 状态栏
                            val isStatusBar =
                                mType.current().method { name = "isStatusBar" }.boolean()
                            val isQuickStatusBar =
                                mType.current().method { name = "isQuickStatusBar" }.boolean()
                            val isPhoneCallView =
                                mType.current().method { name = "isPhoneCallView" }.boolean()
                            val isKeyguardDoze =
                                mType.current().method { name = "isKeyguardDoze" }.boolean()

                            if (isStatusBar || isQuickStatusBar || isKeyguardDoze || isPhoneCallView) {

                                currentDrawable.setTint(finalColor)
                                currentDrawable.alpha =  (battery_alpha / 100.0 * 255).toInt()

////                                // 获取电量显示模式
//                                val context = instance.current().method {
//                                    name = "getContext"
//                                    superClass()
//                                }.call() as? Context
//                                val contentResolver = context?.contentResolver
//                                val batteryPercentageOn = Settings.System.getInt(
//                                    contentResolver,
//                                    "battery_percentage_on",
//                                    2
//                                )
//                                YLog.debug(tag = TAG, msg = "batteryPercentageOn $batteryPercentageOn")
//                                val isInsideDisplayMode = batteryPercentageOn == 1 // 如果在内部显示模式下，则设置透明度为255
//                                if (isInsideDisplayMode) {
//                                    currentDrawable.alpha = 255
//                                }
                                YLog.debug(
                                    tag = TAG,
                                    msg = "isStatusBar $isStatusBar isQuickStatusBar $isQuickStatusBar isPhoneCallView $isPhoneCallView isKeyguardDoze $isKeyguardDoze"
                                )


                            }
                        }

                    } catch (e: Exception) {
                        YLog.error(tag = TAG, msg = "Error in updateBattery after hook: $e")
                    }
                }
            }
            // 监听电量变化
            batteryViewClz.method {
                name = "onBatteryLevelChanged"
                param(IntType, BooleanType,BooleanType,BooleanType,IntType)
            }.hook{
                after {
                     batteryViewClz.method {
                         name = "updateBattery"
                     }.get(instance).call()
                    YLog.debug(tag = TAG, msg = "Battery level changed")
                }
            }
        }
    }
}