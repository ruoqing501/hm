package dev.lackluster.redmagichelper.hook.rules.systemui.qs.nubia


import android.content.res.Configuration
import android.view.ViewGroup
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.getResID

object QSCustom : YukiBaseHooker() {
    private const val TAG = "NubiaQSCustom"

    private val mSwitch get() =
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.QS_CUSTOM_ROW_COLUMN_SWITCH, false)

    private val mRows get() =
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.QS_CUSTOM_ROW, 4)
    private val mRowsLandscape get() =
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.QS_CUSTOM_ROW_LANDSCAPE, 1)

    // 列
    private val mColumns get() =
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.QS_CUSTOM_COLUMN, 5)

    // 列（横屏）
    private val mColumnsLandscape get() =
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.QS_CUSTOM_COLUMN_LANDSCAPE, 7)
    private val mColumnsEditor get() =
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.QS_CUSTOM_COLUMN_EDIT, 5)
    private val mColumnsLandscapeEditor get() =
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.QS_CUSTOM_COLUMN_EDIT_LANDSCAPE, 6)

    override fun onHook() {

        val mfvTileLayoutAdaptClazz =
            "com.zte.adapt.mifavor.qs.MfvTileLayoutAdapt".toClassOrNull()
        val mfvTileLayoutClazz = "com.zte.mifavor.qs.MfvTileLayout".toClassOrNull()
        val mfvTileLayoutClazz2 = "com.android.systemui.qs.SideLabelTileLayout".toClassOrNull()
            val getTileColumnsMe = "com.zte.utils.QsDimenUtils\$Companion".toClassOrNull()?.method {
                name = "getTileColumns"
            }
            // 设置列数(整体下拉)
            getTileColumnsMe?.hook {
                before {
                    if (!mSwitch) return@before
                    /**
                     * 获取当前应用程序的屏幕方向。
                     *
                     * 该代码通过访问Android应用的资源配置来获取当前设备的屏幕方向。
                     * 屏幕方向通常用于判断设备是处于竖屏（portrait）还是横屏（landscape）状态。
                     *
                     * @return 返回当前屏幕的方向，值为Configuration.ORIENTATION_PORTRAIT（竖屏）
                     *         或 Configuration.ORIENTATION_LANDSCAPE（横屏）。
                     */
                    val orientation =
                        (appContext ?: return@before).resources.configuration.orientation
//                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//                        result = mColumns
//                    } else {
//                        result = mColumnsLandscape
//                    }
                    /**
                     * 根据设备屏幕方向选择合适的列数。
                     *
                     * @param orientation 当前设备的屏幕方向，通常由 [Configuration.ORIENTATION_PORTRAIT] 或 [Configuration.ORIENTATION_LANDSCAPE] 表示。
                     * @return 返回适用于当前屏幕方向的列数：竖屏时返回 [mColumns]，横屏时返回 [mColumnsLandscape]。
                     */
                    result =
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) mColumns else mColumnsLandscape


                }
            }
            // 设置可编辑列数(整体下拉)
            val getColumnsMe =
                "com.zte.feature.qs.layout.QsCustomizerModule".toClassOrNull()?.method {
                    name = "getColumns"
                }?.hook {
                    before {
                        if (!mSwitch) return@before
                        val orientation =
                            (appContext ?: return@before).resources.configuration.orientation
//                        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//                            result = mColumnsEditor
//                        } else {
//                            result = mColumnsLandscapeEditor
//                        }
                        result =
                            if (orientation == Configuration.ORIENTATION_PORTRAIT) mColumnsEditor else mColumnsLandscapeEditor
                    }
                }
            // 设置列数(左右下拉)
            mfvTileLayoutAdaptClazz?.method {
                name = "getTileColumns"
                param(IntType)
            }?.hook{
                before {
                    if (!mSwitch) return@before
                    val orientation =
                        (appContext ?: return@before).resources.configuration.orientation
                    result = if (orientation == Configuration.ORIENTATION_PORTRAIT) mColumns else mColumnsLandscape
                }
            }
            // 【新增关键Hook】: 直接修改MfvTileLayout的缓存字段，确保左右下拉时生效
