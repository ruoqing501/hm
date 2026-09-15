package dev.lackluster.redmagichelper.hook.rules.gamehelpmodule

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.toClassOrNull
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.hook.compat.type.java.UnitType
import dev.lackluster.redmagichelper.utils.Prefs
import kotlin.math.roundToInt

// 一键连招 1~10 倍速 + 资格放行 (包名: cn.nubia.gamehelpmodule)
// 移植自 LS_Augment 的 installGameHelperComboSpeedHooks / GH-01~07。
// 机制: 生成时间戳按倍率缩放的私有动作副本替换播放路径, 原始录制不动。
object NubiaComboSpeed : YukiBaseHooker() {

    private const val TAG = "NubiaComboSpeed"
    private const val GAME_HELPER_PACKAGE = "cn.nubia.gamehelpmodule"
    private const val ADJUSTED_FLAG = "rmh_combo_speed_adjusted"

    private const val MACRO_SWITCH_CLASS = "cn.nubia.gamehelper.MacroSwitch"
    private const val DEVICE_UTIL_CLASS = "cn.nubia.gamehelper.utils.DeviceUtil"
    private const val PACKAGE_UTILS_CLASS = "cn.nubia.gamehelper.utils.PackageUtils"
    private const val DB_HELPER_CLASS = "cn.nubia.gamehelper.db.DbHelper"
    private const val DATABASE_CLASS = "cn.nubia.gamehelper.db.RecordMotionDatabase"
    private const val PROVIDER_CLASS = "cn.nubia.gamehelper.db.GameTouchProvider"
    private const val MOTION_MANAGER_CLASS = "cn.nubia.gamehelper.manager.GameMotionHelperManager"
    private const val RECORD_MOTION_BEAN_CLASS = "cn.nubia.gamehelper.bean.RecordMotionBean"
    private const val TRAVEL_ACTION_CLASS = "cn.nubia.gamehelper.travel.action.TravelAction"

    private val motionCache = ComboMotionCache()

    @Volatile
    private var beanHookInstalled = false

    @Volatile
    private var managerHookInstalled = false

    /** Original recover path stashed between before/after of a startPlay call. */
    private val replacedSourcePath = ThreadLocal<String?>()

    override fun onHook() {
        installEligibilityHooks()
        installEngineHooks()
        installProviderHooks()
        installPreviewHook()
    }

    private fun configured(): Boolean =
        Prefs.getBoolean(Pref.Key.GameSpace.GAME_SPACE_COMBO_SPEED_ENABLED, false)

    private fun rate(): Float =
        ComboSpeedPolicy.normalizeRate(
            Prefs.getInt(Pref.Key.GameSpace.GAME_SPACE_COMBO_SPEED_RATE, 1).toFloat()
        )

    private fun isMainThread(): Boolean =
        Looper.getMainLooper() != null && Looper.myLooper() == Looper.getMainLooper()

