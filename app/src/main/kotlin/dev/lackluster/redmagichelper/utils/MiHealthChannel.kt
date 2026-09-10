package dev.lackluster.redmagichelper.utils

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * 小米运动健康步数增强的回传通道。
 *
 * hook 进程(com.mi.health)把运行时状态(当前账户散列、心跳、运行/兼容性信息)
 * 写成自己 filesDir 下 `redmagichelper/status.json`;模块 app 侧经 [RootShell]
 * 读取该文件(读取其他应用私有目录需要 root,root 不可用时 UI 显示未接入)。
 *
 * 不采用 Settings.System:com.mi.health 是第三方应用,不持有 WRITE_SETTINGS,
 * 写入会被系统拒绝;文件通道在 hook 进程沙箱内天然可写,fail-closed。
 */
object MiHealthChannel {
    const val MODULE_DIR_NAME = "redmagichelper"
    const val STATUS_FILE_NAME = "status.json"

    /** 模块 app 侧经 root 读取的绝对路径(/data/data 是指向 /data/user/0 的符号链接)。 */
    const val STATUS_SHELL_PATH =
        "/data/data/com.mi.health/files/$MODULE_DIR_NAME/$STATUS_FILE_NAME"

    class Status(val account: String, val heartbeat: Long, val runtime: String, val compatibility: String) {
        fun fresh(now: Long = System.currentTimeMillis()): Boolean =
            heartbeat > 0 && now - heartbeat in 0..90_000L

        fun serialize(): String = JSONObject()
            .put("account", account)
            .put("heartbeat", heartbeat)
            .put("runtime", runtime)
            .put("compatibility", compatibility)
            .toString()

        companion object {
            fun parse(text: String?): Status? {
                if (text.isNullOrEmpty()) return null
                return try {
                    val json = JSONObject(text)
                    Status(
                        json.optString("account", ""),
                        json.optLong("heartbeat", 0L),
                        json.optString("runtime", ""),
                        json.optString("compatibility", "")
                    )
                } catch (ignored: Exception) {
                    null
                }
            }
        }
    }

    private val writeLock = Any()

    /** hook 侧(com.mi.health 进程)原子写状态文件:临时文件 + rename。 */
    fun writeStatus(context: Context, update: JSONObject.() -> Unit) {
        synchronized(writeLock) {
            runCatching {
                val dir = File(context.filesDir, MODULE_DIR_NAME)
                dir.mkdirs()
                val target = File(dir, STATUS_FILE_NAME)
                val json = runCatching { JSONObject(target.readText()) }.getOrElse { JSONObject() }
                json.update()
                val temp = File(dir, "$STATUS_FILE_NAME.tmp")
                temp.writeText(json.toString())
                if (!temp.renameTo(target)) {
                    target.delete()
                    temp.renameTo(target)
                }
            }
        }
    }

    /** 模块 app 侧经 root shell 读取状态;root 不可用或文件不存在时返回 null。 */
    fun readStatusViaRoot(): Status? {
        val result = RootShell.exec(
            "cat ${RootShell.quote(STATUS_SHELL_PATH)} 2>/dev/null",
            timeoutSeconds = 5,
            maxOutput = 16 * 1024
        )
        if (!result.isSuccess || result.output.isBlank()) return null
        return Status.parse(result.output.trim())
    }
}
