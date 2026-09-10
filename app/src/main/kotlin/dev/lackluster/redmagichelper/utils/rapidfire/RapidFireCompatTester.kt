package dev.lackluster.redmagichelper.utils.rapidfire

import android.content.Context
import android.os.Handler
import android.os.Looper
import dev.lackluster.redmagichelper.BuildConfig
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.natives.RapidFireCompatibility
import dev.lackluster.redmagichelper.hook.natives.RapidFireCrashFuse
import dev.lackluster.redmagichelper.utils.RootShell
import dev.lackluster.hyperx.compose.activity.SafeSP
import java.io.File
import java.util.concurrent.Executors

/**
 * App-side driver of the shoulder rapid-fire compatibility test.
 *
 * Mirrors LS_Augment's FeatureActivity rapid-fire flow. Evidence channels differ
 * because this project has no config provider:
 * - system_server witnesses are written to Settings.Global by the system hook and
 *   read back here through root `settings get global`.
 * - gamelauncher witnesses (route evidence, hook-installed marker) are logged to
 *   logcat by the game-space hook and read back here through root `logcat -d`.
 * - The session itself is mirrored to Settings.Global
 *   ([RapidFireCompatibility.GLOBAL_SESSION_KEY]) so system_server can observe
 *   phase transitions with a ContentObserver.
 *
 * All methods must be called on the main thread unless stated otherwise.
 */
internal class RapidFireCompatTester(private val context: Context) {

    fun interface UpdateListener {
        fun onUpdate()
    }

    var updateListener: UpdateListener? = null
    var toastListener: ((String) -> Unit)? = null

    private val executor = Executors.newSingleThreadExecutor()
    // The blocking physical capture runs on its own thread so UI-driven tasks
    // (refresh/cancel) are not queued behind the up-to-20s shell session.
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    var fingerprint: String? = null
        private set

    @Volatile
    var session: RapidFireCompatibility.Session? = null
        private set

    @Volatile
    var token: RapidFireCompatibility.Token? = null
        private set

    @Volatile
    var fused: Boolean = false
        private set

    @Volatile
    var captureInFlight: Boolean = false
        private set

    @Volatile
    var captureLeft: Boolean = true
        private set

    @Volatile
    var captureDeadline: Long = 0L
        private set

    @Volatile
    var captureFeedback: String? = null
        private set

    @Volatile
    private var fingerprintLoading = false

    @Volatile
    private var autoContinueAttempted = false

    @Volatile
    private var captureSessionId: String? = null

    private var captureGeneration = 0
    private var physicalCapture: RapidFirePhysicalCapture? = null
    private var destroyed = false

    val unlocked: Boolean
        get() {
            val fp = fingerprint
            val tk = token
            return !fused && fp != null && tk != null && tk.validFor(fp)
        }

    fun destroy() {
        destroyed = true
        stopCapture()
        executor.shutdown()
        captureExecutor.shutdown()
    }

    /** Stop the in-flight capture when the page leaves the foreground. */
    fun onPagePaused() {
        if (captureInFlight) {
            stopCapture()
            captureFeedback = "离开页面已停止采集并交还肩键控制，返回后可重新采集"
            notifyUpdate()
        }
    }

    /** (Re)load fingerprint/session/token asynchronously; safe to call repeatedly. */
    fun refresh() {
        if (fingerprintLoading) return
        fingerprintLoading = true
        executor.execute {
            val fp = RapidFireCompatibility.currentFingerprint(context)
            val stored = RapidFireCompatibility.Session.parse(
                SafeSP.getString(Pref.Key.GameSpace.TGK_RAPID_FIRE_TEST_SESSION, "")
            )
            val storedToken = RapidFireCompatibility.Token.parse(
                SafeSP.getString(Pref.Key.GameSpace.TGK_RAPID_FIRE_COMPAT_TOKEN, "")
            )
            val fusedNow = readFuseTripped()
            main.post {
                fingerprintLoading = false
                if (destroyed) return@post
                fingerprint = fp
                session = stored?.takeIf { it.validFor(fp) }
                token = storedToken?.takeIf { it.validFor(fp) }
                fused = fusedNow
                if (!unlocked &&
                    SafeSP.getBoolean(Pref.Key.GameSpace.TGK_RAPID_FIRE_ENABLED, false)
                ) {
                    SafeSP.putAny(Pref.Key.GameSpace.TGK_RAPID_FIRE_ENABLED, false)
                }
                notifyUpdate()
                if (!autoContinueAttempted &&
                    session?.state == RapidFireCompatibility.State.NEEDS_RESTART
                ) {
                    autoContinueAttempted = true
                    session?.let { runPreflight(it) }
                }
            }
        }
    }

