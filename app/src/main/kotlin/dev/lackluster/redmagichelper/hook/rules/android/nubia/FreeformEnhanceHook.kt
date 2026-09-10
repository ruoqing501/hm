package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.hook.compat.type.java.UnitType
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import dev.lackluster.redmagichelper.utils.nubia.Deoptimizer
import java.lang.reflect.Field
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * 小窗增强（移植自 LS_Augment 的 FreeformHook，仅保留 RedMagicHelper 尚未覆盖的部分）。
 *
 * 数量上限解除（FREEFORM_UNLIMITED_COUNT）：
 * isReachWrMaxSizeForMulti 已由 RmWindowReplyLimits 覆盖，这里补齐其余闸门——
 * windowReplySizeForMulti、getFreeformRootTasksVisibleListWrForMulti、
 * alertMessageForReachMultiWrMaxSizeWr，以及 Supervisor 返回 102（厂商专用上限结果码）时
 * 回退到标准 startActivityFromRecents。
 *
 * 全应用资格（FREEFORM_ALL_APPS）：
 * TaskDisplayAreaMifavor / ActivityClientController / ActivityRecord / Task / TaskFragment
 * 上的原厂资格闸门，带系统关键界面白名单与例外清单（FREEFORM_EXCLUDED_APPS）保护，
 * 所有反射与判定均为 fail-closed（任何异常都走原厂逻辑）。
 */
@SuppressLint("PrivateApi")
object FreeformEnhanceHook : YukiBaseHooker() {
    private const val TAG = "[FreeformEnhance]"

    private const val ATM_SERVICE = "com.android.server.wm.ActivityTaskManagerService"
    private const val ACTIVITY_TASK_SUPERVISOR = "com.android.server.wm.ActivityTaskSupervisor"
    private const val ACTIVITY_CLIENT_CONTROLLER = "com.android.server.wm.ActivityClientController"
    private const val ACTIVITY_STARTER = "com.android.server.wm.ActivityStarter"
    private const val ACTIVITY_RECORD = "com.android.server.wm.ActivityRecord"
    private const val TASK = "com.android.server.wm.Task"
    private const val TASK_FRAGMENT = "com.android.server.wm.TaskFragment"
    private const val TASK_DISPLAY_AREA = "com.android.server.wm.TaskDisplayArea"
    private const val TDA_MIFAVOR = "com.android.server.wm.TaskDisplayAreaMifavor"
    private const val TASK_NOT_SUPPORT_WINDOW_REPLY_STATE =
        "com.android.server.wm.TaskNotSupportWindowReplyStateWr"
    private const val SAFE_ACTIVITY_OPTIONS = "com.android.server.wm.SafeActivityOptions"

    /** 系统关键界面/模块管理器白名单，绝不强制小窗资格。 */
    private val PROTECTED_PACKAGES = setOf(
        "android", "com.android.systemui", "com.android.keyguard",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller",
        "com.android.packageinstaller", "com.google.android.packageinstaller",
        "com.android.shell", "com.android.server.telecom", "com.android.incallui",
        "com.android.documentsui", "com.android.providers.settings",
        "com.android.providers.downloads", "com.zte.mifavor.launcher",
        "com.android.launcher3", "com.google.android.apps.nexuslauncher",
        "dev.lackluster.redmagichelper", "org.lsposed.manager",
        "me.weishu.kernelsu", "me.weishu.kernelsu.debug", "com.rifsxd.ksunext",
        "com.topjohnwu.magisk", "me.bmax.apatch"
    )

    private const val ELIGIBILITY_CACHE_MS = 30_000L
    private val eligibilityCache = ConcurrentHashMap<String, Pair<Boolean, Long>>()

    /** isSupportWindowReplyState* 系列在 proceed 期间临时改写 resize 标记的事务句柄。 */
    private val pendingResizeTx = ThreadLocal<ResizeMetadataTransaction?>()

    override fun onHook() {
        hasEnable(Pref.Key.Android.FREEFORM_UNLIMITED_COUNT) {
            hookUnlimitedCount()
        }
        hasEnable(Pref.Key.Android.FREEFORM_ALL_APPS) {
            if (verifyAllAppsCompatibility()) {
                hookAllApps()
            } else {
                YLog.error("$TAG 当前系统缺少全应用小窗所需的框架标识，跳过安装（fail-closed）")
            }
        }
    }

