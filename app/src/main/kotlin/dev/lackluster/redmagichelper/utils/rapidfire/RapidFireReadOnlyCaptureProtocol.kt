package dev.lackluster.redmagichelper.utils.rapidfire

/** A completed, bounded read-only recording of identified shoulder sources. */
internal object RapidFireReadOnlyCaptureProtocol {

    fun isComplete(output: String?, devices: List<RapidFireInputDetector.Device>?): Boolean {
        if (output == null || devices.isNullOrEmpty() || output.contains("LSA_READONLY_ERROR=")) {
            return false
        }
        val expected = HashSet<String>()
        val declared = HashSet<String>()
        for (device in devices) {
            if (!device.path.matches(Regex("/dev/input/event[0-9]+")) ||
                !RapidFireInputDetector.isLikelyShoulderName(device.name)
            ) return false
            expected.add(device.path)
        }
        var ready = false
        var end = false
        for (raw in output.split(Regex("\\r?\\n"))) {
            val line = raw.trim()
            when {
                line.startsWith("LSA_READONLY_SOURCE=") -> {
                    val source = line.substring("LSA_READONLY_SOURCE=".length)
                    if (ready || end || !expected.contains(source) || !declared.add(source)) {
                        return false
                    }
                }
                line == "LSA_READONLY_READY=1" -> {
                    if (ready || end || declared != expected) return false
                    ready = true
                }
                line.startsWith("LSA_READONLY_END=") -> {
                    if (!ready || end || line != "LSA_READONLY_END=window_elapsed") return false
                    end = true
                }
                line.startsWith("/dev/input/") -> {
                    val colon = line.indexOf(':')
                    if (!ready || end || colon < 0 ||
                        !expected.contains(line.substring(0, colon))
                    ) return false
                }
            }
        }
        return ready && end
    }

    fun observedPair(
        output: String?,
        devices: List<RapidFireInputDetector.Device>?
    ): RapidFireInputDetector.Capture? =
        if (isComplete(output, devices)) {
            RapidFireInputDetector.parseCapture(output, devices)
        } else {
            null
        }
}
