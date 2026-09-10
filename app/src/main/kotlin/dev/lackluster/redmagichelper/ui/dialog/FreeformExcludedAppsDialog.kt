package dev.lackluster.redmagichelper.ui.dialog

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.component.FullScreenDialog
import dev.lackluster.hyperx.compose.component.Hint
import dev.lackluster.hyperx.compose.preference.CheckboxPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.TextField
import java.text.Collator

private data class FreeformCandidateApp(
    val pkg: String,
    val label: String,
)

/**
 * 小窗例外清单：可搜索的应用多选列表。
 * 勾选的应用在「允许所有应用使用小窗」开启时仍保持原厂小窗策略。
 * 数据以 StringSet 存入 Prefs，勾选即生效（由 HelperApplication 同步到远程配置）。
 */
@Composable
fun FreeformExcludedAppsDialog(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    val context = LocalContext.current

    var excluded by remember {
        mutableStateOf(SafeSP.getStringSet(Pref.Key.Android.FREEFORM_EXCLUDED_APPS, mutableSetOf()).toSet())
    }
    var search by remember { mutableStateOf("") }

    val apps by produceState(initialValue = emptyList<FreeformCandidateApp>()) {
        value = withContext(Dispatchers.IO) { loadFreeformCandidateApps(context) }
    }

    fun toggleApp(pkg: String, checked: Boolean) {
        val next = if (checked) excluded + pkg else excluded - pkg
        excluded = next
        SafeSP.putStringSet(Pref.Key.Android.FREEFORM_EXCLUDED_APPS, next)
    }

    FullScreenDialog(
        navController,
        adjustPadding,
        stringResource(R.string.android_freeform_excluded_apps),
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
                text = stringResource(R.string.android_freeform_excluded_apps_tips)
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
            CheckboxPreference(
                title = app.label,
                summary = app.pkg,
                defValue = excluded.contains(app.pkg)
            ) { checked ->
                toggleApp(app.pkg, checked)
            }
        }
        if (apps.isNotEmpty() && filtered.isEmpty()) {
            item {
                BasicComponent(
                    title = stringResource(R.string.android_freeform_excluded_apps_empty)
                )
            }
        }
    }
}

private fun loadFreeformCandidateApps(context: Context): List<FreeformCandidateApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val collator = Collator.getInstance()
    return pm.queryIntentActivities(intent, 0)
        .map { FreeformCandidateApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.pkg }
        .sortedWith { a, b -> collator.compare(a.label, b.label) }
}
