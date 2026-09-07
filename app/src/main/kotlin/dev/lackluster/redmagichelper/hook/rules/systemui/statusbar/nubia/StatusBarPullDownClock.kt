package dev.lackluster.redmagichelper.hook.rules.systemui.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object StatusBarPullDownClock : YukiBaseHooker() {
    // 下拉状态栏时间[索引0表示第1个选项]
    private val statusBarPullDownClockTextType by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.CLOCK_SHOW_PULL_DOWN, 0)
    }

    override fun onHook() {
        // 如果默认第1个选项【不显秒】，则不进行hook处理
        if (statusBarPullDownClockTextType == 0) return

        // 尝试两个可能的类名，因为不同系统版本可能不同
        val ccHeaderClazz = "com.zte.controlcenter.widget.CCHeaderView".toClassOrNull()
            ?: return

        ccHeaderClazz.method {
            name = "onFinishInflate"
        }.hook {
            after {
                // 获取 ClockView 字段
                val clockView = this.instance.current().field {
                    name = "mClockView"
                }.any() ?: return@after

                // 使用反射方式设置 mShowSeconds
                try {
                    val clockClass = clockView.javaClass
                    val mShowSecondsField = clockClass.getDeclaredField("mShowSeconds")
                    mShowSecondsField.isAccessible = true
                    mShowSecondsField.setBoolean(clockView, true)

                    // 调用 updateShowSeconds 方法
                    val updateShowSecondsMethod = clockClass.getDeclaredMethod("updateShowSeconds")
                    updateShowSecondsMethod.invoke(clockView)
                } catch (e: Exception) {
                    // 如果使用反射失败，尝试使用 YukiHookAPI 的方式
                    val clockClazz = "com.android.systemui.statusbar.policy.Clock".toClassOrNull()
                    clockClazz?.apply {
                        method {
                            name = "updateShowSeconds"
                        }.get()?.call(clockView)
                    }
                }
            }
        }
    }
}