package dev.lackluster.redmagichelper.utils

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Narrow root process transport. Callers must construct commands from fixed
 * templates and validate every variable before invoking this class.
 */
internal object RootShell {
    private const val DEFAULT_TIMEOUT_SECONDS = 20L
    private const val DEFAULT_MAX_OUTPUT = 512 * 1024

    fun exec(
        command: String,
        stdin: String? = null,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        maxOutput: Int = DEFAULT_MAX_OUTPUT,
        onOutput: ((String) -> Unit)? = null
    ): Result {
        var process: Process? = null
        var reader: OutputReader? = null
        try {
            val su = if (File("/system/bin/su").canExecute()) "/system/bin/su" else "su"
            process = ProcessBuilder(su, "-c", command)
                .redirectErrorStream(true)
                .start()
            reader = OutputReader(process.inputStream, maxOutput, onOutput)
            val readThread = Thread(reader, "redmagichelper-root-output")
            readThread.isDaemon = true
            readThread.start()

            process.outputStream.use { output ->
                if (!stdin.isNullOrEmpty()) {
                    output.write(stdin.toByteArray(StandardCharsets.UTF_8))
                }
            }

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                readThread.join(1000L)
                return Result(-1, reader.text(), true)
            }
            readThread.join(1000L)
            return Result(process.exitValue(), reader.text(), false)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            return Result(130, "INTERRUPTED", false)
        } catch (error: Throwable) {
            val message = error.message?.takeIf { it.isNotEmpty() } ?: "no_message"
            return Result(127, error.javaClass.simpleName + ":" + message, false)
        } finally {
            process?.destroy()
        }
    }

    fun quote(value: String?): String {
        val text = value ?: ""
        return "'" + text.replace("'", "'\"'\"'") + "'"
    }

    private class OutputReader(
        private val input: InputStream,
        limit: Int,
        private val onOutput: ((String) -> Unit)?
    ) : Runnable {
        private val limit = maxOf(1024, limit)
        private val output = ByteArrayOutputStream()

        override fun run() {
            val buffer = ByteArray(4096)
            var remaining = limit
            try {
                while (remaining > 0) {
                    val count = input.read(buffer, 0, minOf(buffer.size, remaining))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    remaining -= count
                    if (onOutput != null) {
                        try {
                            onOutput.invoke(text())
                        } catch (ignored: RuntimeException) {
                        }
                    }
                }
            } catch (ignored: IOException) {
                // The process result remains authoritative.
            } finally {
                try {
                    input.close()
                } catch (ignored: IOException) {
                }
            }
        }

        fun text(): String = String(output.toByteArray(), StandardCharsets.UTF_8).trim()
    }

    class Result internal constructor(
        val exitCode: Int,
        output: String?,
        val timedOut: Boolean
    ) {
        val output: String = output ?: ""

        val isSuccess: Boolean
            get() = !timedOut && exitCode == 0

        fun publicError(): String {
            if (timedOut) return "执行超时"
            if (exitCode == 127) return "Root 不可用"
            var clean = output.replace('\n', ' ').replace('\r', ' ').trim()
            if (clean.length > 220) clean = clean.substring(0, 220)
            return clean.ifEmpty { "退出码 $exitCode" }
        }
    }
}
