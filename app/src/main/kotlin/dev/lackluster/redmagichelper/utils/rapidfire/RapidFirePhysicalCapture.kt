package dev.lackluster.redmagichelper.utils.rapidfire

import android.content.Context
import android.os.Process
import android.util.Log
import dev.lackluster.redmagichelper.BuildConfig
import dev.lackluster.redmagichelper.utils.RootShell
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Short physical-input capture; never enables rapid-fire or changes OEM mappings. */
internal class RapidFirePhysicalCapture {

    class Result internal constructor(
        val capture: RapidFireInputDetector.Capture?,
        val message: String
    )

    private val cancelled = AtomicBoolean()

    @Volatile
    private var lease: File? = null

    @Volatile
    private var completion: File? = null

    fun cancel() {
        cancelled.set(true)
        lease?.delete()
    }

    fun run(
        context: Context,
        devices: List<RapidFireInputDetector.Device>,
        left: Boolean,
        onReady: Runnable,
        onPair: Runnable
    ): Result {
        var device0: String? = null
        var device1: String? = null
        for (device in devices) {
            if ("nubia_tgk_aw_sar0_ch0" == device.name) device0 = device.path
            if ("nubia_tgk_aw_sar1_ch0" == device.name) device1 = device.path
        }
        // A verified driver permits temporary wake writes; it is not a feature
        // admission list. All other discovered shoulder inputs use read-only capture.
        val wakeDriver = if (device0 == null || device1 == null) {
            null
        } else {
            RootShell.exec("sha256sum /vendor_dlkm/lib/modules/aw9620x.ko", timeoutSeconds = 3, maxOutput = 256)
        }
        if (wakeDriver == null || !wakeDriver.isSuccess || !wakeDriver.output.startsWith(
                "3211fdc4b97aabe3c0db8f06f3ec07da8c3c382a6273412adb089940e9d4b532 "
            )
        ) {
            return runReadOnly(context, devices, onReady)
        }
        if (cancelled.get()) return Result(null, "采集已取消")
        val target = RapidFireInputDetector.Device(
            (if (left) device0 else device1) ?: return runReadOnly(context, devices, onReady),
            if (left) "nubia_tgk_aw_sar0_ch0" else "nubia_tgk_aw_sar1_ch0"
        )
        try {
            val script = context.assets.open("rapid_input_capture.sh").use { input ->
                String(input.readBytes(), Charsets.UTF_8)
            }
            val leaseFile = File(context.cacheDir, "rapid-input-${UUID.randomUUID()}.lease")
            lease = leaseFile
            if (!leaseFile.createNewFile()) return Result(null, "无法建立临时肩键测试会话")
            val completionFile = File(leaseFile.canonicalPath + ".complete")
            completion = completionFile
            if (cancelled.get()) return Result(null, "采集已取消")
            val ready = AtomicBoolean()
            val pair = AtomicBoolean()
            val shell = RootShell.exec(
                "/system/bin/sh -c " + RootShell.quote(script) + " -- " + Process.myPid() +
                    " " + RootShell.quote(leaseFile.canonicalPath) +
                    " " + RootShell.quote(device0) + " " + RootShell.quote(device1) +
                    " " + RootShell.quote(target.path),
                timeoutSeconds = 20,
                maxOutput = 256 * 1024,
                onOutput = { output ->
                    if (output.contains("LSA_CAPTURE_READY=") &&
                        !cancelled.get() && ready.compareAndSet(false, true)
                    ) {
                        onReady.run()
                    }
                    if (ready.get() && !cancelled.get() && !pair.get() &&
                        !output.contains("LSA_CAPTURE_END=") &&
                        RapidFireCaptureProtocol.observedPair(output, target) != null &&
                        pair.compareAndSet(false, true)
                    ) {
                        // This requests reader shutdown, not compatibility approval.
                        try {
                            completionFile.createNewFile()
                        } catch (_: Exception) {
                        }
                        onPair.run()
                    }
                }
            )
            Log.i(
                TAG, "physical_capture module=" + BuildConfig.VERSION_NAME +
                    "|time=" + System.currentTimeMillis() + "|exit=" + shell.exitCode +
                    "|timeout=" + shell.timedOut + "|cancelled=" + cancelled.get() +
                    "|pair=" + pair.get()
            )
            if (cancelled.get()) return Result(null, "采集已取消，肩键正在恢复原状态")
            if (shell.output.contains("LSA_CAPTURE_ERROR=restore_failed")) {
                return Result(null, "肩键原状态恢复失败，测试已停止；请重新进入原厂游戏空间恢复肩键")
            }
            if (!shell.isSuccess || !RapidFireCaptureProtocol.isComplete(shell.output, target)) {
                return Result(
                    null,
                    if (shell.output.contains("unverified_driver")) {
                        "肩键驱动尚未验证，无法自动唤醒；测试保持锁定"
                    } else if (shell.output.contains("busy")) {
                        "上一次肩键采集正在恢复，请稍后重试"
                    } else if (pair.get()) {
                        "已收到肩键事件，但结束或恢复确认不完整；未保存，请重新采集"
                    } else {
                        "肩键临时唤醒或监听失败，测试未通过"
                    }
                )
            }
            val capture = RapidFireCaptureProtocol.observedPair(shell.output, target)
            return Result(
                capture,
                if (capture == null) {
                    "肩键已临时唤醒，但未收到单侧完整触摸/松开事件；请只触摸提示的一侧后重试"
                } else {
                    ""
                }
            )
        } catch (_: Exception) {
            return Result(null, "肩键采集异常，测试未通过")
        } finally {
            lease?.delete()
            completion?.delete()
        }
    }