    // ---------------- 数量上限解除 ----------------

    private fun hookUnlimitedCount() {
        val atmClass = ATM_SERVICE.toClassOrNull()
        if (atmClass == null) {
            YLog.error("$TAG 无法找到 $ATM_SERVICE，跳过数量上限解除")
        } else {
            // 小窗数量统计：始终报告 0，使「达到多小窗上限」判断不成立
            atmClass.method {
                name = "windowReplySizeForMulti"
                emptyParam()
                returnType = IntType
            }.ignored().give()?.hook {
                before { result = 0 }
            } ?: YLog.warn("$TAG 未找到 windowReplySizeForMulti，跳过")

            // 可见小窗根任务列表：返回空列表，绕过基于列表数量的拦截
            atmClass.method {
                name = "getFreeformRootTasksVisibleListWrForMulti"
                emptyParam()
            }.ignored().give()?.hook {
                before { result = ArrayList<Any?>() }
            } ?: YLog.warn("$TAG 未找到 getFreeformRootTasksVisibleListWrForMulti，跳过")

            Deoptimizer.deoptimizeMethod(TAG, atmClass, "windowReplySizeForMulti")
        }

        val tdaMifavorClass = TDA_MIFAVOR.toClassOrNull()
        if (tdaMifavorClass == null) {
            YLog.warn("$TAG 无法找到 $TDA_MIFAVOR，跳过上限弹窗抑制")
        } else {
            // 「达到多小窗上限」的弹窗提示
            tdaMifavorClass.method {
                name = "alertMessageForReachMultiWrMaxSizeWr"
                param(ATM_SERVICE)
                returnType = UnitType
            }.ignored().give()?.hook {
                replaceToNull()
            } ?: YLog.warn("$TAG 未找到 alertMessageForReachMultiWrMaxSizeWr，跳过")
        }

        hookSupervisorFallback()
    }

    /**
     * 厂商 AOT 可能保留内联的 `size >= 3` 分支：Supervisor 返回 102 是专用上限结果码，
     * 此时用同一调用者/任务/SafeOptions 回退到标准的 startActivityFromRecents。
     */
    private fun hookSupervisorFallback() {
        val supervisorClass = ACTIVITY_TASK_SUPERVISOR.toClassOrNull() ?: run {
            YLog.warn("$TAG 无法找到 $ACTIVITY_TASK_SUPERVISOR，跳过 102 回退")
            return
        }
        val fallbackMethod = supervisorClass.method {
            name = "startActivityFromRecents"
            param(IntType, IntType, IntType, SAFE_ACTIVITY_OPTIONS)
            returnType = IntType
        }.ignored().give()
        if (fallbackMethod == null) {
            YLog.warn("$TAG 未找到 startActivityFromRecents 回退方法")
        }

        supervisorClass.method {
            name = "startActivityFromRecentsForWR"
            param(IntType, IntType, IntType, IntType, SAFE_ACTIVITY_OPTIONS)
            returnType = IntType
        }.ignored().give()?.hook {
            after {
                if ((result as? Int) != 102) return@after
                val fallback = fallbackMethod ?: return@after
                runCatching {
                    val fallbackResult = fallback.invoke(
                        instance, args[0], args[1], args[2], args[4]
                    )
                    YLog.debug("$TAG startActivityFromRecentsForWR 返回 102，回退结果: $fallbackResult")
                    result = fallbackResult
                }.onFailure {
                    YLog.error("$TAG startActivityFromRecents 回退失败", it)
                }
            }
        } ?: YLog.warn("$TAG 未找到 startActivityFromRecentsForWR，跳过")

        Deoptimizer.deoptimizeMethod(TAG, supervisorClass, "startActivityFromRecentsForWR")
    }

    // ---------------- 全应用资格 ----------------

