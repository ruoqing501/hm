package dev.lackluster.redmagichelper.ui.page

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.hyperx.compose.preference.DropDownPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem

private data class GridComponent(
    val nameRes: Int,
    val zoneKey: String,
    val orderKey: String,
    val sizeKey: String,
    val visibleKey: String,
    val defZone: Int,
    val defOrder: Int,
    val defSize: Int,
    val defVisible: Boolean
)

@Composable
fun StatusBarGridPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    val gridKey = Pref.Key.SystemUI.StatusBarGrid

    var enableGrid by remember { mutableStateOf(SafeSP.getBoolean(gridKey.SWITCH)) }

    val zoneEntries = listOf(
        DropDownEntry(stringResource(R.string.status_bar_grid_zone_l1)),
        DropDownEntry(stringResource(R.string.status_bar_grid_zone_l2)),
        DropDownEntry(stringResource(R.string.status_bar_grid_zone_ls)),
        DropDownEntry(stringResource(R.string.status_bar_grid_zone_c1)),
        DropDownEntry(stringResource(R.string.status_bar_grid_zone_c2)),
        DropDownEntry(stringResource(R.string.status_bar_grid_zone_cs)),
        DropDownEntry(stringResource(R.string.status_bar_grid_zone_r1)),
        DropDownEntry(stringResource(R.string.status_bar_grid_zone_r2)),
        DropDownEntry(stringResource(R.string.status_bar_grid_zone_rs)),
    )
    val networkModeEntries = listOf(
        DropDownEntry(stringResource(R.string.status_bar_grid_network_mode_default)),
        DropDownEntry(stringResource(R.string.status_bar_grid_network_mode_up)),
        DropDownEntry(stringResource(R.string.status_bar_grid_network_mode_down)),
        DropDownEntry(stringResource(R.string.status_bar_grid_network_mode_both_single)),
        DropDownEntry(stringResource(R.string.status_bar_grid_network_mode_both_dual)),
    )

    // 默认值与 LS_Augment StatusBarGridSpec.defaults() 一致
    val components = listOf(
        GridComponent(R.string.status_bar_grid_comp_clock, gridKey.CLOCK_ZONE, gridKey.CLOCK_ORDER, gridKey.CLOCK_SIZE, gridKey.CLOCK_VISIBLE, 2, 0, 13, true),
        GridComponent(R.string.status_bar_grid_comp_notifications, gridKey.NOTIFICATIONS_ZONE, gridKey.NOTIFICATIONS_ORDER, gridKey.NOTIFICATIONS_SIZE, gridKey.NOTIFICATIONS_VISIBLE, 2, 1, 13, true),
        GridComponent(R.string.status_bar_grid_comp_system_icons, gridKey.SYSTEM_ICONS_ZONE, gridKey.SYSTEM_ICONS_ORDER, gridKey.SYSTEM_ICONS_SIZE, gridKey.SYSTEM_ICONS_VISIBLE, 8, 2, 13, true),
        GridComponent(R.string.status_bar_grid_comp_battery, gridKey.BATTERY_ZONE, gridKey.BATTERY_ORDER, gridKey.BATTERY_SIZE, gridKey.BATTERY_VISIBLE, 8, 3, 13, true),
        GridComponent(R.string.status_bar_grid_comp_cpu, gridKey.CPU_ZONE, gridKey.CPU_ORDER, gridKey.CPU_SIZE, gridKey.CPU_VISIBLE, 1, 4, 9, false),
        GridComponent(R.string.status_bar_grid_comp_gpu, gridKey.GPU_ZONE, gridKey.GPU_ORDER, gridKey.GPU_SIZE, gridKey.GPU_VISIBLE, 1, 5, 9, false),
        GridComponent(R.string.status_bar_grid_comp_battery_temp, gridKey.BATTERY_TEMP_ZONE, gridKey.BATTERY_TEMP_ORDER, gridKey.BATTERY_TEMP_SIZE, gridKey.BATTERY_TEMP_VISIBLE, 4, 6, 9, false),
        GridComponent(R.string.status_bar_grid_comp_current, gridKey.CURRENT_ZONE, gridKey.CURRENT_ORDER, gridKey.CURRENT_SIZE, gridKey.CURRENT_VISIBLE, 7, 7, 9, false),
        GridComponent(R.string.status_bar_grid_comp_power, gridKey.POWER_ZONE, gridKey.POWER_ORDER, gridKey.POWER_SIZE, gridKey.POWER_VISIBLE, 7, 8, 9, false),
        GridComponent(R.string.status_bar_grid_comp_network, gridKey.NETWORK_ZONE, gridKey.NETWORK_ORDER, gridKey.NETWORK_SIZE, gridKey.NETWORK_VISIBLE, 5, 9, 9, false),
    )

    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.ui_title_status_bar_grid),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            RebootMenuItem(
                appName = stringResource(R.string.scope_systemui),
                appPkg = Scope.SYSTEM_UI
            )
        }
    ) {
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_status_bar_grid),
                first = true
            ) {
                SwitchPreference(
                    title = stringResource(R.string.status_bar_grid_enable),
                    summary = stringResource(R.string.status_bar_grid_enable_summary) + "\n" + stringResource(R.string.status_bar_grid_enable_takeover),
                    key = gridKey.SWITCH
                ) {
                    enableGrid = it
                }
            }
        }
        if (enableGrid) {
            item {
                PreferenceGroup(
                    title = stringResource(R.string.common_general)
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.status_bar_grid_notification_two_rows),
                        key = gridKey.NOTIFICATION_TWO_ROWS,
                        defValue = true
                    )
                    SwitchPreference(
                        title = stringResource(R.string.status_bar_grid_system_two_rows),
                        key = gridKey.SYSTEM_TWO_ROWS,
                        defValue = true
                    )
                    SeekBarPreference(
                        title = stringResource(R.string.status_bar_grid_dual_row_gap),
                        key = gridKey.DUAL_ROW_GAP,
                        defValue = 0,
                        min = 0,
                        max = 8
                    )
                    DropDownPreference(
                        title = stringResource(R.string.status_bar_grid_network_mode),
                        summary = stringResource(R.string.status_bar_grid_network_mode_summary),
                        entries = networkModeEntries,
                        key = gridKey.NETWORK_DISPLAY_MODE
                    )
                    SeekBarPreference(
                        title = stringResource(R.string.status_bar_grid_margin_left),
                        key = gridKey.MARGIN_LEFT,
                        defValue = 0,
                        min = 0,
                        max = 40
                    )
                    SeekBarPreference(
                        title = stringResource(R.string.status_bar_grid_margin_right),
                        key = gridKey.MARGIN_RIGHT,
                        defValue = 0,
                        min = 0,
                        max = 40
                    )
                    SeekBarPreference(
                        title = stringResource(R.string.status_bar_grid_margin_top),
                        key = gridKey.MARGIN_TOP,
                        defValue = 0,
                        min = 0,
                        max = 12
                    )
                    SeekBarPreference(
                        title = stringResource(R.string.status_bar_grid_margin_bottom),
                        key = gridKey.MARGIN_BOTTOM,
                        defValue = 0,
                        min = 0,
                        max = 12
                    )
                }
            }
            components.forEachIndexed { index, component ->
                item {
                    PreferenceGroup(
                        title = stringResource(component.nameRes),
                        last = index == components.lastIndex
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.status_bar_grid_item_visible),
                            key = component.visibleKey,
                            defValue = component.defVisible
                        )
                        DropDownPreference(
                            title = stringResource(R.string.status_bar_grid_item_zone),
                            entries = zoneEntries,
                            key = component.zoneKey,
                            defValue = component.defZone
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.status_bar_grid_item_order),
                            key = component.orderKey,
                            defValue = component.defOrder,
                            min = 0,
                            max = 99
                        )
                        SeekBarPreference(
                            title = stringResource(R.string.status_bar_grid_item_size),
                            key = component.sizeKey,
                            defValue = component.defSize,
                            min = 6,
                            max = 32
                        )
                    }
                }
            }
        }
    }
}