    // ---------------------------------------------------------------------
    // GH-02~07 资格放行 (GH-01 isBlackPackageName 已由 NubiaComboAttack 覆盖)
    // ---------------------------------------------------------------------
    private fun installEligibilityHooks() {
        // GH-02: 部分路径直接使用黑名单集合而不是调用 isBlackPackageName
        PACKAGE_UTILS_CLASS.toClassOrNull()?.apply {
            method {
                name = "getBlackPackageSet"
            }.hook {
                after {
                    if (configured()) {
                        result = emptySet<String>()
                        YLog.debug(tag = TAG, msg = "GH-02 getBlackPackageSet -> empty")
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "GH-02 $PACKAGE_UTILS_CLASS not found")

        // GH-03/04: recordmotion.db 里的第二份连招黑名单。
        // isPackageCanUseMacro 返回 true 表示在黑名单中, false 才是放行状态。
        for (className in listOf(DB_HELPER_CLASS, DATABASE_CLASS)) {
            className.toClassOrNull()?.apply {
                method {
                    name = "isPackageCanUseMacro"
                    param(StringClass)
                    returnType = BooleanType
                }.hook {
                    after {
                        if (configured()) {
                            result = false
                            YLog.debug(tag = TAG, msg = "GH-03/04 $className.isPackageCanUseMacro -> false")
                        }
                    }
                }
            } ?: YLog.error(tag = TAG, msg = "GH-03/04 $className not found")
        }

        // GH-05: 外部组件直接查询黑名单 provider 时, 返回空的黑名单游标
        PROVIDER_CLASS.toClassOrNull()?.apply {
            method {
                name = "query"
                paramCount = 5
                returnType = Cursor::class.java
            }.hook {
                after {
                    val uri = args[0] as? Uri
                    val cursor = result as? Cursor
                    if (configured() && cursor != null &&
                        uri?.lastPathSegment == "blacklist"
                    ) {
                        result = MatrixCursor(cursor.columnNames)
                        runCatching { cursor.close() }
                        YLog.debug(tag = TAG, msg = "GH-05 provider blacklist cursor emptied")
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "GH-05 $PROVIDER_CLASS not found")

        // GH-06: 宏总开关是另一个独立的执行闸门
        MOTION_MANAGER_CLASS.toClassOrNull()?.apply {
            method {
                name = "canUseMacro"
                returnType = BooleanType
            }.hook {
                after {
                    if (configured()) {
                        result = true
                        YLog.debug(tag = TAG, msg = "GH-06 canUseMacro -> true")
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "GH-06 $MOTION_MANAGER_CLASS.canUseMacro not found")

        // GH-07: 无录制时通过悬浮窗打开连招编辑器, 部分 ROM 误报悬浮窗权限被拒
        Settings::class.java.method {
            name = "canDrawOverlays"
            param(Context::class.java)
            returnType = BooleanType
        }.hook {
            after {
                val context = args[0] as? Context
                if (configured() && context?.packageName == GAME_HELPER_PACKAGE) {
                    result = true
                    YLog.debug(tag = TAG, msg = "GH-07 canDrawOverlays -> true")
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // 倍速引擎: 闸门 + RecordMotionBean.getSpeed + 动作文件副本替换
    // ---------------------------------------------------------------------
    private fun installEngineHooks() {
        // 倍速模式闸门
        MACRO_SWITCH_CLASS.toClassOrNull()?.apply {
            method {
                name = "isSpeedModeEnable"
                returnType = BooleanType
            }.hook {
                after {
                    if (configured()) result = true
                }
            }
        } ?: YLog.error(tag = TAG, msg = "$MACRO_SWITCH_CLASS.isSpeedModeEnable not found")

        // UI 8.5 设备闸门
        DEVICE_UTIL_CLASS.toClassOrNull()?.apply {
            method {
                name = "isUI85"
                returnType = BooleanType
            }.hook {
                after {
                    if (configured()) result = true
                }
            }
        } ?: YLog.error(tag = TAG, msg = "$DEVICE_UTIL_CLASS.isUI85 not found")

        // 引擎读取的整数倍率
        RECORD_MOTION_BEAN_CLASS.toClassOrNull()?.apply {
            val finder = method {
                name = "getSpeed"
                returnType = IntType
            }
            if (finder.give() != null) {
                finder.hook {
                    after {
                        if (configured()) {
                            result = rate().roundToInt().coerceAtLeast(1)
                        }
                    }
                }
                beanHookInstalled = true
            }
        } ?: YLog.error(tag = TAG, msg = "$RECORD_MOTION_BEAN_CLASS.getSpeed not found")

        installMotionFileHook()
        installManagerSpeedHook()
    }

    /**
     * 只替换本次播放发布的路径。原始录制与 touch_key_map 绑定不动,
     * GameMacroHelper 通过 OEM provider 的正常文件描述符路径拿到按倍率
     * 缩放时间戳的私有副本。
     */
    private fun installMotionFileHook() {
        MOTION_MANAGER_CLASS.toClassOrNull()?.apply {
            method {
                name = "notifyChange"
                param(StringClass)
                returnType = UnitType
            }.hook {
                before {
                    val action = args[0] as? String
                    if (action != "startPlay" || !configured()) return@before
                    val targetRate = rate()
                    val cacheDir = appContext?.cacheDir ?: return@before
                    val sourcePath = instance.current().field {
                        name = "mCurrentRecoverPath"
                        superClass()
                    }.string() ?: return@before
                    val scaled = motionCache.lookupOrSchedule(
                        cacheDir, sourcePath, targetRate, !isMainThread()
                    )
                    if (scaled.success) {
                        instance.current().field {
                            name = "mCurrentRecoverPath"
                            superClass()
                        }.set(scaled.outputPath)
                        replacedSourcePath.set(sourcePath)
                        YLog.debug(
                            tag = TAG,
                            msg = "motion file replaced rate=$targetRate cache=${if (scaled.cacheHit) "hit" else "miss"}"
                        )
                    } else {
                        YLog.debug(
                            tag = TAG,
                            msg = "motion file cache unavailable rate=$targetRate reason=${scaled.error}"
                        )
                    }
                }
                after {
                    val action = args[0] as? String
                    if (action == "startPlay") {
                        replacedSourcePath.get()?.let { sourcePath ->
                            instance.current().field {
                                name = "mCurrentRecoverPath"
                                superClass()
                            }.set(sourcePath)
                            replacedSourcePath.remove()
                        }
                    } else if (configured()) {
                        // 非 startPlay 通知: 预生成缩放副本, 失败保持 OEM 行为
                        runCatching {
                            val cacheDir = appContext?.cacheDir ?: return@runCatching
                            val path = instance.current().field {
                                name = "mCurrentRecoverPath"
                                superClass()
                            }.string()
                            motionCache.prepare(cacheDir, path, rate(), true)
                        }
                    }
                }
            }
        } ?: YLog.error(tag = TAG, msg = "$MOTION_MANAGER_CLASS.notifyChange not found")
    }

    /**
     * 把 OEM 看门狗时长换算到目标倍率。recoveryTime/recoverRate 只是元数据,
     * 真正决定手势速度的是文件内时间戳。
     */
    private fun installManagerSpeedHook() {
        MOTION_MANAGER_CLASS.toClassOrNull()?.apply {
            val finder = method {
                name = "recoveryMotion"
                param(StringClass)
                returnType = UnitType
            }
            if (finder.give() == null) return@apply
            finder.hook {
                after {
                    if (!configured() || !beanHookInstalled) return@after
                    val targetRate = rate()
                    val currentTime = instance.current().field {
                        name = "mCurrentPlayTime"
                        superClass()
                    }.long()
                    if (currentTime <= 0L) return@after
                    val engineRate = targetRate.roundToInt().coerceAtLeast(1).toFloat()
                    val adjustedTime = ComboSpeedPolicy.adjustRecoveryTime(
                        currentTime, engineRate, targetRate
                    )
                    if (adjustedTime > 0L) {
                        instance.current().field {
                            name = "mCurrentPlayTime"
                            superClass()
                        }.set(adjustedTime)
                    }
                }
            }
            managerHookInstalled = true
        } ?: YLog.error(tag = TAG, msg = "$MOTION_MANAGER_CLASS.recoveryMotion not found")
    }

    // ---------------------------------------------------------------------
    // Provider bundle 调整 (一键播放的真实路径)
    // ---------------------------------------------------------------------
    private fun installProviderHooks() {
        PROVIDER_CLASS.toClassOrNull()?.apply {
            method {
                name = "recordMotionCall"
                param(Bundle::class.java)
                returnType = Bundle::class.java
            }.hook {
                after {
                    result = adjustComboSpeedBundle(result, "provider_record")
                }
            }
            method {
                name = "call"
                param(StringClass, StringClass, Bundle::class.java)
                returnType = Bundle::class.java
            }.hook {
                after {
                    result = adjustComboSpeedBundle(result, "provider_call")
                }
            }
        } ?: YLog.error(tag = TAG, msg = "$PROVIDER_CLASS not found")
    }

    private fun adjustComboSpeedBundle(result: Any?, source: String): Any? {
        if (result !is Bundle || !configured()) return result
        if (result.getBoolean(ADJUSTED_FLAG, false) ||
            !result.getBoolean("startRecoverRecord", false) ||
            !result.containsKey("motionRecoverFd")
        ) {
            return result
        }

        return try {
            val targetRate = rate()
            val originalRate = result.getFloat("recoverRate", 1.0f)
            val recoveryTime = result.getLong("recoveryTime", 0L)
            val timingSourceRate =
                if (managerReady()) 1.0f else originalRate
            // manager hook 存在时已把看门狗时长换算到目标倍率, 否则在这里
            // 从 provider 的 OEM 倍率先还原原始时长。
            val adjustedTime = if (managerReady()) recoveryTime
            else ComboSpeedPolicy.adjustRecoveryTime(
                recoveryTime, timingSourceRate, targetRate
            )
            if (!ComboSpeedPolicy.isValidRate(targetRate) ||
                adjustedTime <= 0L || recoveryTime <= 0L ||
                timingSourceRate <= 0f ||
                timingSourceRate.isNaN() || timingSourceRate.isInfinite()
            ) {
                YLog.debug(
                    tag = TAG,
                    msg = "invalid bundle source=$source rate=$originalRate target=$targetRate time=$recoveryTime"
                )
                return result
            }

            val adjusted = Bundle(result)
            adjusted.putFloat("recoverRate", targetRate)
            adjusted.putLong("recoveryTime", adjustedTime)
            adjusted.putBoolean(ADJUSTED_FLAG, true)
            YLog.debug(
                tag = TAG,
                msg = "bundle hit source=$source rate=$originalRate->$targetRate recovery=$recoveryTime->$adjustedTime"
            )
            adjusted
        } catch (error: Throwable) {
            YLog.error(tag = TAG, msg = "adjust bundle failed source=$source", e = error)
            result
        }
    }

    private fun managerReady(): Boolean = beanHookInstalled && managerHookInstalled

    // ---------------------------------------------------------------------
    // 编辑器 "测试/预览" 路径: TravelAction 私有 speed 字段
    // ---------------------------------------------------------------------
    private fun installPreviewHook() {
        TRAVEL_ACTION_CLASS.toClassOrNull()?.apply {
            method {
                name = "playScript"
                emptyParam()
                returnType = UnitType
            }.hook {
                before {
                    if (!configured()) return@before
                    instance.current().field {
                        name = "speed"
                        superClass()
                    }.set(rate().roundToInt().coerceAtLeast(1))
                }
            }
        } ?: YLog.error(tag = TAG, msg = "$TRAVEL_ACTION_CLASS.playScript not found")
    }
}
