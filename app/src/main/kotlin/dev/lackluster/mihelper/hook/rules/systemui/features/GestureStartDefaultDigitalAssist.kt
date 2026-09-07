package dev.lackluster.mihelper.hook.rules.systemui.features

import android.content.ComponentName
import android.os.Bundle
import android.widget.TextClock
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.hook.rules.systemui.screenoff.nubia.AodSecondUpdate
import dev.lackluster.mihelper.hook.rules.updatesystem.nubia.MockDeviceInfo
import dev.lackluster.mihelper.hook.rules.updatesystem.nubia.MockDeviceInfo.findBuildFP
import dev.lackluster.mihelper.hook.rules.updatesystem.nubia.MockDeviceInfo.findDeviceModel
import dev.lackluster.mihelper.utils.DexKit
import dev.lackluster.mihelper.utils.DexKit.dexKitBridge
import dev.lackluster.mihelper.utils.factory.hasEnable
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData
import java.lang.reflect.Method

object GestureStartDefaultDigitalAssist : YukiBaseHooker() {
    private const val TAG = "GestureStartDefaultDigitalAssist"

    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.GESTURE_USE_DEFAULT_DIGITAL_ASSIST) {
            YLog.debug("$TAG start")
            handleGetAssistInfoForUserMethod()
            handleHandleStartAssist()
        }
    }
    /*
    * 获取类的方法
    * */

    fun findGetAssistInfoForUser(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            matcher {
                className = "com.zte.adapt.mifavor.navbar.AssistManagerAdapt"
            }
        }.findMethod {
            matcher {
                name = "getAssistInfoForUser"
                paramTypes = listOf("android.content.ComponentName")
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }

    fun handleGetAssistInfoForUserMethod() {
        findGetAssistInfoForUser(dexKitBridge)?.let { method ->
            method.hook {
                before {
                    result = args[0]
                }
            }
        }?: YLog.error("$TAG 未找到getAssistInfoForUser方法")
    }

    fun findHandleStartAssist(dexKitBridge: DexKitBridge): Method? {
        val m = dexKitBridge.findClass {
            matcher {
                className = "com.zte.adapt.mifavor.navbar.AssistManagerAdapt"
            }
        }.findMethod {
            matcher {
                name = "handleStartAssist"
                paramTypes = listOf("android.os.Bundle", "android.content.Context")
            }
        }.singleOrNull()
        return m?.getMethodInstance(appClassLoader!!)
    }

    fun handleHandleStartAssist() {
        findHandleStartAssist(dexKitBridge)?.let { method ->
            method.hook {
                before {
                    result = false
                }
            }
        }?: YLog.error("$TAG 未找到handleStartAssist方法")
    }
}