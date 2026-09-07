package dev.lackluster.hyperx.compose.preference

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lackluster.hyperx.compose.base.ImageIcon
import dev.lackluster.hyperx.compose.base.DrawableResIcon
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TextPreference(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    rightActionColor: RightActionColor = RightActionDefaults.rightActionColors(),
    onClick: (() -> Unit)? = null,
) {
    val updatedOnClick by rememberUpdatedState(onClick)

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
            value?.let {
                Text(
                    modifier = Modifier.widthIn(max = 130.dp),
                    text = it,
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
                    .size(10.dp, 16.dp),
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                colorFilter = ColorFilter.tint(rightActionColor.color(enabled)),
            )
        },
        onClick = {
            if (enabled) {
                updatedOnClick?.invoke()
            }
        },
        enabled = enabled
    )
}

object RightActionDefaults {
    @Composable
    fun rightActionColors() = RightActionColor(
        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
        disabledColor = MiuixTheme.colorScheme.disabledOnSecondaryVariant
    )
}

@Immutable
class RightActionColor(
    private val color: Color,
    private val disabledColor: Color
) {
    @Stable
    fun color(enabled: Boolean): Color = if (enabled) color else disabledColor
}