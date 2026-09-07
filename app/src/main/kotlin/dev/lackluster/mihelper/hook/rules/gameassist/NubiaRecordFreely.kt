package dev.lackluster.mihelper.hook.rules.gameassist

import android.content.Context
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.current
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.android.ContextClass
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType

// 包名：com.zte.gameassist
object NubiaRecordFreely : YukiBaseHooker() {

    private const val TAG = "NubiaRecordFreely"
    // 将目标类名定义为常量（可选）
    private const val TARGET_CLASS = "cn.nubia.gameassist.dessert.tiles.ManualRecordTile"

    override fun onHook() {
        // 加载目标类
        val targetClass = TARGET_CLASS.toClass()
        // 分别调用两个独立的 Hook 方法
        //hookStartManualRecord(targetClass)
        //hookCloseManualRecord(targetClass)
        //hookHandleClickManualRecord(targetClass)

        // 这两个hook方法的作用，允许在节能模式或者破坏神模式下，开启或者关闭UI界面的随心录制的磁贴
        hookPerformanceMode()
        hookChickenMode()
    }

    /**
     * Hook 开始随心录制的方法
     * @param targetClass 目标类
     */
    private fun hookStartManualRecord(targetClass: Class<*>) {
        targetClass.method {
            name = "startManualRecord"
        }.hook {
            after {
                handleRecordAction(this.instance, "随心录制已开启")
            }
        }
    }

    /**
     * Hook 关闭随心录制的方法
     * @param targetClass 目标类
     */
    private fun hookCloseManualRecord(targetClass: Class<*>) {
        targetClass.method {
            name = "closeManualRecord"
        }.hook {
            after {
                handleRecordAction(this.instance, "随心录制已关闭")
            }
        }
    }
    private fun hookHandleClickManualRecord(targetClass: Class<*>){
        targetClass.method {
            name = "handleClick"
            superClass()
            returnType = BooleanType
        }.hook {
            before {
                // 使用反射得到输出  if (this.mPerformanceViewController.getPerformanceMode() == 1 || Utils.isChickenMode(this.mContext)) {
                // 得到它的判断的值，并通过日志打印输出

                // 1.获取 mPerformanceViewController
                val mPerformanceViewController = targetClass.field {
                    name = "mPerformanceViewController"
                    superClass()
                }.get(instance)
                // 2.获取 mPerformanceViewController 的 getPerformanceMode() 方法
                val getPerformanceMode = mPerformanceViewController.current()?.method {
                    name = "getPerformanceMode"
                    emptyParam()
                    superClass()
                }?.call()
                // 3.获取 this.mContext
                val mContext = targetClass.field {
                    name = "mContext"
                    superClass()
                //在获取字段后立即调用 .any() 获取真实对象
                }.get(instance).any() as? Context
                // 4.获取 Utils，并调用 isChickenMode() 方法
                val isChickenMode = "cn.nubia.gameassist.utils.Utils".toClassOrNull()?.method {
                    name = "isChickenMode"
                    param(ContextClass)
                    returnType = BooleanType
                    modifiers { isStatic }
                }?.get()?.call(mContext) as? Boolean ?: false
                YLog.debug(
                    tag = TAG,
                    msg = "handleClick 判断：getPerformanceMode=$getPerformanceMode, isChickenMode=$isChickenMode, 将返回 ${(getPerformanceMode as? Int == 1) || isChickenMode}"
                )
                // 如果当前性能模式为节能模式或者开启了破坏神模式，则进入该方法内部执行
                if((getPerformanceMode as? Int == 1) || isChickenMode){
                    YLog.debug(tag = TAG, msg = "进入 handleClick 逻辑")
                }
            }
        }

    }




    /**
     * 作用：用于在ManualRecordTile类中，随心录制标签中的判断，该判断会影响随心录制功能的使用
     * Hook 初始化时，会默认读取一次，性能模式的值
     * 1 表示 节能模式
     * 2 表示 均衡模式
     * 3 表示 觉醒模式
     * 5 表示 破坏神模式
     *
     */
    private fun hookPerformanceMode() {
        "cn.nubia.gameassist.performance.PerformanceViewController".toClass().method {
            name = "getPerformanceMode"
            emptyParam()
        }.hook {
            after {
                // 判断调用者是否来自 ManualRecordTile
                val stackTrace = Thread.currentThread().stackTrace
                val isFromManualRecordTile = stackTrace.any { it.className.contains("ManualRecordTile") }
                if (isFromManualRecordTile) {
                    val original = result as? Int
                    if (original == 1 || original == 5) {
                        result = 2  // 让磁贴以为不是节能模式(1)或者破坏神模式(5) 改为2 表示 均衡模式(2)
                        YLog.debug(tag = TAG, msg = "让磁贴以为不是节能模式(1)或者破坏神模式(5) 改为2 表示 均衡模式(2)")
                    }
                }
            }
        }
    }


    /**
     * 作用：用于在ManualRecordTile类中，随心录制标签中的判断，该判断会影响随心录制功能的使用
     * 判断当前是否开启了破坏神模式
     */

    private fun hookChickenMode() {
        "cn.nubia.gameassist.utils.Utils".toClass().method {
            name = "isChickenMode"
            param(ContextClass)
            returnType = BooleanType
            modifiers { isStatic }
        }.hook {
            after {
                val stackTrace = Thread.currentThread().stackTrace
                val isFromManualRecordTile = stackTrace.any { it.className.contains("ManualRecordTile") }
                if (isFromManualRecordTile) {
                    val original = result as? Boolean ?: false
                    if (original) {
                        result = false
                        YLog.debug(tag = TAG, msg ="isChickenMode 被 ManualRecordTile 调用，原始值=true，改为false")
                    }
                }
            }
        }
    }
    /**
     * 处理录制动作的公共逻辑：获取当前游戏包名并打印日志
     * @param instance 被 hook 的实例对象
     * @param action 动作描述（开启/关闭）
     */
    private fun handleRecordAction(instance: Any, action: String) {
        val pkg = instance.current().field {
            name = "mCurPackage"
            superClass()   // 允许从父类查找该字段
        }.string() ?: "unknown"
        YLog.debug(tag = TAG, msg = "$action，游戏包名：$pkg")
    }
}