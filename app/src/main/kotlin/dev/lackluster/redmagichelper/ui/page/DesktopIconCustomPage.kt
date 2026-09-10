package dev.lackluster.redmagichelper.ui.page

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.LauncherIconOverrides
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperDialog
import java.text.Collator

private data class LaunchableApp(
    val pkg: String,
    val label: String,
)

/**
 * 桌面个性化:按应用自定义桌面图标与名称(移植自 LS_Augment 的 LauncherCustomizationActivity)。
 * 简化为「选择图片 → 自动居中方形裁剪 → 预览 → 保存」,不支持拖动/缩放微调。
 * 仅作用于当前用户空间,不含 LS_Augment 的多用户空间选择。
 */
@Composable
fun DesktopIconCustomPage(
    navController: NavController,
    adjustPadding: PaddingValues,
    mode: BasePageDefaults.Mode
) {
    val context = LocalContext.current

    var overrides by remember {
        mutableStateOf(LauncherIconOverrides.parse(SafeSP.getString(Pref.Key.SystemDesktop.APP_ICON_OVERRIDES, "")))
    }
    var search by remember { mutableStateOf("") }
    val editingApp: MutableState<LaunchableApp?> = remember { mutableStateOf(null) }

    val apps by produceState(initialValue = emptyList<LaunchableApp>()) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }

    fun saveOverrides(next: Map<String, LauncherIconOverrides.Entry>) {
        overrides = next
        SafeSP.putAny(Pref.Key.SystemDesktop.APP_ICON_OVERRIDES, LauncherIconOverrides.serialize(next))
    }

    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.desktop_icon_custom),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            RebootMenuItem(
                appName = stringResource(R.string.system_desktop),
                appPkg = Scope.SYSTEM_DESKTOP
            )
        }
    ) {
        item {
            PreferenceGroup {
                SwitchPreference(
                    title = stringResource(R.string.desktop_icon_custom_switch),
                    summary = stringResource(R.string.desktop_icon_custom_switch_tips),
                    key = Pref.Key.SystemDesktop.APP_ICON_CUSTOMIZE_SWITCH
                )
            }
        }
        item {
            Hint(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp),
                text = stringResource(R.string.desktop_icon_custom_hint)
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
            val entry = overrides["0:${app.pkg}"]
            val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.pkg, entry?.icon) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        entry?.icon?.takeIf { it.isNotEmpty() }?.let { LauncherIconOverrides.decodeIcon(it) }
                            ?: context.packageManager.getApplicationIcon(app.pkg).toBitmap(96, 96)
                    }.getOrNull()?.asImageBitmap()
                }
            }
            BasicComponent(
                insideMargin = PaddingValues(16.dp),
                title = entry?.name?.takeIf { it.isNotEmpty() } ?: app.label,
                summary = buildString {
                    append(app.pkg)
                    if (entry != null) {
                        append(" · ")
                        append(stringResource(R.string.desktop_icon_custom_modified))
                    }
                },
                leftAction = {
                    iconBitmap?.let {
                        Image(
                            modifier = Modifier.size(36.dp),
                            bitmap = it,
                            contentDescription = app.label
                        )
                    }
                },
                onClick = { editingApp.value = app }
            )
        }
    }

    editingApp.value?.let { app ->
        DesktopIconEditDialog(
            app = app,
            entry = overrides["0:${app.pkg}"],
            onDismiss = { editingApp.value = null },
            onSave = { entry ->
                val next = overrides.toMutableMap()
                if (entry == null || entry.isEmpty) next.remove(entry?.key ?: "0:${app.pkg}")
                else next[entry.key] = entry
                saveOverrides(next)
                Toast.makeText(context, R.string.desktop_icon_custom_saved, Toast.LENGTH_LONG).show()
                editingApp.value = null
            }
        )
    }
}

private fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val collator = Collator.getInstance()
    return pm.queryIntentActivities(intent, 0)
        .map { LaunchableApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.pkg }
        .sortedWith { a, b -> collator.compare(a.label, b.label) }
}

@Composable
private fun DesktopIconEditDialog(
    app: LaunchableApp,
    entry: LauncherIconOverrides.Entry?,
    onDismiss: () -> Unit,
    onSave: (LauncherIconOverrides.Entry?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val show = remember { mutableStateOf(true) }

    var name by remember { mutableStateOf(entry?.name ?: "") }
    var iconBase64 by remember { mutableStateOf(entry?.icon ?: "") }

    val preview by produceState<ImageBitmap?>(initialValue = null, iconBase64, app.pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                iconBase64.takeIf { it.isNotEmpty() }?.let { LauncherIconOverrides.decodeIcon(it) }
                    ?: context.packageManager.getApplicationIcon(app.pkg).toBitmap(96, 96)
            }.getOrNull()?.asImageBitmap()
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val encoded = withContext(Dispatchers.IO) {
                runCatching {
                    val source = context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    } ?: return@runCatching null
                    LauncherIconOverrides.encodeIcon(source)
                }.getOrNull()
            }
            if (encoded != null) {
                iconBase64 = encoded
            } else {
                Toast.makeText(context, R.string.desktop_icon_custom_image_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    SuperDialog(
        title = app.label,
        summary = app.pkg,
        show = show,
        onDismissRequest = {
            show.value = false
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            preview?.let {
                Image(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(72.dp),
                    bitmap = it,
                    contentDescription = app.label
                )
            }
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                value = name,
                singleLine = true,
                label = stringResource(R.string.desktop_icon_custom_name_hint),
                useLabelAsPlaceholder = true,
                onValueChange = { name = it.take(80) }
            )
            Row(modifier = Modifier.padding(bottom = 12.dp)) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.desktop_icon_custom_choose_image),
                    onClick = { pickImage.launch("image/*") }
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.desktop_icon_custom_reset_icon),
                    enabled = iconBase64.isNotEmpty(),
                    onClick = { iconBase64 = "" }
                )
            }
            Row {
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.desktop_icon_custom_reset_all),
                    onClick = {
                        show.value = false
                        onSave(null)
                    }
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.button_cancel),
                    onClick = {
                        show.value = false
                        onDismiss()
                    }
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.button_ok),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.codePoints().anyMatch { Character.isISOControl(it) }) {
                            Toast.makeText(context, R.string.common_invalid_input, Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        show.value = false
                        onSave(LauncherIconOverrides.Entry(user = 0, pkg = app.pkg, name = trimmed, icon = iconBase64))
                    }
                )
            }
        }
    }
}
