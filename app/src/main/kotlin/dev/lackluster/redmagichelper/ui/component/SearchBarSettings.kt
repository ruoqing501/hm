package dev.lackluster.redmagichelper.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.navigation.navigateWithPopup
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.SearchEntry
import dev.lackluster.redmagichelper.data.SearchIndex
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

private class ResolvedSearchEntry(
    val title: String,
    val summary: String?,
    val pageName: String,
    val page: String,
)

@Composable
fun SearchBarSettings(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val searchHint = stringResource(R.string.search_features_hint)
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
    val filteredEntries = remember(searchText) {
        if (searchText.isBlank()) {
            emptyList()
        } else {
            resolvedEntries.filter {
                it.title.contains(searchText, ignoreCase = true) ||
                    it.summary?.contains(searchText, ignoreCase = true) == true
            }
        }
    }

    SearchBar(
        inputField = {
            InputField(
                query = searchText,
                onQueryChange = { searchText = it },
                onSearch = { /* 实时过滤,无需额外操作 */ },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                label = searchHint,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                        imageVector = MiuixIcons.Useful.Search,
                        contentDescription = searchHint
                    )
                }
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        if (searchText.isNotBlank() && filteredEntries.isEmpty()) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = noResult,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
        // SearchBar 展开区域不自带滚动,限制高度并允许滚动,避免结果过多时撑出屏幕
        Column(
            modifier = Modifier
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
        ) {
            filteredEntries.forEach { entry ->
                BasicComponent(
                    insideMargin = PaddingValues(16.dp),
                    title = entry.title,
                    summary = entry.pageName,
                    onClick = {
                        expanded = false
                        searchText = ""
                        navController.navigateWithPopup(entry.page)
                    }
                )
            }
        }
    }
}
