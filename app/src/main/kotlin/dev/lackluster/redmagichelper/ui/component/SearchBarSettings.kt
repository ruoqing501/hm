package dev.lackluster.redmagichelper.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Search

@Composable
fun SearchBarSettings(){
    var searchText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    SearchBar(
        inputField = {
            InputField(
                query = searchText,
                onQueryChange = { searchText = it },
                onSearch = { /* 处理搜索操作 */ },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                        imageVector = MiuixIcons.Useful.Search,
                        contentDescription = "搜索"
                    )
                }
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        // 搜索结果内容
    }
}

