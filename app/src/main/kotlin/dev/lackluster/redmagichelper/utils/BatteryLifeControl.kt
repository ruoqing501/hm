package dev.lackluster.redmagichelper.utils

import dev.lackluster.redmagichelper.hook.compat.log.YLog

/**
 * 电池循环信息读取与「按循环降压」策略开关(移植自 LS_Augment 的
 * BatteryLifeControl/BatteryLifePolicy,工作目录改为 /data/adb/redmagichelper)。
 *
 * 纯 Root 功能,不涉及 Xposed hook。保留 LS_Augment 的可逆设计:
 * 原厂文件与计数器从不被写入,仅通过 mount --bind 用副本覆盖
 * /vendor/etc/.tp/zte_battery_life.conf;关闭开关即 umount 还原。
 * fail-closed:任何一步失败都会回滚 bind 覆盖并尽量恢复原状。
 *
 * 本对象只在模块 app 进程运行,通过 [RootShell] 以 root 执行命令;
 * Root 不可用时所有操作如实失败,由 UI 降级处理。
 */
internal object BatteryLifeControl {
    private const val ORIGINAL = "/vendor/etc/.tp/zte_battery_life.conf"
    private const val DIRECTORY = "/data/adb/redmagichelper/v2/battery"
    private const val OVERLAY = "$DIRECTORY/life.conf"
    private const val SERVICE = "vendor.zte.ldd-default"

    private val FILES = arrayOf(
        "/sys/class/power_supply/battery/cycle_count",
        "/sys/class/qcom-battery/battery_cycle",
        "/mnt/vendor/persist/zstats/cycle.dat",
        "/sys/class/power_supply/battery/charge_full",
        "/sys/class/power_supply/battery/charge_full_design",
        "/sys/class/power_supply/battery/voltage_max",
        "/sys/class/qcom-battery/soh",
        "/sys/class/qcom-battery/cis_level",
        "/sys/class/qcom-battery/expan_level",
        ORIGINAL
    )

    const val PATH_CYCLE_COUNT = "/sys/class/power_supply/battery/cycle_count"
    const val PATH_BATTERY_CYCLE = "/sys/class/qcom-battery/battery_cycle"
    const val PATH_CYCLE_DAT = "/mnt/vendor/persist/zstats/cycle.dat"
    const val PATH_CHARGE_FULL = "/sys/class/power_supply/battery/charge_full"
    const val PATH_CHARGE_FULL_DESIGN = "/sys/class/power_supply/battery/charge_full_design"

    /** 探测 root 是否可用;不可用时 UI 应停用相关功能。 */
    fun rootGranted(): Boolean {
        val result = RootShell.exec("id -u", timeoutSeconds = 5, maxOutput = 1024)
        return result.isSuccess && result.output.trim() == "0"
    }

    /** 读取各信息源原始内容;失败时返回仅含 "error" 键的表。 */
    fun read(): Map<String, String> {
        val command = StringBuilder()
        for (path in FILES) {
            command.append("printf '\\nRMH_FILE:").append(path)
                .append("\\n'; head -c 32768 ").append(RootShell.quote(path))
                .append(" 2>/dev/null; printf '\\nRMH_END\\n'; ")
        }
        val result = RootShell.exec(command.toString(), timeoutSeconds = 12, maxOutput = 65536)
        val values = LinkedHashMap<String, String>()
        if (!result.isSuccess) {
            YLog.warn("BatteryLifeControl read failed: ${result.publicError()}")
            values["error"] = result.publicError()
            return values
        }
        for (part in result.output.split("RMH_FILE:")) {
            val line = part.indexOf('\n')
            val end = part.indexOf("\nRMH_END")
            if (line > 0 && end >= line) {
                val value = part.substring(line + 1, end).trim()
                if (value.isNotEmpty()) values[part.substring(0, line).trim()] = value
            }
        }
        return values
    }

