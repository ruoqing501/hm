package dev.lackluster.redmagichelper.utils.rapidfire

/** Bounded per-session, per-phase evidence. Keys are actual transport codes, not sides. */
internal class RapidFireRouteEvidence private constructor(
    val sessionId: String,
    val phase: String
) {
    private val routes = LinkedHashMap<Int, Int>()
    private var conflict = false

    fun matches(id: String, expectedPhase: String): Boolean =
        sessionId == id && phase == expectedPhase

    fun record(upper: Int, system: Int) {
        if (!code(upper) || !code(system)) {
            conflict = true
            return
        }
        val previous = routes[system]
        if (previous != null) {
            if (previous != upper) conflict = true
            return
        }
        if (routes.size >= 16 || routes.containsValue(upper)) {
            conflict = true
            return
        }
        routes[system] = upper
    }

    fun resolve(observedSystemCode: Int): Int =
        if (conflict) -1 else routes[observedSystemCode] ?: -1

    fun isConflicted(): Boolean = conflict

    fun serialize(): String {
        val out = StringBuilder("RFR1|").append(sessionId).append('|')
            .append(phase).append('|').append(if (conflict) '1' else '0').append('|')
        for ((system, upper) in routes) {
            if (out[out.length - 1] != '|') out.append(',')
            out.append(system).append(':').append(upper)
        }
        return out.toString()
    }

    /** Lives only for a single synchronous upper call; no time-window/code-equality guessing. */
    class Trace {
        private var system = -1
        private var ambiguous = false

        fun observe(systemCode: Int) {
            if (!code(systemCode) || (system > 0 && system != systemCode)) {
                ambiguous = true
            } else {
                system = systemCode
            }
        }

        fun reject() {
            ambiguous = true
        }

        fun resolvedSystem(): Int = if (ambiguous) -1 else system
    }

    class Calls {
        private val current = ThreadLocal<Trace>()

        fun current(): Trace? = current.get()

        fun begin(): Scope = Scope()

        inner class Scope : AutoCloseable {
            val trace = Trace()
            private val previous = current.get()
            private var closed = false

            init {
                if (previous != null) {
                    previous.reject()
                    trace.reject()
                }
                current.set(trace)
            }

            override fun close() {
                if (closed) return
                closed = true
                if (previous == null) current.remove() else current.set(previous)
            }
        }
    }

    companion object {
        const val DIAGNOSTIC_PREFIX = "rmh_tgk_rapid_fire_test_routes_"

        fun start(id: String?, phase: String?): RapidFireRouteEvidence? =
            if (id != null && id.matches(Regex("[0-9a-f]{32}")) &&
                ("WAIT_LEFT" == phase || "WAIT_RIGHT" == phase)
            ) {
                RapidFireRouteEvidence(id, phase)
            } else {
                null
            }

        fun parse(value: String?): RapidFireRouteEvidence? {
            if (value == null || value.length > 1024) return null
            return try {
                // Kotlin split keeps trailing empty fields with the default limit.
                val fields = value.split("|")
                if (fields.size != 5 || "RFR1" != fields[0] ||
                    ("0" != fields[3] && "1" != fields[3])
                ) return null
                val result = start(fields[1], fields[2]) ?: return null
                if (fields[4].isNotEmpty()) {
                    val pairs = fields[4].split(",")
                    if (pairs.size > 16) return null
                    for (pair in pairs) {
                        val codes = pair.split(":")
                        if (codes.size != 2) return null
                        result.record(codes[1].toInt(), codes[0].toInt())
                    }
                }
                if ("1" == fields[3]) result.conflict = true
                result
            } catch (_: RuntimeException) {
                null
            }
        }

        private fun code(value: Int): Boolean = value > 0 && value <= 65535
    }
}