//            mfvTileLayoutClazz?.method {
//                name = "onMeasure"
//                param(IntType, IntType)
//            }?.hook {
//                before {
//                    val orientation = (appContext ?: return@before).resources.configuration.orientation
//                    val customColumns = if (orientation == Configuration.ORIENTATION_PORTRAIT) mColumns else mColumnsLandscape
//                    // 强制更新缓存字段，影响本次布局计算
//
//                    this.instance.current().field {
//                        name = "mColumn"
//                    }.set(customColumns)
//                    this.instance.current().field {
//                        name = "mResourceColumns"
//                    }.set(customColumns)
//                    // 可选: 记录日志用于调试
//                     YLog.debug("$TAG：onMeasure: set columns to $customColumns (orientation: $orientation)")
//                }
//            }
            // 设置行数
            val getRowsMe =
                mfvTileLayoutAdaptClazz?.method {
                    name = "getRows"
                    param(IntType)
                }?.hook {
                    before {
                        if (!mSwitch) return@before
                        val orientation =
                            (appContext ?: return@before).resources.configuration.orientation
                        result =
                            if (orientation == Configuration.ORIENTATION_PORTRAIT) mRows else mRowsLandscape
                    }
                }
            // 设置行数（强制更新）
            val updateMaxRowsMe = mfvTileLayoutClazz?.method {
                name = "updateMaxRows"
                param(IntType, IntType)
            }?.hook{
                after {
                    if (!mSwitch) return@after
                    val viewGroup = instance as ViewGroup
                    val orientation = viewGroup.context.resources.configuration.orientation

                    val orimRows =
                        mfvTileLayoutClazz.getDeclaredField("mRows").apply { isAccessible = true }.getInt(instance)
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        mfvTileLayoutClazz.getDeclaredField("mRows").apply { isAccessible = true }.setInt(instance, mRows)
                        // 打印修改后的值（可选）
                        YLog.debug("$TAG: set mRows to $mRows (portrait)")
                        result = orimRows != mRows
                    } else {
                        mfvTileLayoutClazz.getDeclaredField("mRows").apply { isAccessible = true }
                            .setInt(instance, mRowsLandscape)
                       result = orimRows != mRowsLandscape
                    }
                }
            }
            // 左右下拉（行和列）
            hookControlCenterTileLayoutOnMeasure()

    }
    // 左右下拉（控制中心，磁贴的行和列）
    private fun hookControlCenterTileLayoutOnMeasure() {
        val clazz = "com.zte.controlcenter.view.ControlCenterTileLayout".toClassOrNull()
        clazz?.method {
            name = "onMeasure"
            param(IntType,IntType)
        }?.hook{
            before {
                if (!mSwitch) return@before
                YLog.debug("QSCustom: ControlCenterTileLayout.onMeasure - mSwitch: $mSwitch")
                try {
                    val orientation = (instance as ViewGroup).context.resources.configuration.orientation
                    val userRows = if (orientation == Configuration.ORIENTATION_PORTRAIT) mRows else mRowsLandscape
                    val userColumns = if (orientation == Configuration.ORIENTATION_PORTRAIT) mColumns else mColumnsLandscape

                    setFieldValueSafe(instance, "mRows", userRows, clazz)
                    setFieldValueSafe(instance, "mColumns", userColumns, clazz)
                    YLog.debug("QSCustom: ControlCenterTileLayout.onMeasure - rows: $userRows, columns: $userColumns")
                }catch (e: Exception){
                    YLog.debug("QSCustom: ControlCenterTileLayout.onMeasure error - ${e.message}")
                }
            }
        }
    }
    /**
     * 安全设置字段值的辅助方法
     */
    private fun setFieldValueSafe(obj: Any, fieldName: String, value: Int, clazz: Class<*>) {
        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.setInt(obj, value)
        } catch (e: Exception) {
            YLog.error("QSCustom: Failed to set field $fieldName - ${e.message}")
            val alternativeNames = when (fieldName) {
                "mRows" -> listOf("mRows", "rows", "mMaxRows")
                "mColumns" -> listOf("mColumns", "columns")
                else -> listOf(fieldName)
            }

            for (altName in alternativeNames) {
                try {
                    val altField = clazz.getDeclaredField(altName)
                    altField.isAccessible = true
                    altField.setInt(obj, value)
                    YLog.debug("QSCustom: Successfully set field $altName as alternative to $fieldName")
                    break
                } catch (e2: Exception) {
                    // 继续尝试下一个备选名
                }
            }
        }
    }
}


