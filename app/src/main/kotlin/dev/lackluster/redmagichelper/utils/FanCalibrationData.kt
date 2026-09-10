package dev.lackluster.redmagichelper.utils

import android.os.Build
import java.security.MessageDigest

/**
 * 风扇校准数据(1~5 档实测转速)。
 * 移植自 LS_Augment 的 FanCalibrationData/FanHardwareIdentity,语义保持一致:
 * 序列化格式 FC1 与 LS_Augment 完全兼容,是反馈测量值而非电机物理上限的断言。
 * hook 进程与模块 app 进程共用本文件。
 */
class FanCalibrationData private constructor(
    val firmware: String,
    val measuredAt: Long,
    private val median: IntArray,
    private val peak: IntArray,
) {
    fun currentFor(identity: String): Boolean = firmware == identity

    fun rpm(level: Int): Int = if (level in 1..LEVELS) median[level - 1] else 0

    fun peak(level: Int): Int = if (level in 1..LEVELS) peak[level - 1] else 0

    fun closestLevel(requested: Int, maximumLevel: Int): Int {
        val limit = maximumLevel.coerceIn(1, LEVELS)
        var result = 1
        for (i in 2..limit) {
            if (kotlin.math.abs(requested.toLong() - rpm(i)) < kotlin.math.abs(requested.toLong() - rpm(result))) {
                result = i
            }
        }
        return result
    }

    fun serialize(): String {
        val text = StringBuilder("FC1|").append(firmware).append('|').append(measuredAt)
        for (i in 0 until LEVELS) text.append('|').append(median[i]).append(',').append(peak[i])
        return text.toString()
    }

    companion object {
        const val LEVELS = 5
        const val REQUEST_LIFETIME_MS = 180_000L

        /** 解析 sanity 边界,不是目标上限或电机规格。 */
        fun validRpm(rpm: Int): Boolean = rpm in 500..100_000

        fun fromSamples(firmware: String, time: Long, samples: Array<IntArray>?): FanCalibrationData? {
            if (samples == null || samples.size != LEVELS) return null
            val median = IntArray(LEVELS)
            val peak = IntArray(LEVELS)
            for (level in 0 until LEVELS) {
                val values = samples.getOrNull(level)?.copyOf() ?: return null
                if (values.size < 5) return null
                values.sort()
                median[level] = values[values.size / 2]
                peak[level] = values[values.size - 1]
                if (!validRpm(values[0]) || !validRpm(peak[level]) ||
                    peak[level] - values[0] > median[level] * 0.30
                ) return null
            }
            return create(firmware, time, median, peak)
        }

        /** 手动输入:每档只给一个稳定转速,中位数与峰值取同一值。 */
        fun fromManual(firmware: String, time: Long, rpm: IntArray): FanCalibrationData? {
            if (rpm.size != LEVELS) return null
            return create(firmware, time, rpm.copyOf(), rpm.copyOf())
        }

        private fun create(
            firmware: String,
            time: Long,
            median: IntArray,
            peak: IntArray,
        ): FanCalibrationData? {
            if (!firmware.matches(Regex("[0-9a-f]{64}")) || time <= 0) return null
            for (i in 0 until LEVELS) {
                if (!validRpm(median[i]) || !validRpm(peak[i]) || peak[i] < median[i] ||
                    (i > 0 && median[i] < median[i - 1])
                ) return null
            }
            return FanCalibrationData(firmware, time, median.copyOf(), peak.copyOf())
        }

        fun parse(text: String?): FanCalibrationData? {
            if (text == null || text.length > 256) return null
            return try {
                // Kotlin split 为字面量分隔;FC1 格式字段均非空,默认 limit 行为即可
                val parts = text.split("|")
                if (parts.size != 8 || parts[0] != "FC1") return null
                val median = IntArray(LEVELS)
                val peak = IntArray(LEVELS)
                for (i in 0 until LEVELS) {
                    val pair = parts[i + 3].split(",")
                    if (pair.size != 2) return null
                    median[i] = pair[0].toInt()
                    peak[i] = pair[1].toInt()
                }
                create(parts[1], parts[2].toLong(), median, peak)
            } catch (ignored: RuntimeException) {
                null
            }
        }

        fun validRequest(request: String?, now: Long): Boolean {
            if (request == null || !request.matches(Regex("[0-9]{13}:[0-9a-f]{32}"))) return false
            val created = request.substring(0, 13).toLong()
            return now >= created && now - created <= REQUEST_LIFETIME_MS
        }
    }
}

/** 机型/固件指纹:系统或内核变化后,旧校准数据自动失效(fail-closed)。 */
object FanHardwareIdentity {
    fun current(): String = runCatching {
        val digest = MessageDigest.getInstance("SHA-256").digest(
            (Build.FINGERPRINT + "|" + System.getProperty("os.version", "") +
                "|soc_fan.level.0-5.oem_max128.v2").toByteArray(Charsets.UTF_8)
        )
        digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }.getOrDefault("unavailable")
}

/**
 * 校准结果回传通道常量。
 * hook 进程(cn.nubia.fan,系统应用,持有 WRITE_SETTINGS)把测量结果/状态写入
 * Settings.System;模块 app 侧读取系统设置无需任何权限,避免跨 uid 读写文件。
 * 与 ScreenOffHideExecutor 使用 Settings.Global 做跨进程通道是同一套路。
 */
object FanCalibrationChannel {
    const val MEASUREMENT_KEY = "rmh_fan_measurement"
    const val STATUS_KEY = "rmh_fan_status"
}
