package dev.lackluster.redmagichelper.hook.rules.gamespace

import android.app.Application
import android.content.Context
import android.util.Log
import dev.lackluster.redmagichelper.BuildConfig
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.XposedEnv
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.natives.RapidFireCompatibility
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.rapidfire.RapidFireRouteEvidence
import io.github.libxposed.api.XposedInterface

/**
 * 肩键极速连点 — 游戏空间侧(移植自 LS_Augment 的 hook/TgkRapidFireHook.java)。
 *
 * 把用户在模块里选择的 CPS 从 GameSpace 传输到 InputManager:原厂 UI 只暴露
 * 2/5/10 三档,真正突破上限的间隔改写在 system_server 的原生层完成。
 *
 * 与 LS_Augment 的差异:配置经 [Prefs](libxposed 远程配置,每次调用实时读取)获取,
 * 路由证据(RapidFireRouteEvidence)通过 logcat 发布(本项目无 config provider),
 * 由模块 app 侧的 RapidFireCompatTester 用 root 读回。
 *
 * 注意:本 hooker 不用 hasEnable 在加载期门控 —— 兼容性测试要求 Hook 在功能开关
 * 关闭时也已就位,开关判定在每次调用时进行(与 LS_Augment 一致)。加载期门控由
 * HookEntry 的 gameFunctionUnfrozen 承担。
 */
object TgkRapidFireHook : YukiBaseHooker() {

    private const val TAG = "TgkRapidFireHook"
    private const val EVIDENCE_TAG = "RmhTgkRapidFire"
    private const val PROXY = "cn.nubia.tgk.proxy.InputManagerProxy"
    private const val TRANSPORT = "android.hardware.input.IInputManager\$Stub\$Proxy"

    @Volatile
    private var lastInstalledPublish = 0L

    @Volatile
    private var lastDiagnostic = 0L

    @Volatile
    private var installedDescriptor: String? = null

    @Volatile
    private var installError: String? = null

    private val routeCalls = RapidFireRouteEvidence.Calls()
    private val routeEvidenceLock = Any()
    private var routeEvidence: RapidFireRouteEvidence? = null

    override fun onHook() {
        runCatching { install() }.onFailure {
            installError = "module=" + BuildConfig.VERSION_NAME + "|" +
                it.javaClass.simpleName + ":" + safe(it.message)
            YLog.error(tag = TAG, msg = "TGK_RAPID_FIRE_FAILED", e = it)
        }
    }

