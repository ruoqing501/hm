package dev.lackluster.redmagichelper.hook.rules.systemui.screenoff.nubia

import android.view.View
import android.view.ViewGroup
import android.widget.TextClock
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.constructor
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.LongType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.DexKit
import dev.lackluster.redmagichelper.utils.Prefs
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.MethodData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AodSecondUpdate : YukiBaseHooker() {
    private const val TAG = "AodSecondUpdate"
    const val AOD_CLOCK_PKG_NAME = "com.zte.feature.doze.AodClock.AodClockStyle"

    // 需要修改 mTimeView 的类列表
    private val TARGET_TIME_VIEW_CLASSES = listOf(
        "com.zte.feature.doze.AodClock.nubia.NubiaAodTextWidgetBaseLayout",
        "com.zte.feature.doze.AodClock.AodClockStyle.AodClockTraveler",
        "com.zte.feature.doze.nubia.style.timing.NubiaAodWidgetTiming",
        "com.zte.feature.doze.nubia.style.weather.NubiaAodStyleTimeWeather"
    )

    override fun onHook() {
        YLog.info("$TAG hooking...")

        // 1. 处理包含 mScreenOffClock 的时钟控件
        handleScreenOffClockClasses()

        // 2. 处理包含 mTimeView 的时钟控件
        handleTimeViewClasses()

        // 3. 处理 AodTextStyle 的 mDataView
        handleAodTextStyle()

        // 4. 处理通过 AodClockViewContainer 加载的时钟（数字人、旅行者等）
        handleAodClockViewContainer()

        // 5. 修改 DozeUi 的 roundToNextMinute 以实现每秒刷新
        hookDozeUiMethods()
    }

    /** 熄屏显秒开关（回调内实时读取，切换即时生效） */
    private val showSeconds get() =
        Prefs.getBoolean(Pref.Key.SystemUI.LockScreen.SCREEN_OFF_SHOW_SECONDS, false)

    /**
     * 为 TextClock 开启秒显示
     * 自动处理 12/24 小时制，避免重复添加
     */
    private fun TextClock.enableSeconds() {
        val old24 = format24Hour?.toString() ?: "kk:mm"
        val old12 = format12Hour?.toString() ?: "h:mm"

        if (!old24.contains(":ss")) format24Hour = old24.replace(":mm", ":mm:ss")
        if (!old12.contains(":ss")) format12Hour = old12.replace(":mm", ":mm:ss")
    }

    /**
     * 递归遍历 ViewGroup 中所有 TextClock 并启用秒
     */
    private fun enableSecondsInViewGroup(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            when (child) {
                is TextClock -> child.enableSeconds()
                is ViewGroup -> enableSecondsInViewGroup(child)
            }
        }
    }

    /**
     * 处理包含 mScreenOffClock 的类
     * 包含对 AodClockStyleHorizen 的特殊布局处理
     */
    private fun handleScreenOffClockClasses() {
        val classList = findScreenOffClockClasses(DexKit.dexKitBridge)

        classList.forEach { classData ->
            val className = classData.name
            YLog.debug("$TAG found screen off clock class: $className")

            val clazz = className.toClassOrNull(appClassLoader) ?: return@forEach

            // 获取方法实例
            val methods = classData.methods
            val onFinishInflateMethod = methods.find { it.name == "onFinishInflate" }?.getMethodInstance(appClassLoader!!)
            val refreshAmPmMethod = methods.find { it.name == "refreshAmPm" }?.getMethodInstance(appClassLoader!!)

            // Hook onFinishInflate
            onFinishInflateMethod?.let { method ->
                method.hook {
                    after {
                        if (!showSeconds) return@after
                        val clock = clazz.field {
                            name = "mScreenOffClock"
                        }.get(instance).any() as? TextClock
                        clock?.enableSeconds()

//                        // 假设存在
//                        val clock2 = clazz.field {
//                            name = "mClock"
//                        }.get(instance).any() as? TextClock
//                        clock2?.enableSeconds()
                        // 如果是 AodClockStyleHorizen，进行布局调整
                        if (className.contains("AodClockStyleHorizen", ignoreCase = true)) {
                            adjustHorizenLayout(clock)
                        }
                        YLog.debug("$TAG $className.onFinishInflate: enabled seconds")
                    }
                }
            }

            // Hook refreshAmPm
            refreshAmPmMethod?.let { method ->
                method.hook {
                    after {
                        if (!showSeconds) return@after
                        val clock = clazz.field {
                            name = "mScreenOffClock"
                        }.get(instance).any() as? TextClock
                        clock?.enableSeconds()
//                        // 假设存在
//                        val clock2 = clazz.field {
//                            name = "mClock"
//                        }.get(instance).any() as? TextClock
//                        clock2?.enableSeconds()
                        YLog.debug("$TAG $className.refreshAmPm: enabled seconds")
                    }
                }
            }
        }
    }

    /**
     * 调整 Horizon 样式布局
     */
    private fun adjustHorizenLayout(clock: TextClock?) {
        clock?.let {
            var currentView: View? = clock.parent as? View
            var count = 0
            val maxLoop = 5

            while (currentView != null && count < maxLoop) {
                if (currentView.javaClass.simpleName.contains("AodClockStyleHorizen", ignoreCase = true)) {
                    currentView.layoutParams?.let { params ->
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        currentView.layoutParams = params
                        currentView.requestLayout()
                    }
                    YLog.debug("$TAG adjusted AodClockStyleHorizen layout")
                    break
                }
                currentView = currentView.parent as? View
                count++
            }
        }
    }

    /**
     * 处理包含 mTimeView 的时钟控件
     */
    private fun handleTimeViewClasses() {
        TARGET_TIME_VIEW_CLASSES.forEach { className ->
            val clazz = className.toClassOrNull(appClassLoader) ?: run {
                YLog.error("$TAG class not found: $className")
                return@forEach
            }

            // 检查是否存在 mTimeView 字段
            try {
                clazz.getDeclaredField("mTimeView")
            } catch (e: NoSuchFieldException) {
                YLog.error("$TAG $className has no mTimeView field")
                return@forEach
            }

            // Hook onFinishInflate
            clazz.method {
                name = "onFinishInflate"
                emptyParam()
            }.hook {
                after {
                    if (!showSeconds) return@after
                    val clock = clazz.field {
                        name = "mTimeView"
                    }.get(instance).any() as? TextClock
                    clock?.enableSeconds()
                    YLog.debug("$TAG $className.onFinishInflate: enabled seconds for mTimeView")
                }
            }
        }
    }

    /**
     * 处理 AodTextStyle 的 mDataView
     */
    private fun handleAodTextStyle() {
        val className = "com.zte.feature.doze.AodClock.AodClockStyle.AodTextStyle"
        val clazz = className.toClassOrNull(appClassLoader) ?: run {
            YLog.error("$TAG AodTextStyle class not found")
            return
        }

        // 检查是否存在 mDataView 字段
        try {
            clazz.getDeclaredField("mDataView")
        } catch (e: NoSuchFieldException) {
            YLog.error("$TAG AodTextStyle has no mDataView field")
            return
        }

        // Hook onFinishInflate
        clazz.method {
            name = "onFinishInflate"
            emptyParam()
        }.hook {
            after {
                if (!showSeconds) return@after
                val clock = clazz.field {
                    name = "mDataView"
                }.get(instance).any() as? TextClock
                clock?.enableSeconds()
                YLog.debug("$TAG AodTextStyle.onFinishInflate: enabled seconds for mDataView")
            }
        }
    }

    /**
     * 处理 AodClockViewContainer 中的 TextClock
     * 适用于数字人、旅行者等通过容器加载时钟的样式
     */
    private fun handleAodClockViewContainer() {
        val containerClass = "com.zte.feature.doze.AodClock.AodClockStyle.AodClockViewContainer"
        val clazz = containerClass.toClassOrNull(appClassLoader) ?: run {
            YLog.error("$TAG AodClockViewContainer class not found")
            return
        }

        // Hook inflateView 方法，该方法在加载时钟布局时调用
        clazz.method {
            name = "inflateView"
            emptyParam()
        }.hook {
            after {
                if (!showSeconds) return@after
                val container = instance<ViewGroup>() // AodClockViewContainer 继承自 LinearLayout
                enableSecondsInViewGroup(container)
                YLog.debug("$TAG AodClockViewContainer.inflateView: enabled seconds for all TextClock inside")
            }
        }
    }

    /**
     * Hook DozeUi 的相关方法
     */
    private fun hookDozeUiMethods() {
        val dozeUiClass = "com.android.systemui.doze.DozeUi".toClassOrNull(appClassLoader)
        if (dozeUiClass == null) {
            YLog.error("$TAG DozeUi class not found")
            return
        }

        // Hook roundToNextMinute
        dozeUiClass.method {
            name = "roundToNextMinute"
            param(LongType)
        }.hook {
            before {
                if (!showSeconds) return@before
                val currentTime = args[0] as Long
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentTime
                calendar.add(Calendar.SECOND, 1)
                calendar.set(Calendar.MILLISECOND, 0)
                result = calendar.timeInMillis
                YLog.debug("$TAG roundToNextMinute: modified to +1 second")
            }
        }

        // Hook onTimeTick 用于调试日志
        dozeUiClass.method {
            name = "onTimeTick"
            emptyParam()
        }.hook {
            after {
                if (!showSeconds) return@after
                val shortDateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                val currentTime = shortDateFormat.format(Date())
                YLog.debug("$TAG DozeUi.onTimeTick | Time = $currentTime")
            }
        }
    }


    /**
     * DexKit 查找：包含字段 mScreenOffClock 且方法 onFinishInflate / refreshAmPm 的类
     */
    private fun findScreenOffClockClasses(bridge: DexKitBridge): List<ClassData> {
        return bridge.findClass {
            searchPackages(AOD_CLOCK_PKG_NAME)
            matcher {
                fields {
                    add {
                        name = "mScreenOffClock"
                        type = "android.widget.TextClock"
                    }
                }
                methods {
                    add { name = "onFinishInflate" }
                    add { name = "refreshAmPm" }
                }
            }
        }
    }
}