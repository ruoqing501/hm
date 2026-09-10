package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Process
import android.provider.Settings
import dev.lackluster.redmagichelper.BuildConfig
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.XposedEnv
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.natives.RapidFireCompatibility
import dev.lackluster.redmagichelper.hook.natives.RapidFireCrashFuse
import dev.lackluster.redmagichelper.hook.natives.TgkRapidFireNative
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

/**
 * 肩键极速连点 — system_server 侧(移植自 LS_Augment 的 hook/TgkRapidFireSystemHook.java)。
 *
 * 把 GameSpace 的 TGK 请求桥接到 system_server 的原生 EventProducer 配置;原生层只在
 * 请求确实超过原厂 10 CPS 上限时才加载。三层入口:InputManagerService 包装方法、
 * NativeInputManagerService$NativeImpl(应对 ART 内联)、IInputManager$Stub.onTransact
 * (动态解析 TRANSACTION,应对两层都被内联)。
 *
 * 与 LS_Augment 的差异(本项目无 config provider):
 * - 开关/CPS/令牌经 [Prefs](libxposed 远程配置)实时读取,等价于原快照缓存失效后的读取;
 * - 测试会话经 Settings.Global [RapidFireCompatibility.GLOBAL_SESSION_KEY] 传输,
 *   用 ContentObserver 替代原 provider 的快照监听(模块 app 侧 root 同步写入,无时序竞争);
 * - 诊断/测试证据写入 Settings.Global(应用侧经 root `settings get` 读回)。
 */
object TgkRapidFireSystemHook : YukiBaseHooker() {

    private const val TAG = "TgkRapidFireSystemHook"
    private const val INPUT_MANAGER_SERVICE = "com.android.server.input.InputManagerService"
    private const val NATIVE_INPUT_MANAGER_IMPL =
        "com.android.server.input.NativeInputManagerService\$NativeImpl"
    private const val INPUT_MANAGER_STUB = "android.hardware.input.IInputManager\$Stub"
    private const val INPUT_MANAGER_DESCRIPTOR = "android.hardware.input.IInputManager"
    private const val GLOBAL_LAST_ERROR = "rmh_tgk_rapid_fire_native_last_error"
    private const val GLOBAL_SNAPSHOT_SEEN = "rmh_tgk_rapid_fire_snapshot_seen"
    private const val OEM_MAX_CPS = 10
    private const val MAX_CPS = 50

    @Volatile
    private var lastDiagnostic = 0L

    @Volatile
    private var lastInstalledPublish = 0L

    @Volatile
    private var installedDescriptor: String? = null

    @Volatile
    private var observerInstalled = false

    @Volatile
    private var contextReadyAttempts = 0

    @Volatile
    private var observationSessionKey = ""

    @Volatile
    private var observationBaseline = 0L

    @Volatile
    private var observationPollScheduled = false

    private var testCleanupHandler: Handler? = null
    private var testCleanup: Runnable? = null
    private val intercepting = ThreadLocal<Boolean>()
    private val noAfterCall = Runnable { }

    override fun onHook() {
        // 与游戏空间侧共用"解除游戏功能限制"开关作为加载期门控;
        // 功能开关/令牌判定在每次调用时进行(兼容性测试要求 Hook 提前就位)。
        hasEnable(Pref.Key.GameSpace.GAME_FUCTION_UNFREEZE_SWITCH) {
            runCatching { install() }
                .onFailure { YLog.error(tag = TAG, msg = "TGK_RAPID_FIRE_SYSTEM_FAILED", e = it) }
        }
    }