    /** The single action button of the test panel; behaviour depends on the state. */
    fun onAction() {
        if (captureInFlight) return
        val now = System.currentTimeMillis()
        val current = session
        val state = current?.state ?: RapidFireCompatibility.State.UNTESTED
        when {
            current == null || current.isExpired(now) ||
                state == RapidFireCompatibility.State.UNTESTED ||
                state == RapidFireCompatibility.State.FAILED ||
                state == RapidFireCompatibility.State.PASSED -> {
                stopCapture()
                captureFeedback = null
                val created = RapidFireCompatibility.Session.start(fingerprint, now)
                if (created == null) {
                    toast("无法建立兼容性测试会话，请重新进入此页面")
                    return
                }
                session = created
                notifyUpdate()
                runPreflight(created)
            }
            state == RapidFireCompatibility.State.NEEDS_RESTART ->
                toast("请重启设备后重新进入本页，测试会自动继续")
            state == RapidFireCompatibility.State.WAIT_LEFT ->
                if (current.physicalLeft <= 0) capturePhysicalWithRoot(current, true)
                else verifySide(current, true)
            state == RapidFireCompatibility.State.WAIT_RIGHT ->
                if (current.physicalRight <= 0) capturePhysicalWithRoot(current, false)
                else verifySide(current, false)
            state == RapidFireCompatibility.State.VERIFYING -> finishVerification(current)
            else -> Unit
        }
    }

    fun cancelTest() {
        stopCapture()
        executor.execute {
            persistSession(null, null, clearToken = true)
            main.post {
                if (destroyed) return@post
                session = null
                token = null
                toast("测试已取消，原生目标已归零")
                notifyUpdate()
            }
        }
    }

    fun requestReboot() {
        executor.execute { RootShell.exec("reboot", timeoutSeconds = 5, maxOutput = 1024) }
    }

    fun clearFuse() {
        executor.execute {
            val clear = RootShell.exec(
                "settings put global ${RapidFireCrashFuse.FUSED} 0; " +
                    "settings put global ${RapidFireCrashFuse.ATTEMPTS} 0; " +
                    "settings put global ${RapidFireCrashFuse.PENDING} 0",
                timeoutSeconds = 6, maxOutput = 4096
            )
            if (clear.isSuccess) {
                persistSession(null, null, clearToken = true)
            }
            main.post {
                if (destroyed) return@post
                if (!clear.isSuccess) {
                    toast("清除失败：" + clear.publicError())
                } else {
                    fused = false
                    session = null
                    token = null
                    notifyUpdate()
                    onAction()
                }
            }
        }
    }

    private fun runPreflight(current: RapidFireCompatibility.Session) {
        if (current.isExpired(System.currentTimeMillis())) return
        val preflight = current.withState(
            RapidFireCompatibility.State.PREFLIGHT, System.currentTimeMillis()
        )
        session = preflight
        notifyUpdate()
        executor.execute {
            var result = RapidFireCompatibility.State.WAIT_LEFT
            val root = RootShell.exec("id -u", timeoutSeconds = 5, maxOutput = 1024)
            if (!root.isSuccess || root.output.trim() != "0") {
                result = RapidFireCompatibility.State.FAILED
            } else if (RapidFireCompatibility.nativeProfile() == null ||
                !packageInstalled("cn.nubia.gamelauncher") ||
                !packageInstalled("cn.nubia.gameassist") ||
                !nativeLibraryPresent()
            ) {
                result = RapidFireCompatibility.State.FAILED
            } else if (readFuseTripped()) {
                result = RapidFireCompatibility.State.FUSED
            } else if (!systemHookIsCurrent()) {
                result = RapidFireCompatibility.State.NEEDS_RESTART
            }
            val resolved = preflight.withState(result, System.currentTimeMillis())
            persistSession(resolved, null, resetCps = true, clearToken = true)
            main.post {
                if (destroyed) return@post
                session = resolved
                fused = result == RapidFireCompatibility.State.FUSED
                notifyUpdate()
                if (resolved.state == RapidFireCompatibility.State.WAIT_LEFT &&
                    isCurrentSession(resolved)
                ) {
                    main.postDelayed({ capturePhysicalWithRoot(resolved, true) }, 250L)
                }
            }
        }
    }