    private fun install() {
        val classLoader = appClassLoader
        val proxy = Class.forName(PROXY, false, classLoader)
        val method = proxy.getDeclaredMethod(
            "setTgkRapidFireCount", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        // This is the actual outgoing Binder proxy, after any vendor translation.
        // A ThreadLocal ties its code to the enclosing upper call, even when the
        // OEM refreshes both keys sequentially or concurrently.
        val transport = Class.forName(TRANSPORT, false, classLoader)
        val transportMethod = transport.getDeclaredMethod(
            "setTgkRapidFireCount", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
        )
        transportMethod.isAccessible = true
        val transportDeoptimized = XposedEnv.module.deoptimize(transportMethod)
        XposedEnv.module.deoptimize(method)
        XposedEnv.module.hook(transportMethod).intercept { chain -> interceptTransport(chain) }
        XposedEnv.module.hook(method).intercept { chain -> interceptCount(chain) }
        installedDescriptor = method.toGenericString() +
            "|route=binder_proxy|route_deopt=" + transportDeoptimized +
            "|module=" + BuildConfig.VERSION_NAME
        Log.i(EVIDENCE_TAG, RapidFireCompatibility.LOG_GAMESPACE_INSTALLED + " " +
            installedDescriptor)
        YLog.info(tag = TAG, msg = "TGK_RAPID_FIRE_INSTALLED ${method.toGenericString()}")
    }

    private fun interceptTransport(chain: XposedInterface.Chain): Any? {
        val trace = routeCalls.current()
        try {
            val result = chain.proceed()
            if (trace != null) {
                trace.observe((chain.getArg(1) as? Number)?.toInt() ?: -1)
            }
            return result
        } catch (error: Throwable) {
            trace?.reject()
            throw error
        }
    }

    private fun interceptCount(chain: XposedInterface.Chain): Any? {
        val keyCode = (chain.getArg(1) as? Number)?.toInt() ?: -1
        if (keyCode <= 0 || keyCode > 65535) return chain.proceed()

        val context = contextFrom(chain.thisObject)
        publishInstalled()
        val old = (chain.getArg(0) as? Number)?.toInt() ?: -1
        // The OEM sends zero (or a negative sentinel on some builds) when a
        // rapid-fire mapping is being cleared. Preserve that lifecycle signal;
        // only positive mode values are transport requests.
        if (old <= 0) return chain.proceed()

        val now = System.currentTimeMillis()
        val session = RapidFireCompatibility.Session.parse(
            Prefs.getString(Pref.Key.GameSpace.TGK_RAPID_FIRE_TEST_SESSION, "")
        )
        if (session != null && testWindowActive(session, now, context)) {
            if (session.state == RapidFireCompatibility.State.WAIT_LEFT ||
                session.state == RapidFireCompatibility.State.WAIT_RIGHT
            ) {
                routeCalls.begin().use { call ->
                    val result =
                        if (old == RapidFireCompatibility.TEST_CPS) chain.proceed()
                        else chain.proceed(arrayOf(RapidFireCompatibility.TEST_CPS, keyCode))
                    val systemCode = call.trace.resolvedSystem()
                    if (systemCode > 0) {
                        recordRapidRoute(session.id, session.state.name, keyCode, systemCode)
                    } else {
                        YLog.debug(
                            tag = TAG,
                            msg = "test_route_error id=" + session.id +
                                "|phase=" + session.state.name + "|upper=" + keyCode +
                                "|reason=no_unique_synchronous_transport"
                        )
                    }
                    return result
                }
            }
            if (sideForUpper(session, keyCode) != null) {
                return if (old == RapidFireCompatibility.TEST_CPS) chain.proceed()
                else chain.proceed(arrayOf(RapidFireCompatibility.TEST_CPS, keyCode))
            }
        }

        val token = RapidFireCompatibility.Token.parse(
            Prefs.getString(Pref.Key.GameSpace.TGK_RAPID_FIRE_COMPAT_TOKEN, "")
        )
        val enabled = Prefs.getBoolean(Pref.Key.GameSpace.TGK_RAPID_FIRE_ENABLED, false)
        if (!enabled || token == null ||
            !token.validFor(RapidFireCompatibility.currentFingerprint(context)) ||
            !token.acceptsUpper(keyCode)
        ) {
            return chain.proceed()
        }
        val count = Prefs.getInt(Pref.Key.GameSpace.TGK_RAPID_FIRE_CPS, 20).coerceIn(10, 50)
        // Always send the current value. The OEM TGK cache can still contain a
        // previous higher value after the user lowers the slider; retaining the
        // old >= count shortcut would make the system_server layer see a stale
        // target and keep the previous speed active.
        if (old == count) return chain.proceed()
        hit("key=" + keyCode + "|" + old + "->" + count)
        return chain.proceed(arrayOf(count, keyCode))
    }

    private fun sideForUpper(
        session: RapidFireCompatibility.Session,
        keyCode: Int
    ): String? = when (keyCode) {
        session.upperLeft -> "left"
        session.upperRight -> "right"
        else -> null
    }

    private fun testWindowActive(
        session: RapidFireCompatibility.Session?,
        now: Long,
        context: Context?
    ): Boolean = session != null && session.active(now) &&
        session.validFor(RapidFireCompatibility.currentFingerprint(context)) &&
        (session.state != RapidFireCompatibility.State.VERIFYING ||
            now - session.verifyingSince <= RapidFireCompatibility.STABILITY_REQUIRED_MS)

    private fun recordRapidRoute(sessionId: String, phase: String, upper: Int, system: Int) {
        val evidence = synchronized(routeEvidenceLock) {
            val current = routeEvidence
            val next = if (current != null && current.matches(sessionId, phase)) {
                current
            } else {
                RapidFireRouteEvidence.start(sessionId, phase).also { routeEvidence = it }
            }
            next?.record(upper, system)
            next
        } ?: return
        val side = if (phase == "WAIT_LEFT") "left" else "right"
        Log.i(
            EVIDENCE_TAG,
            RapidFireRouteEvidence.DIAGNOSTIC_PREFIX + side + " " + evidence.serialize()
        )
    }

    private fun hit(value: String) {
        val now = System.currentTimeMillis()
        if (now - lastDiagnostic < 500L) return
        lastDiagnostic = now
        YLog.debug(tag = TAG, msg = "rmh_tgk_rapid_fire_last_hit $value|$now")
    }

    private fun publishInstalled() {
        val error = installError
        if (error != null) {
            YLog.debug(tag = TAG, msg = "rmh_tgk_rapid_fire_last_error $error")
            return
        }
        val descriptor = installedDescriptor ?: return
        val now = System.currentTimeMillis()
        if (now - lastInstalledPublish < 5_000L) return
        lastInstalledPublish = now
        Log.i(EVIDENCE_TAG, RapidFireCompatibility.LOG_GAMESPACE_INSTALLED + " " + descriptor)
    }

    /** Resolve a context from the hooked proxy instance, like LS_Augment's FeatureSettings.from. */
    private fun contextFrom(owner: Any?): Context? {
        if (owner is Context) return owner
        if (owner != null) {
            for (field in arrayOf("mContext", "context", "mApplication", "this$0")) {
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
        return currentApplication()
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

    private fun currentApplication(): Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val method = activityThread.getDeclaredMethod("currentApplication")
        method.isAccessible = true
        method.invoke(null) as? Application
    }.getOrNull()

    private fun safe(value: String?): String =
        value?.replace('|', '_')?.replace('\n', ' ') ?: ""
}