    private fun hookAllApps() {
        hookTdaMifavorGates()
        hookClientControllerGates()
        hookActivityTaskGates()

        // 资格判定发生在新 ActivityRecord 附加到 Task 之前；厂商 AOT 可能把旧的系统应用
        // 白名单内联进这些调用方，反优化以保证上面的 hook 生效。
        ACTIVITY_STARTER.toClassOrNull()?.let {
            Deoptimizer.deoptimizeMethod(TAG, it, "startActivityInner")
        }
        TDA_MIFAVOR.toClassOrNull()?.let {
            Deoptimizer.deoptimizeMethod(TAG, it, "isSupportWindowReply")
        }
    }

    private fun hookTdaMifavorGates() {
        val tdaMifavorClass = TDA_MIFAVOR.toClassOrNull() ?: run {
            YLog.error("$TAG 无法找到 $TDA_MIFAVOR，跳过原厂资格闸门")
            return
        }

        tdaMifavorClass.method {
            name = "checkTaskSupportForCompWr"
            param(Context::class.java, ComponentName::class.java)
            returnType = BooleanType
        }.ignored().give()?.hook {
            before {
                val component = args[1] as? ComponentName
                if (isEligibleComponent(component)) result = true
            }
        } ?: YLog.warn("$TAG 未找到 checkTaskSupportForCompWr，跳过")

        tdaMifavorClass.method {
            name = "checkTaskSupportWr"
            param(Context::class.java, TASK)
            returnType = BooleanType
        }.ignored().give()?.hook {
            before {
                if (isEligiblePackage(packageFromTask(args[1]))) result = true
            }
        } ?: YLog.warn("$TAG 未找到 checkTaskSupportWr，跳过")

        // 不可调整大小的应用会在这里以 state=2 被拒绝并弹出「不允许调整大小」提示，
        // 后续标准 hook 再无机会放行。proceed 期间临时把 resize 标记改为可调整。
        tdaMifavorClass.method {
            name = "isSupportWindowReplyState"
            param(ACTIVITY_RECORD, Context::class.java)
            returnType = IntType
        }.ignored().give()?.hook {
            before {
                val activity = args[0]
                if (isEligibleActivity(activity)) {
                    pendingResizeTx.set(ResizeMetadataTransaction.begin(activity))
                }
            }
            after {
                pendingResizeTx.get()?.restore()
                pendingResizeTx.remove()
                if (isEligibleActivity(args[0]) && (result as? Int) != 0) result = 0
            }
        } ?: YLog.warn("$TAG 未找到 isSupportWindowReplyState(ActivityRecord)，跳过")

        // 返回 TaskNotSupportWindowReplyStateWr 的重载：原厂约定 null 表示「无不支持状态」
        tdaMifavorClass.method {
            name = "isSupportWindowReplyState"
            param(TASK, Context::class.java)
            returnType = TASK_NOT_SUPPORT_WINDOW_REPLY_STATE
        }.ignored().give()?.hook {
            before {
                val task = args[0]
                if (isEligibleTask(task)) {
                    pendingResizeTx.set(ResizeMetadataTransaction.begin(task))
                }
            }
            after {
                pendingResizeTx.get()?.restore()
                pendingResizeTx.remove()
                if (isEligibleTask(args[0]) && result != null) result = null
            }
        } ?: YLog.warn("$TAG 未找到 isSupportWindowReplyState(Task)，跳过")

        tdaMifavorClass.method {
            name = "isSupportWindowReplyStateForTask"
            param(TASK, Context::class.java)
            returnType = IntType
        }.ignored().give()?.hook {
            before {
                val task = args[0]
                if (isEligibleTask(task)) {
                    pendingResizeTx.set(ResizeMetadataTransaction.begin(task))
                }
            }
            after {
                pendingResizeTx.get()?.restore()
                pendingResizeTx.remove()
                if (isEligibleTask(args[0]) && (result as? Int) != 0) result = 0
            }
        } ?: YLog.warn("$TAG 未找到 isSupportWindowReplyStateForTask，跳过")

        tdaMifavorClass.method {
            name = "isResizeable"
            param(ACTIVITY_RECORD)
            returnType = BooleanType
        }.ignored().give()?.hook {
            before {
                if (isEligibleActivity(args[0])) result = true
            }
        } ?: YLog.warn("$TAG 未找到 isResizeable，跳过")

        tdaMifavorClass.method {
            name = "isSupportResize"
            param(Context::class.java, ComponentName::class.java)
            returnType = BooleanType
        }.ignored().give()?.hook {
            before {
                val component = args[1] as? ComponentName
                if (isEligibleComponent(component)) result = true
            }
        } ?: YLog.warn("$TAG 未找到 isSupportResize，跳过")

        Deoptimizer.deoptimizeMethod(TAG, tdaMifavorClass, "isSupportWindowReplyState")
        Deoptimizer.deoptimizeMethod(TAG, tdaMifavorClass, "isSupportWindowReplyStateForTask")
        Deoptimizer.deoptimizeMethod(TAG, tdaMifavorClass, "isResizeable")
    }