    private fun capturePhysicalWithRoot(
        current: RapidFireCompatibility.Session,
        left: Boolean
    ) {
        if (destroyed || captureInFlight ||
            !isCurrentSession(current) ||
            current.isExpired(System.currentTimeMillis()) ||
            (left && current.state != RapidFireCompatibility.State.WAIT_LEFT) ||
            (!left && current.state != RapidFireCompatibility.State.WAIT_RIGHT)
        ) return
        captureInFlight = true
        captureLeft = left
        captureDeadline = 0L
        captureSessionId = current.id
        val generation = ++captureGeneration
        captureFeedback = null
        val controller = RapidFirePhysicalCapture()
        physicalCapture = controller
        notifyUpdate()
        captureExecutor.execute {
            val inventory = RootShell.exec(
                "cat /proc/bus/input/devices", timeoutSeconds = 4, maxOutput = 64 * 1024
            )
            val devices = if (inventory.isSuccess) {
                RapidFireInputDetector.discover(inventory.output)
            } else {
                emptyList()
            }
            if (devices.isEmpty()) {
                main.post {
                    finishCaptureFailure(
                        generation, current, "未识别到可安全监听的肩键输入设备；原厂肩键保持不变"
                    )
                }
                return@execute
            }
            val result = controller.run(
                context.applicationContext, devices, left,
                {
                    main.post {
                        if (generation != captureGeneration || destroyed) return@post
                        captureDeadline = System.currentTimeMillis() +
                            RapidFirePhysicalCapture.CAPTURE_MS
                        notifyUpdate()
                    }
                },
                {
                    main.post {
                        if (generation != captureGeneration || destroyed) return@post
                        captureFeedback = "已收到" + (if (left) "左" else "右") +
                            "肩键的按下和松开，正在结束采集并恢复原状态…"
                        notifyUpdate()
                    }
                }
            )
            val capture = result.capture
            main.post {
                if (generation != captureGeneration ||
                    !isCurrentSession(current) || destroyed
                ) return@post
                captureInFlight = false
                physicalCapture = null
                captureDeadline = 0L
                captureSessionId = null
                if (current.isExpired(System.currentTimeMillis())) {
                    notifyUpdate()
                    return@post
                }
                if (capture == null) {
                    captureFeedback = result.message
                    toast(result.message)
                    notifyUpdate()
                    return@post
                }
                if (!left && capture.code == current.physicalLeft) {
                    captureFeedback = "左右肩键输入码冲突，请确认本次只触摸右肩键后重试"
                    toast(captureFeedback ?: return@post)
                    notifyUpdate()
                    return@post
                }
                val updated = if (left) {
                    current.withLeft(capture.code, current.upperLeft, current.systemLeft)
                } else {
                    current.withRight(capture.code, current.upperRight, current.systemRight)
                }
                captureFeedback = "已采集" + (if (left) "左" else "右") +
                    "肩键的触摸/松开事件，肩键已恢复原状态"
                executor.execute {
                    persistSession(updated, null)
                    main.post {
                        if (destroyed || !isCurrentSession(updated)) return@post
                        session = updated
                        toast(captureFeedback ?: return@post)
                        notifyUpdate()
                    }
                }
            }
        }
    }

