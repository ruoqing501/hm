package dev.lackluster.redmagichelper.ui.page

import android.content.Context
import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.component.Hint
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.utils.ScreenOffHideExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.TextField
import java.text.Collator

private data class HideableApp(
    val pkg: String,
    val label: String,
)

/**
 * 熄屏自动隐藏:目标应用选择(参考 LS_Augment 的 AppSelectionDialog,仅保留当前用户空间)。
 * 勾选结果以 StringSet 存入 Prefs,hook 侧每次熄屏事件实时重读,无需重启。
 * 取消勾选会立即通过 root 恢复该应用可见(fail-closed)。
 */
@Composable
fun ScreenOffHideAppsPage(
    navController: NavController,
    adjustPadding: PaddingValues,
    mode: BasePageDefaults.Mode
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selected by remember {
        mutableStateOf(SafeSP.getStringSet(Pref.Key.Other.SCREEN_OFF_HIDE_TARGETS, mutableSetOf()).toSet())
    }
    var search by remember { mutableStateOf("") }
    val apps by produceState(initialValue = emptyList<HideableApp>()) {
        value = withContext(Dispatchers.IO) { loadThirdPartyApps(context, selected) }
    }

    fun toggle(pkg: String, check: Boolean) {
        val next = if (check) selected + pkg else selected - pkg
        selected = next
        SafeSP.putStringSet(Pref.Key.Other.SCREEN_OFF_HIDE_TARGETS, next)
        if (!check) {
            // 目标离开列表前先恢复可见,避免应用被永久隐藏
            scope.launch {
                val outcome = withContext(Dispatchers.IO) { ScreenOffHideExecutor.unhide(pkg) }
                if (!outcome.success) {
                    Toast.makeText(context, R.string.screen_off_hide_restore_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.screen_off_hide_pick_apps),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode
    ) {
        item {
            Hint(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp),
                text = stringResource(R.string.screen_off_hide_pick_hint)
            )
        }
        item {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp),
                value = search,
                singleLine = true,
                label = stringResource(R.string.common_search),
                useLabelAsPlaceholder = true,
                onValueChange = { search = it }
            )
        }
        val filtered = apps.filter {
            search.isBlank() || it.label.contains(search, true) || it.pkg.contains(search, true)
        }
        items(filtered, key = { it.pkg }) { app ->
            val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.pkg) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        context.packageManager.getApplicationIcon(app.pkg).toBitmap(96, 96)
                    }.getOrNull()?.asImageBitmap()
                }
            }
            BasicComponent(
                insideMargin = PaddingValues(16.dp),
                title = app.label,
                summary = app.pkg,
                leftAction = {
                    iconBitmap?.let {
                        Image(
                            modifier = Modifier.size(36.dp),
                            bitmap = it,
                            contentDescription = app.label
                        )
                    }
                },
                rightActions = {
                    Checkbox(
                        checked = app.pkg in selected,
                        onCheckedChange = { toggle(app.pkg, it) }
                    )
                },
                onClick = { toggle(app.pkg, app.pkg !in selected) }
            )
        }
    }
}

private fun loadThirdPartyApps(context: Context, selected: Set<String>): List<HideableApp> {
    val pm = context.packageManager
    val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
    val collator = Collator.getInstance()
    val apps = pm.getInstalledApplications(0)
        .asSequence()
        .filter { (it.flags and systemFlags) == 0 && it.packageName != context.packageName }
        .map { info ->
            val label = runCatching { info.loadLabel(pm).toString() }.getOrNull()
            HideableApp(info.packageName, label?.takeIf { it.isNotEmpty() } ?: info.packageName)
        }
        .toMutableList()
    // 已选中但已被隐藏(pm hide 后对普通枚举不可见)的应用也要列出,保证能取消勾选恢复
    val listed = apps.mapTo(HashSet()) { it.pkg }
    for (pkg in selected) {
        if (pkg !in listed && pkg != context.packageName) apps.add(HideableApp(pkg, pkg))
    }
    return apps.sortedWith { a, b -> collator.compare(a.label, b.label) }
}
