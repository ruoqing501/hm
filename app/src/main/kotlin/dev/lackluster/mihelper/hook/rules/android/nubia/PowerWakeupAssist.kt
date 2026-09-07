package dev.lackluster.mihelper.hook.rules.android.nubia

import android.annotation.SuppressLint
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.Context
import android.os.Bundle
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.constructor
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.LongType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

/*
1. 获取 PhoneWindowManager 实例
2. 调用 getResolvedLongPressOnPowerBehavior() 获取 powerBH
3. 获取 mContext 字段
4. 从 Context 获取 SearchManager 和 RoleManager 服务
5. 如果 powerBH != 1:
   a. 通过 RoleManager.getDefaultApplication() 获取默认助理应用包名
   b. 如果获取不到，回退到关机行为 (powerBH = 1)
6. 根据 powerBH 决定执行：
   - powerBH != 1 且有默认助理：启动语音助手
   - 否则：执行正常关机

* */


/*
powerLongPress 被调用
↓
获取当前电源长按行为配置
↓
获取 Context 和相关系统服务
↓
判断是否启用语音助手功能
↓
根据配置执行相应操作：
  1. 启用语音助手 → 启动语音助手
  2. 禁用语音助手 → 显示关机菜单
↓
阻止原始方法继续执行
*
* */
@SuppressLint("PrivateApi")
object PowerWakeupAssist : YukiBaseHooker() {
    private const val TAG = "[PowerWakeupAssist]"
    override fun onHook() {
        hasEnable(Pref.Key.Android.ANDROID_LONG_POWER_KEY_WAKEUP_ASSIST) {
            YLog.debug("$TAG 开始加载Hook模块")

            val phoneWMClazz = "com.android.server.policy.PhoneWindowManager".toClassOrNull()
            if (phoneWMClazz == null) {
                YLog.error("$TAG PhoneWindowManager类未找到")
                return@hasEnable
            }

            YLog.debug("$TAG 成功加载PhoneWindowManager类")
            YLog.debug("$TAG 开始Hook powerLongPress方法")

            phoneWMClazz.method {
                name = "powerLongPress"
                param(LongType)
            }.hook {
                before {
                    YLog.debug("$TAG powerLongPress方法被调用")

                    YLog.debug("$TAG 获取到PhoneWindowManager实例: $instance")

                    YLog.debug("$TAG 尝试调用getResolvedLongPressOnPowerBehavior方法")
                    val resolvePowerLongBH = phoneWMClazz.method {
                        name = "getResolvedLongPressOnPowerBehavior"
                        emptyParam()
                    }.get(instance).call() as? Int ?: 0
                    //power behavior 4 打开数字助理，1正常关机，2，3cts测试，5威图life，应该没人在威图手机上用这个模块吧
                    YLog.info("$TAG powerKey长按事件触发，当前电源长按行为设置为: $resolvePowerLongBH")

                    // 修复：添加 .get()
                    YLog.debug("$TAG 尝试获取mContext字段")
                    val mContext = phoneWMClazz.field {
                        name = "mContext"
                        type = ContextClass
                    }.get(this.instance).any() as? Context

                    if (mContext == null) {
                        YLog.error("$TAG mContext为空或不是Context类型")
                        return@before
                    }

                    YLog.debug("$TAG 成功获取mContext: $mContext")

                    var powerBH = resolvePowerLongBH
                    var defaultAssistPkgName: String? = null

                    YLog.debug("$TAG 尝试获取SearchManager服务")
                    val searchManager = mContext.getSystemService(Context.SEARCH_SERVICE) as? SearchManager

                    if (searchManager == null) {
                        YLog.error("$TAG SearchManager为空")
                        return@before
                    }

                    YLog.debug("$TAG 成功获取SearchManager: $searchManager")

                    YLog.debug("$TAG 尝试获取RoleManager服务")
                    val roleManager = mContext.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                    if (roleManager == null) {
                        YLog.error("$TAG RoleManager为空")
                        return@before
                    }

                    YLog.debug("$TAG 成功获取RoleManager: $roleManager")

                    if (powerBH != 1) {
                        YLog.debug("$TAG 当前电源长按行为不是关机(1)，尝试获取默认助理应用")
                        try {
                            YLog.debug("$TAG 调用RoleManager.getDefaultApplication方法")
                            defaultAssistPkgName = roleManager.javaClass.method {
                                name = "getDefaultApplication"
                                param(StringClass)
                            }.get(roleManager).call( RoleManager.ROLE_ASSISTANT) as String?

                            YLog.debug("$TAG 获取到的默认助理应用包名: $defaultAssistPkgName")
                        } catch (e: Exception) {
                            YLog.error("$TAG 获取默认助理应用失败: ${e.message}")
                        }

                        if (defaultAssistPkgName == null) {
                            powerBH = 1
                            YLog.warn("$TAG 未找到默认数字助理，回退到正常关机行为")
                        } else {
                            YLog.debug("$TAG 找到默认数字助理: $defaultAssistPkgName")
                        }
                    }

                    if (powerBH != 1 && defaultAssistPkgName != null) {
                        YLog.info("$TAG 默认助理应用包名: $defaultAssistPkgName，将启动语音助手")

                        YLog.debug("$TAG 设置mPowerKeyHandled为true")
                        phoneWMClazz.field {
                            name = "mPowerKeyHandled"
                            type = BooleanType
                        }.get(instance).setTrue()

                        YLog.debug("$TAG 执行触觉反馈")
                        phoneWMClazz.method {
                            name = "performHapticFeedback"
                            param(IntType, StringClass)
                        }.get(instance).call(
                            10003,
                            "Power - Long Press - Go To Voice Assist"
                        )

                        YLog.debug("$TAG 调用SearchManager.launchAssist方法")
                        searchManager.javaClass.method {
                            name = "launchAssist"
                            param(Bundle::class.java)
                        }.get(searchManager).call(Bundle())

                        YLog.info("$TAG 语音助手处理完成")
                        result = null
                    } else {
                        YLog.info("$TAG 执行正常关机行为")

                        // 修复：添加 .get()
                        YLog.debug("$TAG 设置mPowerKeyHandled为true")
                        phoneWMClazz.field {
                            name = "mPowerKeyHandled"
                            type = BooleanType
                        }.get(instance).setTrue()

                        YLog.debug("$TAG 执行触觉反馈")
                        phoneWMClazz.method {
                            name = "performHapticFeedback"
                            param(IntType,StringClass)
                        }.get(instance).call(
                            10003,
                            "Power - Long Press - Global Actions"
                        )


                        YLog.debug("$TAG 调用showGlobalActions方法")
                        phoneWMClazz.method {
                            name = "showGlobalActions"
                            emptyParam()
                        }.get(instance).call()
                        YLog.info("$TAG 关机行为处理完成")
                        result = null
                    }

                    YLog.debug("$TAG powerLongPress方法Hook处理结束")
                }
            }

            YLog.debug("$TAG Hook模块加载完成")
        }
    }
}