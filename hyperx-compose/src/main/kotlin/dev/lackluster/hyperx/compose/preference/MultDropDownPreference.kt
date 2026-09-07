package dev.lackluster.hyperx.compose.preference


import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.lackluster.hyperx.compose.R
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.ImageIcon
import dev.lackluster.hyperx.compose.base.DrawableResIcon
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SpinnerEntry
import top.yukonga.miuix.kmp.extra.SpinnerItemImpl
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.basic.ArrowUpDownIntegrated
import top.yukonga.miuix.kmp.interfaces.HoldDownInteraction
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Collections.emptySet

@Composable
fun MultDropDownPreference(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    entries: List<DropDownEntry>,
    key: String? = null,
    defValue: Int = 0,
    mode: DropDownMode = DropDownMode.AlwaysOnRight,
    showValue: Boolean = true,
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    rightActionColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onSelectedIndexChange: ((Int) -> Unit)? = null,
    // 新增多选相关参数
    multiple: Boolean = false,
    onSelectedValuesChange: ((Set<Int>) -> Unit)? = null,
) {
    if (multiple) {
        // ==================== 多选模式 ====================
        MultiSelectDropDownContent(
            icon = icon,
            title = title,
            summary = summary,
            entries = entries,
            key = key,
            showValue = showValue,
            enabled = enabled,
            titleColor = titleColor,
            summaryColor = summaryColor,
            rightActionColor = rightActionColor,
            onSelectedValuesChange = onSelectedValuesChange,
        )
    } else {
        // ==================== 原单选模式（保持不变） ====================
        var spValue by remember {
            mutableIntStateOf(
                (key?.let { SafeSP.getInt(it, defValue) } ?: defValue).coerceIn(
                    minimumValue = 0,
                    maximumValue = entries.size - 1
                )
            )
        }
        val updatedOnSelectedIndexChange by rememberUpdatedState(onSelectedIndexChange)
        val wrappedEntries = entries.map { entry ->
            SpinnerEntry(
                icon = { imageModifier ->
                    entry.iconVector?.let {
                        Image(
                            modifier = imageModifier,
                            imageVector = it,
                            contentDescription = null,
                            colorFilter = entry.iconTint?.let { tint -> ColorFilter.tint(tint) }
                        )
                    } ?: entry.iconRes?.let {
                        Image(
                            modifier = imageModifier,
                            painter = painterResource(it),
                            contentDescription = null,
                            colorFilter = entry.iconTint?.let { tint -> ColorFilter.tint(tint) }
                        )
                    } ?: entry.iconBitmap?.let {
                        Image(
                            modifier = imageModifier,
                            bitmap = it,
                            contentDescription = null,
                            colorFilter = entry.iconTint?.let { tint -> ColorFilter.tint(tint) }
                        )
                    }
                },
                title = entry.title,
                summary = entry.summary,
            )
        }

        val interactionSource = remember { MutableInteractionSource() }
        val isDropdownExpanded = remember { mutableStateOf(false) }
        val showPopup = remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val held = remember { mutableStateOf<HoldDownInteraction.HoldDown?>(null) }
        val hapticFeedback = LocalHapticFeedback.current
        var alignLeft by rememberSaveable { mutableStateOf(true) }

        DisposableEffect(Unit) {
            onDispose {
                showPopup.value = false
                isDropdownExpanded.value = false
            }
        }

        if (!isDropdownExpanded.value) {
            held.value?.let { oldValue ->
                coroutineScope.launch {
                    interactionSource.emit(HoldDownInteraction.Release(oldValue))
                }
                held.value = null
            }
        }

        BasicComponent(
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (enabled) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Move) {
                                val eventChange = event.changes.first()
                                alignLeft = eventChange.position.x < (size.width / 2)
                            }
                        }
                    }
                },
            interactionSource = interactionSource,
            insideMargin = PaddingValues((icon?.getHorizontalPadding() ?: 16.dp), 16.dp, 16.dp, 16.dp),
            title = title,
            titleColor = titleColor,
            summary = summary,
            summaryColor = summaryColor,
            leftAction = {
                if (mode != DropDownMode.Dialog) {
                    if (isDropdownExpanded.value) {
                        ListPopup(
                            show = showPopup,
                            alignment = if ((mode == DropDownMode.AlwaysOnRight || !alignLeft))
                                PopupPositionProvider.Align.Right
                            else
                                PopupPositionProvider.Align.Left,
                            onDismissRequest = {
                                showPopup.value = false
                                isDropdownExpanded.value = false
                            }
                        ) {
                            ListPopupColumn {
                                wrappedEntries.forEachIndexed { index, spinnerEntry ->
                                    SpinnerItemImpl(
                                        entry = spinnerEntry,
                                        entryCount = wrappedEntries.size,
                                        isSelected = spValue == index,
                                        index = index,
                                        dialogMode = false
                                    ) { newValue ->
                                        spValue = newValue
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                        key?.let { SafeSP.putAny(it, newValue) }
                                        updatedOnSelectedIndexChange?.let { it1 -> it1(newValue) }
                                        showPopup.value = false
                                        isDropdownExpanded.value = false
                                    }
                                }
                            }
                        }
                        showPopup.value = true
                    }
                }
                icon?.let {
                    DrawableResIcon(it)
                }
            },
            rightActions = {
                if (showValue) {
                    Text(
                        modifier = Modifier.widthIn(max = 130.dp),
                        text = wrappedEntries[spValue].title ?: "",
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = rightActionColor.color(enabled),
                        textAlign = TextAlign.End,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }
                Image(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(10.dp, 16.dp)
                        .align(Alignment.CenterVertically),
                    imageVector = MiuixIcons.Basic.ArrowUpDownIntegrated,
                    colorFilter = ColorFilter.tint(rightActionColor.color(enabled)),
                    contentDescription = null
                )
            },
            onClick = {
                if (enabled) {
                    isDropdownExpanded.value = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch {
                        interactionSource.emit(HoldDownInteraction.HoldDown().also {
                            held.value = it
                        })
                    }
                }
            },
            enabled = enabled
        )
        if (mode == DropDownMode.Dialog) {
            SuperDialog(
                title = title,
                show = isDropdownExpanded,
                onDismissRequest = {
                    isDropdownExpanded.value = false
                },
                insideMargin = DpSize(0.dp, 24.dp)
            ) {
                Layout(
                    content = {
                        LazyColumn {
                            items(wrappedEntries.size) { index ->
                                SpinnerItemImpl(
                                    entry = wrappedEntries[index],
                                    entryCount = wrappedEntries.size,
                                    isSelected = spValue == index,
                                    index = index,
                                    dialogMode = true
                                ) { newValue ->
                                    spValue = newValue
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                    key?.let { SafeSP.putAny(it, newValue) }
                                    updatedOnSelectedIndexChange?.let { it1 -> it1(newValue) }
                                    isDropdownExpanded.value = false
                                }
                            }
                        }
                        TextButton(
                            modifier = Modifier
                                .padding(start = 24.dp, top = 12.dp, end = 24.dp)
                                .fillMaxWidth(),
                            text = stringResource(R.string.button_cancel),
                            minHeight = 50.dp,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                isDropdownExpanded.value = false
                            }
                        )
                    }
                ) { measurables, constraints ->
                    if (measurables.size != 2) {
                        layout(0, 0) { }
                    }
                    val button = measurables[1].measure(constraints)
                    val lazyList = measurables[0].measure(
                        constraints.copy(
                            maxHeight = constraints.maxHeight - button.height
                        )
                    )
                    layout(constraints.maxWidth, lazyList.height + button.height) {
                        lazyList.place(0, 0)
                        button.place(0, lazyList.height)
                    }
                }
            }
        }
    }
}

