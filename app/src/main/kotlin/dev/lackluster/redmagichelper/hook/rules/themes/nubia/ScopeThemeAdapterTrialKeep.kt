package dev.lackluster.redmagichelper.hook.rules.themes.nubia

import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.UnitType
import dev.lackluster.redmagichelper.utils.factory.hasEnable

/**
 * 阻断 com.zte.beautifyadapter 的本地试用到期/重置路径。
 *
 * 该适配 APK 独立于 com.zte.beautify 更新且方法名被混淆，因此与
 * [ScopeThemeTrialKeep] 分开覆盖。当前固件在 ThemeApplyService 里做试用恢复，
 * 旧固件走两个 JobService，两代都保留。
 */
object ScopeThemeAdapterTrialKeep : YukiBaseHooker() {
    private const val TAG = "ScopeThemeAdapterTrialKeep"

    private const val APPLY_SERVICE = "com.zte.beautifyadapter.ThemeApplyService"
    private const val TRY_JOB = "com.zte.beautifyadapter.tryuse.TryResourceJobService"
    private const val RESET_JOB = "com.zte.beautifyadapter.tryuse.ResetResourceJobService"

    override fun onHook() {
        hasEnable(Pref.Key.NubiaTheme.UNLIMITED_TRIAL) {
            runCatching { hookApplyService() }
                .onFailure { YLog.error("$TAG: hook ThemeApplyService failed", it) }
            runCatching { hookJob(TRY_JOB) }
                .onFailure { YLog.error("$TAG: hook TryResourceJobService failed", it) }
            runCatching { hookJob(RESET_JOB) }
                .onFailure { YLog.error("$TAG: hook ResetResourceJobService failed", it) }
        }
    }

    private fun hookApplyService() {
        val service = APPLY_SERVICE.toClassOrNull() ?: run {
            YLog.warn("$TAG: ThemeApplyService not found")
            return
        }

        // 开机后/试用到期时使用的合并重置路径
        service.method {
            name = "X"
            param(IntType)
            returnType = IntType
        }.hook {
            before {
                YLog.debug("$TAG: adapter_reset_to_pre_or_default blocked")
                result = 0
            }
        }
        service.method {
            name = "a0"
            param(IntType)
            returnType = UnitType
        }.hook {
            before {
                YLog.debug("$TAG: adapter_reset_to_pre_or_default_async blocked")
                result = null
            }
        }

        // 当前适配层直接调用的资源重置路径（Y/Z/U 存在无参重载）
        hookResetInt(service, "Y", "adapter_reset_theme")
        hookResetInt(service, "Z", "adapter_reset_wallpaper")
        hookResetInt(service, "U", "adapter_reset_font")

        // 适配层会在重置前后清掉 trial_key，开启后保留该标记位
        service.method {
            name = "b0"
            param(IntType)
            returnType = UnitType
        }.hook {
            before {
                val resetType = (args[0] as? Number)?.toInt() ?: -1
                if (resetType == 1 || resetType == 4 || resetType == 6 || resetType == 7 || resetType == 0x111) {
                    YLog.debug("$TAG: adapter_trial_flag_reset blocked, type=$resetType")
                    result = null
                }
            }
        }

        // 试用状态查询强制为"仍在试用中"
        hookTrialStatus(service, "G", "adapter_theme_trial_status")
        hookTrialStatus(service, "D", "adapter_wallpaper_trial_status")
    }

    private fun hookResetInt(service: Class<*>, name: String, id: String) {
        service.method {
            this.name = name
            param(IntType)
            returnType = IntType
        }.remedys {
            method {
                this.name = name
                emptyParam()
                returnType = IntType
            }
        }.hook {
            before {
                YLog.debug("$TAG: $id blocked")
                result = 0
            }
        }
    }

    private fun hookTrialStatus(service: Class<*>, name: String, id: String) {
        service.method {
            this.name = name
            emptyParam()
            returnType = BooleanType
        }.hook {
            before {
                YLog.debug("$TAG: $id forced true")
                result = true
            }
        }
    }

    private fun hookJob(className: String) {
        className.toClassOrNull()
            ?.method {
                name = "onStartJob"
                paramCount = 1
            }?.hook {
                before {
                    YLog.debug("$TAG: $className.onStartJob blocked")
                    result = false
                }
            } ?: YLog.warn("$TAG: $className.onStartJob not found")
    }
}
