package dev.lackluster.mihelper.hook.rules.android.nubia


import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass

object Debugger : YukiBaseHooker() {
      private const val TAG = "NubiaDebugger"
    override fun onHook() {
//        "android.os.Debug".toClass().method {
//            name = "isDebuggerConnected"
//            modifiers { isStatic }
//        }.hook {
//            after {
//                result = true
//                YLog.debug(tag = TAG, msg = "isDebuggerConnected: $result")
//            }
//        }

    }
}

//object CpuFreezerHook : YukiBaseHooker() {
//    override fun onHook() {
//        // 目标类：com.android.server.am.CpuFreezerManagerServiceV2
//        "com.android.server.am.CpuFreezerManagerServiceV2".toClass().apply {
//            // Hook needFilterFreezerApp 方法
//            method {
//                name = "needFilterFreezerApp"
//                param(IntType)
//            }.hook {
//                after {
//                    val uid = args[0] as Int
//                    // 如果 uid 是 SystemUI 的，强制返回 false（表示不需要过滤）
//                    if (uid == 10336) {
//                        result = false
//                    }
//                }
//            }
//
//            // 同时 Hook 可能影响冻结的其他方法，如 setCpuFreezerForUid
//            method {
//                name = "setCpuFreezerForUid"
//                param(IntType, IntType, StringClass, StringClass)
//            }.hook {
//                before {
//                    val uid = args[0] as Int
//                    val action = args[3] as String
//                    // 如果是 SystemUI，拦截冻结操作
//                    if (uid == 10336 && action == "freeze") {
//                        result = false  // 阻止冻结
//                    }
//                }
//            }
//        }
//    }
//}

//object CpuFreezerHook : YukiBaseHooker() {
//    private var systemUiUid: Int? = null
//
//    override fun onHook() {
//        // 获取 SystemUI 的 UID（需要在 system_server 中执行）
//        val packageManager = appContext?.packageManager
//        if (packageManager != null) {
//            try {
//                val ai = packageManager.getApplicationInfo("com.android.systemui", 0)
//                systemUiUid = ai.uid
//                YLog.debug("CpuFreezerHook: SystemUI UID = $systemUiUid")
//            } catch (e: Exception) {
//                YLog.error("CpuFreezerHook: Failed to get SystemUI UID", e)
//            }
//        }
//
//        // 如果获取失败，回退到硬编码（针对已知机型）
//        val targetUid = systemUiUid ?: 10336
//
//        // 目标类：com.android.server.am.CpuFreezerManagerServiceV2
//        "com.android.server.am.CpuFreezerManagerServiceV2".toClass().apply {
//            // Hook needFilterFreezerApp 方法
//            method {
//                name = "needFilterFreezerApp"
//                param(IntType)
//            }.hook {
//                after {
//                    val uid = args[0] as Int
//                    if (uid == targetUid) {
//                        result = false // 不冻结 SystemUI
//                    }
//                }
//            }
//
//            // Hook setCpuFreezerForUid
//            method {
//                name = "setCpuFreezerForUid"
//                param(IntType, IntType, StringClass, StringClass)
//            }.hook {
//                before {
//                    val uid = args[0] as Int
//                    val action = args[3] as String
//                    if (uid == targetUid && action == "freeze") {
//                        result = false // 阻止冻结 SystemUI
//                    }
//                }
//            }
//        }
//    }
//}