package dev.lackluster.redmagichelper.hook.rules.themes.nubia

import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.param.HookParam
import dev.lackluster.redmagichelper.utils.factory.hasEnable

/**
 * 阻断 com.zte.beautify 本地主题试用的到期重置，让已确认的试用资源保持生效。
 * 不改变付费权益本身。
 *
 * 与 [ScopeThemeUpdate] 的区别：后者处理"下载/试用前的免登录"，本 Hook 处理
 * "试用到期后的本地重置与弹窗"。
 */
object ScopeThemeTrialKeep : YukiBaseHooker() {
    private const val TAG = "ScopeThemeTrialKeep"

    override fun onHook() {
        hasEnable(Pref.Key.NubiaTheme.UNLIMITED_TRIAL) {
            runCatching { hookSchedule() }
                .onFailure { YLog.error("$TAG: hook TryUseJobHelper.schedule failed", it) }
            runCatching { hookRealResetJobService() }
                .onFailure { YLog.error("$TAG: hook RealResetResourceJobService failed", it) }
            runCatching { hookStartTryResourcePage() }
                .onFailure { YLog.error("$TAG: hook StartTryResourcePage failed", it) }
            runCatching { hookMainResetProcesses() }
                .onFailure { YLog.error("$TAG: hook ResetResourceJobService failed", it) }
            runCatching { hookExpiredDialogs() }
                .onFailure { YLog.error("$TAG: hook expired dialogs failed", it) }
        }
    }

    /** 外屏路径：schedule(context, type == 0) 安排的到期重置任务直接丢弃。 */
    private fun hookSchedule() {
        "com.zte.beautify.view.common.preview.tryuse.TryUseJobHelper".toClassOrNull()
            ?.method {
                name = "schedule"
                paramCount = 2
            }?.hook {
                before {
                    if ((args[1] as? Int) == 0) {
                        YLog.debug("$TAG: trial reset schedule blocked")
                        result = defaultValue()
                    }
                }
            } ?: YLog.warn("$TAG: TryUseJobHelper.schedule not found")
    }

    /** 到期 JobService 触发时直接报告失败，不执行重置。 */
    private fun hookRealResetJobService() {
        "com.zte.beautify.view.common.preview.tryuse.RealResetResourceJobService".toClassOrNull()
            ?.method {
                name = "onStartJob"
                paramCount = 1
            }?.hook {
                before {
                    YLog.debug("$TAG: trial expiry job blocked")
                    result = false
                }
            } ?: YLog.warn("$TAG: RealResetResourceJobService.onStartJob not found")
    }

    /**
     * 主题、字体、动态壁纸走 StartTryResourcePage 而不是外屏的 schedule(int=0) 路径。
     * 类型过滤保持刻意收窄，避免影响无关的厂商任务。
     */
    private fun hookStartTryResourcePage() {
        "com.zte.beautify.view.common.preview.tryuse.TryUseJobHelper".toClassOrNull()
            ?.method {
                name = "StartTryResourcePage"
                paramCount = 1
            }?.hook {
                before {
                    val type = (args[0] as? Number)?.toInt() ?: -1
                    if (type == 1 || type == 4 || type == 6) {
                        YLog.debug("$TAG: main expiry entry blocked, type=$type")
                        result = defaultValue()
                    }
                }
            } ?: YLog.warn("$TAG: TryUseJobHelper.StartTryResourcePage not found")
    }

    /** 阻断主题/字体/动态壁纸试用各自的本地重置方法。 */
    private fun hookMainResetProcesses() {
        "com.zte.beautify.view.common.preview.tryuse.ResetResourceJobService".toClassOrNull()
            ?.method {
                name("processTheme", "processFont", "processLiveWallpaper")
                paramCount = 0
            }?.hook {
                before {
                    YLog.debug("$TAG: trial reset blocked: ${member.name}")
                    result = defaultValue()
                }
            } ?: YLog.warn("$TAG: ResetResourceJobService process* not found")
    }

    /** 只屏蔽那两个已知的本地试用到期弹窗。 */
    private fun hookExpiredDialogs() {
        val classes = listOf(
            "com.zte.beautify.view.common.preview.BeautyPreviewActivity",
            "com.zte.beautify.view.common.preview.online.OnlineThemePreviewFragment"
        )
        for (className in classes) {
            className.toClassOrNull()
                ?.method {
                    name = "popTryUseExpiredDialog"
                    paramCount = 0
                }?.hook {
                    before {
                        YLog.debug("$TAG: trial expired dialog blocked ($className)")
                        result = defaultValue()
                    }
                } ?: YLog.warn("$TAG: popTryUseExpiredDialog not found in $className")
        }
    }

    private fun HookParam.defaultValue(): Any? = when (method.returnType) {
        java.lang.Boolean.TYPE -> false
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        java.lang.Character.TYPE -> Char(0)
        Void.TYPE -> null
        else -> if (method.returnType.isPrimitive) 0 else null
    }
}
