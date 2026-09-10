package dev.lackluster.redmagichelper.hook.rules.nubiafan

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.utils.FanCalibrationChannel
import dev.lackluster.redmagichelper.utils.FanCalibrationData
import dev.lackluster.redmagichelper.utils.FanHardwareIdentity
import dev.lackluster.redmagichelper.utils.Prefs
import java.io.File
import java.lang.reflect.Method

/**
 * OEM 会话绑定的风扇档位控制,移植自 LS_Augment 的 FanControlHook(已验证驱动:NX809J)。
 *
 * 语义要点(与 LS_Augment 一致):
 * - 永不写 fan_enable,只跟随原厂风扇会话;OEM 关闭风扇时恢复原档位
 * - 捕获/恢复原厂状态(mode/manual/level),OEM 在轮询间隔内改状态则锁定会话(fail-closed)
 * - 热保护:热状态 >= SEVERE 或电池温度 >= 50°C 立即交还控制
 * - 5 档解限借用原厂 fanMaxSpeed()/notifyCubeFan()/cancelFanFullSpeed()
 * - 配置全部经 Prefs(远程只读)运行时重读,无需重启风扇 app 即可生效;
 *   因此不做安装期 hasEnable 门控——门控会导致运行中改开关必须重启进程才生效
 *
 * 校准结果回传:hook 进程(cn.nubia.fan,系统应用,持有 WRITE_SETTINGS)写入
 * Settings.System(见 [FanCalibrationChannel]),模块 app 侧读取;写失败时退化
 * 为日志输出序列化结果,用户在 UI 手动填入各档转速。
 */
object FanControlHook : YukiBaseHooker() {
    private const val TAG = "FanControlHook"
    private const val CONTROLLER_CLASS = "cn.nubia.fan.policy.FanControllerImpl"
    private const val UTILS_CLASS = "cn.nubia.fan.util.Utils"

    /** 只读:模块永不写 fan_enable。 */
    private const val FAN_ENABLE = "sys/kernel/fan/fan_enable"
    private const val FAN_LEVEL = "sys/kernel/fan/fan_speed_level"
    private const val FAN_RPM = "sys/kernel/fan/fan_speed_count"
    private const val OEM_MANUAL = "fan_state_of_manual"
    private const val OEM_MODE = "fan_state_of_mode"
    private const val POLL_MS = 900L

    /** 功能全关时的慢速轮询,用于感知运行时开关切换(无快照监听器可用)。 */
    private const val IDLE_POLL_MS = 2_000L
    private const val MAX_BATTERY_TENTHS_C = 500

    /** fail-closed 支持机型列表;新机型验证通过后在此追加。 */
    private val SUPPORTED_DEVICES = setOf("NX809J")

    @Volatile
    private var controller: Controller? = null

    override fun onHook() {
        val type = CONTROLLER_CLASS.toClassOrNull() ?: run {
            YLog.warn(tag = TAG, msg = "未找到 $CONTROLLER_CLASS,风扇控制未安装")
            return
        }
        val utils = UTILS_CLASS.toClassOrNull() ?: run {
            YLog.warn(tag = TAG, msg = "未找到 $UTILS_CLASS,风扇控制未安装")
            return
        }
        val writeNode = runCatching {
            utils.getDeclaredMethod("setNodeValue", String::class.java, String::class.java)
                .apply { isAccessible = true }
        }.getOrNull() ?: run {
            YLog.error(tag = TAG, msg = "Utils.setNodeValue 解析失败")
            return
        }
        val readNode = runCatching {
            utils.getDeclaredMethod("readNodeValue", String::class.java).apply { isAccessible = true }
        }.getOrNull() ?: run {
            YLog.error(tag = TAG, msg = "Utils.readNodeValue 解析失败")
            return
        }

        val installedController = Controller(writeNode, readNode) { owner ->
            appContext ?: contextFrom(owner)
        }
        controller = installedController

        type.method {
            name = "start"
            emptyParam()
        }.hook {
            after { controller?.start(instanceOrNull) }
        }
        type.method {
            name = "notifyCubeFan"
            emptyParam()
        }.hook {
            after { controller?.start(instanceOrNull) }
        }
        type.method {
            name = "setFanEnable"
            param(BooleanType)
        }.hook {
            after { controller?.start(instanceOrNull) }
        }
        YLog.info(
            tag = TAG,
            msg = "FAN_CONTROL_READY device=${Build.DEVICE} oem_session_only=true fan_enable_writes=0"
        )
    }

