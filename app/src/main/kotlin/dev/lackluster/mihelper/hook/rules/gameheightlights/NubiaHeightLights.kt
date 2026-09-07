package dev.lackluster.mihelper.hook.rules.gameheightlights



import android.app.Dialog
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.android.ViewClass
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.hook.compat.type.java.StringClass
import dev.lackluster.mihelper.hook.compat.type.java.UnitType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import java.util.concurrent.atomic.AtomicBoolean

// 红魔时刻 包名：cn.nubia.gamehighlights
// 让其允许在录制时随心录制？，允许使用破坏神模式和节能模式
object NubiaHeightLights : YukiBaseHooker() {

    private const val TAG = "NubiaHeightLights"

    override fun onHook() {
        hookPClass()
    }

    /**
    *p.L() 对应破坏神模式（Chicken Mode）
    * */
    private fun hookPClass() {
        val pClass = "p.p".toClassOrNull() ?: return

        // Hook 破坏神模式 L()
        pClass.method {
            name = "L"
            emptyParam()
            returnType = BooleanType
        }.hook {
            after {
                val stackTrace = Thread.currentThread().stackTrace
                val isFromRecording = stackTrace.any {
                    it.className.contains("MainActivity") ||
                            it.className.contains("ScreenRecordService") ||
                            it.className.contains("I1")  // 可选，更精确||
                            it.className.contains("TopService") // 新增
                }
                if (isFromRecording) {
                    val original = result as? Boolean ?: false
                    if (original) {
                        result = false
                        YLog.debug(tag = TAG, msg =  "p.L() 被录制相关调用，原始值=true，改为false")
                    }
                }
            }
        }

        /**
         *p.M() 对应节能模式（Endurance Mode）
         * */
        // Hook 节能模式 M()
        pClass.method {
            name = "M"
            emptyParam()
            returnType = BooleanType
        }.hook {
            after {
                val stackTrace = Thread.currentThread().stackTrace
                val isFromRecording = stackTrace.any {
                    it.className.contains("MainActivity") ||
                            it.className.contains("ScreenRecordService")
                }
                if (isFromRecording) {
                    val original = result as? Boolean ?: false
                    if (original) {
                        result = false
                        YLog.debug(tag = TAG, msg =  "p.M() 被录制相关调用，原始值=true，改为false")
                    }
                }
            }
        }

        // 可选：如果还需要绕过其他条件（如 p.R()、p.O()），可以类似处理
        // 但通常 L() 和 M() 已足够
        // 拦截 MainActivity$g$a 的 c(int) 方法，阻止类型 10 的执行
        "cn.nubia.gamehighlights.Activity.MainActivity\$g\$a".toClass().method {
            name = "c"
            param(IntType)
            returnType = UnitType
        }.hook {
            before {
                val type = args[0] as Int
                if (type == 10) {
                    YLog.debug(tag = TAG, msg = "Blocked TopService onAbnormalHandling type 10")
                    result = null // 阻止原方法执行
                }
            }
        }
    }
}