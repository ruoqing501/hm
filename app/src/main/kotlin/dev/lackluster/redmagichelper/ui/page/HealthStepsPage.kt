package dev.lackluster.redmagichelper.ui.page

import android.app.TimePickerDialog
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.preference.CheckboxPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.hook.rules.mihealth.StepPlan
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.utils.MiHealthChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * 小米运动健康步数增强设置页(对应 LS_Augment 的 HealthSettingsActivity)。
 *
 * 说明:步数计划关闭后停止后续新增,已保存记录保留;同步走健康应用自己的同步入口;
 * 模块无网络权限。账户绑定/运行状态经 [MiHealthChannel] 状态文件 + root shell 读取。
 */
@Composable
fun HealthStepsPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val existingPlan = remember { StepPlan.parse(SafeSP.getString(Pref.Key.MiHealth.PLAN, "")) }

    var bound by remember { mutableStateOf(SafeSP.getString(Pref.Key.MiHealth.ACCOUNT, "")) }
    var status by remember { mutableStateOf<MiHealthChannel.Status?>(null) }

    var planId by remember { mutableStateOf(existingPlan?.id ?: UUID.randomUUID().toString().replace("-", "")) }
    var planSeed by remember { mutableLongStateOf(existingPlan?.seed ?: Random.nextLong()) }
    var planDate by remember { mutableStateOf(existingPlan?.startDate ?: LocalDate.now()) }
    var fromMinute by remember { mutableIntStateOf(existingPlan?.fromMinute ?: SafeSP.getInt(Pref.Key.MiHealth.PLAN_FROM, 540)) }
    var toMinute by remember { mutableIntStateOf(existingPlan?.toMinute ?: SafeSP.getInt(Pref.Key.MiHealth.PLAN_TO, 1080)) }
    var executions by remember {
        mutableIntStateOf(
            existingPlan?.let { if (it.executions > 0) it.executions else 1 }
                ?: SafeSP.getInt(Pref.Key.MiHealth.PLAN_EXECUTIONS, 10)
        )
    }
    var stepsPerRun by remember {
        mutableIntStateOf(
            existingPlan?.let { if (it.executions > 0) it.stepsPerExecution else it.amount }
                ?: SafeSP.getInt(Pref.Key.MiHealth.PLAN_STEPS, 200)
        )
    }
    var weekdays by remember { mutableIntStateOf(existingPlan?.weekdays ?: SafeSP.getInt(Pref.Key.MiHealth.PLAN_WEEKDAYS, 31)) }
    var planValid by remember { mutableStateOf(true) }
    var preview by remember { mutableStateOf("") }

    fun timeText(minute: Int): String = String.format(Locale.ROOT, "%02d:%02d", (minute / 60) % 24, minute % 60)

    fun rebuildPlan() {
        if (bound.isEmpty()) {
            preview = context.getString(R.string.health_plan_need_account)
            return
        }
        try {
            val plan = StepPlan(
                planId, bound, ZoneId.systemDefault().id, planDate,
                fromMinute, toMinute, executions, stepsPerRun, weekdays, planSeed
            )
            SafeSP.putAny(Pref.Key.MiHealth.PLAN, plan.serialize())
            planValid = true
            var day = LocalDate.now()
            if (day.isBefore(plan.startDate)) day = plan.startDate
            var i = 0
            while (i < 7 && !plan.runsOn(day)) {
                day = day.plusDays(1)
                i++
            }
            val text = StringBuilder(context.getString(R.string.health_plan_preview, plan.amount))
            if (plan.runsOn(day)) {
                text.append('\n')
                var n = 0
                for ((at, steps) in plan.timetable(day)) {
                    if (n++ >= 8) {
                        text.append("……")
                        break
                    }
                    text.append(
                        Instant.ofEpochSecond(at).atZone(ZoneId.of(plan.zone))
                            .format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
                    ).append(" +").append(steps).append('\n')
                }
            }
            preview = text.toString().trim()
        } catch (ignored: Exception) {
            planValid = false
            preview = context.getString(R.string.health_plan_invalid)
        }
    }

    fun refreshStatus() {
        coroutineScope.launch {
            status = withContext(Dispatchers.IO) { MiHealthChannel.readStatusViaRoot() }
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
        rebuildPlan()
    }

    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.page_health_steps),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
    ) {
        // 账户绑定
        item {
            PreferenceGroup(
                title = stringResource(R.string.health_account_group),
                first = true
            ) {
                TextPreference(
                    title = stringResource(R.string.health_account_status),
                    summary = if (bound.isEmpty()) stringResource(R.string.health_account_unbound)
                    else stringResource(R.string.health_account_bound, bound.take(8))
                )
                TextPreference(
                    title = stringResource(R.string.health_runtime_status),
                    summary = status?.let {
                        listOf(it.runtime, it.compatibility).filter { s -> s.isNotEmpty() }.joinToString("\n")
                    }?.ifEmpty { null } ?: stringResource(R.string.health_status_unavailable),
                    onClick = { refreshStatus() }
                )
                TextPreference(
                    title = stringResource(R.string.health_bind_account),
                    summary = stringResource(R.string.health_bind_account_tips),
                    onClick = {
                        coroutineScope.launch {
                            val current = withContext(Dispatchers.IO) { MiHealthChannel.readStatusViaRoot() }
                            status = current
                            val active = current?.account ?: ""
                            if (!active.matches(Regex("[0-9a-f]{64}")) || current == null || !current.fresh()) {
                                makeText(context, R.string.health_bind_not_ready, LENGTH_LONG).show()
                                return@launch
                            }
                            if (bound != active) {
                                planId = UUID.randomUUID().toString().replace("-", "")
                                planSeed = Random.nextLong()
                                planDate = LocalDate.now()
                                SafeSP.putAny(
                                    Pref.Key.MiHealth.SINCE,
                                    (System.currentTimeMillis() / 1000).toString()
                                )
                            }
                            bound = active
                            SafeSP.putAny(Pref.Key.MiHealth.ACCOUNT, active)
                            rebuildPlan()
                            makeText(context, R.string.health_bind_success, LENGTH_SHORT).show()
                        }
                    }
                )
                TextPreference(
                    title = stringResource(R.string.health_open_app),
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(Scope.MI_HEALTH)
                        if (intent == null) {
                            makeText(context, R.string.health_open_app_not_found, LENGTH_LONG).show()
                        } else {
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
        // 总开关
        item {
            PreferenceGroup(title = null) {
                SwitchPreference(
                    title = stringResource(R.string.health_master_switch),
                    summary = stringResource(R.string.health_master_switch_tips),
                    key = Pref.Key.MiHealth.ENABLED
                )
            }
        }
        // 真实步数加倍
        item {
            var multiplyEnabled by remember {
                mutableStateOf(SafeSP.getBoolean(Pref.Key.MiHealth.MULTIPLY_ENABLED))
            }
            PreferenceGroup(title = null) {
                SwitchPreference(
                    title = stringResource(R.string.health_multiply_switch),
                    summary = stringResource(R.string.health_multiply_switch_tips),
                    key = Pref.Key.MiHealth.MULTIPLY_ENABLED,
                    onCheckedChange = { newValue ->
                        multiplyEnabled = newValue
                        if (newValue) {
                            if (bound.isEmpty()) {
                                makeText(context, R.string.health_plan_need_account, LENGTH_SHORT).show()
                            }
                            // 与 LS_Augment 一致:启用倍率时刷新生效起始时间,历史记录不加倍
                            SafeSP.putAny(
                                Pref.Key.MiHealth.SINCE,
                                (System.currentTimeMillis() / 1000).toString()
                            )
                        }
                    }
                )
                AnimatedVisibility(multiplyEnabled) {
                    SeekBarPreference(
                        title = stringResource(R.string.health_multiplier_value),
                        key = Pref.Key.MiHealth.MULTIPLIER,
                        defValue = 1,
                        min = 1,
                        max = 10
                    )
                }
            }
        }
        // 随机增加步数
        item {
            var planEnabled by remember {
                mutableStateOf(SafeSP.getBoolean(Pref.Key.MiHealth.PLAN_ENABLED))
            }
            PreferenceGroup(title = null) {
                SwitchPreference(
                    title = stringResource(R.string.health_plan_switch),
                    summary = stringResource(R.string.health_plan_switch_tips),
                    key = Pref.Key.MiHealth.PLAN_ENABLED,
                    onCheckedChange = { newValue ->
                        planEnabled = newValue
                        if (newValue) {
                            if (bound.isEmpty()) {
                                makeText(context, R.string.health_plan_need_account, LENGTH_SHORT).show()
                            } else if (!planValid) {
                                makeText(context, R.string.health_plan_invalid, LENGTH_LONG).show()
                            }
                        }
                    }
                )
                AnimatedVisibility(planEnabled) {
                    androidx.compose.foundation.layout.Column {
                        TextPreference(
                            title = stringResource(R.string.health_plan_from),
                            value = timeText(fromMinute),
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        fromMinute = h * 60 + m
                                        SafeSP.putAny(Pref.Key.MiHealth.PLAN_FROM, fromMinute)
                                        rebuildPlan()
                                    },
                                    fromMinute / 60, fromMinute % 60, true
                                ).show()
                            }
                        )
                        TextPreference(
                            title = stringResource(R.string.health_plan_to),
                            value = timeText(toMinute),
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        toMinute = h * 60 + m
                                        SafeSP.putAny(Pref.Key.MiHealth.PLAN_TO, toMinute)
                                        rebuildPlan()
                                    },
                                    toMinute / 60, toMinute % 60, true
                                ).show()
                            }
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.health_plan_executions),
                            key = Pref.Key.MiHealth.PLAN_EXECUTIONS,
                            defValue = 10,
                            min = 1,
                            max = 100,
                            onValueChange = { newValue ->
                                executions = newValue
                                rebuildPlan()
                            }
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.health_plan_steps),
                            key = Pref.Key.MiHealth.PLAN_STEPS,
                            defValue = 200,
                            min = 1,
                            max = 5000,
                            onValueChange = { newValue ->
                                stepsPerRun = newValue
                                rebuildPlan()
                            }
                        )
                        val weekdayLabels = listOf(
                            stringResource(R.string.health_weekday_1),
                            stringResource(R.string.health_weekday_2),
                            stringResource(R.string.health_weekday_3),
                            stringResource(R.string.health_weekday_4),
                            stringResource(R.string.health_weekday_5),
                            stringResource(R.string.health_weekday_6),
                            stringResource(R.string.health_weekday_7),
                        )
                        TextPreference(
                            title = stringResource(R.string.health_plan_weekdays),
                            summary = stringResource(R.string.health_plan_weekdays_tips)
                        )
                        weekdayLabels.forEachIndexed { index, label ->
                            CheckboxPreference(
                                title = label,
                                defValue = (weekdays and (1 shl index)) != 0,
                                onCheckedChange = { checked ->
                                    weekdays = if (checked) weekdays or (1 shl index)
                                    else weekdays and (1 shl index).inv()
                                    SafeSP.putAny(Pref.Key.MiHealth.PLAN_WEEKDAYS, weekdays)
                                    rebuildPlan()
                                }
                            )
                        }
                        TextPreference(
                            title = preview
                        )
                    }
                }
            }
        }
        // 说明
        item {
            PreferenceGroup(title = null) {
                TextPreference(
                    title = stringResource(R.string.health_notes)
                )
            }
        }
    }
}