    private enum class ControlKind { NONE, FIXED_LEVEL, OEM_EXTREME, CALIBRATION }

    private class Controller(
        private val writeNode: Method,
        private val readNode: Method,
        private val contextProvider: (Any?) -> Context?,
    ) : Runnable {
        private val lock = Any()
        private var thread: HandlerThread? = null
        private var handler: Handler? = null
        private var context: Context? = null
        private var owner: Any? = null
        private var maxPowerOwned = false
        private var lastOemEnabled = false
        private var sessionLocked = false
        private var validRpmSamples = 0
        private var actualRpm = 0
        private var commandedLevel = -1
        private var capturedLevel = -1
        private var capturedMode = Int.MIN_VALUE
        private var capturedManual = Int.MIN_VALUE
        private var controlKind = ControlKind.NONE
        private var lastDiagnosticAt = 0L
        private var lastCalibrationRequest = ""
        private var activeCalibrationRequest = ""
        private var calibrationSamples: Array<IntArray>? = null
        private var calibrationLevel = 0
        private var calibrationSample = 0
        private var calibrationLevelSince = 0L

        fun start(newOwner: Any?) {
            val newContext = contextProvider(newOwner)
            if (newContext == null || newOwner == null) return
            synchronized(lock) {
                if (owner != null && owner !== newOwner && controlKind != ControlKind.NONE) {
                    safeRelease("vendor_owner_changed", false)
                    sessionLocked = true
                }
                context = newContext.applicationContext
                owner = newOwner
                ensureThreadLocked()
                requestImmediateLocked()
            }
        }

        private fun ensureThreadLocked() {
            if (handler != null) return
            val newThread = HandlerThread("RedMagicHelperFanControl")
            newThread.start()
            thread = newThread
            handler = Handler(newThread.looper)
        }

        private fun requestImmediateLocked() {
            handler?.let {
                it.removeCallbacks(this)
                it.post(this)
            }
        }

        override fun run() {
            var keepRunning = true
            var nextDelayMs = POLL_MS
            try {
                val current = context
                if (current == null || !supportedDevice()) {
                    safeRelease("unsupported_device", false)
                    status("idle;reason=unsupported;device=${Build.DEVICE}", 10_000L)
                    keepRunning = false
                    return
                }

                val fixed = Prefs.getBoolean(Pref.Key.Fan.FIXED_ENABLED, false)
                val unlock = Prefs.getBoolean(Pref.Key.Fan.UNLOCK_MAX, false)
                val request = Prefs.getString(Pref.Key.Fan.CALIBRATION_REQUEST, "") ?: ""
                val validRequest = FanCalibrationData.validRequest(request, System.currentTimeMillis())
                if (controlKind == ControlKind.CALIBRATION &&
                    (!validRequest || request != activeCalibrationRequest)
                ) {
                    safeRelease("calibration_cancelled", true)
                }
                val calibrationPending = validRequest && request != lastCalibrationRequest
                if (!fixed && !unlock && !calibrationPending && controlKind != ControlKind.CALIBRATION) {
                    safeRelease("feature_off", true)
                    status("idle;reason=feature_off", 10_000L)
                    nextDelayMs = IDLE_POLL_MS
                    return
                }

                val enable = readInt(FAN_ENABLE)
                if (!enable.valid || (enable.value != 0 && enable.value != 1)) {
                    failSession("fan_enable_read", false)
                    return
                }
                val oemEnabled = enable.value != 0
                if (!oemEnabled) {
                    // 模块永不写 fan_enable。若此前选定过转速,在 enable 保持 0 时
                    // 恢复捕获的原厂档位,避免下一次 OEM 会话继承模块选定的转速。
                    safeRelease("oem_off", true)
                    lastOemEnabled = false
                    sessionLocked = false
                    validRpmSamples = 0
                    resetCapture()
                    status("armed;waiting_for_oem_fan", 3_000L)
                    return
                }
                if (!lastOemEnabled) {
                    lastOemEnabled = true
                    sessionLocked = false
                    validRpmSamples = 0
                    resetCapture()
                }
                if (sessionLocked) {
                    status("locked;until_next_oem_session", 3_000L)
                    return
                }

                val mode: Int
                val manual: Int
                try {
                    mode = Settings.System.getInt(current.contentResolver, OEM_MODE)
                    manual = Settings.System.getInt(current.contentResolver, OEM_MANUAL)
                } catch (error: Throwable) {
                    failSession("vendor_state_read", false)
                    return
                }
                if (controlKind != ControlKind.NONE &&
                    (mode != capturedMode || manual != capturedManual)
                ) {
                    // OEM 在轮询间隔内取得了控制权,绝不用模块捕获的旧值覆盖它的新状态。
                    safeRelease("vendor_state_changed", false)
                    sessionLocked = true
                    validRpmSamples = 0
                    status("locked;reason=vendor_state_changed;until_next_oem_session", 0L)
                    return
                }
                if (!thermalNormal(current)) {
                    failSession("thermal_unsafe")
                    return
                }

                val rpm = readInt(FAN_RPM)
                if (!rpm.valid || !FanCalibrationData.validRpm(rpm.value)) {
                    failSession("rpm_invalid")
                    return
                }
                actualRpm = rpm.value
                validRpmSamples++
                if (validRpmSamples < 2) {
                    status("armed;rpm_check=$validRpmSamples/2;actual=$actualRpm", 0L)
                    return
                }

                if (calibrationPending || controlKind == ControlKind.CALIBRATION) {
                    if (controlKind != ControlKind.CALIBRATION) {
                        safeRelease("calibration_start", true)
                        if (!captureOemState(mode, manual)) {
                            failSession("calibration_capture_failed")
                            return
                        }
                        lastCalibrationRequest = request
                        activeCalibrationRequest = request
                        controlKind = ControlKind.CALIBRATION
                        calibrationSamples = Array(FanCalibrationData.LEVELS) { IntArray(5) }
                        calibrationLevel = 1
                        calibrationSample = 0
                        calibrationLevelSince = SystemClock.elapsedRealtime()
                        ensureLevel(1)
                    } else {
                        calibrationStep()
                    }
                    return
                }

                if (fixed) {
                    val measurement = FanCalibrationData.parse(
                        Prefs.getString(Pref.Key.Fan.MEASUREMENT, "")
                    )
                    if (measurement == null || !measurement.currentFor(FanHardwareIdentity.current())) {
                        safeRelease("measurement_required", true)
                        status("armed;reason=measurement_required;actual=$actualRpm", 3_000L)
                        return
                    }
                    val allowLevelFive = unlock && mode == 0
                    if (controlKind != ControlKind.FIXED_LEVEL) {
                        safeRelease("control_mode_change", true)
                        if (!captureOemState(mode, manual)) {
                            failSession("capture_failed")
                            return
                        }
                        controlKind = ControlKind.FIXED_LEVEL
                    }
                    val requested = Prefs.getInt(Pref.Key.Fan.TARGET_RPM, 12_000)
                        .coerceIn(500, 100_000)
                    val level = targetLevel(requested, allowLevelFive, measurement)
                    val applied = ensureLevel(level)
                    val effective = effectiveTargetRpm(requested, allowLevelFive, measurement)
                    status(
                        (if (applied) "controlled" else "applying") +
                            ";kind=fixed;target=$effective;actual=$actualRpm;level=$level" +
                            ";measured=${measurement.rpm(level)};oem_extreme=${mode == 0}",
                        3_000L
                    )
                } else if (shouldForceUnlockedExtreme(unlock, mode)) {
                    if (controlKind != ControlKind.OEM_EXTREME) {
                        safeRelease("control_mode_change", true)
                        if (!captureOemState(mode, manual)) {
                            failSession("capture_failed")
                            return
                        }
                        controlKind = ControlKind.OEM_EXTREME
                    }
                    val applied = ensureLevel(UNLOCKED_MAX_LEVEL)
                    status(
                        (if (applied) "controlled" else "applying") +
                            ";kind=oem_extreme;level=5;actual=$actualRpm",
                        3_000L
                    )
                } else {
                    safeRelease("waiting_for_oem_extreme", true)
                    status("armed;waiting_for_oem_extreme", 3_000L)
                }
            } catch (error: Throwable) {
                failSession("control_exception")
                report("loop", error)
            } finally {
                synchronized(lock) {
                    val currentHandler = handler
                    if (keepRunning && currentHandler != null) {
                        currentHandler.postDelayed(this, nextDelayMs)
                    } else {
                        handler = null
                        thread?.quitSafely()
                        thread = null
                    }
                }
            }
        }

        private fun calibrationStep() {
            if (calibrationLevel == UNLOCKED_MAX_LEVEL && !maxPowerOwned) {
                activateMaxPower()
                calibrationLevelSince = SystemClock.elapsedRealtime()
            }
            if (!ensureLevel(calibrationLevel)) {
                calibrationLevelSince = SystemClock.elapsedRealtime()
                return
            }
            if (SystemClock.elapsedRealtime() - calibrationLevelSince < 3_000L) return
            val samples = calibrationSamples ?: return
            samples[calibrationLevel - 1][calibrationSample++] = actualRpm
            status(
                "measuring;level=$calibrationLevel;sample=$calibrationSample/5;actual=$actualRpm",
                0L
            )
            if (calibrationSample < 5) return
            if (calibrationLevel < UNLOCKED_MAX_LEVEL) {
                calibrationLevel++
                calibrationSample = 0
                calibrationLevelSince = SystemClock.elapsedRealtime()
                ensureLevel(calibrationLevel)
                return
            }
            val data = FanCalibrationData.fromSamples(
                FanHardwareIdentity.current(), System.currentTimeMillis(), samples
            )
            safeRelease("calibration_complete", true)
            if (data == null) {
                failSession("measurement_unstable")
                return
            }
            val current = context
            val saved = current != null && runCatching {
                Settings.System.putString(
                    current.contentResolver,
                    FanCalibrationChannel.MEASUREMENT_KEY,
                    data.serialize()
                )
            }.getOrDefault(false)
            if (!saved) {
                // 退化方案:序列化结果进日志,用户可在 UI 手动填入各档转速
                YLog.error(tag = TAG, msg = "校准结果写入 Settings.System 失败,可手动填入: ${data.serialize()}")
                failSession("measurement_save_failed")
                return
            }
            status("measured;highest_level=5;median=${data.rpm(5)};peak=${data.peak(5)};restored=1", 0L)
            YLog.info(tag = TAG, msg = "风扇校准完成: ${data.serialize()}")
        }

        private fun captureOemState(mode: Int, manual: Int): Boolean {
            val oldLevel = readInt(FAN_LEVEL)
            // NX809J 的 PWM 节点接受写入但固件不响应;可读、已验证的 level 节点
            // 既是控制接口,也是稳定的 OEM 恢复点。
            if (!oldLevel.valid || oldLevel.value < 0 || oldLevel.value > UNLOCKED_MAX_LEVEL) return false
            capturedLevel = oldLevel.value
            capturedMode = mode
            capturedManual = manual
            return true
        }

        private fun failSession(reason: String, restoreIfRunning: Boolean = true) {
            safeRelease(reason, restoreIfRunning)
            sessionLocked = true
            validRpmSamples = 0
            status("locked;reason=$reason;until_next_oem_session", 0L)
            YLog.warn(tag = TAG, msg = "风扇会话锁定: $reason")
        }

        private fun safeRelease(reason: String, restoreIfRunning: Boolean) {
            val wasControlling = controlKind != ControlKind.NONE
            try {
                restoreOemPower()
            } catch (error: Throwable) {
                report("restore_power_$reason", error)
            }
            try {
                if (wasControlling && restoreIfRunning && capturedLevel >= 0 && restoreStateStillOwned()) {
                    write(FAN_LEVEL, capturedLevel.toString())
                }
            } catch (error: Throwable) {
                report("restore_$reason", error)
            } finally {
                controlKind = ControlKind.NONE
                activeCalibrationRequest = ""
                calibrationSamples = null
                commandedLevel = -1
                resetCapture()
                if (wasControlling) {
                    lastDiagnosticAt = 0L
                    YLog.debug(tag = TAG, msg = "风扇控制释放: $reason")
                }
            }
        }

        private fun ensureLevel(desiredLevel: Int): Boolean {
            if (desiredLevel < MIN_LEVEL || desiredLevel > UNLOCKED_MAX_LEVEL) {
                throw IllegalArgumentException("invalid_fan_level")
            }
            if (desiredLevel == UNLOCKED_MAX_LEVEL && controlKind != ControlKind.CALIBRATION) {
                activateMaxPower()
            } else if (desiredLevel < UNLOCKED_MAX_LEVEL && maxPowerOwned) {
                restoreOemPower()
            }
            val observed = readInt(FAN_LEVEL)
            if (!observed.valid || observed.value < 0 || observed.value > UNLOCKED_MAX_LEVEL) {
                throw IllegalStateException("fan_level_read_failed")
            }
            if (observed.value == desiredLevel) {
                commandedLevel = desiredLevel
                return true
            }
            if (commandedLevel == desiredLevel) {
                // 上一次写入已有一个完整轮询周期稳定;仍不匹配说明控制失败,
                // 本周期内直接交还控制权,而不是继续覆盖 OEM。
                throw IllegalStateException("fan_level_not_applied")
            } else {
                commandedLevel = desiredLevel
            }
            write(FAN_LEVEL, desiredLevel.toString())
            return false
        }

        private fun activateMaxPower() {
            if (maxPowerOwned) return
            val currentOwner = owner ?: throw IllegalStateException("missing_oem_power_owner")
            val method = currentOwner.javaClass.getDeclaredMethod("fanMaxSpeed")
            method.isAccessible = true
            // 先置标志再调用:抛出异常的 OEM 调用也可能已经取得了令牌
            maxPowerOwned = true
            method.invoke(currentOwner)
        }

        private fun restoreOemPower() {
            val currentOwner = owner
            if (!maxPowerOwned || currentOwner == null) return
            val enabled = readInt(FAN_ENABLE)
            val method = currentOwner.javaClass.getDeclaredMethod(
                if (enabled.valid && enabled.value == 1) "notifyCubeFan" else "cancelFanFullSpeed"
            )
            method.isAccessible = true
            method.invoke(currentOwner)
            maxPowerOwned = false
        }

        private fun oemStateStillCaptured(): Boolean {
            val current = context
            if (current == null || capturedMode == Int.MIN_VALUE || capturedManual == Int.MIN_VALUE) {
                return false
            }
            return try {
                Settings.System.getInt(current.contentResolver, OEM_MODE) == capturedMode &&
                    Settings.System.getInt(current.contentResolver, OEM_MANUAL) == capturedManual
            } catch (ignored: Throwable) {
                false
            }
        }

        private fun restoreStateStillOwned(): Boolean {
            val enabled = readInt(FAN_ENABLE)
            if (!enabled.valid) return false
            if (enabled.value == 1) return oemStateStillCaptured()
            val current = context
            if (enabled.value != 0 || current == null || capturedMode == Int.MIN_VALUE) return false
            return try {
                // 关闭 OEM 总开关会合法地改变 manual 状态;此时 mode 是所有权守卫
                Settings.System.getInt(current.contentResolver, OEM_MODE) == capturedMode
            } catch (ignored: Throwable) {
                false
            }
        }

        private fun resetCapture() {
            capturedLevel = -1
            capturedMode = Int.MIN_VALUE
            capturedManual = Int.MIN_VALUE
        }

        private fun thermalNormal(current: Context): Boolean {
            return try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
                val power = current.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (power == null || power.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
                    return false
                }
                val battery = current.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?: return false
                val temperature = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                temperature != Int.MIN_VALUE && temperature >= 0 && temperature < MAX_BATTERY_TENTHS_C
            } catch (ignored: Throwable) {
                false
            }
        }

