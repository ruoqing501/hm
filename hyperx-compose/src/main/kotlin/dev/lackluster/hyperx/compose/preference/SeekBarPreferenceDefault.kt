package dev.lackluster.hyperx.compose.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
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
import kotlin.math.abs
import kotlin.math.roundToInt

private const val UNSPECIFIED_INT = -1
private const val UNSPECIFIED_FLOAT = -1f
private const val EPSILON = 1e-6f

/**
 * 整型滑块偏好设置项（带默认区域）
 * @param defaultThreshold 默认区域宽度（整数），滑块值在 [min, min+defaultThreshold] 范围内时视为默认
 */
@Composable
fun SeekBarPreferenceDefault(
    icon: ImageIcon? = null,
    title: String,
    key: String? = null,
    defValue: Int = -1,
    min: Int = 0,
    max: Int = 1,
    defaultThreshold: Int = 0,          // 新增参数
    showValue: Boolean = true,
    format: String = "%d",
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    valueColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onValueChange: ((Int) -> Unit)? = null,
) {
    // 初始存储值
    val initialStored = if (key != null) {
        SafeSP.getInt(key, UNSPECIFIED_INT)
    } else {
        defValue
    }
    // 初始滑块位置：若存储为默认，则置于 min；否则为存储值
    val initialSlider = if (initialStored == UNSPECIFIED_INT && key != null) min else initialStored
    var sliderValue by remember { mutableIntStateOf(initialSlider) }
    var storedValue by remember { mutableIntStateOf(initialStored) }
    val updatedOnValueChange by rememberUpdatedState(onValueChange)

    val dialogVisibility = remember { mutableStateOf(false) }

    // 根据滑块值更新存储值
    fun updateFromSlider(newSlider: Int) {
        sliderValue = newSlider
        val newStored = if (key != null) {
            // 在默认区域内存储 -1，否则存储实际值
            if (newSlider <= min + defaultThreshold) UNSPECIFIED_INT else newSlider
        } else {
            newSlider
        }
        if (storedValue != newStored) {
            storedValue = newStored
            key?.let { SafeSP.putAny(it, newStored) }
            updatedOnValueChange?.let { it(newStored) }
        }
    }

    Column {
        BasicComponent(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (enabled) dialogVisibility.value = true
            },
            insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 12.dp),
            title = title,
            titleColor = titleColor,
            leftAction = { icon?.let { DrawableResIcon(it) } },
            rightActions = {
                if (showValue) {
                    Text(
                        text = if (storedValue == UNSPECIFIED_INT && key != null) {
                            stringResource(R.string.default_value)
                        } else {
                            String.format(Locale.current.platformLocale, format, sliderValue)
                        },
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
            progress = sliderValue.toFloat(),
            minValue = min.toFloat(),
            maxValue = max.toFloat(),
            height = 28.dp,
            enabled = enabled,
            onProgressChange = { newFloat ->
                val newInt = newFloat.roundToInt().coerceIn(min, max)
                updateFromSlider(newInt)
            }
        )

        EditTextDialog(
            visibility = dialogVisibility,
            title = title,
            message = stringResource(R.string.slider_dialog_message_decimal, defValue, min, max),
            placeholder = defValue.toString(),
            value = sliderValue.toString(),
            onInputConfirm = { newString ->
                val newInt = newString.toIntOrNull()
                if (newInt != null && newInt in min..max) {
                    updateFromSlider(newInt)
                }
            }
        )
    }
}






/**
 * 整型滑块偏好设置项（带默认区域）
 * @param defaultThreshold 默认区域宽度（整数），滑块值在 [min, min+defaultThreshold] 范围内时视为默认
 */
@Composable
fun SeekBarPreferenceDefault2(
    icon: ImageIcon? = null,
    title: String,
    key: String? = null,
    defValue: Int = -1,
    min: Int = 0,
    max: Int = 1,
    defaultThreshold: Int = 0,          // 默认区域宽度
    showValue: Boolean = true,
    format: String = "%d",
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    valueColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onValueChange: ((Int) -> Unit)? = null,
) {
    // 初始存储值
    val initialStored = if (key != null) {
        SafeSP.getInt(key, UNSPECIFIED_INT)
    } else {
        defValue
    }
    // 初始滑块位置：若存储为默认，则置于 defValue；否则为存储值
    val initialSlider = if (initialStored == UNSPECIFIED_INT && key != null) defValue else initialStored
    var sliderValue by remember { mutableIntStateOf(initialSlider) }
    var storedValue by remember { mutableIntStateOf(initialStored) }
    val updatedOnValueChange by rememberUpdatedState(onValueChange)

    val dialogVisibility = remember { mutableStateOf(false) }

    /**
     * 更新滑块值和存储状态
     * @param fromDialog true 表示来自对话框输入，此时即使值在默认区域也存储实际值
     */
    fun updateFromSlider(newSlider: Int, fromDialog: Boolean = false) {
        val newStored = if (key != null) {
            // 只有来自滑块且位于默认区域时才存 UNSPECIFIED_INT，否则存实际值
            if (!fromDialog && newSlider <= min + defaultThreshold) {
                // 存储为默认，同时将滑块位置跳转到 defValue
                sliderValue = defValue
                UNSPECIFIED_INT
            } else {
                sliderValue = newSlider
                newSlider
            }
        } else {
            sliderValue = newSlider
            newSlider
        }

        if (storedValue != newStored) {
            storedValue = newStored
            key?.let { SafeSP.putAny(it, newStored) }
            updatedOnValueChange?.let { it(newStored) }
        }
    }

    Column {
        BasicComponent(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (enabled) dialogVisibility.value = true
            },
            insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 12.dp),
            title = title,
            titleColor = titleColor,
            leftAction = { icon?.let { DrawableResIcon(it) } },
            rightActions = {
                if (showValue) {
                    Text(
                        text = if (storedValue == UNSPECIFIED_INT && key != null) {
                            stringResource(R.string.default_value)
                        } else {
                            String.format(Locale.current.platformLocale, format, sliderValue)
                        },
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
            progress = sliderValue.toFloat(),
            minValue = min.toFloat(),
            maxValue = max.toFloat(),
            height = 28.dp,
            enabled = enabled,
            onProgressChange = { newFloat ->
                val newInt = newFloat.roundToInt().coerceIn(min, max)
                updateFromSlider(newInt, fromDialog = false) // 滑块拖动
            }
        )

        EditTextDialog(
            visibility = dialogVisibility,
            title = title,
            message = stringResource(R.string.slider_dialog_message_decimal, defValue, min, max),
            placeholder = defValue.toString(),
            value = sliderValue.toString(),
            onInputConfirm = { newString ->
                val newInt = newString.toIntOrNull()
                if (newInt != null && newInt in min..max) {
                    updateFromSlider(newInt, fromDialog = true) // 对话框输入
                }
            }
        )
    }
}

/**
 * 整型滑块偏好设置项（带默认区域）
 * @param defaultThreshold 默认区域宽度（整数），滑块值在 [min, min+defaultThreshold] 范围内时视为默认
 */
@Composable
fun SeekBarPreferenceDefault3(
    icon: ImageIcon? = null,
    title: String,
    key: String? = null,
    defValue: Int = -1,
    min: Int = 0,
    max: Int = 1,
    defaultThreshold: Int = 0,          // 默认区域宽度
    showValue: Boolean = true,
    format: String = "%d",
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    valueColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onValueChange: ((Int) -> Unit)? = null,
) {
    // 初始存储值
    val initialStored = if (key != null) {
        SafeSP.getInt(key, UNSPECIFIED_INT)
    } else {
        defValue
    }
    // 初始滑块位置：若存储为默认，则置于 defValue；否则为存储值
    val initialSlider = if (initialStored == UNSPECIFIED_INT && key != null) defValue else initialStored
    var sliderValue by remember { mutableIntStateOf(initialSlider) }
    var storedValue by remember { mutableIntStateOf(initialStored) }
    val updatedOnValueChange by rememberUpdatedState(onValueChange)

    val dialogVisibility = remember { mutableStateOf(false) }

    /**
     * 更新滑块值和存储状态
     * @param fromDialog true 表示来自对话框输入，此时即使值在默认区域也存储实际值
     */
    fun updateFromSlider(newSlider: Int, fromDialog: Boolean = false) {
        val newStored = if (key != null) {
            // 只有来自滑块且位于默认区域时才存 UNSPECIFIED_INT，否则存实际值
            if (!fromDialog && newSlider <= min + defaultThreshold) {
                // 存储为默认，同时将滑块位置跳转到 defValue
                sliderValue = defValue
                UNSPECIFIED_INT
            } else {
                sliderValue = newSlider
                newSlider
            }
        } else {
            sliderValue = newSlider
            newSlider
        }

        if (storedValue != newStored) {
            storedValue = newStored
            key?.let { SafeSP.putAny(it, newStored) }
            updatedOnValueChange?.let { it(newStored) }
        }
    }

    Column {
        BasicComponent(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (enabled) dialogVisibility.value = true
            },
            insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 12.dp),
            title = title,
            titleColor = titleColor,
            leftAction = { icon?.let { DrawableResIcon(it) } },
            rightActions = {
                if (showValue) {
                    Text(
                        text = if (storedValue == UNSPECIFIED_INT && key != null) {
                            stringResource(R.string.default_value)
                        } else {
                            String.format(Locale.current.platformLocale, format, sliderValue)
                        },
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
            progress = sliderValue.toFloat(),
            minValue = min.toFloat(),
            maxValue = max.toFloat(),
            height = 28.dp,
            enabled = enabled,
            onProgressChange = { newFloat ->
                val newInt = newFloat.roundToInt().coerceIn(min, max)
                updateFromSlider(newInt, fromDialog = false) // 滑块拖动
            }
        )

        EditTextDialog(
            visibility = dialogVisibility,
            title = title,
//            message = stringResource(R.string.slider_dialog_message_decimal, defValue, min, max),
            message = stringResource(R.string.slider_dialog_message_decimal2,  min, max),
            placeholder = defValue.toString(),
            value = sliderValue.toString(),
            onInputConfirm = { newString ->
                val newInt = newString.toIntOrNull()
                if (newInt != null && newInt in min..max) {
                    updateFromSlider(newInt, fromDialog = true) // 对话框输入
                }
            }
        )
    }
}

/**
 * 浮点型滑块偏好设置项
 * @param icon 左侧图标
 * @param title 标题
 * @param key SharedPreferences 的键名，若为 null 则不持久化
 * @param defValue 默认值（当存储值为 -1f 或首次使用时生效）
 * @param min 滑块最小值
 * @param max 滑块最大值
 * @param showValue 是否在右侧显示当前值（若当前值为默认则显示“默认”文字）
 * @param format 数值格式化字符串，如 "%.2f"
 * @param enabled 是否启用交互
 * @param titleColor 标题颜色
 * @param valueColor 右侧数值颜色
 * @param onValueChange 值变化回调（参数为实际存储值，-1f 表示使用默认值）
 */
@Composable
fun SeekBarPreferenceFloatDefault(
    icon: ImageIcon? = null,
    title: String,
    key: String? = null,
    defValue: Float = -1.0f,
    min: Float = 0.0f,
    max: Float = 1.0f,
    showValue: Boolean = true,
    format: String = "%.2f",
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    valueColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onValueChange: ((Float) -> Unit)? = null,
) {
    // 初始存储值：有 key 且存在时读实际值，否则为 -1f（表示未指定）；无 key 时直接使用 defValue
    val initialStored = if (key != null) {
        SafeSP.getFloat(key, UNSPECIFIED_FLOAT)
    } else {
        defValue
    }
    var storedValue by remember { mutableFloatStateOf(initialStored) }
    val updatedOnValueChange by rememberUpdatedState(onValueChange)

    // 实际展示值：若存储为 -1f 且存在 key，则使用 defValue；否则使用存储值
    val displayValue = if (storedValue == UNSPECIFIED_FLOAT && key != null) defValue else storedValue

    val dialogVisibility = remember { mutableStateOf(false) }

    // 判断当前值是否为“默认”状态（即存储值为 -1f 且存在 key）
    val isDefault = key != null && storedValue == UNSPECIFIED_FLOAT

    // 判断两个浮点数是否相等（考虑精度）
    fun isApproxEqual(a: Float, b: Float): Boolean = abs(a - b) < EPSILON

    fun updateValue(newRawValue: Float) {
        val newStored = if (key != null) {
            // 有 key 时，若新值约等于 defValue 则存 -1f，否则存新值
            if (isApproxEqual(newRawValue, defValue)) UNSPECIFIED_FLOAT else newRawValue
        } else {
            newRawValue
        }
        if (!isApproxEqual(storedValue, newStored)) {
            storedValue = newStored
            key?.let { SafeSP.putAny(it, newStored) }
            updatedOnValueChange?.let { it(if (key != null) newStored else newRawValue) }
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
                icon?.let { DrawableResIcon(it) }
            },
            rightActions = {
                if (showValue) {
                    Text(
                        text = if (isDefault) {
                            stringResource(R.string.default_value) // 需要定义字符串资源，如 "默认"
                        } else {
                            String.format(Locale.current.platformLocale, format, displayValue)
                        },
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
            progress = displayValue,
            minValue = min,
            maxValue = max,
            height = 28.dp,
            enabled = enabled,
            onProgressChange = { newValue ->
                val newClamped = newValue.coerceIn(min, max)
                updateValue(newClamped)
            }
        )

        EditTextDialog(
            visibility = dialogVisibility,
            title = title,
//            message = stringResource(R.string.slider_dialog_message_float, defValue, min, max),
            message = stringResource(R.string.slider_dialog_message_float2,  min, max),
            placeholder = defValue.toString(),
            value = displayValue.toString(), // 对话框显示当前实际数值（默认时显示 defValue）
            onInputConfirm = { newString ->
                val newFloat = newString.toFloatOrNull()
                if (newFloat != null && newFloat in min..max) {
                    updateValue(newFloat)
                }
            }
        )
    }
}


/**
 * 浮点型滑块偏好设置项（带默认值吸附）
 * @param defaultThreshold 判断是否接近默认值的阈值，当滑块值与 defValue 之差小于此值时视为默认（建议设为 0.005 以匹配两位小数精度）
 */
@Composable
fun SeekBarPreferenceFloatDefault2(
    icon: ImageIcon? = null,
    title: String,
    key: String? = null,
    defValue: Float = -1.0f,
    min: Float = 0.0f,
    max: Float = 1.0f,
    showValue: Boolean = true,
    format: String = "%.2f",
    defaultThreshold: Float = 0.1f, // 默认区域宽度，可根据需要调整
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    valueColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onValueChange: ((Float) -> Unit)? = null,
) {
    // 初始存储值
    val initialStored = if (key != null) {
        SafeSP.getFloat(key, UNSPECIFIED_FLOAT)
    } else {
        defValue
    }
    // 初始滑块位置：若存储为默认，则置于最小值；否则为存储值
    val initialSlider = if (initialStored == UNSPECIFIED_FLOAT && key != null) min else initialStored
    var sliderValue by remember { mutableFloatStateOf(initialSlider) }
    var storedValue by remember { mutableFloatStateOf(initialStored) }
    val updatedOnValueChange by rememberUpdatedState(onValueChange)

    val dialogVisibility = remember { mutableStateOf(false) }

    // 根据滑块值更新存储值（并同步SP和回调）
    fun updateFromSlider(newSlider: Float) {
        sliderValue = newSlider
        val newStored = if (key != null) {
            // 在默认区域内存储 -1，否则存储实际值
            if (newSlider < min + defaultThreshold) UNSPECIFIED_FLOAT else newSlider
        } else {
            newSlider
        }
        if (abs(storedValue - newStored) >= EPSILON) {
            storedValue = newStored
            key?.let { SafeSP.putAny(it, newStored) }
            updatedOnValueChange?.let { it(newStored) }
        }
    }

    Column {
        BasicComponent(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (enabled) dialogVisibility.value = true
            },
            insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 12.dp),
            title = title,
            titleColor = titleColor,
            leftAction = { icon?.let { DrawableResIcon(it) } },
            rightActions = {
                if (showValue) {
                    Text(
                        text = if (storedValue == UNSPECIFIED_FLOAT && key != null) {
                            stringResource(R.string.default_value) // 显示“默认”
                        } else {
                            String.format(Locale.current.platformLocale, format, sliderValue)
                        },
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
            progress = sliderValue,
            minValue = min,
            maxValue = max,
            height = 28.dp,
            enabled = enabled,
            onProgressChange = { newValue ->
                val newClamped = newValue.coerceIn(min, max)
                updateFromSlider(newClamped)
            }
        )

        EditTextDialog(
            visibility = dialogVisibility,
            title = title,
//            message = stringResource(R.string.slider_dialog_message_float, defValue, min, max),
            message = stringResource(R.string.slider_dialog_message_float2,  min, max),
            placeholder = defValue.toString(),
            value = sliderValue.toString(),
            onInputConfirm = { newString ->
                val newFloat = newString.toFloatOrNull()
                if (newFloat != null && newFloat in min..max) {
                    updateFromSlider(newFloat)
                }
            }
        )
    }
}

@Composable
fun SeekBarPreferenceFloatDefault3(
    icon: ImageIcon? = null,
    title: String,
    key: String? = null,
    defValue: Float = -1.0f,
    min: Float = 0.0f,
    max: Float = 1.0f,
    showValue: Boolean = true,
    format: String = "%.2f",
    defaultThreshold: Float = 0.1f,
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    valueColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onValueChange: ((Float) -> Unit)? = null,
) {
    val initialStored = if (key != null) {
        SafeSP.getFloat(key, UNSPECIFIED_FLOAT)
    } else {
        defValue
    }
    val initialSlider = if (initialStored == UNSPECIFIED_FLOAT && key != null) min else initialStored
    var sliderValue by remember { mutableFloatStateOf(initialSlider) }
    var storedValue by remember { mutableFloatStateOf(initialStored) }
    val updatedOnValueChange by rememberUpdatedState(onValueChange)
    val dialogVisibility = remember { mutableStateOf(false) }

    // 更新函数，增加 fromDialog 参数
    fun updateFromSlider(newSlider: Float, fromDialog: Boolean = false) {
        sliderValue = newSlider
        val newStored = if (key != null) {
            // 只有来自滑块且位于默认区域时才存 -1f，否则存实际值
            if (!fromDialog && newSlider < min + defaultThreshold) UNSPECIFIED_FLOAT else newSlider
        } else {
            newSlider
        }
        if (abs(storedValue - newStored) >= EPSILON) {
            storedValue = newStored
            key?.let { SafeSP.putAny(it, newStored) }
            updatedOnValueChange?.let { it(newStored) }
        }
    }

    Column {
        BasicComponent(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (enabled) dialogVisibility.value = true
            },
            insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 12.dp),
            title = title,
            titleColor = titleColor,
            leftAction = { icon?.let { DrawableResIcon(it) } },
            rightActions = {
                if (showValue) {
                    Text(
                        // 右侧显示逻辑保持不变：根据 storedValue 判断是否显示“默认”
                        text = if (storedValue == UNSPECIFIED_FLOAT && key != null) {
                            stringResource(R.string.default_value)
                        } else {
                            String.format(Locale.current.platformLocale, format, sliderValue)
                        },
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
            progress = sliderValue,
            minValue = min,
            maxValue = max,
            height = 28.dp,
            enabled = enabled,
            onProgressChange = { newValue ->
                val newClamped = newValue.coerceIn(min, max)
                updateFromSlider(newClamped, fromDialog = false) // 滑块拖动
            }
        )

        EditTextDialog(
            visibility = dialogVisibility,
            title = title,
            message = stringResource(R.string.slider_dialog_message_float2, min, max),
            placeholder = defValue.toString(),
            value = sliderValue.toString(),
            onInputConfirm = { newString ->
                val newFloat = newString.toFloatOrNull()
                if (newFloat != null && newFloat in min..max) {
                    updateFromSlider(newFloat, fromDialog = true) // 对话框输入
                }
            }
        )
    }
}


