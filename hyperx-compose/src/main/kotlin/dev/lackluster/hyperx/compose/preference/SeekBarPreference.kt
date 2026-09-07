package dev.lackluster.hyperx.compose.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lackluster.hyperx.compose.R
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.ImageIcon
import dev.lackluster.hyperx.compose.base.DrawableResIcon
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SeekBarPreference(
    icon: ImageIcon? = null,
    title: String,
    key: String? = null,
    defValue: Int = 0,
    min: Int = 0,
    max: Int = 1,
    showValue: Boolean = true,
    format: String = "%d",
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    valueColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onValueChange: ((Int) -> Unit)? = null,
) {
    var spValue by remember { mutableIntStateOf(
        key?.let { SafeSP.getInt(it, defValue) } ?: defValue
    ) }
    val updatedOnValueChange by rememberUpdatedState(onValueChange)
    val dialogVisibility = remember { mutableStateOf(false) }

    val doOnInputConfirm: (String) -> Unit = { newString: String ->
        val oldValue = spValue
        val newValue = newString.toIntOrNull()
        if (newValue != null && newValue in min..max && oldValue != newValue) {
            spValue = newValue
            key?.let { SafeSP.putAny(it, newValue) }
            updatedOnValueChange?.let { it(newValue) }
        }
    }

    Column {
        BasicComponent(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (enabled) {
                    dialogVisibility.value = true
                }
            },
            insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 12.dp),
            title = title,
            titleColor = titleColor,
            leftAction = {
                icon?.let {
                    DrawableResIcon(it)
                }
            },
            rightActions = {
                if (showValue) {
                    Text(
                        text = String.format(Locale.current.platformLocale, format, spValue),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = valueColor.color(true),
                        textAlign = TextAlign.End,
                    )
                }
            },
            enabled = enabled
        )
        Slider(
            modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp),
            progress = spValue.toFloat(),
            minValue = min.toFloat(),
            maxValue = max.toFloat(),
            height = 28.dp,
            enabled = enabled,
            onProgressChange = { newValue ->
                val newInt = newValue.toInt()
                spValue = newInt
                key?.let { SafeSP.putAny(it, newInt) }
                updatedOnValueChange?.let { it1 -> it1(newInt) }
            }
        )
        EditTextDialog(
            visibility = dialogVisibility,
            title = title,
            message = stringResource(R.string.slider_dialog_message_decimal, defValue, min, max),
            placeholder = defValue.toString(),
            value = spValue.toString(),
            onInputConfirm = { newString ->
                doOnInputConfirm(newString)
            }
        )
    }
}

@Composable
fun SeekBarPreference(
    icon: ImageIcon? = null,
    title: String,
    key: String? = null,
    defValue: Float = 0.0f,
    min: Float = 0.0f,
    max: Float = 1.0f,
    showValue: Boolean = true,
    format: String = "%.2f",
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    valueColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onValueChange: ((Float) -> Unit)? = null,
) {
    var spValue by remember { mutableFloatStateOf(
        key?.let { SafeSP.getFloat(it, defValue) } ?: defValue
    ) }
    val updatedOnValueChange by rememberUpdatedState(onValueChange)
    val dialogVisibility = remember { mutableStateOf(false) }

    val doOnInputConfirm: (String) -> Unit = { newString: String ->
        val oldValue = spValue
        val newValue = newString.toFloatOrNull()
        if (newValue != null && newValue in min..max && oldValue != newValue) {
            spValue = newValue
            key?.let { SafeSP.putAny(it, newValue) }
            updatedOnValueChange?.let { it(newValue) }
        }
    }

    Column {
        BasicComponent(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (enabled) {
                    dialogVisibility.value = true
                }
            },
            insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 12.dp),
            title = title,
            titleColor = titleColor,
            leftAction = {
                icon?.let {
                    DrawableResIcon(it)
                }
            },
            rightActions = {
                if (showValue) {
                    Text(
                        text = String.format(Locale.current.platformLocale, format, spValue),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = valueColor.color(true),
                        textAlign = TextAlign.End,
                    )
                }
            },
            enabled = enabled
        )
        Slider(
            modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp),
            progress = spValue,
            minValue = min,
            maxValue = max,
            height = 28.dp,
            enabled = enabled,
            onProgressChange = { newValue ->
                spValue = newValue
                key?.let { SafeSP.putAny(it, newValue) }
                updatedOnValueChange?.let { it1 -> it1(newValue) }
            }
        )
        EditTextDialog(
            visibility = dialogVisibility,
            title = title,
            message = stringResource(R.string.slider_dialog_message_float, defValue, min, max),
            placeholder = defValue.toString(),
            value = spValue.toString(),
            onInputConfirm = { newString ->
                doOnInputConfirm(newString)
            }
        )
    }
}