    private fun runReadOnly(
        context: Context,
        devices: List<RapidFireInputDetector.Device>,
        onReady: Runnable
    ): Result {
        if (cancelled.get()) return Result(null, "采集已取消")
        try {
            val script = context.assets.open("rapid_input_readonly.sh").use { input ->
                String(input.readBytes(), Charsets.UTF_8)
            }
            val leaseFile = File(context.cacheDir, "rapid-input-${UUID.randomUUID()}.lease")
            lease = leaseFile
            if (!leaseFile.createNewFile() || cancelled.get()) return Result(null, "采集已取消")
            val command = StringBuilder("/system/bin/sh -c ")
                .append(RootShell.quote(script))
                .append(" -- ").append(Process.myPid()).append(' ')
                .append(RootShell.quote(leaseFile.canonicalPath))
            for (device in devices) command.append(' ').append(RootShell.quote(device.path))
            val ready = AtomicBoolean()
            val shell = RootShell.exec(
                command.toString(),
                timeoutSeconds = 18,
                maxOutput = 256 * 1024,
                onOutput = { output ->
                    if (output.contains("LSA_READONLY_READY=1") &&
                        !cancelled.get() && ready.compareAndSet(false, true)
                    ) {
                        onReady.run()
                    }
                }
            )
            Log.i(
                TAG, "physical_capture module=" + BuildConfig.VERSION_NAME +
                    "|mode=read_only|exit=" + shell.exitCode +
                    "|time=" + System.currentTimeMillis()
            )
            if (cancelled.get()) return Result(null, "采集已取消，肩键原状态未改变")
            if (!shell.isSuccess || !RapidFireReadOnlyCaptureProtocol.isComplete(shell.output, devices)) {
                return Result(null, "肩键监听未完整结束，请重试；原厂肩键状态未改变")
            }
            val capture = RapidFireReadOnlyCaptureProtocol.observedPair(shell.output, devices)
            return Result(
                capture,
                if (capture == null) {
                    "未收到单侧触摸和松开。请先在原厂游戏空间启用肩键，再返回只触摸提示的一侧；无需更换机型或系统版本"
                } else {
                    ""
                }
            )
        } catch (_: Exception) {
            return Result(null, "肩键只读采集失败，请重试")
        } finally {
            lease?.delete()
        }
    }

    companion object {
        const val CAPTURE_MS = 8_000L
        private const val TAG = "RmhRapidFire"
    }
}