    private fun install() {
        val classLoader = appClassLoader
        val service = Class.forName(INPUT_MANAGER_SERVICE, false, classLoader)
        val entryPoints = ArrayList<String>()
        var installed = 0

        val method = findMethod(
            service, "setTgkRapidFireCount",
            Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
        )
        if (method != null) {
            method.isAccessible = true
            val deoptimized = XposedEnv.module.deoptimize(method)
            XposedEnv.module.hook(method).intercept { chain -> intercept(chain) }
            installed++
            entryPoints.add(method.toGenericString() + ":deopt=" + deoptimized)
        }

        // Some ART builds inline the tiny InputManagerService wrapper. Hook the
        // concrete native bridge too; the ThreadLocal prevents handling the same
        // call twice when the wrapper was not inlined.
        try {
            val nativeImpl = Class.forName(NATIVE_INPUT_MANAGER_IMPL, false, classLoader)
            val nativeMethod = findMethod(
                nativeImpl, "setTgkRapidFireCount",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )
            if (nativeMethod != null) {
                nativeMethod.isAccessible = true
                XposedEnv.module.hook(nativeMethod).intercept { chain -> intercept(chain) }
                installed++
                entryPoints.add(nativeMethod.toGenericString())
            }
        } catch (error: Throwable) {
            YLog.info(tag = TAG, msg = "TGK_NATIVE_BRIDGE_UNAVAILABLE ${error.javaClass.simpleName}")
        }

        // The generated AIDL dispatcher is the narrowest stable boundary that
        // cannot be skipped when ART inlines both service wrappers. Resolve the
        // OEM transaction code from its generated field so a different framework
        // build never inherits a hard-coded number.
        try {
            val stub = Class.forName(INPUT_MANAGER_STUB, false, classLoader)
            val transactionField = stub.getDeclaredField("TRANSACTION_setTgkRapidFireCount")
            transactionField.isAccessible = true
            val transactionCode = transactionField.getInt(null)
            val onTransact = findMethod(
                stub, "onTransact",
                Int::class.javaPrimitiveType, Parcel::class.java,
                Parcel::class.java, Int::class.javaPrimitiveType
            )
            if (transactionCode > 0 && onTransact != null) {
                onTransact.isAccessible = true
                XposedEnv.module.hook(onTransact)
                    .intercept { chain -> interceptBinder(chain, transactionCode) }
                installed++
                entryPoints.add(onTransact.toGenericString() + ":transaction=" + transactionCode)
            }
        } catch (error: Throwable) {
            YLog.info(tag = TAG, msg = "TGK_BINDER_ENTRY_UNAVAILABLE ${error.javaClass.simpleName}")
        }

        if (installed == 0) {
            throw NoSuchMethodException(
                "$INPUT_MANAGER_SERVICE/$NATIVE_INPUT_MANAGER_IMPL.setTgkRapidFireCount"
            )
        }
        installedDescriptor = entryPoints.joinToString("+") +
            "|module=" + BuildConfig.VERSION_NAME +
            "|pid=" + Process.myPid()
        initializeWhenContextReady()
        for (lifecycleName in arrayOf("start", "systemRunning")) {
            val lifecycle = findMethod(service, lifecycleName) ?: continue
            lifecycle.isAccessible = true
            XposedEnv.module.hook(lifecycle).intercept { chain ->
                val result = chain.proceed()
                initializeWithContext(contextFrom(chain.thisObject))
                result
            }
            installed++
        }
        YLog.info(tag = TAG, msg = "TGK_RAPID_FIRE_SYSTEM_INSTALLED $installedDescriptor")
    }

    // ---- interceptors ----

    private fun intercept(chain: XposedInterface.Chain): Any? {
        if (intercepting.get() == true) return chain.proceed()
        intercepting.set(true)
        try {
            return interceptOnce(chain)
        } finally {
            intercepting.remove()
        }
    }

    private fun interceptOnce(chain: XposedInterface.Chain): Any? {
        val keyCode = (chain.getArg(1) as? Number)?.toInt() ?: -1
        if (keyCode <= 0 || keyCode > 65535) return chain.proceed()

        val context = contextFrom(chain.thisObject)
        val requested = (chain.getArg(0) as? Number)?.toInt() ?: 0
        val afterCall = beforeCall(context, requested, keyCode)
        val result = chain.proceed()
        afterCall.run()
        return result
    }

    private fun interceptBinder(chain: XposedInterface.Chain, transactionCode: Int): Any? {
        val code = (chain.getArg(0) as? Number)?.toInt() ?: -1
        if (code != transactionCode || intercepting.get() == true) return chain.proceed()
        val values = readRapidFireParcel(chain.getArg(1)) ?: return chain.proceed()
        intercepting.set(true)
        try {
            val context = contextFrom(chain.thisObject)
            val afterCall = beforeCall(context, values[0], values[1])
            val result = chain.proceed()
            afterCall.run()
            return result
        } finally {
            intercepting.remove()
        }
    }