    private fun hookClientControllerGates() {
        val controllerClass = ACTIVITY_CLIENT_CONTROLLER.toClassOrNull() ?: run {
            YLog.warn("$TAG 无法找到 $ACTIVITY_CLIENT_CONTROLLER，跳过客户端资格闸门")
            return
        }

        // 游戏助手/桌面也会经 ActivityClient 查询资格，包名/组件两个重载都要覆盖
        controllerClass.method {
            name = "checkTaskSupportForCompWr"
            param(ComponentName::class.java)
            returnType = BooleanType
        }.ignored().give()?.hook {
            before {
                val component = args[0] as? ComponentName
                if (isEligibleComponent(component)) result = true
            }
        } ?: YLog.warn("$TAG 未找到 ACC.checkTaskSupportForCompWr，跳过")

        controllerClass.method {
            name = "checkTaskSupportForPkgWr"
            param(StringClass)
            returnType = BooleanType
        }.ignored().give()?.hook {
            before {
                if (isEligiblePackage(args[0] as? String)) result = true
            }
        } ?: YLog.warn("$TAG 未找到 ACC.checkTaskSupportForPkgWr，跳过")

        // 仅反优化（不做行为变更）：保证能力 hook 在原厂 surface 事务之前生效
        Deoptimizer.deoptimizeMethod(TAG, controllerClass, "toggleSwitchFromFullScreenToFreeformWrInner")
        Deoptimizer.deoptimizeMethod(TAG, controllerClass, "toggleSwitchFromFullScreenToFreeformWrDelay")
    }

    private fun hookActivityTaskGates() {
        val tdaClass = TASK_DISPLAY_AREA.toClassOrNull()

        ACTIVITY_RECORD.toClassOrNull()?.let { recordClass ->
            if (tdaClass != null) {
                recordClass.method {
                    name = "supportsFreeformInDisplayArea"
                    param(tdaClass)
                    returnType = BooleanType
                }.ignored().give()?.hook {
                    before {
                        if (isEligibleActivity(instanceOrNull)) result = true
                    }
                } ?: YLog.warn("$TAG 未找到 ActivityRecord.supportsFreeformInDisplayArea，跳过")
            }
            recordClass.method {
                name = "supportsMultiWindow"
                emptyParam()
                returnType = BooleanType
            }.ignored().give()?.hook {
                before {
                    if (isEligibleActivity(instanceOrNull)) result = true
                }
            } ?: YLog.warn("$TAG 未找到 ActivityRecord.supportsMultiWindow，跳过")
            if (tdaClass != null) {
                recordClass.method {
                    name = "supportsMultiWindowInDisplayArea"
                    param(tdaClass)
                    returnType = BooleanType
                }.ignored().give()?.hook {
                    before {
                        if (isEligibleActivity(instanceOrNull)) result = true
                    }
                } ?: YLog.warn("$TAG 未找到 ActivityRecord.supportsMultiWindowInDisplayArea，跳过")
            }
        } ?: YLog.warn("$TAG 无法找到 $ACTIVITY_RECORD，跳过 ActivityRecord 闸门")

        TASK.toClassOrNull()?.let { taskClass ->
            if (tdaClass != null) {
                taskClass.method {
                    name = "supportsFreeformInDisplayArea"
                    param(tdaClass)
                    returnType = BooleanType
                }.ignored().give()?.hook {
                    before {
                        if (isEligibleTask(instanceOrNull)) result = true
                    }
                } ?: YLog.warn("$TAG 未找到 Task.supportsFreeformInDisplayArea，跳过")
            }
        } ?: YLog.warn("$TAG 无法找到 $TASK，跳过 Task 闸门")

        TASK_FRAGMENT.toClassOrNull()?.let { fragmentClass ->
            for (methodName in arrayOf("supportsMultiWindow", "supportsMultiWindowCheckForOtherMultifromWr")) {
                fragmentClass.method {
                    name = methodName
                    emptyParam()
                    returnType = BooleanType
                }.ignored().give()?.hook {
                    before {
                        if (isEligibleTaskFragment(instanceOrNull)) result = true
                    }
                } ?: YLog.warn("$TAG 未找到 TaskFragment.$methodName，跳过")
            }
            if (tdaClass != null) {
                for (methodName in arrayOf(
                    "supportsMultiWindowInDisplayArea",
                    "supportsMultiWindowInDisplayAreaCheckForOtherMultifromWr"
                )) {
                    fragmentClass.method {
                        name = methodName
                        param(tdaClass)
                        returnType = BooleanType
                    }.ignored().give()?.hook {
                        before {
                            if (isEligibleTaskFragment(instanceOrNull)) result = true
                        }
                    } ?: YLog.warn("$TAG 未找到 TaskFragment.$methodName，跳过")
                }
            }
        } ?: YLog.warn("$TAG 无法找到 $TASK_FRAGMENT，跳过 TaskFragment 闸门")
    }

