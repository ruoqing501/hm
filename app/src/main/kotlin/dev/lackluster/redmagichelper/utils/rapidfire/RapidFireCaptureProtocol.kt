package dev.lackluster.redmagichelper.utils.rapidfire

/** Only events fenced before hardware restoration can become physical evidence. */
internal object RapidFireCaptureProtocol {

    fun isComplete(output: String?, target: RapidFireInputDetector.Device?): Boolean {
        if (output == null || target == null || output.contains("LSA_CAPTURE_ERROR=")) return false
        val source = singleValue(output, "LSA_CAPTURE_SOURCE=")
        val ready = singleValue(output, "LSA_CAPTURE_READY=")
        val end = singleValue(output, "LSA_CAPTURE_END=")
        val restored = singleValue(output, "LSA_CAPTURE_RESTORED=")
        return target.path == source && ready != null && ready.matches(Regex("[12]:[12]")) &&
            ready == restored &&
            ("pair_received" == end || "window_elapsed" == end) &&
            output.indexOf("LSA_CAPTURE_SOURCE=") < output.indexOf("LSA_CAPTURE_READY=") &&
            output.indexOf("LSA_CAPTURE_READY=") < output.indexOf("LSA_CAPTURE_END=") &&
            output.indexOf("LSA_CAPTURE_END=") < output.indexOf("LSA_CAPTURE_RESTORED=")
    }

    /** Progress is provisional: callers MUST verify isComplete before saving any mapping. */
    fun observedPair(
        output: String?,
        target: RapidFireInputDetector.Device?
    ): RapidFireInputDetector.Capture? {
        if (output == null || target == null || output.contains("LSA_CAPTURE_ERROR=")) return null
        if (target.path != singleValue(output, "LSA_CAPTURE_SOURCE=")) return null
        val ready = singleValue(output, "LSA_CAPTURE_READY=")
        if (ready == null || !ready.matches(Regex("[12]:[12]"))) return null
        val start = output.indexOf('\n', output.indexOf("LSA_CAPTURE_READY="))
        if (start < 0 || output.indexOf("LSA_CAPTURE_SOURCE=") > start) return null
        var end = output.indexOf("LSA_CAPTURE_END=")
        if (end < 0) end = output.length
        if (end <= start) return null
        val events = StringBuilder()
        for (line in output.substring(start, end).split(Regex("\\r?\\n"))) {
            val match = EVENT.matchEntire(line.trim())
            if (match != null) {
                events.append(target.path).append(": ")
                    .append(match.groupValues[1]).append(' ')
                    .append(match.groupValues[2]).append(' ')
                    .append(match.groupValues[3]).append('\n')
            }
        }
        return RapidFireInputDetector.parseCapture(events.toString(), listOf(target))
    }

    private val EVENT = Regex(
        "^\\[\\s*[0-9]+\\.[0-9]+\\]\\s+(0001)\\s+([0-9a-fA-F]{4,8})\\s+([0-9a-fA-F]{8})$"
    )

    private fun singleValue(output: String, prefix: String): String? {
        var result: String? = null
        for (raw in output.split(Regex("\\r?\\n"))) {
            val line = raw.trim()
            if (!line.startsWith(prefix)) continue
            if (result != null) return null
            result = line.substring(prefix.length)
        }
        return result
    }
}
