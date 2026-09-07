package dev.lackluster.mihelper.hook.rules.systemui.statusbar.nubia

import android.view.View
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object SunShineFeatureHooker : YukiBaseHooker() {
    private const val TAG = "SunShineFeatureHooker"

    override fun onHook() {

        hasEnable(Pref.Key.SystemUI.StatusBar.HOLES_OFTEN_SHOW){
            //1. Hook FakeNotchFeature.updateBlackVisibility，强制显示黑色圆圈
            "com.zte.feature.fake.FakeNotchFeature".toClassOrNull()?.apply {
                method {
                    name = "updateBlackVisibility"
                    param(BooleanType, BooleanType, BooleanType)
                }.hook {
                    before {
                        YLog.debug(tag = TAG, msg =  "强制显示挖孔圆圈")
                        //args[0] = false  // 不显示人脸动画
                        //args[0] = true  // 显示人脸动画
                        args[1] = true   // 显示黑色圆圈
                        //args[2] = false  // 不显示小屏
                        //args[2] = true  // 显示小屏
                    }
                }
            } ?: YLog.error(tag = TAG, msg =  "未找到 FakeNotchFeature 类")
            // 2. 可选：拦截任何试图隐藏挖孔视图的操作
            View::class.java.method {
                name = "setVisibility"
                param(IntType)
            }.hook {
                before {
                    val view = instance as? View ?: return@before
                    // 通过资源 ID 判断是否为挖孔相关视图（可根据实际系统资源名调整）
                    try {
                        val id = view.id
                        val entryName = view.context.resources.getResourceEntryName(id)
                        if (entryName == "circle_view_parent" || entryName == "black_circle_view") {
                            val newVisibility = args[0] as Int
                            if (newVisibility != View.VISIBLE) {
                                YLog.debug(tag = TAG, msg =  "阻止隐藏挖孔视图: $entryName")
                                args[0] = View.VISIBLE
                            }
                        }
                    } catch (e: Exception) {
                        // 忽略无法获取资源名的视图
                    }
                }
            }
        }

    }
}