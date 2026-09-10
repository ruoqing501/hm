package dev.lackluster.redmagichelper.utils.rapidfire

import java.util.Locale

/** Fail-closed discovery and parsing for physical shoulder-key input devices. */
internal object RapidFireInputDetector {
    private val NAME = Regex("^N:\\s+Name=\"(.*)\"$")
    private val EVENT_HANDLER = Regex("\\bevent([0-9]+)\\b")
    private val RAW_KEY_EVENT = Regex(
        "(?im)^.*?(/dev/input/event[0-9]+):\\s+(?:\\[[^\\]\\r\\n]+\\]\\s+)?0001\\s+" +
            "([0-9a-f]{4,8})\\s+([0-9a-f]{8})\\s*$"
    )

    class Device(val path: String, val name: String)

    class Capture(val path: String, val name: String, val code: Int)

    /**
     * Discovers only input sources that explicitly identify themselves as gaming shoulder
     * triggers. Generic touchscreens, GPIO keys and volume/power devices are never candidates.
     */
    fun discover(procInputDevices: String?): List<Device> {
        if (procInputDevices.isNullOrBlank()) return emptyList()
        val devices = ArrayList<Device>()
        var name: String? = null
        var event: String? = null
        for (raw in procInputDevices.split(Regex("\\r?\\n"))) {
            val line = raw.trim()
            if (line.startsWith("I:") || line.isEmpty()) {
                addCandidate(devices, name, event)
                name = null
                event = null
                continue
            }
            val nameMatch = NAME.matchEntire(line)
            if (nameMatch != null) {
                name = nameMatch.groupValues[1].trim()
                continue
            }
            if (line.startsWith("H:")) {
                val eventMatch = EVENT_HANDLER.find(line)
                if (eventMatch != null) event = "event" + eventMatch.groupValues[1]
            }
        }
        addCandidate(devices, name, event)
        return devices
    }

    fun isLikelyShoulderName(name: String?): Boolean {
        if (name == null) return false
        val compact = name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "")
        return compact.contains("tgk") || compact.contains("shoulder") ||
            compact.contains("airtrigger") || compact.contains("gametrigger")
    }

    /** Accepts complete pairs from exactly one physical key; mixed sides are ambiguous. */
    fun parseCapture(geteventOutput: String?, allowedDevices: List<Device>?): Capture? {
        if (geteventOutput == null || allowedDevices.isNullOrEmpty()) return null
        val allowed = LinkedHashMap<String, Device>()
        for (device in allowedDevices) {
            if (isLikelyShoulderName(device.name)) allowed[device.path] = device
        }
        val pressed = LinkedHashSet<String>()
        var completed: Capture? = null
        for (match in RAW_KEY_EVENT.findAll(geteventOutput)) {
            val device = allowed[match.groupValues[1]] ?: continue
            try {
                val code = match.groupValues[2].toInt(16)
                val value = match.groupValues[3].toLong(16)
                if (code <= 0 || code > 65535) continue
                val key = device.path + ":" + code
                if (value == 1L) {
                    pressed.add(key)
                } else if (value == 0L && pressed.remove(key)) {
                    if (completed != null &&
                        (completed.path != device.path || completed.code != code)
                    ) return null
                    completed = Capture(device.path, device.name, code)
                }
            } catch (_: Throwable) {
                // Malformed or unexpected lines are ignored; absence of a pair fails closed.
            }
        }
        return completed
    }

    private fun addCandidate(out: MutableList<Device>, name: String?, event: String?) {
        if (name == null || event == null || !isLikelyShoulderName(name)) return
        val path = "/dev/input/$event"
        for (existing in out) if (path == existing.path) return
        out.add(Device(path, name))
    }
}
