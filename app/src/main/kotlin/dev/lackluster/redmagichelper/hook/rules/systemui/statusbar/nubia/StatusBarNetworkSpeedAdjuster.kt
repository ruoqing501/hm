package dev.lackluster.redmagichelper.hook.rules.systemui.statusbar.nubia

import android.net.TrafficStats
import android.os.Handler
import android.os.SystemClock
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import cn.fkj233.ui.activity.dp2px
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.LongType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.DexKit.dexKitBridge
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.min

object StatusBarNetworkSpeedAdjuster : YukiBaseHooker() {
    private const val TAG = "StatusBarNetworkSpeedAdjuster"
    private const val SPEED_REFRESH_DELAY_MS = 1000L

    // 状态变量，用于速度计算
    private var mLastTotalUp: Long = 0
    private var mLastTotalDown: Long = 0
    private var mLastUPTimeStamp: Long = 0
    private var mLastDownTimeStamp: Long = 0
    private var mLastUpSpeed: Double = 0.0
    private var mLastDownSpeed: Double = 0.0

    // 配置项
    private val dualRowEnabled by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW_NETWORK_SPEED, false)
    }
    private val dualRowSize by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_NETWORK_SPEED_DUAL_ROW_SIZE, 6)
    }
    private val dualRowWidth by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_NETWORK_SPEED_DUAL_ROW_WIDTH, 35)
    }
    private val lowSpeedHideKb by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.LOW_SPEED_HIDE_KILO_BYTES, -1)
    }
    private val digitLen by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.STATUS_BAR_NETWORK_SPEED_DUAL_ROW_DIGIT_LEN, 3)
    }
    private val hideUnitPerSec by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.SPEED_UNIT_HIDE_PER_SECOND, false)
    }
    private val refreshSpeedEnabled by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.STATUS_BAR_NETWORK_SPEED_REFRESH_SPEED, false)
    }

    // 网格重排开启时，网速显示由 StatusBarGridHook 接管，整体跳过避免重复显示
    private val gridLayoutEnabled by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBarGrid.SWITCH, false)
    }

    override fun onHook() {
        if (gridLayoutEnabled) {
            YLog.info("$TAG 状态栏网格重排已启用，跳过网速调整")
            return
        }
        //// 网速秒刷新
        //if (refreshSpeedEnabled) {
        //    networkSpeedSeconds()
        //}
        //
        //// 双排网速
        //if (dualRowEnabled) {
        //    statusBarDualRowNetworkSpeed()
        //}
        // 网速秒刷新
        hasEnable(Pref.Key.SystemUI.StatusBar.STATUS_BAR_NETWORK_SPEED_REFRESH_SPEED){
            networkSpeedSeconds()
        }
        // 双排网速
        hasEnable(Pref.Key.SystemUI.StatusBar.STATUS_BAR_DUAL_ROW_NETWORK_SPEED){
            statusBarDualRowNetworkSpeed()
        }
    }

    // ========================= 网速秒刷新 =========================
    private fun networkSpeedSeconds() {
        YLog.debug("$TAG 状态栏网速秒刷新功能已开启")

        // 获取 Handler.postDelayed 方法
        val postDelayedMethod = Handler::class.java.getMethod(
            "postDelayed",
            Runnable::class.java,
            Long::class.javaPrimitiveType
        )

        // 获取目标 Runnable 的类（通过 DexKit 查找）
        val targetRunnableClass = findNetRunnerClazz(dexKitBridge).declaringClass
            ?: return

        // hook postDelayed
        postDelayedMethod.hook {
            before {
                if (args.size < 2) return@before
                val runnable = args[0] as? Runnable ?: return@before
                val delay = args[1] as? Long ?: return@before

                // 只处理延迟为 3000ms (0xbb8) 且 Runnable 属于目标类的调用
                if (delay != 0xbb8L) return@before
                if (runnable.javaClass.name != targetRunnableClass.name) return@before

                // 修改延迟为 1000ms
                args[1] = SPEED_REFRESH_DELAY_MS
                YLog.debug("$TAG 状态栏网速秒刷新已修改延迟")
            }
        }
    }

    // 使用 DexKit 查找网速更新中使用的 Runnable 类
    private fun findNetRunnerClazz(bridge: DexKitBridge): Method {
        val methodData = bridge.findClass {
            searchPackages("com.zte.feature.speed")
            matcher {
                interfaces {
                    add("java.lang.Runnable")
                }
                methods {
                    add {
                        name = "run"
                        usingNumbers(0xbb8L)
                    }
                }
            }
        }.findMethod {
            matcher {
                name = "run"
                usingNumbers(0xbb8L)
            }
        }.singleOrNull() ?: error("NetRunnerMethod not found")
        return methodData.getMethodInstance(appClassLoader!!)
    }

    // ========================= 双排网速 =========================
    private fun statusBarDualRowNetworkSpeed() {
        YLog.debug("$TAG 开始 Hook 双排网速功能")

        // 1. Hook StatusBarNetSpeedMFV.init() —— 初始化时调整视图大小和字体
        val netSpeedMfvClass = "com.zte.feature.speed.StatusBarNetSpeedMFV".toClassOrNull()
        netSpeedMfvClass?.method {
            name = "init"
        }?.hook {
            after {
                val instance = this.instance
                // 获取字段
                val speedText = instance.current().field { name = "mSpeedText" }.cast<TextView>()
                val speedUnit = instance.current().field { name = "mSpeedUnit" }.cast<TextView>()
                val speedGroup = instance.current().field { name = "mSpeedViewGroup" }.cast<ViewGroup>()

                if (speedText == null || speedUnit == null || speedGroup == null) {
                    YLog.warn("$TAG 无法获取网速视图字段")
                    return@after
                }

                val context = speedGroup.context
                val widthPx = if (dualRowWidth > 38) {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    dp2px(context, dualRowWidth.toFloat())
                }

                // 调整布局宽度
                speedGroup.layoutParams = speedGroup.layoutParams.apply { width = widthPx }
                speedText.layoutParams = speedText.layoutParams.apply { width = widthPx }
                speedUnit.layoutParams = speedUnit.layoutParams.apply { width = widthPx }

                // 调整文字大小
                speedText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dualRowSize.toFloat())
                speedUnit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dualRowSize.toFloat())

                YLog.debug("$TAG 双排网速视图初始化调整完成")
            }
        } ?: YLog.warn("$TAG 未找到 StatusBarNetSpeedMFV 类")

        // 2. Hook StatusBarNetSpeedPolicy.updateNetSpeedDisplay(NetSpeedState) —— 修改速度值和可见性
        val policyClass = "com.zte.feature.speed.StatusBarNetSpeedPolicy".toClassOrNull()
        val stateClass = "com.zte.feature.speed.StatusBarNetSpeedPolicy\$NetSpeedState".toClassOrNull()

        if (policyClass != null && stateClass != null) {
            policyClass.method {
                name = "updateNetSpeedDisplay"
                param(stateClass)
            }.hook {
                before {
                    // 获取 NetSpeedState 对象
//                    val netState = this.args[0].any() ?: return@before
                    val netState = this.args[0] ?: return@before

                    val curTimestamp = SystemClock.elapsedRealtime()

                    // 如果距离上次更新太近，跳过本次更新
                    if (curTimestamp - mLastDownTimeStamp < SPEED_REFRESH_DELAY_MS ||
                        curTimestamp - mLastUPTimeStamp < SPEED_REFRESH_DELAY_MS
                    ) {
                        this.result = null
                        return@before
                    }

                    // 更新上下行速度
                    updateUpSpeed()
                    updateDownSpeed()

                    // 设置速度文本
                    netState.current().field { name = "speedText" }.set(formatSpeed(mLastUpSpeed))
                    netState.current().field { name = "speedUnit" }.set(formatSpeed(mLastDownSpeed))

                    // 根据低速阈值控制可见性
                    val visible = !(mLastUpSpeed / 1024 <= lowSpeedHideKb && mLastDownSpeed / 1024 <= lowSpeedHideKb)
                    netState.current().field { name = "visible" }.set(visible)
                }
            }
        } else {
            YLog.warn("$TAG 未找到 StatusBarNetSpeedPolicy 或 NetSpeedState 类")
        }

        // 3. Hook SpeedControllerImpl.updateNetSpeed() —— 绕过原方法（加速）
        val controllerImplClass = "com.zte.feature.speed.SpeedControllerImpl".toClassOrNull()
        controllerImplClass?.method {
            name = "updateNetSpeed"
        }?.hook {
            before {
                // 设置一个无意义的 mLevel 值，并阻止原方法执行
                this.instance.current().field { name = "mLevel" }.set(114514L)
                this.result = null
            }
        } ?: YLog.warn("$TAG 未找到 SpeedControllerImpl 类")
    }

    // ===================== 速度计算工具方法 =====================
    private fun updateUpSpeed() {
        val nowTotalTxBytes = TrafficStats.getTotalTxBytes()
        val nowTimeStamp = SystemClock.elapsedRealtime()
        if (nowTimeStamp - mLastDownTimeStamp < SPEED_REFRESH_DELAY_MS) {
            return
        }
        if (nowTotalTxBytes - mLastTotalUp <= 0) {
            mLastUpSpeed = 0.0
            mLastUPTimeStamp = nowTimeStamp
            mLastTotalUp = nowTotalTxBytes
            return
        }
        val upBytesPerSecond = (nowTotalTxBytes - mLastTotalUp) * 1000 / (nowTimeStamp - mLastUPTimeStamp).toDouble()
        mLastTotalUp = nowTotalTxBytes
        mLastUpSpeed = upBytesPerSecond
        mLastUPTimeStamp = nowTimeStamp
    }

    private fun updateDownSpeed() {
        val currentTotalRxBytes = TrafficStats.getTotalRxBytes()
        val nowTimeStamp = SystemClock.elapsedRealtime()
        if (nowTimeStamp - mLastDownTimeStamp < SPEED_REFRESH_DELAY_MS) {
            return
        }
        if (currentTotalRxBytes - mLastTotalDown <= 0) {
            mLastDownSpeed = 0.0
            mLastDownTimeStamp = nowTimeStamp
            mLastTotalDown = currentTotalRxBytes
            return
        }
        val downBytesPerSecond = (currentTotalRxBytes - mLastTotalDown) * 1000 / (nowTimeStamp - mLastDownTimeStamp).toDouble()
        mLastTotalDown = currentTotalRxBytes
        mLastDownSpeed = downBytesPerSecond
        mLastDownTimeStamp = nowTimeStamp
    }

    private fun formatSpeed(speed: Double): String {
        val units = listOf("B", "KiB", "MiB", "GiB", "TiB")
        var speedCp = speed
        val perSec = if (hideUnitPerSec) "" else "/s"
        var unitIndex = 0
        while (speedCp >= 1024 && unitIndex < units.size - 1) {
            speedCp /= 1024
            unitIndex++
        }
        val df = DecimalFormat()
        val integerDigits = if (speedCp == 0.0) 1 else log10(speedCp).toInt() + 1
        val decimalPlaces = min(4 - integerDigits, digitLen)
        df.maximumFractionDigits = decimalPlaces.coerceAtLeast(0)
        df.isGroupingUsed = false
        return if (unitIndex == units.size - 1 && speedCp > 1000) {
            "%.0f".format(speedCp) + units[unitIndex] + perSec
        } else {
            df.format(speedCp) + units[unitIndex] + perSec
        }
    }
}