package dev.lackluster.hyperx.compose.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.DrawableResIcon
import dev.lackluster.hyperx.compose.base.ImageIcon
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxColors
import top.yukonga.miuix.kmp.basic.CheckboxDefaults

@Composable
fun CheckboxPreference(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    key: String? = null,
    defValue: Boolean = false,
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    checkboxColors: CheckboxColors = CheckboxDefaults.checkboxColors(),
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
            Checkbox(
                modifier = Modifier,
                checked = spValue,
                onCheckedChange = { newValue ->
                    spValue = !spValue
                    key?.let { SafeSP.putAny(it, newValue) }
                    updatedOnCheckedChange?.invoke(newValue)
                },
                enabled = enabled,
                colors = checkboxColors
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