    // ---------------- 资格判定（全部 fail-closed） ----------------

    /**
     * 部分厂商闸门不完整是不安全的：一个闸门放行而后续事务仍按不可调整处理。
     * 仅当身份字段与标准类型判定都存在时才允许安装全应用 hook。
     */
    private fun verifyAllAppsCompatibility(): Boolean = runCatching {
        val activity = ACTIVITY_RECORD.toClassOrNull() ?: return false
        val task = TASK.toClassOrNull() ?: return false
        val standardMethods = findNoArgBoolean(activity, "isActivityTypeStandard") &&
            findNoArgBoolean(task, "isActivityTypeStandard")
        val identityFields = hasAnyField(activity, "info", "mActivityComponent") &&
            hasAnyField(task, "realActivity", "mRealActivity", "mLastNonFinishingActivity", "mResumedActivity")
        standardMethods && identityFields
    }.getOrDefault(false)

    private fun findNoArgBoolean(type: Class<*>, name: String): Boolean {
        var current: Class<*>? = type
        while (current != null) {
            val clazz = current
            val found = runCatching {
                clazz.declaredMethods.any {
                    it.name == name && it.parameterCount == 0 && it.returnType == BooleanType
                }
            }.getOrDefault(false)
            if (found) return true
            current = clazz.superclass
        }
        return false
    }

    private fun hasAnyField(type: Class<*>, vararg names: String): Boolean {
        for (name in names) {
            var current: Class<*>? = type
            while (current != null) {
                val clazz = current
                val found = runCatching {
                    clazz.getDeclaredField(name)
                    true
                }.getOrDefault(false)
                if (found) return true
                current = clazz.superclass
            }
        }
        return false
    }

    private fun isEligibleActivity(owner: Any?): Boolean {
        if (owner == null) return false
        if (!isStandard(owner)) {
            // 新建的普通 Activity 在 ActivityStarter 附加前类型为 UNDEFINED(0)；
            // HOME/RECENTS/ASSISTANT 已有非零类型，保持受保护。
            val type = runCatching {
                val method = findNoArg(owner.javaClass, "getActivityType") ?: return false
                method.invoke(owner) as? Int
            }.getOrNull() ?: return false
            if (type != 0) return false
            val intent = field(owner, "intent") as? Intent ?: return false
            if (intent.hasCategory(Intent.CATEGORY_HOME) ||
                intent.hasCategory("android.intent.category.SECONDARY_HOME")
            ) return false
        }
        val info = field(owner, "info")
        var packageName = (info as? ActivityInfo)?.packageName ?: packageFromObject(info)
        if (packageName == null) packageName = packageFromObject(field(owner, "mActivityComponent"))
        return isEligiblePackage(packageName)
    }

