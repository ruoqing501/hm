package dev.lackluster.redmagichelper.utils

/**
 * 熄屏自动隐藏应用的执行层(移植自 LS_Augment 的 RootHideManager,仅保留隐藏/恢复语义)。
 *
 * 隐藏语义:`am force-stop --user <uid> <pkg>` 后 `pm hide --user <uid> <pkg>`,
 * 再用 dumpsys 校验 `hidden=true`,最多重试 3 次;恢复为 `pm unhide` 并同样校验。
 * LS_Augment 不在亮屏时自动恢复,本移植保持一致:恢复只能手动触发。
 *
 * 本对象同时运行在 system_server(hook 侧)与模块 app(兜底/UI)进程中,
 * 通过 [RootShell] 以 root 执行命令;Root 不可用时调用方负责降级,本对象只如实上报。
 */
internal object ScreenOffHideExecutor {

    /** system_server 无法直接 su 时写入 Settings.Global 的待执行标记,由模块 app 侧消费。 */
    const val PENDING_KEY = "rmh_screen_off_hide_pending"

    private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+")

    private val PROTECTED = setOf(
        "android", "system", "com.android.systemui", "com.android.settings",
        "dev.lackluster.redmagichelper",
        "me.weishu.kernelsu", "me.weishu.kernelsu.debug",
        "com.rifsxd.ksunext", "org.lsposed.manager",
        "com.topjohnwu.magisk", "me.bmax.apatch"
    )

    enum class State { VISIBLE, HIDDEN, MISSING, ERROR }

    class Outcome(val success: Boolean, val message: String)

    fun isValidPackage(pkg: String): Boolean =
        pkg.length <= 255 && PACKAGE.matches(pkg) && !pkg.contains("..")

    /** 过滤无效与受保护目标,排序保证执行顺序稳定。 */
    fun sanitize(targets: Collection<String>): List<String> =
        targets.filter { isValidPackage(it) && it !in PROTECTED }.sorted()

    /** 探测 root 是否在当前进程可用(system_server 内通常因 SELinux/KernelSU 策略失败)。 */
    fun rootGranted(): Boolean {
        val result = RootShell.exec("id -u", timeoutSeconds = 5, maxOutput = 1024)
        return result.isSuccess && result.output.trim() == "0"
    }

    /** 解析当前前台用户,失败时回退到 user 0(fail-closed 之外的唯一合理默认)。 */
    fun currentUserId(): Int {
        for (cmd in arrayOf("cmd activity get-current-user", "am get-current-user")) {
            val result = RootShell.exec("$cmd 2>/dev/null", timeoutSeconds = 5, maxOutput = 1024)
            if (result.isSuccess) {
                result.output.trim().toIntOrNull()?.let { if (it in 0..99999) return it }
            }
        }
        return 0
    }

    fun queryState(pkg: String, userId: Int): State {
        val marker = "User $userId:"
        val result = RootShell.exec(
            "dumpsys package ${RootShell.quote(pkg)} 2>/dev/null | grep -F ${RootShell.quote(marker)} | head -n 1",
            timeoutSeconds = 12,
            maxOutput = 16 * 1024
        )
        if (!result.isSuccess && result.output.isEmpty()) return State.ERROR
        val line = result.output
        return when {
            line.contains("installed=false") -> State.MISSING
            line.contains("hidden=true") -> State.HIDDEN
            line.contains("hidden=false") -> State.VISIBLE
            else -> State.MISSING
        }
    }

    private fun change(pkg: String, userId: Int, hide: Boolean): Boolean {
        val expected = if (hide) State.HIDDEN else State.VISIBLE
        when (queryState(pkg, userId)) {
            expected -> return true
            State.MISSING -> return false
            else -> Unit
        }
        val mode = if (hide) "hide" else "unhide"
        repeat(3) {
            val command = buildString {
                if (hide) {
                    append("/system/bin/am force-stop --user ").append(userId).append(' ')
                        .append(RootShell.quote(pkg)).append(" </dev/null >/dev/null 2>&1 || true; ")
                }
                append("/system/bin/pm ").append(mode).append(" --user ").append(userId).append(' ')
                    .append(RootShell.quote(pkg)).append(" </dev/null >/dev/null 2>&1")
            }
            val result = RootShell.exec(command, timeoutSeconds = 15, maxOutput = 4096)
            if (queryState(pkg, userId) == expected) return true
            if (result.timedOut) return false
        }
        return false
    }

    /** 对当前用户隐藏全部目标;调用前需确认 root 可用与屏幕已熄灭。 */
    fun hideAll(targets: Collection<String>): Outcome {
        val valid = sanitize(targets)
        if (valid.isEmpty()) return Outcome(true, "no_targets")
        if (!rootGranted()) return Outcome(false, "root_unavailable")
        val userId = currentUserId()
        var success = 0
        val failures = mutableListOf<String>()
        for (pkg in valid) {
            if (change(pkg, userId, true)) success++ else failures.add(pkg)
        }
        return if (failures.isEmpty()) Outcome(true, "hidden=$success user=$userId")
        else Outcome(false, "hidden=$success failed=${failures.size} first=${failures.first()}")
    }

    /** 恢复(取消隐藏)全部目标,允许目标列表为空时直接成功。 */
    fun restoreAll(targets: Collection<String>): Outcome {
        val valid = sanitize(targets)
        if (valid.isEmpty()) return Outcome(true, "no_targets")
        if (!rootGranted()) return Outcome(false, "root_unavailable")
        val userId = currentUserId()
        var success = 0
        val failures = mutableListOf<String>()
        for (pkg in valid) {
            if (change(pkg, userId, false)) success++ else failures.add(pkg)
        }
        return if (failures.isEmpty()) Outcome(true, "restored=$success user=$userId")
        else Outcome(false, "restored=$success failed=${failures.size} first=${failures.first()}")
    }

    /** 取消勾选时的即时恢复(fail-closed:目标离开列表前必须先恢复可见)。 */
    fun unhide(pkg: String): Outcome {
        if (!isValidPackage(pkg)) return Outcome(false, "invalid_target")
        if (!rootGranted()) return Outcome(false, "root_unavailable")
        val userId = currentUserId()
        return if (change(pkg, userId, false)) Outcome(true, "restored user=$userId")
        else Outcome(false, "restore_failed:$pkg")
    }

    /** 由模块 app 侧(root)清除待执行标记;清除失败只影响重复执行,可容忍。 */
    fun clearPending() {
        RootShell.exec("settings delete global $PENDING_KEY", timeoutSeconds = 8, maxOutput = 1024)
    }
}