        private fun write(path: String, value: String) {
            writeNode.invoke(null, path, value)
        }

        private fun readInt(path: String): ReadResult {
            return try {
                val raw = readNode.invoke(null, path) ?: return ReadResult.invalid()
                ReadResult.valid(raw.toString().trim().toInt())
            } catch (ignored: Throwable) {
                ReadResult.invalid()
            }
        }

        /** 状态经 Settings.System 回传给模块 app 的 UI(读取无需权限),同时进日志。 */
        private fun status(value: String, throttleMs: Long) {
            val now = System.currentTimeMillis()
            if (throttleMs > 0L && now - lastDiagnosticAt < throttleMs) return
            lastDiagnosticAt = now
            val text = "$value;ts=$now"
            context?.let { ctx ->
                runCatching {
                    Settings.System.putString(ctx.contentResolver, FanCalibrationChannel.STATUS_KEY, text)
                }
            }
            YLog.debug(tag = TAG, msg = "fan_status: $text")
        }

        private fun report(stage: String, error: Throwable) {
            YLog.error(tag = TAG, msg = "FAN_CONTROL_$stage: ${safeMessage(error)}", e = error)
        }
    }

    private class ReadResult private constructor(val valid: Boolean, val value: Int) {
        companion object {
            fun valid(value: Int) = ReadResult(true, value)
            fun invalid() = ReadResult(false, 0)
        }
    }

    /** 纯档位边界与已验证的档位选择(FanControlPolicy 移植)。 */
    private const val MIN_LEVEL = 1
    private const val UNLOCKED_MAX_LEVEL = 5

    private fun effectiveTargetRpm(requestedRpm: Int, unlocked: Boolean, data: FanCalibrationData): Int =
        requestedRpm.coerceIn(data.rpm(1), data.rpm(if (unlocked) 5 else 4))

    private fun targetLevel(requestedRpm: Int, unlocked: Boolean, data: FanCalibrationData): Int =
        data.closestLevel(requestedRpm, if (unlocked) 5 else 4)

    private fun shouldForceUnlockedExtreme(unlockEnabled: Boolean, oemMode: Int): Boolean =
        unlockEnabled && oemMode == 0

    private fun supportedDevice(): Boolean {
        val deviceMatch = SUPPORTED_DEVICES.any {
            it.equals(Build.DEVICE, ignoreCase = true) ||
                it.equals(Build.PRODUCT, ignoreCase = true) ||
                it.equals(Build.MODEL, ignoreCase = true)
        }
        return deviceMatch &&
            File("/$FAN_ENABLE").exists() &&
            File("/$FAN_LEVEL").exists() &&
            File("/$FAN_RPM").exists()
    }

    /** FeatureSettings.from 的精简移植:从 OEM 控制器实例反射找 Context。 */
    private fun contextFrom(owner: Any?): Context? {
        if (owner is Context) return owner
        if (owner != null) {
            for (fieldName in arrayOf("mContext", "context", "mApplication", "this$0", "mActivity")) {
                val value = fieldValue(owner, fieldName) ?: continue
                if (value is Context) return value
                viaGetContext(value)?.let { return it }
            }
            viaGetContext(owner)?.let { return it }
        }
        return null
    }

    private fun fieldValue(owner: Any, name: String): Any? {
        var type: Class<*>? = owner.javaClass
        while (type != null) {
            try {
                val field = type.getDeclaredField(name)
                field.isAccessible = true
                return field.get(owner)
            } catch (ignored: NoSuchFieldException) {
                type = type.superclass
            } catch (ignored: Throwable) {
                return null
            }
        }
        return null
    }

    private fun viaGetContext(owner: Any?): Context? {
        if (owner == null) return null
        return try {
            val value = owner.javaClass.getMethod("getContext").invoke(owner)
            value as? Context
        } catch (ignored: Throwable) {
            null
        }
    }

    private fun safeMessage(error: Throwable): String {
        var cause: Throwable? = error
        while (cause?.cause != null && cause.cause !== cause) {
            cause = cause.cause
        }
        var message = cause?.message
        if (message.isNullOrEmpty()) {
            message = cause?.javaClass?.simpleName ?: "unknown"
        }
        return message.replace('\n', ' ').replace('\r', ' ')
    }
}
