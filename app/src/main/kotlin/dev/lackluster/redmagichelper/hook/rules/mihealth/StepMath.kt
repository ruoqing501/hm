package dev.lackluster.redmagichelper.hook.rules.mihealth

import kotlin.math.max
import kotlin.math.min

/** Mirrors the target app's maximum-per-source merge within an aggregation interval. */
object StepMath {

    fun multiply(original: Int, percent: Int): Int {
        require(original >= 0 && percent in 100..2000) { "step range" }
        return min(Int.MAX_VALUE.toLong(), original.toLong() * percent / 100).toInt()
    }

    fun merged(sourceTotals: Map<String, Long>): Long {
        var maximum = 0L
        for (value in sourceTotals.values) {
            require(value >= 0) { "negative source" }
            maximum = max(maximum, value)
        }
        return maximum
    }

    fun phoneAddition(natural: Map<String, Long>, phone: String, bonus: Int): Int {
        require(bonus >= 0) { "negative bonus" }
        val result = Math.addExact(merged(natural) - (natural[phone] ?: 0L), bonus.toLong())
        require(result <= Int.MAX_VALUE) { "step overflow" }
        return result.toInt()
    }
}