    private fun readRapidFireParcel(parcelValue: Any?): IntArray? {
        if (parcelValue !is Parcel) return null
        val size = parcelValue.dataSize()
        if (size <= 0 || size > 1024) return null
        val copy = Parcel.obtain()
        try {
            copy.appendFrom(parcelValue, 0, size)
            copy.setDataPosition(0)
            copy.enforceInterface(INPUT_MANAGER_DESCRIPTOR)
            val requested = copy.readInt()
            val keyCode = copy.readInt()
            if (Build.VERSION.SDK_INT >= 33) copy.enforceNoDataAvail()
            else if (copy.dataAvail() != 0) return null
            return intArrayOf(requested, keyCode)
        } catch (_: Throwable) {
            return null
        } finally {
            copy.recycle()
        }
    }

    // ---- core decision (port of beforeCall) ----

    private fun beforeCall(context: Context?, requestedValue: Int, keyCode: Int): Runnable {
        if (keyCode <= 0 || keyCode > 65535) return noAfterCall
        publishInstalled(context, false)
        ensureSnapshotObserver(context)
        val requested = requestedValue.coerceIn(0, MAX_CPS)

        val now = System.currentTimeMillis()
        val session = RapidFireCompatibility.Session.parse(sessionString(context))
        if (session != null && testWindowActive(session, now, context)) {
            if (session.state == RapidFireCompatibility.State.WAIT_LEFT ||
                session.state == RapidFireCompatibility.State.WAIT_RIGHT
            ) {
                // A settings refresh may contain BOTH keys. It is not physical-side
                // evidence. Only the native observer selects a side's system code.
                return noAfterCall
            }
            val side = testSide(session, keyCode)
            if (side != null) {
                writeGlobal(
                    context, RapidFireCompatibility.EVIDENCE_TEST_SYSTEM_PREFIX + side,
                    "id=" + session.id + "|code=" + keyCode +
                        "|count=" + requested + "|time=" + now
                )
                if (!TgkRapidFireNative.ensureInstalled(context)) {
                    writeGlobal(
                        context, GLOBAL_LAST_ERROR,
                        "test_fallback|state=" + safe(TgkRapidFireNative.state(context))
                    )
                    return noAfterCall
                }
                TgkRapidFireNative.configureTestKey(context, side, keyCode)
                TgkRapidFireNative.setTarget(context, keyCode, RapidFireCompatibility.TEST_CPS)
                scheduleSessionCleanup(context, session)
                return Runnable { scheduleNativeWitness(context, session, side, keyCode) }
            }
        } else if (session != null && session.state != RapidFireCompatibility.State.PASSED) {
            TgkRapidFireNative.clearTargets(context)
        }

        val token = RapidFireCompatibility.Token.parse(
            Prefs.getString(Pref.Key.GameSpace.TGK_RAPID_FIRE_COMPAT_TOKEN, "")
        )
        val enabled = Prefs.getBoolean(Pref.Key.GameSpace.TGK_RAPID_FIRE_ENABLED, false) &&
            token != null &&
            token.validFor(RapidFireCompatibility.currentFingerprint(context)) &&
            token.acceptsSystem(keyCode) &&
            !RapidFireCrashFuse.isFused(context)
        if (!enabled || token == null) {
            TgkRapidFireNative.clearTargets(context)
            return noAfterCall
        }
        if (requested <= OEM_MAX_CPS) {
            // Clearing the native target is enough to restore the OEM path. The
            // inline hook remains installed for the lifetime of system_server to
            // avoid an unsafe concurrent unhook.
            TgkRapidFireNative.setTarget(context, keyCode, 0)
            return noAfterCall
        }

        YLog.debug(tag = TAG, msg = "rmh_tgk_rapid_fire_native_state java_hit|key=$keyCode|requested=$requested")

        if (!TgkRapidFireNative.ensureInstalled(context)) {
            writeGlobal(
                context, GLOBAL_LAST_ERROR,
                "fallback|state=" + safe(TgkRapidFireNative.state(context))
            )
            return noAfterCall
        }

        TgkRapidFireNative.configureKeys(context, token.systemLeft, token.systemRight)
        TgkRapidFireNative.setTarget(context, keyCode, requested)
        hit(keyCode, requested)
        return noAfterCall
    }

