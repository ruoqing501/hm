package dev.lackluster.mihelper.hook.rules.gamespace

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ViewClass
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.UnitType
import androidx.core.content.edit
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.highcapable.yukihookapi.hook.type.java.CharSequenceClass
import com.highcapable.yukihookapi.hook.type.java.StringClass

// 包名为 cn.nubia.gamelauncher
object NubiaGameSpace : YukiBaseHooker() {

    private const val TAG = "NubiaGameSpace"

    override fun onHook() {
        //// 1. 允许在节能模式下点击红魔时刻开关
        hookAllowRedMagicTimeInPowerSavingMode()
        // 2. 解决破坏神模式（极高性能模式）下红魔时刻开关被禁用的问题
        hookUpdateUIDiabloMode()
        // 3. 阻止 disable/enable 方法强制修改开关状态（保持原钩子）
        hookDisableAndEnableRedMagicTime()



        // 4. 修正 initView 中可能错误设置的开关状态（保持原钩子）
        hookInitViewCorrection()
        hookSwitchState()

    }









    /**
    *游戏空间-红魔时刻开关状态Hook
    * */
    private fun hookSwitchState() {
        val viewClass = "cn.nubia.gamelauncher.gamecontrolpanel.GameFunctionAllocationView".toClass()
        viewClass.method {
            name = "setChoiceGameSettings"
            param(BooleanType)
        }.hook {
            after {
                val newState = args[0] as Boolean
                // 游戏空间打开的应用的对应的应用包名
                val packageName = instance.current().field { name = "mCurrentPackageName" }.string() ?: "unknown"
                // 获取 Context（可通过实例的 mContext 字段）
                val context = instance.current().field { name = "mContext" }.any() as? Context
                if (context != null) {
                    val prefs = context.getSharedPreferences("redmagic_time_prefs", Context.MODE_PRIVATE)
                    prefs.edit { putBoolean("diablo_${packageName}", newState) }
                    YLog.debug(tag = TAG, msg =  "持久化保存破坏神模式状态: $newState  for $packageName")
                }

                val stateStr = if (newState) "开启" else "关闭"
                YLog.debug(tag = TAG, msg = "红魔时刻 $stateStr，当前游戏包名：$packageName")
                if (getPerformanceMode(this.instance) == 4) {
                    YLog.debug(tag = TAG, msg = "当前性能模式为4，即“极高性能模式(破坏神模式)”，请自行检查是否正确设置")
                }
            }
        }
    }


    private fun hookAllowRedMagicTimeInPowerSavingMode() {
        val listenerClass = "cn.nubia.gamelauncher.gamecontrolpanel.GameFunctionAllocationView\$10".toClassOrNull()
        if (listenerClass == null) {
            YLog.error(tag = TAG, msg = "Failed to find GameFunctionAllocationView\$10")
            return
        }

        listenerClass.method {
            name = "onClick"
            param(ViewClass)
            returnType = UnitType
        }.hook {
            before {
                val outer = instance.current().field { name = "this\$0" }.any()
                if (outer == null) {
                    YLog.error(tag = TAG, msg = "Failed to get outer instance")
                    return@before
                }

                val view = args[0] as? View ?: return@before

                try {
                    val onClickMethod = outer.javaClass.getMethod("onClick", View::class.java)
                    onClickMethod.invoke(outer, view)
                    result = null // 阻止原监听器执行
                } catch (e: Throwable) {
                    YLog.error(tag = TAG, msg = "Failed to invoke outer onClick: ${e}")
                }
            }
        }
        YLog.debug(tag = TAG, msg = "GameFunctionAllocationView\$10 onClick hooked")
    }

    private fun hookDisableAndEnableRedMagicTime() {
        val viewClass = "cn.nubia.gamelauncher.gamecontrolpanel.GameFunctionAllocationView".toClass()

        viewClass.method {
            name = "disableRedMagicTime"
            returnType = UnitType
        }.hook {
            before {
                YLog.debug(tag = TAG, msg = "disableRedMagicTime blocked")
                result = null
            }
        }

        viewClass.method {
            name = "enableRedMagicTime"
            returnType = UnitType
        }.hook {
            before {
                YLog.debug(tag = TAG, msg = "enableRedMagicTime blocked")
                result = null
            }
        }
        YLog.debug(tag = TAG, msg = "disable/enable hooks applied")
    }

