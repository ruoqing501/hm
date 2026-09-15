package dev.lackluster.redmagichelper.ui.page

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.promeg.pinyinhelper.Pinyin
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.navigation.navigateWithPopup
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.SearchEntry
import dev.lackluster.redmagichelper.data.SearchIndex
import dev.lackluster.redmagichelper.ui.MainActivity
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 非中文字符原样保留(转小写),空白字符丢弃,便于与去空格后的 query 做子串匹配
private fun toPinyinFull(text: String): String = buildString {
    text.forEach { char ->
        if (Pinyin.isChinese(char)) {
            append(Pinyin.toPinyin(char).lowercase())
        } else if (!char.isWhitespace()) {
            append(char.lowercaseChar())
        }
    }
}

private fun toPinyinInitials(text: String): String = buildString {
    text.forEach { char ->
        if (Pinyin.isChinese(char)) {
            Pinyin.toPinyin(char).firstOrNull()?.let { append(it.lowercaseChar()) }
        } else if (!char.isWhitespace()) {
            append(char.lowercaseChar())
        }
    }
}

private data class ResolvedSearchEntry(
    val title: String,
    val summary: String?,
    val pageName: String,
    val page: String,
) {
    val titlePinyinFull: String by lazy { toPinyinFull(title) }
    val titlePinyinInitials: String by lazy { toPinyinInitials(title) }
    val summaryPinyinFull: String by lazy { toPinyinFull(summary.orEmpty()) }
    val summaryPinyinInitials: String by lazy { toPinyinInitials(summary.orEmpty()) }

    fun matches(searchText: String, query: String): Boolean =
        title.contains(searchText, ignoreCase = true) ||
            summary?.contains(searchText, ignoreCase = true) == true ||
            titlePinyinFull.contains(query) ||
            titlePinyinInitials.contains(query) ||
            summaryPinyinFull.contains(query) ||
            summaryPinyinInitials.contains(query)
}

@Composable
fun SearchPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    var searchText by remember { mutableStateOf("") }

    val noResult = stringResource(R.string.search_no_result)

    // stringResource 只能在 Composable 中取,先解析成普通字符串再过滤
    val resolvedEntries = SearchIndex.entries.map { entry: SearchEntry ->
        ResolvedSearchEntry(
            title = stringResource(entry.titleRes),
            summary = entry.summaryRes?.let { stringResource(it) },
            pageName = stringResource(entry.pageNameRes),
            page = entry.page
        )
    }
    val filteredEntries = remember(searchText, resolvedEntries) {
        val query = searchText.filter { !it.isWhitespace() }.lowercase()
        if (searchText.isBlank()) {
            emptyList()
        } else {
            resolvedEntries.filter { it.matches(searchText, query) }
        }
    }

    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.search_features_hint),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
    ) {
        item {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp, bottom = 6.dp),
                value = searchText,
                singleLine = true,
                label = stringResource(R.string.search_features_hint),
                useLabelAsPlaceholder = true,
                onValueChange = { searchText = it }
            )
        }
        if (searchText.isNotBlank() && filteredEntries.isEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = noResult,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
        if (filteredEntries.isNotEmpty()) {
            item {
                PreferenceGroup(last = true) {
                    filteredEntries.forEach { entry ->
                        BasicComponent(
                            title = entry.title,
                            summary = entry.summary ?: entry.pageName,
                            onClick = {
                                navController.navigateWithPopup(entry.page)
                            }
                        )
                    }
                }
            }
        }
    }
}