    private fun testSide(session: RapidFireCompatibility.Session, keyCode: Int): String? {
        if (session.state == RapidFireCompatibility.State.VERIFYING) {
            if (keyCode == session.systemLeft) return "left"
            if (keyCode == session.systemRight) return "right"
        }
        return null
    }

    private fun testWindowActive(
        session: RapidFireCompatibility.Session?,
        now: Long,
        context: Context?
    ): Boolean = session != null && session.active(now) &&
        session.validFor(RapidFireCompatibility.currentFingerprint(context)) &&
        (session.state != RapidFireCompatibility.State.VERIFYING ||
            now - session.verifyingSince <= RapidFireCompatibility.STABILITY_REQUIRED_MS)

    // ---- session-change driven reconfiguration (snapshot-listener equivalent) ----

    @Synchronized
    private fun ensureSnapshotObserver(context: Context?) {
        if (observerInstalled || context == null) return
        try {
            val handler = Handler(Looper.getMainLooper())
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(RapidFireCompatibility.GLOBAL_SESSION_KEY),
                false,
                object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean) {
                        runCatching { onSnapshotChanged(context) }
                            .onFailure { YLog.error(tag = TAG, msg = "snapshot change failed", e = it) }
                    }
                }
            )
            observerInstalled = true
        } catch (error: Throwable) {
            YLog.error(tag = TAG, msg = "register session observer failed: $error")
        }
    }

    /** system_server may load the module before ActivityThread exposes a Context. */
    private fun initializeWhenContextReady() {
        val context = contextFrom(null)
        if (context != null) {
            initializeWithContext(context)
            return
        }
        val attempt = ++contextReadyAttempts
        if (attempt > 60) return
        try {
            val looper = Looper.getMainLooper() ?: return
            Handler(looper).postDelayed({ initializeWhenContextReady() }, 1_000L)
        } catch (_: Throwable) {
        }
    }

    private fun initializeWithContext(context: Context?) {
        if (context == null) return
        contextReadyAttempts = 0
        RapidFireCrashFuse.onSystemStart(context)
        ensureSnapshotObserver(context)
        publishInstalled(context, false)
        onSnapshotChanged(context)
    }

    private fun onSnapshotChanged(context: Context?) {
        TgkRapidFireNative.clearTargets(context)
        val now = System.currentTimeMillis()
        val session = RapidFireCompatibility.Session.parse(sessionString(context))
        writeGlobal(
            context, GLOBAL_SNAPSHOT_SEEN,
            "module=" + BuildConfig.VERSION_NAME + "|state=" +
                (session?.state?.name ?: "none") + "|time=" + now
        )
        val activeSession = session != null && session.active(now) &&
            session.validFor(RapidFireCompatibility.currentFingerprint(context))
        if (session != null && activeSession) {
            scheduleSessionCleanup(context, session)
        } else {
            cancelSessionCleanup()
        }
        if (session != null && activeSession &&
            (session.state == RapidFireCompatibility.State.WAIT_LEFT ||
                session.state == RapidFireCompatibility.State.WAIT_RIGHT ||
                session.state == RapidFireCompatibility.State.VERIFYING)
        ) {
            if (!TgkRapidFireNative.ensureInstalled(context)) {
                writeGlobal(
                    context, GLOBAL_LAST_ERROR,
                    "test_install_unavailable|state=" + safe(TgkRapidFireNative.state(context))
                )
                return
            }
            if (session.state == RapidFireCompatibility.State.WAIT_LEFT ||
                session.state == RapidFireCompatibility.State.WAIT_RIGHT
            ) {
                beginNativeObservation(context, session)
                return
            }
        }

        if (session != null && activeSession &&
            session.state == RapidFireCompatibility.State.VERIFYING &&
            session.verifyingSince > 0L &&
            now - session.verifyingSince <= RapidFireCompatibility.STABILITY_REQUIRED_MS &&
            session.systemLeft > 0 && session.systemLeft <= 65535 &&
            session.systemRight > 0 && session.systemRight <= 65535 &&
            session.systemLeft != session.systemRight &&
            TgkRapidFireNative.isLoaded
        ) {
            TgkRapidFireNative.configureKeys(context, session.systemLeft, session.systemRight)
            TgkRapidFireNative.setTarget(
                context, session.systemLeft, RapidFireCompatibility.TEST_CPS
            )
            TgkRapidFireNative.setTarget(
                context, session.systemRight, RapidFireCompatibility.TEST_CPS
            )
            scheduleStabilityWitness(context, session)
            return
        }

        val token = RapidFireCompatibility.Token.parse(
            Prefs.getString(Pref.Key.GameSpace.TGK_RAPID_FIRE_COMPAT_TOKEN, "")
        )
        val enabled = Prefs.getBoolean(Pref.Key.GameSpace.TGK_RAPID_FIRE_ENABLED, false) &&
            token != null &&
            token.validFor(RapidFireCompatibility.currentFingerprint(context)) &&
            !RapidFireCrashFuse.isFused(context)
        if (!enabled || token == null) {
            resetNativeObservation()
            return
        }
        if (!TgkRapidFireNative.ensureInstalled(context)) return
        TgkRapidFireNative.configureKeys(context, token.systemLeft, token.systemRight)
        val count = Prefs.getInt(Pref.Key.GameSpace.TGK_RAPID_FIRE_CPS, 20).coerceIn(10, MAX_CPS)
        TgkRapidFireNative.setTarget(context, token.systemLeft, count)
        TgkRapidFireNative.setTarget(context, token.systemRight, count)
    }

    // ---- native observation (compatibility test only) ----

    @Synchronized
    private fun beginNativeObservation(context: Context?, session: RapidFireCompatibility.Session?) {
        if (context == null || session == null || !TgkRapidFireNative.isLoaded) return
        val key = session.id + "|" + session.state.name
        if (key != observationSessionKey) {
            val observation = NativeObservation.parse(TgkRapidFireNative.observation())
            observationSessionKey = key
            observationBaseline = observation?.sequence ?: 0L
        }
        if (observationPollScheduled) return
        val looper = Looper.getMainLooper() ?: return
        observationPollScheduled = true
        Handler(looper).postDelayed({ pollNativeObservation(context) }, 100L)
    }

    private fun pollNativeObservation(context: Context) {
        synchronized(this) {
            observationPollScheduled = false
        }
        val now = System.currentTimeMillis()
        val session = RapidFireCompatibility.Session.parse(sessionString(context))
        if (session == null || !session.active(now) ||
            !session.validFor(RapidFireCompatibility.currentFingerprint(context)) ||
            (session.state != RapidFireCompatibility.State.WAIT_LEFT &&
                session.state != RapidFireCompatibility.State.WAIT_RIGHT)
        ) {
            resetNativeObservation()
            return
        }
        val expectedKey = session.id + "|" + session.state.name
        if (expectedKey != observationSessionKey) {
            beginNativeObservation(context, session)
            return
        }
        val observation = NativeObservation.parse(TgkRapidFireNative.observation())
        if (observation != null && observation.sequence > observationBaseline &&
            observation.keyCode > 0 && observation.keyCode <= 65535
        ) {
            val side =
                if (session.state == RapidFireCompatibility.State.WAIT_LEFT) "left" else "right"
            writeGlobal(
                context, RapidFireCompatibility.EVIDENCE_TEST_SYSTEM_PREFIX + side,
                "id=" + session.id + "|code=" + observation.keyCode +
                    "|count=" + RapidFireCompatibility.TEST_CPS +
                    "|source=native_observer|time=" + now
            )
            TgkRapidFireNative.configureTestKey(context, side, observation.keyCode)
            TgkRapidFireNative.setTarget(
                context, observation.keyCode, RapidFireCompatibility.TEST_CPS
            )
            TgkRapidFireNative.armCadence(session.id, session.state.name, observation.keyCode)
            scheduleCadenceWitness(context, session, side)
            if (!replayRapidFireCall(observation.keyCode)) {
                writeGlobal(
                    context, GLOBAL_LAST_ERROR,
                    "test_replay_failed|key=" + observation.keyCode
                )
            }
            scheduleNativeWitness(context, session, side, observation.keyCode)
            return
        }
        beginNativeObservation(context, session)
    }

    private fun replayRapidFireCall(keyCode: Int): Boolean {
        if (keyCode <= 0 || keyCode > 65535) return false
        try {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val binder = serviceManager.getMethod("getService", String::class.java)
                .invoke(null, "input") ?: return false
            val stub = Class.forName(INPUT_MANAGER_STUB)
            val service = stub.getMethod("asInterface", android.os.IBinder::class.java)
                .invoke(null, binder) ?: return false
            val method = service.javaClass.getMethod(
                "setTgkRapidFireCount",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )
            intercepting.set(true)
            try {
                method.invoke(service, RapidFireCompatibility.TEST_CPS, keyCode)
            } finally {
                intercepting.remove()
            }
            return true
        } catch (_: Throwable) {
            intercepting.remove()
            return false
        }
    }

    private fun scheduleCadenceWitness(
        context: Context?,
        expected: RapidFireCompatibility.Session,
        side: String
    ) {
        val handler = Handler(Looper.getMainLooper())
        val task = object : Runnable {
            override fun run() {
                val current = RapidFireCompatibility.Session.parse(sessionString(context))
                if (current == null || current.id != expected.id ||
                    current.state != expected.state ||
                    !current.active(System.currentTimeMillis())
                ) return
                val evidence = TgkRapidFireNative.cadence()
                writeGlobal(
                    context, RapidFireCompatibility.EVIDENCE_TEST_CADENCE_PREFIX + side,
                    "id=" + current.id + "|phase=" + current.state.name + "|" + evidence +
                        "|pid=" + Process.myPid() + "|time=" + System.currentTimeMillis()
                )
                if (!evidence.endsWith("|passed=1")) handler.postDelayed(this, 500)
            }
        }
        handler.postDelayed(task, 500)
    }

    @Synchronized
    private fun resetNativeObservation() {
        observationSessionKey = ""
        observationBaseline = 0L
    }

    private class NativeObservation(val keyCode: Int, val sequence: Long) {
        companion object {
            fun parse(value: String?): NativeObservation? {
                if (value.isNullOrEmpty()) return null
                var keyCode = -1
                var sequence = -1L
                for (part in value.split("|")) {
                    try {
                        if (part.startsWith("key=")) {
                            keyCode = part.substring(4).toInt()
                        } else if (part.startsWith("sequence=")) {
                            sequence = part.substring(9).toLong()
                        }
                    } catch (_: Throwable) {
                        return null
                    }
                }
                return if (sequence >= 0L) NativeObservation(keyCode, sequence) else null
            }
        }
    }

    // ---- witnesses and cleanup ----

    private fun scheduleStabilityWitness(
        context: Context?,
        session: RapidFireCompatibility.Session
    ) {
        try {
            val completedAt = session.verifyingSince + RapidFireCompatibility.STABILITY_REQUIRED_MS
            val delay = (completedAt - System.currentTimeMillis()).coerceAtLeast(1L)
            Handler(Looper.getMainLooper()).postDelayed({
                val current = RapidFireCompatibility.Session.parse(sessionString(context))
                if (current == null ||
                    current.state != RapidFireCompatibility.State.VERIFYING ||
                    session.id != current.id ||
                    !current.validFor(RapidFireCompatibility.currentFingerprint(context)) ||
                    System.currentTimeMillis() < completedAt
                ) {
                    // A callback from a completed/cancelled/replaced test no longer
                    // owns the native targets. The latest snapshot owns recovery.
                    return@postDelayed
                }
                if (current.isExpired(System.currentTimeMillis())) {
                    onSnapshotChanged(context)
                    return@postDelayed
                }
                val state = TgkRapidFireNative.state(context)
                TgkRapidFireNative.clearTargets(context)
                writeGlobal(
                    context, RapidFireCompatibility.EVIDENCE_TEST_STABILITY,
                    "id=" + session.id + "|complete=1|pid=" + Process.myPid() +
                        "|state=" + diagnosticState(state) +
                        "|time=" + System.currentTimeMillis()
                )
            }, delay)
        } catch (_: Throwable) {
            TgkRapidFireNative.clearTargets(context)
        }
    }

    @Synchronized
    private fun cancelSessionCleanup() {
        val handler = testCleanupHandler
        val cleanup = testCleanup
        if (handler != null && cleanup != null) handler.removeCallbacks(cleanup)
        testCleanup = null
    }

    @Synchronized
    private fun scheduleSessionCleanup(
        context: Context?,
        scheduled: RapidFireCompatibility.Session
    ) {
        try {
            cancelSessionCleanup()
            val delay = (scheduled.expiresAt - System.currentTimeMillis() + 100L)
                .coerceIn(1L, RapidFireCompatibility.SESSION_MAX_MS)
            val handler = Handler(Looper.getMainLooper())
            val cleanup = Runnable {
                val current = RapidFireCompatibility.Session.parse(sessionString(context))
                if (current == null ||
                    scheduled.id != current.id ||
                    !current.isExpired(System.currentTimeMillis())
                ) return@Runnable
                // Reconcile the latest configuration; never blindly zero an approved
                // feature or a newer test's targets from this old timer.
                onSnapshotChanged(context)
            }
            testCleanupHandler = handler
            testCleanup = cleanup
            handler.postDelayed(cleanup, delay)
        } catch (_: Throwable) {
        }
    }

    private fun scheduleNativeWitness(
        context: Context?,
        session: RapidFireCompatibility.Session?,
        side: String?,
        keyCode: Int
    ) {
        if (context == null || session == null || side == null) return
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                val state = TgkRapidFireNative.state(context)
                writeGlobal(
                    context, RapidFireCompatibility.EVIDENCE_TEST_NATIVE_PREFIX + side,
                    "id=" + session.id + "|code=" + keyCode + "|state=" +
                        diagnosticState(state) + "|time=" + System.currentTimeMillis()
                )
            }, 500L)
        } catch (_: Throwable) {
        }
    }

    // ---- diagnostics ----

    private fun hit(keyCode: Int, cps: Int) {
        val now = System.currentTimeMillis()
        if (now - lastDiagnostic < 500L) return
        lastDiagnostic = now
        YLog.debug(tag = TAG, msg = "rmh_tgk_rapid_fire_native_last_hit key=$keyCode|cps=$cps|$now")
    }

    private fun publishInstalled(context: Context?, force: Boolean) {
        val descriptor = installedDescriptor ?: return
        if (context == null) return
        val now = System.currentTimeMillis()
        if (!force && now - lastInstalledPublish < 5_000L) return
        lastInstalledPublish = now
        writeGlobal(context, RapidFireCompatibility.EVIDENCE_SYSTEM_INSTALLED, descriptor)
    }

    private fun sessionString(context: Context?): String {
        if (context == null) return ""
        return try {
            Settings.Global.getString(
                context.contentResolver, RapidFireCompatibility.GLOBAL_SESSION_KEY
            ) ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    private fun writeGlobal(context: Context?, key: String, value: String) {
        if (context == null) return
        try {
            Settings.Global.putString(context.contentResolver, key, value)
        } catch (_: Throwable) {
            // A diagnostic write must never control or crash a feature.
        }
    }

    private fun findMethod(type: Class<*>, name: String, vararg parameters: Class<*>?): Method? {
        var current: Class<*>? = type
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, *parameters)
            } catch (_: NoSuchMethodException) {
                current = current.superclass
            }
        }
        return null
    }

    private fun contextFrom(owner: Any?): Context? {
        if (owner is Context) return owner
        if (owner != null) {
            for (field in arrayOf("mContext", "context", "this$0")) {
                val value = fieldValue(owner, field)
                if (value is Context) return value
            }
            runCatching {
                val method = owner.javaClass.getMethod("getContext")
                method.isAccessible = true
                val value = method.invoke(owner)
                if (value is Context) return value
            }
        }
        return runCatching { systemContext }.getOrNull()
    }

    private fun fieldValue(owner: Any, name: String): Any? {
        var type: Class<*>? = owner.javaClass
        while (type != null) {
            try {
                val field = type.getDeclaredField(name)
                field.isAccessible = true
                return field.get(owner)
            } catch (_: NoSuchFieldException) {
                type = type.superclass
            } catch (_: Throwable) {
                return null
            }
        }
        return null
    }

    private fun safe(value: String?): String =
        value?.replace('|', '_')?.replace('\n', ' ') ?: ""

    private fun diagnosticState(value: String?): String =
        value?.replace('\n', ' ')?.replace('\r', ' ') ?: ""
}
