package dev.lackluster.redmagichelper.hook.rules.mihealth

import java.time.LocalDate
import java.time.ZoneId
import java.util.Collections
import java.util.Random
import java.util.SortedMap
import java.util.TreeMap

/** A stable random timetable: a saved plan never draws new times on restart. */
class StepPlan private constructor(
    val id: String,
    val account: String,
    val zone: String,
    val startDate: LocalDate,
    val fromMinute: Int,
    val toMinute: Int,
    val amount: Int,
    val repeat: Boolean,
    val seed: Long,
    val executions: Int,
    val stepsPerExecution: Int,
    val weekdays: Int,
) {
    constructor(
        id: String, account: String, zone: String, date: LocalDate,
        from: Int, to: Int, amount: Int, repeat: Boolean, seed: Long,
    ) : this(id, account, zone, date, from, to, amount, repeat, seed, 0, 0, 127) {
        require(
            id.matches(Regex("[0-9a-f]{32}")) && account.matches(Regex("[0-9a-f]{64}")) &&
                from >= 0 && to <= 1440 && to > from && amount in 1..1_000_000
        ) { "invalid step plan" }
        ZoneId.of(zone)
    }

    constructor(
        id: String, account: String, zone: String, date: LocalDate,
        from: Int, to: Int, executions: Int, steps: Int, weekdays: Int, seed: Long,
    ) : this(id, account, zone, date, from, to, executions * steps, true, seed, executions, steps, weekdays) {
        require(
            id.matches(Regex("[0-9a-f]{32}")) && account.matches(Regex("[0-9a-f]{64}")) &&
                from >= 0 && from < 1440 && to >= 0 && to <= 1440 && from != to &&
                executions in 1..100 && steps >= 1 && steps.toLong() * executions <= 1_000_000 &&
                weekdays in 1..127
        ) { "invalid plan" }
        val minutes = (if (to > from) to else to + 1440) - from
        require(executions <= minutes) { "not enough distinct minutes" }
        ZoneId.of(zone)
    }

    fun runsOn(day: LocalDate): Boolean =
        !day.isBefore(startDate) && (repeat || day == startDate) &&
            (weekdays and (1 shl (day.dayOfWeek.value - 1))) != 0

    fun serialize(): String =
        if (executions > 0) listOf(
            "SP2", id, account, zone, startDate.toString(), fromMinute.toString(),
            toMinute.toString(), executions.toString(), stepsPerExecution.toString(),
            weekdays.toString(), seed.toString()
        ).joinToString("|")
        else listOf(
            "SP1", id, account, zone, startDate.toString(), fromMinute.toString(),
            toMinute.toString(), amount.toString(), if (repeat) "1" else "0", seed.toString()
        ).joinToString("|")

    fun timetable(day: LocalDate): SortedMap<Long, Int> {
        val result = TreeMap<Long, Int>()
        if (!runsOn(day)) return result
        val random = Random(seed xor (day.toEpochDay() * -7046029254386353131L) xor account.hashCode().toLong())
        if (executions > 0) {
            val end = if (toMinute > fromMinute) toMinute else toMinute + 1440
            val minutes = (fromMinute until end).toMutableList()
            minutes.shuffle(random)
            val zoneId = ZoneId.of(zone)
            for (i in 0 until executions) {
                result[day.atStartOfDay(zoneId).plusMinutes(minutes[i].toLong()).toEpochSecond()] = stepsPerExecution
            }
            return Collections.unmodifiableSortedMap(result)
        }
        var left = amount
        while (left > 0) {
            val minute = fromMinute + random.nextInt(toMinute - fromMinute)
            val chunk = minOf(left, 1 + random.nextInt(200))
            val at = day.atStartOfDay(ZoneId.of(zone)).plusMinutes(minute.toLong()).toEpochSecond()
            result.merge(at, chunk) { a, b -> a + b }
            left -= chunk
        }
        return Collections.unmodifiableSortedMap(result)
    }

    companion object {
        fun parse(text: String?): StepPlan? {
            if (text.isNullOrEmpty()) return null
            return try {
                val v = text.split("|").toTypedArray()
                if (v.size == 11 && v[0] == "SP2") {
                    StepPlan(
                        v[1], v[2], v[3], LocalDate.parse(v[4]), v[5].toInt(), v[6].toInt(),
                        v[7].toInt(), v[8].toInt(), v[9].toInt(), v[10].toLong()
                    )
                } else if (v.size == 10 && v[0] == "SP1" && (v[8] == "0" || v[8] == "1")) {
                    StepPlan(
                        v[1], v[2], v[3], LocalDate.parse(v[4]), v[5].toInt(), v[6].toInt(),
                        v[7].toInt(), v[8] == "1", v[9].toLong()
                    )
                } else null
            } catch (ignored: Exception) {
                null
            }
        }
    }
}