    private fun isEligibleTask(owner: Any?): Boolean {
        if (owner == null || !isStandard(owner)) return false
        return isEligiblePackage(packageFromTask(owner))
    }

    private fun isEligibleTaskFragment(owner: Any?): Boolean {
        if (owner == null || !isStandard(owner)) return false
        return isEligiblePackage(packageFromOwner(owner))
    }

    private fun packageFromOwner(owner: Any?): String? {
        if (owner == null) return null
        return packageFromActivity(owner)
            ?: packageFromTask(owner)
            ?: packageFromObject(field(owner, "realActivity"))
    }

    private fun isStandard(owner: Any): Boolean = runCatching {
        val method = findNoArg(owner.javaClass, "isActivityTypeStandard") ?: return false
        method.invoke(owner) as? Boolean == true
    }.getOrDefault(false)

    private fun packageFromTask(owner: Any?): String? {
        if (owner == null) return null
        return packageFromObject(field(owner, "realActivity"))
            ?: packageFromObject(field(owner, "mRealActivity"))
            ?: packageFromActivity(field(owner, "mLastNonFinishingActivity"))
            ?: packageFromActivity(field(owner, "mLastPausedActivity"))
            ?: packageFromActivity(field(owner, "mResumedActivity"))
            ?: runCatching {
                findNoArg(owner.javaClass, "topRunningActivity")
                    ?.let { packageFromActivity(it.invoke(owner)) }
            }.getOrNull()
    }

    private fun packageFromActivity(owner: Any?): String? {
        if (owner == null) return null
        val info = field(owner, "info")
        if (info is ActivityInfo) return info.packageName
        return packageFromObject(field(owner, "mActivityComponent"))
            ?: packageFromObject(field(owner, "componentName"))
    }

    private fun packageFromObject(value: Any?): String? = when (value) {
        is ComponentName -> value.packageName
        is ActivityInfo -> value.packageName
        is ApplicationInfo -> value.packageName
        else -> null
    }

    private fun isEligiblePackage(packageName: String?): Boolean {
        if (!isSanePackage(packageName)) return false
        if (excludedPackages().contains(packageName)) return false
        val cacheKey = "package:$packageName"
        val now = System.currentTimeMillis()
        eligibilityCache[cacheKey]?.let { (allowed, checkedAt) ->
            if (now - checkedAt < ELIGIBILITY_CACHE_MS) return allowed
        }
        val allowed = runCatching {
            val pm = contextOrNull()?.packageManager ?: return false
            val info = pm.getApplicationInfo(
                packageName!!,
                PackageManager.MATCH_DISABLED_COMPONENTS or
                    PackageManager.MATCH_DIRECT_BOOT_AWARE or
                    PackageManager.MATCH_DIRECT_BOOT_UNAWARE
            )
            // 标准 Activity/Task 检查保护特殊窗口；系统标志或无桌面图标不构成调整限制
            info.enabled &&
                (info.flags and ApplicationInfo.FLAG_INSTALLED) != 0 &&
                (info.flags and ApplicationInfo.FLAG_SUSPENDED) == 0
        }.getOrDefault(false)
        eligibilityCache[cacheKey] = allowed to now
        return allowed
    }

    private fun isEligibleComponent(component: ComponentName?): Boolean {
        if (component == null || !isEligiblePackage(component.packageName)) return false
        val cacheKey = "component:${component.flattenToShortString()}"
        val now = System.currentTimeMillis()
        eligibilityCache[cacheKey]?.let { (allowed, checkedAt) ->
            if (now - checkedAt < ELIGIBILITY_CACHE_MS) return allowed
        }
        val allowed = runCatching {
            val pm = contextOrNull()?.packageManager ?: return false
            val requested = pm.getActivityInfo(component, PackageManager.MATCH_DISABLED_COMPONENTS)
            // 应用内部页面也要能在小窗恢复；Android 仍会执行正常的组件访问检查
            requested.enabled && component.packageName == requested.packageName
        }.getOrDefault(false)
        eligibilityCache[cacheKey] = allowed to now
        return allowed
    }

