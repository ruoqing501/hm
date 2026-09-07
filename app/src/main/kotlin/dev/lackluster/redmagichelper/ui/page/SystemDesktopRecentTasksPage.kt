package dev.lackluster.redmagichelper.ui.page




import android.graphics.drawable.Icon
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.AlertDialog
import dev.lackluster.hyperx.compose.base.AlertDialogMode
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.navigation.navigateTo
import dev.lackluster.hyperx.compose.preference.DropDownEntry
import dev.lackluster.hyperx.compose.preference.DropDownMode
import dev.lackluster.hyperx.compose.preference.DropDownPreference
import dev.lackluster.hyperx.compose.preference.EditTextDataType
import dev.lackluster.hyperx.compose.preference.EditTextPreference
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.SeekBarPreference
import dev.lackluster.hyperx.compose.preference.SwitchPreference
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.MainActivity
import dev.lackluster.redmagichelper.ui.component.RebootMenuItem
import dev.lackluster.redmagichelper.data.Pages
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.utils.ShellUtils
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ColorPalette
/**
 * 颜色选择器组件（封装方法）
 * @param key 用于保存颜色值的 SharedPreferences 键
 * @param initialColor 默认颜色值（当没有保存的值时使用）
 */
@Composable
fun ColorPalettePreference(
    key: String,
    initialColor: Color = Color.White
) {
    var selectedColor by remember {
        mutableStateOf(
            Color(SafeSP.getInt(key, initialColor.toArgb()))
        )
    }

    ColorPalette(
        initialColor = selectedColor,
        onColorChanged = { newColor ->
            selectedColor = newColor
            SafeSP.putAny(key, newColor.toArgb())
        }
    )
}
@Composable
fun SystemDesktopRecentTasksPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    var enableShowMemory by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemDesktop.SYSTEM_DESKTOP_RECENT_TASK_DISPLAY_MEMORY)) }
    //var displayStyleMemory by remember { mutableStateOf(SafeSP.getBoolean(Pref.Key.SystemDesktop.DISPLAY_STYLE))}
    var darkThemeSelectedColor by remember { mutableStateOf(Color.White) }
    var lightThemeSelectedColor by remember { mutableStateOf(Color.Black) }
    var displayStyleMemory by remember {
        mutableStateOf(
            SafeSP.getInt(
                Pref.Key.SystemDesktop.DISPLAY_STYLE,
                0
            )
        )
    }
    var displayFontColorMemory by remember {
        mutableStateOf(
            SafeSP.getInt(
                Pref.Key.SystemDesktop.MEMORY_FONT_COLOR_OPTION,
                0
            )
        )
    }

    // 显示内存信息-显示风格
    val displayStyleEntries = listOf(
        // 简约
        DropDownEntry(stringResource(R.string.simple)),
        // 经典
        DropDownEntry(stringResource(R.string.classics)),
    )

    // 字体颜色
    val fontColorEntries = listOf(
        // 跟随主题
        DropDownEntry(stringResource(R.string.follow_theme)),
        // 自定义
        DropDownEntry(stringResource(R.string.custom)),
    )
    //  内存显示模式
    val memoryDisplayModeEntries = listOf(
        // 剩余内存+已用内存+总内存
        DropDownEntry(stringResource(R.string.memory_display_mode_0)),
        // 剩余内存+已用内存
        DropDownEntry(stringResource(R.string.memory_display_mode_1)),
        // 剩余内存
        DropDownEntry(stringResource(R.string.memory_display_mode_2)),
    )




    BasePage(
        navController,
        adjustPadding,
        // 标题：系统桌面
        stringResource(R.string.system_desktop),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
        actions = {
            // 系统更新重启
            // appPkg 重启的对应包名：com.zte.zdm
            RebootMenuItem(
                // 弹窗标题的提示名称
                appName = stringResource(R.string.system_desktop),
                // 重启：com.zte.zdm
                appPkg = Scope.SYSTEM_DESKTOP
                // 重启系统
//                appPkg = "android"
            )
        }
    ) {

        // 红魔-桌面-最近任务界面
        item {
            PreferenceGroup(
                // 最近任务界面
                stringResource(R.string.system_desktop_recent_task_Interface),
                visible = true //
            ) {
                // 显示内存信息
                SwitchPreference(
                    title = stringResource(R.string.system_desktop_recent_task_display_memory),
                    key = Pref.Key.SystemDesktop.SYSTEM_DESKTOP_RECENT_TASK_DISPLAY_MEMORY,
                ) {
                    enableShowMemory = it
                }

                // 显示风格
                AnimatedVisibility(
                    enableShowMemory
                ) {
                    Column() {
                        // 显示风格:简约、经典
                        DropDownPreference(
                            title = stringResource(R.string.display_style),
                            entries = displayStyleEntries, // 选项
                            key = Pref.Key.SystemDesktop.DISPLAY_STYLE,  //唯一id
                        ) {
                            displayStyleMemory = it
                        }
                        // 显示风格-简约的组件
                        AnimatedVisibility(
                            displayStyleMemory in listOf(0)
                        ) {
                            Column {
                                // 竖屏模式下的布局参数
                                PreferenceGroup(
                                    stringResource(R.string.portrait_screen),
                                    visible = true //
                                ) {
                                    // 字体大小 8-20
                                    SeekBarPreference(
                                        title = stringResource(R.string.font_size),
                                        key = Pref.Key.SystemDesktop.SIMPLE_PORTRAITSCREEN_MEMORY_FONT_SIZE,
                                        defValue = 10f,
                                        min = 8f,
                                        max = 20f
                                    )
                                    // 高度
                                    SeekBarPreference(
                                        title = stringResource(R.string.height),
                                        key = Pref.Key.SystemDesktop.SIMPLE_PORTRAITSCREEN_COMPONENT_HEIGHT,
                                        defValue = 85,
                                        min = 60,
                                        max = 90
                                    )
                                }
                                // 横屏模式下的布局参数
                                PreferenceGroup(
                                    stringResource(R.string.landscape),
                                    visible = true //
                                ) {
                                    // 字体大小 8-20
                                    SeekBarPreference(
                                        title = stringResource(R.string.font_size),
                                        key = Pref.Key.SystemDesktop.SIMPLE_LANDSACPE_MEMORY_FONT_SIZE,
                                        defValue = 8f,
                                        min = 8f,
                                        max = 20f
                                    )
                                    //高度
                                    SeekBarPreference(
                                        title = stringResource(R.string.height),
                                        key = Pref.Key.SystemDesktop.SIMPLE_LANDSACPE_COMPONENT_HEIGHT,
                                        defValue = 80,
                                        min = 60,
                                        max = 90
                                    )
                                }
                            }
                        }
                        // 显示风格-经典的组件
                        AnimatedVisibility(
                            displayStyleMemory in listOf(1)
                        ) {
                            Column {
                                // 显示模式
                                Column() {
                                    DropDownPreference(
                                        title = stringResource(R.string.display_mode),
                                        entries = memoryDisplayModeEntries, // 选项
                                        key = Pref.Key.SystemDesktop.MEMORY_DISPLAY_STYLE,  //唯一id
                                    )
                                }
                                // 竖屏模式下的布局参数
                                PreferenceGroup(
                                    stringResource(R.string.portrait_screen),
                                    visible = true //
                                ) {
                                    // 字体大小 8-20
                                    SeekBarPreference(
                                        //CLASSICS
                                        title = stringResource(R.string.font_size),
                                        key = Pref.Key.SystemDesktop.CLASSICS_PORTRAITSCREEN_MEMORY_FONT_SIZE,
                                        defValue = 10f,
                                        min = 8f,
                                        max = 20f
                                    )
                                    // 高度
                                    SeekBarPreference(
                                        title = stringResource(R.string.height),
                                        key = Pref.Key.SystemDesktop.CLASSICS_PORTRAITSCREEN_COMPONENT_HEIGHT,
                                        defValue = 85,
                                        min = 60,
                                        max = 90
                                    )
                                }
                                // 横屏模式下的布局参数
                                PreferenceGroup(
                                    stringResource(R.string.landscape),
                                    visible = true //
                                ) {
                                    // 字体大小 8-20
                                    SeekBarPreference(
                                        title = stringResource(R.string.font_size),
                                        key = Pref.Key.SystemDesktop.CLASSICS_LANDSACPE_MEMORY_FONT_SIZE,
                                        defValue = 8f,
                                        min = 8f,
                                        max = 20f
                                    )
                                    //高度
                                    SeekBarPreference(
                                        title = stringResource(R.string.height),
                                        key = Pref.Key.SystemDesktop.CLASSICS_LANDSACPE_COMPONENT_HEIGHT,
                                        defValue = 80,
                                        min = 60,
                                        max = 90
                                    )
                                }
                            }
                        }

                    }
                }
                // 字体颜色
                AnimatedVisibility(
                    enableShowMemory
                ) {
                    Column() {
                        DropDownPreference(
                            title = stringResource(R.string.font_color),
                            entries = fontColorEntries, // 选项
                            key = Pref.Key.SystemDesktop.MEMORY_FONT_COLOR_OPTION,  //唯一id
                        ) {
                            displayFontColorMemory = it
                        }
                        //// 自定义颜色
                        //AnimatedVisibility(
                        //    displayFontColorMemory in listOf(1)
                        //) {
                        //    Column {
                        //        ColorPalette(
                        //            // 初始化颜色
                        //            initialColor = lightThemeSelectedColor,
                        //            // 选择后的颜色
                        //            onColorChanged = { newColor ->
                        //                lightThemeSelectedColor = newColor
                        //            }
                        //        )
                        //    }
                        //}
                        // 自定义颜色
                        AnimatedVisibility(
                            displayFontColorMemory in listOf(1)
                        ) {
                            Column {
                                //亮色主题颜色
                                // 浅色主题颜色
                                PreferenceGroup(
                                    stringResource(R.string.light_theme_color),
                                ) {
                                    ColorPalettePreference(
                                        key = Pref.Key.SystemDesktop.LIGHT_THEME_COLOR,
                                        initialColor = lightThemeSelectedColor
                                    )
                                }
                                // 深色主题颜色
                                PreferenceGroup(
                                    stringResource(R.string.dark_theme_color),
                                ) {
                                    ColorPalettePreference(
                                        key = Pref.Key.SystemDesktop.DARK_THEME_COLOR,
                                        initialColor = darkThemeSelectedColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


