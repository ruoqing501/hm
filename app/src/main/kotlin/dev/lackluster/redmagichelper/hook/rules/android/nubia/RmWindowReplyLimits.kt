package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.annotation.SuppressLint
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object RmWindowReplyLimits : YukiBaseHooker() {
    private const val TAG = "[RmWindowReplyLimits]"

    @SuppressLint("PrivateApi")
    override fun onHook() {
        YLog.debug("$TAG onHook() 开始执行")

        // 第一个功能：移除窗口回复限制
        YLog.debug("$TAG 开始Hook移除窗口回复限制功能")

        // 尝试加载WindowReplyUtils类
        YLog.debug("$TAG 尝试加载android.app.WindowReplyUtils类")
        val windowReplyUtilsClass = "android.app.WindowReplyUtils".toClassOrNull()
        if (windowReplyUtilsClass == null) {
            YLog.error("$TAG 无法找到android.app.WindowReplyUtils类，可能类名或路径不正确")
        } else {
            YLog.debug("$TAG 成功加载android.app.WindowReplyUtils类")

            // Hook isForceSupportWhiteListForWR方法
            YLog.debug("$TAG 准备Hook isForceSupportWhiteListForWR方法")
            windowReplyUtilsClass.method {
                name = "isForceSupportWhiteListForWR"
                param(StringClass)
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.Android.REMOVE_RESTRICTIONS_WINDOW, false)) return@before
                    YLog.debug("$TAG isForceSupportWhiteListForWR方法被调用，参数: ${args.firstOrNull()}")
                    result = true
                }
            }
            YLog.debug("$TAG isForceSupportWhiteListForWR方法Hook完成")
        }

//            // 一些其他的hook点记录（原注释中的代码）
//            YLog.debug("$TAG 尝试加载其他相关类")
//            val activityTaskManagerServiceClass = "com.android.server.wm.ActivityTaskManagerService".toClassOrNull()
//            if (activityTaskManagerServiceClass != null) {
//                YLog.debug("$TAG 成功加载ActivityTaskManagerService类")
//
//                // Hook isSupportWindowReply方法
//                YLog.debug("$TAG 准备Hook isSupportWindowReply方法")
//                activityTaskManagerServiceClass.method {
//                    name = "isSupportWindowReply"
//                }.hook {
//                    before {
//                        YLog.debug("$TAG isSupportWindowReply方法被调用")
//                    }
//                    after {
//                        YLog.debug("$TAG isSupportWindowReply方法返回: ${result}")
//                    }
//                    replaceToTrue()
//                }
//                YLog.debug("$TAG isSupportWindowReply方法Hook完成")
//
//                // Hook isSupportWindowReplyState方法
//                YLog.debug("$TAG 准备Hook isSupportWindowReplyState方法")
//                activityTaskManagerServiceClass.method {
//                    name = "isSupportWindowReplyState"
//                    paramCount = 2
//                }.hook {
//                    before {
//                        YLog.debug("$TAG isSupportWindowReplyState方法被调用，参数数量: ${args.size}")
//                    }
//                    after {
//                        YLog.debug("$TAG isSupportWindowReplyState方法返回: ${result}")
//                    }
//                    replaceTo(0)
//                }
//                YLog.debug("$TAG isSupportWindowReplyState方法Hook完成")
//
//                // Hook isSupportWindowReplyStateForTask方法
//                YLog.debug("$TAG 准备Hook isSupportWindowReplyStateForTask方法")
//                activityTaskManagerServiceClass.method {
//                    name = "isSupportWindowReplyStateForTask"
//                    paramCount = 2
//                }.hook {
//                    before {
//                        YLog.debug("$TAG isSupportWindowReplyStateForTask方法被调用，参数数量: ${args.size}")
//                    }
//                    after {
//                        YLog.debug("$TAG isSupportWindowReplyStateForTask方法返回: ${result}")
//                    }
//                    replaceTo(0)
//                }
//                YLog.debug("$TAG isSupportWindowReplyStateForTask方法Hook完成")
//            } else {
//                YLog.warn("$TAG 无法找到ActivityTaskManagerService类，跳过相关Hook")
//            }
//
//            YLog.debug("$TAG 移除窗口回复限制功能Hook全部完成")

        // 第二个功能：移除窗口数量限制
        YLog.debug("$TAG 开始Hook移除窗口数量限制功能")

        // Hook isReachWrMaxSizeForMulti方法
        YLog.debug("$TAG 准备Hook isReachWrMaxSizeForMulti方法")
        val activityTaskManagerServiceClass = "com.android.server.wm.ActivityTaskManagerService".toClassOrNull()
        if (activityTaskManagerServiceClass != null) {
            YLog.debug("$TAG 成功加载ActivityTaskManagerService类")
            activityTaskManagerServiceClass.method {
                name = "isReachWrMaxSizeForMulti"
            }.hook {
                before {
                    if (!Prefs.getBoolean(Pref.Key.Android.REMOVE_RESTRICTIONS_WINDOW_NUMBER, false)) return@before
                    YLog.debug("$TAG isReachWrMaxSizeForMulti方法被调用")
                    result = false
                }
            }
            YLog.debug("$TAG isReachWrMaxSizeForMulti方法Hook完成")
        } else {
            YLog.error("$TAG 无法找到ActivityTaskManagerService类，无法Hook isReachWrMaxSizeForMulti方法")
        }

        // 注意：原注释中提到调整小窗大小的方法没有对小窗数量超过三个的情况进行处理
        // 所以只能有三个小窗进入挂起（缩成一个图标），除了重写，没啥好办法
        // Lcom/android/server/wm/TaskMifavor;->resizeForWR(Lcom/android/server/wm/Task;Lcom/android/server/wm/DisplayContent;IIILandroid/content/Context;Landroid/graphics/Rect;ZZ)V
        // 这部分原代码中没有实现，我们也不实现
        YLog.debug("$TAG 注意: resizeForWR方法未实现Hook，保持原逻辑")

        YLog.debug("$TAG 移除窗口数量限制功能Hook完成")

        YLog.debug("$TAG onHook() 执行完成")
    }
}