    private fun isSanePackage(packageName: String?): Boolean =
        packageName != null &&
            packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) &&
            !PROTECTED_PACKAGES.contains(packageName)

    private fun excludedPackages(): Set<String> = runCatching {
        Prefs.getStringSet(Pref.Key.Android.FREEFORM_EXCLUDED_APPS, mutableSetOf())
    }.getOrDefault(mutableSetOf())

    private fun contextOrNull(): Context? = runCatching { systemContext }.getOrNull()

    private fun field(owner: Any?, name: String): Any? {
        if (owner == null) return null
        var type: Class<*>? = owner.javaClass
        while (type != null) {
            try {
                val value = type.getDeclaredField(name)
                value.isAccessible = true
                return value.get(owner)
            } catch (_: NoSuchFieldException) {
                type = type.superclass
            } catch (_: Throwable) {
                return null
            }
        }
        return null
    }

    private fun findNoArg(type: Class<*>, name: String): java.lang.reflect.Method? {
        var current: Class<*>? = type
        while (current != null) {
            val clazz = current
            runCatching {
                for (method in clazz.declaredMethods) {
                    if (method.name == name && method.parameterCount == 0) {
                        method.isAccessible = true
                        return method
                    }
                }
            }
            current = clazz.superclass
        }
        return null
    }

    /** 恢复全部临时 resize 标记，嵌套调用与异常情况下也能完整还原。 */
    private class ResizeMetadataTransaction {
        private val changed = ArrayList<Triple<Any, Field, Int>>()
        private val visited = IdentityHashMap<Any, MutableSet<String>>()

        private fun captureRelated(owner: Any?) {
            if (owner == null) return
            capture(owner, "mResizeMode")
            val info = fieldOf(owner, "info")
            if (info is ActivityInfo) capture(info, "resizeMode")
            for (name in arrayOf("mLastNonFinishingActivity", "mLastPausedActivity", "mResumedActivity")) {
                val activity = fieldOf(owner, name) ?: continue
                capture(activity, "mResizeMode")
                val activityInfo = fieldOf(activity, "info")
                if (activityInfo is ActivityInfo) capture(activityInfo, "resizeMode")
            }
        }

        private fun capture(owner: Any, name: String) {
            val names = visited.getOrPut(owner) { mutableSetOf() }
            if (!names.add(name)) return
            runCatching {
                val target = findIntFieldOf(owner, name) ?: return@runCatching
                val original = target.getInt(owner)
                if (original == RESIZE_MODE_RESIZEABLE) return@runCatching
                target.setInt(owner, RESIZE_MODE_RESIZEABLE)
                changed.add(Triple(owner, target, original))
            }
        }

        fun restore() {
            for (index in changed.indices.reversed()) {
                val (owner, field, original) = changed[index]
                runCatching { field.setInt(owner, original) }
            }
            changed.clear()
        }

        companion object {
            private const val RESIZE_MODE_RESIZEABLE = 2

            fun begin(owner: Any?): ResizeMetadataTransaction {
                val transaction = ResizeMetadataTransaction()
                transaction.captureRelated(owner)
                return transaction
            }

            private fun fieldOf(owner: Any, name: String): Any? {
                var type: Class<*>? = owner.javaClass
                while (type != null) {
                    try {
                        val value = type.getDeclaredField(name)
                        value.isAccessible = true
                        return value.get(owner)
                    } catch (_: NoSuchFieldException) {
                        type = type.superclass
                    } catch (_: Throwable) {
                        return null
                    }
                }
                return null
            }

            private fun findIntFieldOf(owner: Any, name: String): Field? {
                var type: Class<*>? = owner.javaClass
                while (type != null) {
                    try {
                        val target = type.getDeclaredField(name)
                        target.isAccessible = true
                        return if (target.type == Integer.TYPE) target else null
                    } catch (_: NoSuchFieldException) {
                        type = type.superclass
                    } catch (_: Throwable) {
                        return null
                    }
                }
                return null
            }
        }
    }
}
