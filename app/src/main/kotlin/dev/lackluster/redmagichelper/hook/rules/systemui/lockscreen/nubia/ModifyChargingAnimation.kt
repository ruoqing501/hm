package dev.lackluster.redmagichelper.hook.rules.systemui.lockscreen.nubia

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.rules.systemui.lockscreen.LockScreenBatteryMsg
import dev.lackluster.redmagichelper.utils.Prefs

object ModifyChargingAnimation : YukiBaseHooker() {
    private const val TAG = "ModifyChargingAnimation"
    private var lastTriggerTime = 0L


    // 锁屏充电动画总开关
    private val lockScreenChargingAnimationSwitch by lazy {
        Prefs.getBoolean(  Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_CHARGING_ANIMATION_SWITCH, false)
    }


    // 每次进入锁屏界面都显示充电动画
    private val lockScreenChargingAnimation by lazy {
        Prefs.getBoolean(  Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_CHARGING_ANIMATION, false)
    }





    //  充电动画持续时间(单位：秒)
    private val UPDATE_INTERVAL_SEC by lazy {
        Prefs.getInt(Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_DISPLAY_ANIMATION_DURATION, 6)
    }
    //  充电动画持续时间(毫秒)
    private val UPDATE_INTERVAL_MS get() = UPDATE_INTERVAL_SEC * 1000L


    // 延迟指定秒后才开始显示充电动画(单位：秒)

    private val UPDATE_INTERVAL_SEC_Delay by lazy {
        Prefs.getInt(Pref.Key.SystemUI.LockScreen.LOCK_SCREEN_DISPLAY_DELAY_TIME, 0)
    }

    // 延迟指定秒后才开始显示充电动画(单位：毫秒)
    private val UPDATE_INTERVAL_MS_Delay get() = UPDATE_INTERVAL_SEC_Delay * 1000L



    override fun onHook() {
        // 锁屏充电动画总开关
        if (!lockScreenChargingAnimationSwitch) return
        // 1. 修改隐藏延迟（延长充电动画显示时间）
        "com.zte.feature.charging.ChargingFeature".toClass().method {
            name = "hideChargingViewDelayed"
            paramCount = 0
        }.hook {
            before {
                val instance = this.instance
                val handlerField = instance.javaClass.getDeclaredField("mHandler")
                handlerField.isAccessible = true
                val handler = handlerField.get(instance) as Handler
                handler.removeMessages(11001)
                //val newDelay = 30000L
                val newDelay = UPDATE_INTERVAL_MS
                handler.sendEmptyMessageDelayed(11001, newDelay)
                YLog.debug(tag = TAG, msg = "hideChargingViewDelayed overridden, new delay: $newDelay ms")
                result = Unit
            }
        }

        // 2. 修改插入电源后到显示充电界面的延迟（可选）
        "com.zte.feature.charging.ChargingFeature".toClass().method {
            name = "startChargingAnimation"
            param(Long::class.java)
        }.hook {
            before {
                //args[0] = 1000L
                args[0] = UPDATE_INTERVAL_MS_Delay
                YLog.debug(tag = TAG, msg = "startChargingAnimation delay set to ${args[0]} ms")
            }
        }

        // 每次进入锁屏界面都显示充电动画
        if (!lockScreenChargingAnimation) return
        "com.android.systemui.keyguard.KeyguardViewMediator".toClass().method {
            name = "onStartedWakingUp"
            paramCount = 2
        }.hook {
            after {
                val now = System.currentTimeMillis()
                if (now - lastTriggerTime < 1000) {
                    YLog.debug(tag = TAG, msg = "Trigger too frequent, skip.")
                    return@after
                }
                lastTriggerTime = now

                val mediator = this.instance
                val isShowingMethod = mediator.javaClass.getMethod("isShowing")

                Handler(Looper.getMainLooper()).post {
                    try {
                        val isShowing = isShowingMethod.invoke(mediator) as Boolean
                        if (!isShowing) {
                            YLog.debug(tag = TAG, msg = "Keyguard not showing, skip.")
                            return@post
                        }

                        val classLoader = mediator.javaClass.classLoader
                        val featureClass = Class.forName("com.zte.base.Feature", true, classLoader)
                        val getMethod = featureClass.getMethod("get", String::class.java)
                        val chargingFeature = getMethod.invoke(null, "CHARGING_VIEW")

                        if (chargingFeature == null) {
                            YLog.warn(tag = TAG, msg = "ChargingFeature not found")
                            return@post
                        }

                        // 获取充电状态
                        val isChargingField = tryGetField(chargingFeature.javaClass, "mIsCharging")
                        val pluggedInField = tryGetField(chargingFeature.javaClass, "mPluggedIn")
                        val chargeSeparationField = tryGetField(chargingFeature.javaClass, "mChargeIsSeparation")

                        if (isChargingField == null || pluggedInField == null || chargeSeparationField == null) {
                            YLog.warn(tag = TAG, msg = "Failed to get charging state fields")
                            return@post
                        }

                        val isCharging = isChargingField.getBoolean(chargingFeature)
                        val pluggedIn = pluggedInField.getBoolean(chargingFeature)
                        val chargeSeparation = chargeSeparationField.getBoolean(chargingFeature)

                        // 只有真实充电且未处于分离模式时才显示动画
                        if (!(isCharging || pluggedIn) || chargeSeparation) {
                            YLog.debug(tag = TAG, msg = "Device not charging or in separation mode, skip animation. isCharging=$isCharging, pluggedIn=$pluggedIn, separation=$chargeSeparation")
                            return@post
                        }

                        // ---------- 新增：如果已有动画在显示，先隐藏 ----------
                        val chargingViewShowingField = tryGetField(chargingFeature.javaClass, "mChargingViewShowing")
                        if (chargingViewShowingField != null) {
                            val isShowingNow = chargingViewShowingField.getBoolean(chargingFeature)
                            if (isShowingNow) {
                                try {
                                    val hideMethod = chargingFeature.javaClass.getDeclaredMethod("hideChargingView")
                                    hideMethod.invoke(chargingFeature)
                                    YLog.debug(tag = TAG, msg = "Hidden existing charging view before showing new one")
                                } catch (e: Exception) {
                                    YLog.warn(tag = TAG, msg = "Failed to hide charging view", e = e)
                                }
                            }
                        }
                        // ------------------------------------------------

                        // 启动新动画
                        val startChargingAnimationMethod = chargingFeature.javaClass.getMethod("startChargingAnimation", Long::class.java)
                        startChargingAnimationMethod.invoke(chargingFeature, UPDATE_INTERVAL_MS_Delay)
                        YLog.debug(tag = TAG, msg = "Charging animation scheduled with delay=${UPDATE_INTERVAL_MS_Delay}ms")

                    } catch (e: Exception) {
                        YLog.error(tag = TAG, msg = "Failed to trigger charging animation", e = e)
                    }
                }
            }
        }
    }

    private fun tryGetField(clazz: Class<*>, fieldName: String): java.lang.reflect.Field? {
        return try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field
        } catch (e: NoSuchFieldException) {
            YLog.warn(tag = TAG, msg = "Field $fieldName not found in ${clazz.simpleName}")
            null
        }
    }
}