    /**
     * 按开关目标状态对齐系统:开启时 bind 覆盖原厂配置并重启 vendor.zte.ldd-default,
     * 关闭时 umount 还原。可重复调用(幂等);任何失败都不留半成品状态。
     */
    @Synchronized
    fun reconcile(enabled: Boolean): RootShell.Result {
        val ownership = "[ -f '$OVERLAY' ] && [ \"\$(stat -c %d:%i '$OVERLAY')\" = \"\$(stat -c %d:%i '$ORIGINAL')\" ]"
        val result: RootShell.Result
        if (!enabled) {
            val script = restartFunction() + "if " + ownership + "; then " +
                    "umount '$ORIGINAL' || exit 41; " +
                    "if ! rmh_restart; then mount --bind '$OVERLAY' '$ORIGINAL'; " +
                    "setprop ctl.restart '$SERVICE'; exit 42; fi; fi; echo restored_vendor_policy"
            result = rootNamespace(script, null)
        } else {
            val current = rootNamespace("cat '$ORIGINAL'", null)
            val reduction = if (current.isSuccess) BatteryLifePolicy.reductionEnabled(current.output) else null
            if (reduction == null) {
                result = RootShell.Result(40, "未识别到可确认的原厂按循环降压配置，未进行修改", false)
            } else if (!reduction) {
                result = RootShell.Result(0, "按循环降压已关闭；未改动循环次数、健康度或充电保护", false)
            } else {
                val changed = BatteryLifePolicy.disableReduction(current.output)
                val script = "set -e; " + restartFunction() +
                        "rmh_committed=0; rollback() { if " + ownership + "; then umount '$ORIGINAL" +
                        "' || return; setprop ctl.restart '$SERVICE'; fi; }; " +
                        "trap 'if [ \$rmh_committed -ne 1 ]; then rollback; fi' EXIT; " +
                        "trap 'exit 46' HUP INT TERM; umask 077; mkdir -p '$DIRECTORY'; " +
                        "if " + ownership + "; then umount '$ORIGINAL'; fi; " +
                        "cat > '$OVERLAY'; chmod 0644 '$OVERLAY'; " +
                        "chcon \"\$(ls -Zd '$ORIGINAL' | awk '{print \$1}')\" '$OVERLAY'; " +
                        "mount --bind '$OVERLAY' '$ORIGINAL'; " +
                        "cmp -s '$OVERLAY' '$ORIGINAL' || exit 43; " +
                        "rmh_restart || exit 45; rmh_committed=1; trap - EXIT HUP INT TERM; " +
                        "echo age_voltage_reduction_disabled_and_service_reloaded"
                result = rootNamespace(script, changed)
            }
        }
        if (!result.isSuccess) {
            YLog.warn("BatteryLifeControl reconcile(enabled=$enabled) failed: ${result.publicError()}")
        }
        return result
    }

    private fun rootNamespace(script: String, input: String?): RootShell.Result {
        return RootShell.exec("nsenter -t 1 -m -- sh -c " + RootShell.quote(script), input, 18, 65536)
    }

    private fun restartFunction(): String {
        return "rmh_restart() { rmh_old_pid=\$(pidof vendor.zte.ldd.cmd-service); " +
                "setprop ctl.restart '$SERVICE' || return 1; rmh_n=0; " +
                "while [ \"\$(getprop init.svc.$SERVICE)\" != running ] || " +
                "[ -z \"\$(pidof vendor.zte.ldd.cmd-service)\" ] || " +
                "[ \"\$(pidof vendor.zte.ldd.cmd-service)\" = \"\$rmh_old_pid\" ]; do " +
                "rmh_n=\$((rmh_n+1)); [ \$rmh_n -lt 10 ] || return 1; sleep 1; done; }; "
    }
}

/** 原厂按循环降压策略独立于学习容量与循环计数器;解析与 LS_Augment 一致。 */
internal object BatteryLifePolicy {
    private val REDUCTION = Regex("(?m)^(\\s*zbl,reduce-fcv-enable\\s*=\\s*<)([01])(>[^\\r\\n]*)$")

    /** 无法唯一确认时返回 null(fail-closed,调用方不得修改)。 */
    fun reductionEnabled(config: String?): Boolean? {
        if (config == null || config.length > 32768) return null
        val match = REDUCTION.find(config) ?: return null
        val value = match.groupValues[2] == "1"
        return if (REDUCTION.find(config, match.range.last + 1) != null) null else value
    }

    fun disableReduction(config: String?): String? {
        if (reductionEnabled(config) == null) return null
        val match = REDUCTION.find(config!!) ?: return null
        return config.substring(0, match.groups[2]!!.range.first) + "0" +
                config.substring(match.groups[2]!!.range.last + 1)
    }

    /** cycle.dat 为「1:NNN;」格式记录时取最后一条循环数。 */
    fun lastRecordedCycles(record: String?): Long? {
        if (record == null) return null
        val pattern = Regex("(?:^|;)\\s*1:([0-9]+);")
        var result: Long? = null
        for (match in pattern.findAll(record)) {
            match.groupValues[1].toLongOrNull()?.let { result = it }
        }
        return result
    }
}