    private fun verifySide(current: RapidFireCompatibility.Session, left: Boolean) {
        executor.execute {
            val side = if (left) "left" else "right"
            val system = diagnosticCode(
                readGlobal(RapidFireCompatibility.EVIDENCE_TEST_SYSTEM_PREFIX + side),
                current.id
            )
            val routes = RapidFireRouteEvidence.parse(readRoutes(side))
            val expectedPhase = if (left) "WAIT_LEFT" else "WAIT_RIGHT"
            val upper = if (routes != null && routes.matches(current.id, expectedPhase)) {
                routes.resolve(system)
            } else {
                -1
            }
            val physical = if (left) current.physicalLeft else current.physicalRight
            val nativeApplied = system > 0 && (
                nativeWitnessApplied(
                    readGlobal(RapidFireCompatibility.EVIDENCE_TEST_NATIVE_PREFIX + side),
                    current.id, side
                ) || nativeLogContains(system, current.createdAt)
                )
            if (physical <= 0 || upper <= 0 || system <= 0 || !nativeApplied) {
                main.post {
                    if (!isCurrentSession(current) || destroyed) return@post
                    captureFeedback =
                        if (routes != null && routes.matches(current.id, expectedPhase) &&
                            routes.isConflicted()
                        ) {
                            "同一测试阶段出现不唯一的上下层对应关系，未放行；请取消本次测试后重新开始"
                        } else {
                            "尚未取得当前肩键的完整调用对应关系。请在游戏空间重新选择原厂连点，按住" +
                                (if (left) "左" else "右") + "肩键后再检查"
                        }
                    toast(captureFeedback ?: return@post)
                    notifyUpdate()
                }
                return@execute
            }
            if (!cadenceWitnessReady(current, side, system)) {
                main.post {
                    if (!isCurrentSession(current) || destroyed) return@post
                    captureFeedback =
                        "调用链已经接通，还需测量实际连点与松开停止。请再次按住" +
                            (if (left) "左" else "右") + "肩键约 3 秒，松开后再检查。"
                    toast(captureFeedback ?: return@post)
                    notifyUpdate()
                }
                return@execute
            }
            var updated = if (left) {
                current.withLeft(physical, upper, system).withState(
                    RapidFireCompatibility.State.WAIT_RIGHT, System.currentTimeMillis()
                )
            } else {
                current.withRight(physical, upper, system).withState(
                    RapidFireCompatibility.State.VERIFYING, System.currentTimeMillis()
                )
            }
            if (!left && (updated.physicalLeft == updated.physicalRight ||
                    updated.upperLeft == updated.upperRight ||
                    updated.systemLeft == updated.systemRight)
            ) {
                updated = updated.withState(
                    RapidFireCompatibility.State.FAILED, System.currentTimeMillis()
                )
            }
            val finalUpdated = updated
            executor.execute {
                persistSession(finalUpdated, null)
                main.post {
                    if (!isCurrentSession(finalUpdated) || destroyed) return@post
                    session = finalUpdated
                    captureFeedback =
                        if (finalUpdated.state == RapidFireCompatibility.State.FAILED) {
                            "左右键映射冲突，测试失败"
                        } else if (left) {
                            "左键三层调用已通过，请继续右键"
                        } else {
                            "双侧已通过，开始 10 秒稳定验证"
                        }
                    toast(captureFeedback ?: return@post)
                    notifyUpdate()
                }
            }
        }
    }

    private fun finishVerification(current: RapidFireCompatibility.Session) {
        val now = System.currentTimeMillis()
        val elapsed = now - current.verifyingSince
        if (elapsed < RapidFireCompatibility.STABILITY_REQUIRED_MS) {
            toast(
                "还需等待约 " +
                    ((RapidFireCompatibility.STABILITY_REQUIRED_MS - elapsed + 999L) / 1000L) +
                    " 秒"
            )
            return
        }
        executor.execute {
            // stabilityWitnessReady 经 root shell 读取,必须离开主线程
            if (!stabilityWitnessReady(current)) {
                main.post {
                    if (!destroyed) toast("system_server 尚未返回完整的 10 秒稳定证明，请稍后再试")
                }
                return@execute
            }
            val safe = !readFuseTripped() && systemHookIsCurrent() && gameSpaceHookIsCurrent()
            val fp = fingerprint
            val issued = if (safe && fp != null) {
                RapidFireCompatibility.Token.issue(
                    fp,
                    current.physicalLeft, current.upperLeft, current.systemLeft,
                    current.physicalRight, current.upperRight, current.systemRight, now
                )
            } else {
                null
            }
            val completed = current.withState(
                if (issued == null) {
                    RapidFireCompatibility.State.FAILED
                } else {
                    RapidFireCompatibility.State.PASSED
                },
                now
            )
            persistSession(completed, issued)
            main.post {
                if (destroyed || !isCurrentSession(completed)) return@post
                session = completed
                token = issued
                toast(
                    if (issued == null) "稳定验证失败，功能保持锁定"
                    else "兼容性测试通过；开关已解锁但未自动开启"
                )
                notifyUpdate()
            }
        }
    }

