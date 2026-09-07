package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia

import android.telephony.SubscriptionManager
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.constructor
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.nubia.KotlinFlowHelper.ReadonlyStateFlow
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.nubia.KotlinFlowHelper

object StatusBarHideCellularIcon : YukiBaseHooker() {
    private const val TAG = "StatusBarHideCellularIcon"
    private val hideSimOne by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.HIDE_SIM_ONE, false)
    }
    private val hideSimTwo by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.HIDE_SIM_TWO, false)
    }
    private val hideMobileActivity by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.HIDE_MOBILE_ACTIVITY, false)
    }
    private val hideMobileType by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.HIDE_MOBILE_TYPE, false)
    }
    private val hideHDNew by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.IconTurner.HIDE_HD_SMALL, false)
    }





    // 目标类
    private val cellularViewModelClass by lazy {
        "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.CellularIconViewModel".toClassOrNull()
    }
    private val mobileIconInteractorImplClass by lazy {
        "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MobileIconInteractorImpl".toClassOrNull()
    }
    private val ImsUpdateFeatureClass by lazy {
        "com.zte.feature.signal.ImsUpdateFeature".toClassOrNull()
    }







    override fun onHook() {
        //  D  updateImsIcon imsIconId = 0, ImsIconRes = (null)
        //  2026-02-20 10:13:25.970 19386-19386 SystemUI_I...ateFeature com.android.systemui
        //  DupdateImsIcon imsIconId = 2131235874, ImsIconRes = com.android.systemui:drawable/
        // 高清通话图标
//        ImsUpdateFeatureClass?.method {
//            name = "updateImsIcon"
//        }?.hook{
//            before {
//                if(hideHDNew){ //如果开启隐藏，则隐藏
//                    args[0] = 0
//                    YLog.debug(tag = TAG, msg = "Hide HD icon")
//                }else{
//                    args[0] = 2131235874 // 显示
//                    YLog.debug(tag = TAG, msg = "Show HD icon")
//                }
//            }
//        }


        // 无隐藏需求时直接返回，避免无效 Hook
        if (!hideSimOne && !hideSimTwo && !hideMobileActivity && !hideMobileType) return





        cellularViewModelClass?.constructor()?.hookAll {
            after {
                val interactor =
                    args.firstOrNull { mobileIconInteractorImplClass?.isInstance(it) == true }
                        ?: run {
                            YLog.debug(tag = TAG, msg = "interactor is null, skip")
                            return@after
                        }

                try {
                    YLog.debug(tag = TAG, msg = "interactor: $interactor")

                    // 获取 connectionRepository 字段
                    val repo = interactor.current().field {
                        name = "connectionRepository"
                    }.any() ?: run {
                        YLog.debug(tag = TAG, msg = "connectionRepository is null, skip")
                        return@after
                    }

                    // 调用 getSubId() 方法
                    val subId = repo.current().method {
                        name = "getSubId"
                    }.call() as? Int ?: run {
                        YLog.debug(tag = TAG, msg = "subId is null, skip")
                        return@after
                    }
                    YLog.debug(tag = TAG, msg = "getSubId() returned $subId")

                    // 获取卡槽索引
                    val slotIndex = SubscriptionManager.getSlotIndex(subId)
                    YLog.debug(tag = TAG, msg = "getSlotIndex() returned $slotIndex")

                    // 判断是否需要隐藏当前卡槽的图标
                    val needHide = when {
                        // 兼容 slotIndex 为 -1 的情况（部分设备 subId 直接对应卡槽）
                        slotIndex == -1 -> (subId == 1 && hideSimOne) || (subId == 2 && hideSimTwo)
                        // 正常卡槽索引（0=卡槽1，1=卡槽2）
                        else -> (slotIndex == 0 && hideSimOne) || (slotIndex == 1 && hideSimTwo)
                    }

//                    if (needHide) {
//                        // 创建只读 StateFlow 并赋值给 isVisible
//                        val readonlyFlow = ReadonlyStateFlow(false) ?: run {
//                            YLog.error(tag = TAG, msg = "Create ReadonlyStateFlow failed")
//                            return@after
//                        }
//                        this.instance.current().field {
//                            name = "isVisible"
//                        }.set(readonlyFlow)
//                        YLog.debug(tag = TAG, msg = "Hide cellular icon for subId: $subId, slotIndex: $slotIndex")
//                    }
                    if (needHide) {
                        val field = this.instance.javaClass.getDeclaredField("isVisible")
                        field.isAccessible = true
                        val newFlow =
                            with(KotlinFlowHelper) { with(packageParam) { MutableStateFlow(false) } }
                        field.set(this.instance, newFlow)
                    }


                    if (hideMobileType) { // 隐藏移动网络类型
                        val fieldOut3 =
                            this.instance.javaClass.getDeclaredField("networkTypeIcon")
                        fieldOut3.isAccessible = true
                        val newFlow3 =
                            with(KotlinFlowHelper) { with(packageParam) { MutableStateFlow(null) } }
                        fieldOut3.set(this.instance, newFlow3)

                        YLog.debug(
                            tag = TAG,
                            msg = "set activityOutVisible and activityInVisible to false"
                        )
                    }

                } catch (e: Exception) {
                    YLog.error(tag = TAG, msg = "Failed to hide cellular icon", e = e)
                }
            }
        }
    }
}