package dev.lackluster.redmagichelper.hook.rules.permissioncontroller

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.DexKit
import dev.lackluster.redmagichelper.utils.DexKit.dexKitBridge
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object AllowThirdpartyLauncher : YukiBaseHooker() {
    private const val TAG = "AllowThirdpartyLauncher"

    override fun onHook() {
        hasEnable(Pref.Key.Other.ALLOW_THIRDPARTY_LAUNCHER) {
            if (appClassLoader == null) return@hasEnable

            val ctsMethod = findIsCtsMethod(dexKitBridge)
            val dACFClazzName = "com.android.permissioncontroller.role.ui.DefaultAppChildFragment"
            val meName = "onRoleChanged"

            ctsMethod.hook {
                before {
                    @Suppress("DEPRECATION")
                    val stackWalkerClass = Class.forName("java.lang.StackWalker")
                    val getInstanceMethod = stackWalkerClass.getMethod("getInstance")
                    val walker = getInstanceMethod.invoke(null)

                    val walkMethod = stackWalkerClass.getMethod("walk", java.util.function.Function::class.java)
                    val frames = walkMethod.invoke(walker, java.util.function.Function<java.util.stream.Stream<Any>, Any?> { stream ->
                        stream.limit(10).filter { frame ->
                            val className = frame.javaClass.getMethod("getClassName").invoke(frame) as String
                            val methodName = frame.javaClass.getMethod("getMethodName").invoke(frame) as String
                            className == dACFClazzName && methodName == meName
                        }.findFirst().orElse(null)
                    })
                    if (frames == null) {
                        return@before
                    }
                    result = true
                    YLog.debug(tag = TAG, msg = "AllowThirdpartyLauncher: $frames")
                }
            }
        }
    }

    private fun findIsCtsMethod(bridge: DexKitBridge): Method {
        val classData = bridge.findClass {
            searchPackages("com.android.permissioncontroller.permission.utils")
            matcher {
                methods {
                    add {
                        name = "<clinit>"
                        usingStrings(
                            listOf("com.android.calendar", "com.zte.manual"), StringMatchType.Equals
                        )
                    }
                }
            }
        }.singleOrNull() ?: error("utilsClass not found")

        val methodData = classData.findMethod {
            matcher {
                paramTypes(String::class.java)
                returnType(Boolean::class.javaPrimitiveType!!)
                usingStrings(listOf("android.", ".cts."), StringMatchType.Equals)
            }
        }.singleOrNull() ?: error("utils->isCtsMethod not found")

        return methodData.getMethodInstance(appClassLoader!!)
    }
}