/**
 * 多选下拉内容（内部使用）
 */
@Composable
private fun MultiSelectDropDownContent(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    entries: List<DropDownEntry>,
    key: String? = null,
    showValue: Boolean = true,
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    rightActionColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onSelectedValuesChange: ((Set<Int>) -> Unit)? = null,
) {
    // 从 SharedPreferences 读取初始选中集合（存为 StringSet，每个索引转成字符串）
    val initialSelected = remember(key, entries.size) {
        key?.let {
            SafeSP.getStringSet(it, emptySet())
                ?.mapNotNull { str -> str.toIntOrNull() }
                ?.filter { index -> index in entries.indices }
                ?.toSet()
        } ?: emptySet()
    }
    var selectedIndices by remember { mutableStateOf(initialSelected) }
    val updatedOnSelectedValuesChange by rememberUpdatedState(onSelectedValuesChange)

    // 包装 SpinnerEntry 供 SpinnerItemImpl 使用
    val wrappedEntries = entries.map { entry ->
        SpinnerEntry(
            icon = { imageModifier ->
                entry.iconVector?.let {
                    Image(
                        modifier = imageModifier,
                        imageVector = it,
                        contentDescription = null,
                        colorFilter = entry.iconTint?.let { tint -> ColorFilter.tint(tint) }
                    )
                } ?: entry.iconRes?.let {
                    Image(
                        modifier = imageModifier,
                        painter = painterResource(it),
                        contentDescription = null,
                        colorFilter = entry.iconTint?.let { tint -> ColorFilter.tint(tint) }
                    )
                } ?: entry.iconBitmap?.let {
                    Image(
                        modifier = imageModifier,
                        bitmap = it,
                        contentDescription = null,
                        colorFilter = entry.iconTint?.let { tint -> ColorFilter.tint(tint) }
                    )
                }
            },
            title = entry.title,
            summary = entry.summary,
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isDropdownExpanded = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val held = remember { mutableStateOf<HoldDownInteraction.HoldDown?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    // 显示文本：当 showValue 为 true 时，根据选中项生成描述
    val displayText = remember(selectedIndices) {
        if (selectedIndices.isEmpty()) {
            // 可替换为资源字符串 "未选择"
            "未选择"
        } else if (selectedIndices.size <= 3) {
            selectedIndices
                .mapNotNull { entries.getOrNull(it)?.title }
                .joinToString(", ")
        } else {
            // 可替换为资源字符串 "${selectedIndices.size} 项已选"
            "${selectedIndices.size} 项已选"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isDropdownExpanded.value = false
        }
    }

    if (!isDropdownExpanded.value) {
        held.value?.let { oldValue ->
            coroutineScope.launch {
                interactionSource.emit(HoldDownInteraction.Release(oldValue))
            }
            held.value = null
        }
    }

    // 主组件
    BasicComponent(
        modifier = Modifier, // 多选模式下不需要 pointerInput 判断左右，因为强制用 Dialog
        interactionSource = interactionSource,
        insideMargin = PaddingValues((icon?.getHorizontalPadding() ?: 16.dp), 16.dp, 16.dp, 16.dp),
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        leftAction = {
            icon?.let { DrawableResIcon(it) }
        },
        rightActions = {
            if (showValue) {
                Text(
                    modifier = Modifier.widthIn(max = 130.dp),
                    text = displayText,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = rightActionColor.color(enabled),
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
            }
            Image(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(10.dp, 16.dp)
                    .align(Alignment.CenterVertically),
                imageVector = MiuixIcons.Basic.ArrowUpDownIntegrated,
                colorFilter = ColorFilter.tint(rightActionColor.color(enabled)),
                contentDescription = null
            )
        },
        onClick = {
            if (enabled) {
                isDropdownExpanded.value = true
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                coroutineScope.launch {
                    interactionSource.emit(HoldDownInteraction.HoldDown().also {
                        held.value = it
                    })
                }
            }
        },
        enabled = enabled
    )

    // 多选对话框（强制使用 Dialog 模式）
    if (isDropdownExpanded.value) {
        SuperDialog(
            title = title,
            show = isDropdownExpanded,
            onDismissRequest = {
                isDropdownExpanded.value = false
            },
            insideMargin = DpSize(0.dp, 24.dp)
        ) {
            // 临时选中状态，初始为当前选中的集合
            var tempSelected by remember { mutableStateOf(selectedIndices) }

            Layout(
                content = {
                    // 选项列表
                    LazyColumn {
                        items(wrappedEntries.size) { index ->
                            SpinnerItemImpl(
                                entry = wrappedEntries[index],
                                entryCount = wrappedEntries.size,
                                isSelected = index in tempSelected,
                                index = index,
                                dialogMode = true
                            ) { clickedIndex ->
                                // 切换选中状态，不关闭对话框
                                tempSelected = if (clickedIndex in tempSelected) {
                                    tempSelected - clickedIndex
                                } else {
                                    tempSelected + clickedIndex
                                }
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            }
                        }
                    }
                    // 底部按钮行：取消 + 确定
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 12.dp, end = 24.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(android.R.string.cancel), // 或使用 R.string.button_cancel
                            minHeight = 50.dp,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                isDropdownExpanded.value = false
                                // 放弃临时选择，直接关闭
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(android.R.string.ok), // 可替换为 R.string.button_confirm
                            minHeight = 50.dp,
                            onClick = {
                                // 保存临时选择
                                selectedIndices = tempSelected
                                key?.let {
                                    // 将 Set<Int> 转为 Set<String> 存储
                                    SafeSP.putStringSet(it, tempSelected.map { idx -> idx.toString() }.toSet())
                                }
                                updatedOnSelectedValuesChange?.let { it(tempSelected) }
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                isDropdownExpanded.value = false
                            }
                        )
                    }
                }
            ) { measurables, constraints ->
                // measurables[0] = LazyColumn, measurables[1] = Row（按钮行）
                if (measurables.size != 2) {
                    layout(0, 0) { }
                }
                val buttonRow = measurables[1].measure(constraints)
                val list = measurables[0].measure(
                    constraints.copy(
                        maxHeight = constraints.maxHeight - buttonRow.height
                    )
                )
                layout(constraints.maxWidth, list.height + buttonRow.height) {
                    list.place(0, 0)
                    buttonRow.place(0, list.height)
                }
            }
        }
    }
}