    /**
     * 获取当前性能模式设置值。
     * @return 性能模式整数值，如果上下文获取失败则返回null。
     */
    private fun getPerformanceMode(instance: Any): Int? {
        val context = instance.current().field { name = "mContext" }.any() as? Context ?: return null
        val perfMode = Settings.Global.getInt(context.contentResolver, "performance_mode_value", 2)
        YLog.debug(tag = TAG, msg = "updateUI: perfMode=$perfMode")
        return perfMode
    }
    private fun hookInitViewCorrection() {
        val viewClass = "cn.nubia.gamelauncher.gamecontrolpanel.GameFunctionAllocationView".toClass()

        viewClass.method {
            name = "initView"
            emptyParam()
            returnType = UnitType
        }.hook {
            after {
                val instance = this.instance
                val currentOpen = instance.current().field { name = "isRedmagicTimeCheckboxOpen" }.boolean()
                var persistedOpen = instance.current().method { name = "isGameSwitchOn"; emptyParam(); returnType = BooleanType }.call() as Boolean
                YLog.debug(tag = TAG, msg = "initView: currentOpen=$currentOpen, persistedOpen=$persistedOpen")
                val context = instance.current().field { name = "mContext" }.any() as? Context ?: return@after
                val perfMode = Settings.Global.getInt(context.contentResolver, "performance_mode_value", 2)
                YLog.debug(tag = TAG, msg = "updateUI: perfMode=$perfMode")
                if (perfMode == 1) { //节能模式
                    //// 1. 允许在节能模式下点击红魔时刻开关
                    //hookAllowRedMagicTimeInPowerSavingMode()
                    //// 3. 阻止 disable/enable 方法强制修改开关状态（保持原钩子）
                    //hookDisableAndEnableRedMagicTime()
                 }
                if(perfMode==4){
                    // 2. 解决破坏神模式（极高性能模式）下红魔时刻开关被禁用的问题
                    //hookUpdateUIDiabloMode()
                    // 得到破坏神模式持久化保存的开关状态
                    val prefs = context.getSharedPreferences("redmagic_time_prefs", Context.MODE_PRIVATE)
                    val packageName = instance.current().field { name = "mCurrentPackageName" }.string() ?: "unknown"
                    val savedState = prefs.getBoolean("diablo_${packageName}", false) // 默认 false
                    //val savedState = prefs.getBoolean("diablo_com.tencent.tmgp.sgame", false) // 默认 false
                    // 真正开关的持久化保存的状态的地方 persistedOpen 为真正的持久化红魔时刻的开关的状态
                    if(savedState){
                        persistedOpen =  true
                    }else{
                        persistedOpen = false
                    }
                }
                if (!currentOpen && persistedOpen) {
                    instance.current().field { name = "isRedmagicTimeCheckboxOpen" }.set(true)

                    val checkbox = instance.current().field { name = "redmagicTimeCheckbox" }.any() as ImageView
                    /**
                     * if (this.isInternalVersion) {
                     *     // ...
                     * } else if (this.mFunctionAllocationHelper.supportGameHighLight(this.mCurrentPackageName)) {
                     *     // ...
                     *     boolean isGameSwitchOn = isGameSwitchOn();
                     *     this.isRedmagicTimeCheckboxOpen = isGameSwitchOn;
                     *     setChecked(this.redmagicTimeCheckbox, isGameSwitchOn);
                     * } else {
                     *     // ...
                     *     this.isRedmagicTimeCheckboxOpen = true;
                     * }
                    * */
                    instance.current().method {
                        name = "setChecked"
                        param(ImageView::class.java, Boolean::class.java)
                    }.call(checkbox, true)

                    instance.current().method {
                        name = "isShowMoreVideo"
                        param(Boolean::class.java)
                    }.call(true)

                    YLog.debug(tag = TAG, msg = "Corrected RedMagicTime switch state on init")
                }
            }
        }
        YLog.debug(tag = TAG, msg = "initView correction hook applied")
    }

    /**
     * 解决破坏神模式（极高性能模式）下红魔时刻开关被禁用的问题
     * 当 updateUI(z) 中的 z 为 true 时，判断当前性能模式是否为 5（破坏神模式），
     * 若是，则将参数强制改为 false，避免执行禁用代码。
     */
    private fun hookUpdateUIDiabloMode() {
        val viewClass = "cn.nubia.gamelauncher.gamecontrolpanel.GameFunctionAllocationView".toClass()

        viewClass.method {
            name = "updateUI"
            param(BooleanType)
        }.hook {
            before {
                val z = args[0] as Boolean
                if (z) {
                    val context = instance.current().field { name = "mContext" }.any() as? Context ?: return@before
                    val perfMode = Settings.Global.getInt(context.contentResolver, "performance_mode_value", 2)
                    YLog.debug(tag = TAG, msg = "updateUI: perfMode=$perfMode")
                    //if (perfMode == 5) {  // 破坏神模式
                    if (perfMode == 4) {  // 破坏神模式
                        YLog.debug(tag = TAG, msg = "updateUI 当前模式为破坏神模式")
                        args[0] = false   // 修改参数，阻止禁用,能够被点击
                    }
                }
            }
        }
        YLog.debug(tag = TAG, msg = "updateUI diablo mode hook applied")
    }
}
