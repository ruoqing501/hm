package dev.lackluster.hyperx.compose.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.ImageIcon
import dev.lackluster.hyperx.compose.base.DrawableResIcon
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.SwitchColors
import top.yukonga.miuix.kmp.basic.SwitchDefaults

@Composable
fun SwitchPreference(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    key: String? = null,
    defValue: Boolean = false,
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    switchColors: SwitchColors = SwitchDefaults.switchColors(),
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    var spValue by remember { mutableStateOf(
        key?.let { SafeSP.getBoolean(it, defValue) } ?: defValue
    ) }
    val updatedOnCheckedChange by rememberUpdatedState(onCheckedChange)

    BasicComponent(
        insideMargin = PaddingValues((icon?.getHorizontalPadding() ?: 16.dp), 16.dp, 16.dp, 16.dp),
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        leftAction = {
            icon?.let {
                DrawableResIcon(it)
            }
        },
        rightActions = {
            Switch(
                checked = spValue,
                onCheckedChange = { newValue ->
                    spValue = !spValue
                    key?.let { SafeSP.putAny(it, newValue) }
                    updatedOnCheckedChange?.invoke(newValue)
                },
                enabled = enabled,
                colors = switchColors
            )
        },
        onClick = {
            if (enabled) {
                spValue = !spValue
                key?.let { SafeSP.putAny(it, spValue) }
                updatedOnCheckedChange?.invoke(spValue)
            }
        },
        enabled = enabled
    )
}

@Composable
fun SwitchPreference(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    key: String? = null,
    defValue: Boolean = false,
    enabled: Boolean = true,
    checked: MutableState<Boolean>,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    switchColors: SwitchColors = SwitchDefaults.switchColors(),
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    key?.let {
        checked.value = SafeSP.getBoolean(it, defValue)
    }
    val updatedOnCheckedChange by rememberUpdatedState(onCheckedChange)

    BasicComponent(
        insideMargin = PaddingValues((icon?.getHorizontalPadding() ?: 16.dp), 16.dp, 16.dp, 16.dp),
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        leftAction = {
            icon?.let {
                DrawableResIcon(it)
            }
        },
        rightActions = {
            Switch(
                checked = checked.value,
                onCheckedChange = { newValue ->
                    key?.let { SafeSP.putAny(it, newValue) }
                    updatedOnCheckedChange?.invoke(newValue)
                },
                enabled = enabled,
                colors = switchColors
            )
        },
        onClick = {
            if (enabled) {
                checked.value = !checked.value
                key?.let { SafeSP.putAny(it, checked.value) }
                updatedOnCheckedChange?.invoke(checked.value)
            }
        },
        enabled = enabled
    )
}