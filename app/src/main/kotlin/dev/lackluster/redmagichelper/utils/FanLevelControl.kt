package dev.lackluster.redmagichelper.utils

/**
 * 风扇等级固定:以 root 直接写 /sys/kernel/fan/fan_speed_level(1~5 级)。
 * 纯 Root 功能,不涉及 Xposed hook;只在模块 app 进程运行。
 */
internal object FanLevelControl {
    const val NODE = "/sys/kernel/fan/fan_speed_level"
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 5

    /** 写入档位并回读校验;调用方需自行比对 output 与目标档位。 */
    fun apply(level: Int): RootShell.Result {
        val safeLevel = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        return RootShell.exec(
            "echo $safeLevel > $NODE && cat $NODE",
            timeoutSeconds = 5,
            maxOutput = 1024
        )
    }

    /** 读取当前档位;失败返回 null。 */
    fun current(): Int? {
        val result = RootShell.exec("cat $NODE 2>/dev/null", timeoutSeconds = 5, maxOutput = 1024)
        return if (result.isSuccess) result.output.trim().toIntOrNull() else null
    }
}