    // ---- evidence readers (run on the executor; each uses a root shell) ----

    private fun readFuseTripped(): Boolean {
        val fuse = RootShell.exec(
            "settings get global ${RapidFireCrashFuse.FUSED}",
            timeoutSeconds = 5, maxOutput = 1024
        )
        return fuse.isSuccess && fuse.output.trim() == "1"
    }

    private fun readGlobal(key: String): String {
        val result = RootShell.exec(
            "settings get global $key", timeoutSeconds = 5, maxOutput = 4096
        )
        if (!result.isSuccess) return ""
        val value = result.output.trim()
        return if (value == "null") "" else value
    }

    private fun systemHookIsCurrent(): Boolean {
        val value = readGlobal(RapidFireCompatibility.EVIDENCE_SYSTEM_INSTALLED)
        if (!value.contains("|module=" + BuildConfig.VERSION_NAME)) return false
        val marker = value.indexOf("|pid=")
        if (marker < 0) return false
        val end = value.indexOf('|', marker + 5)
        val expected = if (end < 0) value.substring(marker + 5) else value.substring(marker + 5, end)
        if (!expected.matches(Regex("[0-9]+"))) return false
        val process = RootShell.exec("pidof system_server", timeoutSeconds = 4, maxOutput = 1024)
        if (!process.isSuccess) return false
        return process.output.trim().split(Regex("\\s+")).any { it == expected }
    }

    private fun gameSpaceHookIsCurrent(): Boolean =
        logcatGrep(RapidFireCompatibility.LOG_GAMESPACE_INSTALLED)
            .contains("|module=" + BuildConfig.VERSION_NAME + "|")

    private fun readRoutes(side: String): String {
        val output = logcatGrep(RapidFireRouteEvidence.DIAGNOSTIC_PREFIX + side)
        var latest: String? = null
        for (line in output.split(Regex("\\r?\\n"))) {
            val start = line.indexOf("RFR1|")
            if (start >= 0) latest = line.substring(start).trim()
        }
        return latest ?: ""
    }

    private fun nativeLogContains(systemCode: Int, afterMillis: Long): Boolean {
        val output = logcatGrep("NATIVE_HIT key=$systemCode")
        val marker = "NATIVE_HIT key=$systemCode cps=" + RapidFireCompatibility.TEST_CPS
        for (line in output.split(Regex("\\r?\\n"))) {
            if (!line.contains(marker)) continue
            val space = line.indexOf(' ')
            try {
                val seconds = (if (space < 0) line else line.substring(0, space)).toDouble()
                if ((seconds * 1000.0).toLong() + 2_000L >= afterMillis) return true
            } catch (_: Throwable) {
            }
        }
        return false
    }

    private fun logcatGrep(marker: String): String {
        val result = RootShell.exec(
            "logcat -d -v epoch 2>/dev/null | grep " + RootShell.quote(marker) + " | tail -n 50",
            timeoutSeconds = 6, maxOutput = 128 * 1024
        )
        return result.output
    }

    private fun cadenceWitnessReady(
        current: RapidFireCompatibility.Session,
        side: String,
        code: Int
    ): Boolean {
        val value = readGlobal(RapidFireCompatibility.EVIDENCE_TEST_CADENCE_PREFIX + side)
        return value.contains("id=" + current.id + "|") &&
            value.contains("|phase=" + current.state.name + "|") &&
            value.contains("|ready=1|") &&
            value.contains("|key=$code|") &&
            value.contains("|passed=1|")
    }

