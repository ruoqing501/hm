package dev.lackluster.redmagichelper.hook.rules.gamehelpmodule

import kotlin.math.ceil
import kotlin.math.round

/**
 * Converts the OEM recovery timing and rate into a user-selected playback
 * rate while preserving the original motion duration. Ported from
 * LS_Augment's ComboSpeedPolicy.
 */
object ComboSpeedPolicy {
    const val MIN_RATE = 1.0f
    const val MAX_RATE = 10.0f

    fun isValidRate(rate: Float): Boolean =
        !rate.isNaN() && !rate.isInfinite() &&
                rate >= MIN_RATE && rate <= MAX_RATE &&
                rate == round(rate)

    fun normalizeRate(rate: Float): Float = if (isValidRate(rate)) rate else MIN_RATE

    fun adjustRecoveryTime(recoveryTime: Long, originalRate: Float, targetRate: Float): Long {
        if (recoveryTime <= 0L || originalRate.isNaN() ||
            originalRate.isInfinite() || originalRate <= 0f ||
            !isValidRate(targetRate)
        ) {
            return recoveryTime
        }
        val adjusted = recoveryTime * originalRate.toDouble() / targetRate
        if (adjusted >= Long.MAX_VALUE) return Long.MAX_VALUE
        return maxOf(1L, ceil(adjusted).toLong())
    }

    /**
     * Scale an absolute MotionEvent timestamp around the first event. The
     * OEM player schedules every event from these timestamps, so changing a
     * separate playback-duration field cannot affect the real gesture speed.
     */
    fun scaleTimestamp(timestamp: Long, origin: Long, targetRate: Float): Long {
        if (timestamp < 0L || origin < 0L || timestamp <= origin ||
            !isValidRate(targetRate)
        ) {
            return timestamp
        }
        val scaled = origin + (timestamp - origin.toDouble()) / targetRate
        if (scaled >= Long.MAX_VALUE) return Long.MAX_VALUE
        return maxOf(origin, round(scaled).toLong())
    }
}
