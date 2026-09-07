package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import kotlin.math.abs

object StatusBarDoubleTapToSleep : YukiBaseHooker() {

    @SuppressLint("PrivateApi")
    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DOUBLE_CLICKED_LOCKED_SCREEN) {
            YLog.debug("[StatusBarDoubleTapToSleep] 开始Hook状态栏双击睡眠功能")

            // Hook PhoneStatusBarView 类的 onFinishInflate 方法
            "com.android.systemui.statusbar.phone.PhoneStatusBarView".toClass().apply {
                method {
                    name = "onFinishInflate"
                }.hook {
                    after {
                        // 添加双击睡眠功能
                        addDoubleTapToSleep(this.instance as ViewGroup)
                    }
                }
            }

            YLog.debug("[StatusBarDoubleTapToSleep] Hook状态栏双击睡眠功能完成")
        }
    }

    /**
     * 为ViewGroup添加双击睡眠功能
     */
    private fun addDoubleTapToSleep(viewGroup: ViewGroup) {
        // 使用额外的字段来存储触摸状态
        var currentTouchTime: Long = 0L
        var currentTouchX: Float = 0f
        var currentTouchY: Float = 0f

        viewGroup.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_DOWN) {
                return@setOnTouchListener false
            }

            // 记录上次触摸的时间和位置
            val lastTouchTime = currentTouchTime
            val lastTouchX = currentTouchX
            val lastTouchY = currentTouchY

            // 更新当前触摸的时间和位置
            currentTouchTime = System.currentTimeMillis()
            currentTouchX = event.x
            currentTouchY = event.y

            // 检查是否为双击（时间间隔小于250ms，位置偏移小于100像素）
            if (currentTouchTime - lastTouchTime < 250L &&
                abs(currentTouchX - lastTouchX) < 100f &&
                abs(currentTouchY - lastTouchY) < 100f
            ) {
                YLog.debug("[StatusBarDoubleTapToSleep] 检测到双击，执行睡眠操作")

                // 执行睡眠操作
                try {
                    val powerService = v.context.getSystemService(Context.POWER_SERVICE)
                    powerService?.javaClass?.getMethod("goToSleep", Long::class.java)?.invoke(
                        powerService,
                        SystemClock.uptimeMillis()
                    )
                    YLog.debug("[StatusBarDoubleTapToSleep] 睡眠操作执行成功")
                } catch (e: Exception) {
                    YLog.error("[StatusBarDoubleTapToSleep] 执行睡眠操作失败: ${e.message}")
                }

                // 重置触摸状态
                currentTouchTime = 0L
                currentTouchX = 0f
                currentTouchY = 0f
            }

            // 执行点击事件
            v.performClick()
            false
        }

        YLog.debug("[StatusBarDoubleTapToSleep] 已为状态栏添加双击睡眠功能")
    }
}