    private fun stabilityWitnessReady(current: RapidFireCompatibility.Session): Boolean {
        val witness = readGlobal(RapidFireCompatibility.EVIDENCE_TEST_STABILITY)
        return witness.contains("id=" + current.id + "|") &&
            witness.contains("|complete=1|") &&
            nativeWitnessApplied(witness, current.id, "left") &&
            nativeWitnessApplied(witness, current.id, "right")
    }

    // ---- persistence ----

    /**
     * Session writes go to Settings.Global synchronously FIRST (system_server
     * observes this exact key), then to local prefs (mirrored into the remote
     * preferences for the gamelauncher hook by HelperApplication).
     */
    private fun persistSession(
        value: RapidFireCompatibility.Session?,
        issued: RapidFireCompatibility.Token?,
        resetCps: Boolean = false,
        clearToken: Boolean = false
    ) {
        RootShell.exec(
            "settings put global ${RapidFireCompatibility.GLOBAL_SESSION_KEY} " +
                RootShell.quote(value?.serialize() ?: ""),
            timeoutSeconds = 6, maxOutput = 1024
        )
        SafeSP.putAny(Pref.Key.GameSpace.TGK_RAPID_FIRE_ENABLED, false)
        SafeSP.putAny(
            Pref.Key.GameSpace.TGK_RAPID_FIRE_TEST_SESSION, value?.serialize() ?: ""
        )
        // Mid-test persists must not touch the token (LS_Augment semantics);
        // only preflight/cancel/fuse-clear explicitly reset it.
        if (issued != null) {
            SafeSP.putAny(Pref.Key.GameSpace.TGK_RAPID_FIRE_COMPAT_TOKEN, issued.serialize())
        } else if (clearToken) {
            SafeSP.putAny(Pref.Key.GameSpace.TGK_RAPID_FIRE_COMPAT_TOKEN, "")
        }
        if (resetCps) {
            SafeSP.putAny(
                Pref.Key.GameSpace.TGK_RAPID_FIRE_CPS, RapidFireCompatibility.TEST_CPS
            )
        }
    }

    // ---- small helpers ----

    private fun stopCapture() {
        physicalCapture?.cancel()
        physicalCapture = null
        captureGeneration++
        captureInFlight = false
        captureDeadline = 0L
        captureSessionId = null
    }

    private fun finishCaptureFailure(
        generation: Int,
        current: RapidFireCompatibility.Session,
        message: String
    ) {
        if (generation != captureGeneration || !isCurrentSession(current) || destroyed) return
        captureInFlight = false
        physicalCapture = null
        captureDeadline = 0L
        captureSessionId = null
        captureFeedback = message
        toast(message)
        notifyUpdate()
    }

    private fun isCurrentSession(value: RapidFireCompatibility.Session): Boolean =
        session?.id == value.id

    private fun packageInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Throwable) {
        false
    }

    private fun nativeLibraryPresent(): Boolean = try {
        val directory = context.applicationInfo.nativeLibraryDir
        directory != null && File(directory, "librmh_tgk.so").isFile
    } catch (_: Throwable) {
        false
    }

    private fun notifyUpdate() {
        updateListener?.onUpdate()
    }

    private fun toast(message: String) {
        toastListener?.invoke(message)
    }

    companion object {
        private fun diagnosticCode(value: String, sessionId: String): Int {
            if (value.isEmpty() || !value.contains("id=$sessionId|")) return -1
            var start = value.indexOf("|code=")
            if (start < 0) return -1
            start += 6
            val end = value.indexOf('|', start)
            return try {
                (if (end < 0) value.substring(start) else value.substring(start, end)).toInt()
            } catch (_: Throwable) {
                -1
            }
        }

        private fun nativeWitnessApplied(value: String, sessionId: String, side: String): Boolean {
            if (value.isEmpty() || !value.contains("id=$sessionId|")) return false
            val field = if ("left" == side) "left_applied=" else "right_applied="
            var start = value.indexOf(field)
            if (start < 0) return false
            start += field.length
            val end = value.indexOf('|', start)
            return try {
                (if (end < 0) value.substring(start) else value.substring(start, end)).toLong() > 0L
            } catch (_: Throwable) {
                false
            }
        }
    }
}
