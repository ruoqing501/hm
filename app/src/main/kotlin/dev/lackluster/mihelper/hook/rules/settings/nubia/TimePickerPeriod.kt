package dev.lackluster.mihelper.hook.rules.settings.nubia

import android.annotation.SuppressLint
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.ArrayClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object TimePickerPeriod : YukiBaseHooker() {

    // 控制变量：是否在12小时制下显示时段
    private var is12HourDisplayPeriod: Boolean = true

    // 控制变量：是否在24小时制下显示时段
    private var is24HourDisplayPeriod: Boolean = true

    @SuppressLint("PrivateApi")
    override fun onHook() {
        hasEnable(Pref.Key.NubiaSystemSettings.TIME_PICKER_PERIOD) {

            // 更新标题-显示时段
            updateTitlePeriod()

            // 新增：安装AM/PM Spinner钩子
            installAmPmSpinnerHook()
        }
    }

    // 更新标题-显示时段
    private fun updateTitlePeriod(){
        // Hook TimePickerDialogZTE 的 updateTitle 方法
        "com.zte.mifavor.widget.TimePickerDialogZTE".toClass().apply {
            method {
                name = "updateTitle"
                param("com.zte.mifavor.widget.TimePickerZTE".toClass(), IntType, IntType)
            }.hook {
                after {
                    val timePicker = this.args(0).any()
                    if (timePicker != null) {
                        handleTimePickerUpdate(this.instance, timePicker, this.args(1).int(), this.args(2).int())
                    } else {
                        YLog.warn("[TimePickerPeriod] timePicker 参数为 null，跳过处理")
                    }
                }
            }
        }
    }

    // 新增：安装AM/PM Spinner钩子
    private fun installAmPmSpinnerHook() {
        YLog.info("[TimePickerPeriod] 正在安装AM/PM Spinner钩子")

        val numberPickerClass = "com.zte.mifavor.widget.NumberPickerZTE"
        val timePickerClass = "com.zte.mifavor.widget.TimePickerZTE"

        // Hook 1: 修改NumberPickerZTE的setMaxValue方法
        numberPickerClass.toClass().apply {
            method {
                name = "setMaxValue"
                param(IntType)
            }.hook {
                before {
                    try {
                        // 检查调用栈，判断是否来自TimePickerZTE
                        val stackTrace = Thread.currentThread().stackTrace
                        val isTimePickerContext = stackTrace.any {
                            it.className?.contains("TimePickerZTE") == true
                        }

                        if (isTimePickerContext) {
                            val originalMax = this.args(0).int()
                            YLog.debug("[TimePickerPeriod] setMaxValue - 原始MaxValue: $originalMax")

                            // 如果原始最大值为1（AM/PM选择器），则改为6（7个选项）
                            if (originalMax == 1) {
                                this.args(0).set(6)
                                YLog.debug("[TimePickerPeriod] setMaxValue - 设置MaxValue为: 6")
                            }
                        }
                    } catch (e: Exception) {
                        YLog.error("[TimePickerPeriod] setMaxValue钩子异常: ${e.message}")
                    }
                }
            }
        }

        // Hook 2: 修改NumberPickerZTE的setDisplayedValues方法
        numberPickerClass.toClass().apply {
            method {
                name = "setDisplayedValues"
                param(Array<String>::class.java)
            }.hook {
                before {
                    try {
                        val originalValues = this.args(0).array<String>()
                        YLog.debug("[TimePickerPeriod] setDisplayedValues - 原始值: ${originalValues.contentToString()}")

                        // 检查调用栈，判断是否来自TimePickerZTE
                        val stackTrace = Thread.currentThread().stackTrace
                        val isTimePickerContext = stackTrace.any {
                            it.className?.contains("TimePickerZTE") == true
                        }

                        // 如果是来自TimePickerZTE且原始数组大小为2（AM/PM），则替换为7个时段
                        if (isTimePickerContext && originalValues != null && originalValues.size == 2) {
                            val periodValues = arrayOf("凌晨", "早上", "上午", "中午", "下午", "傍晚", "晚上")
                            this.args(0).set(periodValues)
                            YLog.debug("[TimePickerPeriod] setDisplayedValues - 替换为: ${periodValues.contentToString()}")
                        }
                    } catch (e: Exception) {
                        YLog.error("[TimePickerPeriod] setDisplayedValues钩子异常: ${e.message}")
                    }
                }
            }
        }

        // Hook 3: 修改TimePickerZTE的updateAmPmControl方法
        timePickerClass.toClass().apply {
            method {
                name = "updateAmPmControl"
                emptyParam()
            }.hook {
                after {
                    try {
                        val instance = this.instance

                        // 获取mAmPmSpinner字段
                        val mAmPmSpinner = instance.current().field {
                            name = "mAmPmSpinner"
                        }.any()

                        if (mAmPmSpinner != null) {
                            // 获取当前小时
                            val currentHour = try {
                                instance.current().method {
                                    name = "getHour"
                                    emptyParam()
                                }.int()
                            } catch (e: Exception) {
                                // 备用方法
                                instance.current().method {
                                    name = "getCurrentHour"
                                    emptyParam()
                                }.int()
                            }

                            // 根据小时计算时段索引
                            val periodIndex = getPeriodIndex(currentHour)
                            YLog.debug("[TimePickerPeriod] updateAmPmControl - 当前小时: $currentHour, 时段索引: $periodIndex")

                            // 设置Spinner的值范围
                            mAmPmSpinner.current().method {
                                name = "setMinValue"
                                param(IntType)
                            }.call(0)

                            mAmPmSpinner.current().method {
                                name = "setMaxValue"
                                param(IntType)
                            }.call(6)

                            // 设置正确的选中索引
                            mAmPmSpinner.current().method {
                                name = "setValue"
                                param(IntType)
                            }.call(periodIndex)

                            YLog.debug("[TimePickerPeriod] updateAmPmControl - 已设置时段索引: $periodIndex")
                        }
                    } catch (e: Exception) {
                        YLog.error("[TimePickerPeriod] updateAmPmControl钩子异常: ${e.message}")
                    }
                }
            }
        }

        // Hook 4: 修改TimePickerZTE的onTimeChanged方法
        timePickerClass.toClass().apply {
            method {
                name = "onTimeChanged"
                paramCount = 3
            }.hook {
                after {
                    try {
                        val instance = this.instance

                        // 获取变化后的小时数
                        val currentHour = this.args(1).int()
                        val periodIndex = getPeriodIndex(currentHour)

                        // 获取mAmPmSpinner字段
                        val mAmPmSpinner = instance.current().field {
                            name = "mAmPmSpinner"
                        }.any()

                        if (mAmPmSpinner != null) {
                            // 更新时段Spinner的选中项
                            mAmPmSpinner.current().method {
                                name = "setValue"
                                param(IntType)
                            }.call(periodIndex)

                            YLog.debug("[TimePickerPeriod] onTimeChanged - 小时: $currentHour, 时段索引: $periodIndex")
                        }
                    } catch (e: Exception) {
                        YLog.error("[TimePickerPeriod] onTimeChanged钩子异常: ${e.message}")
                    }
                }
            }
        }

        // Hook 5: 修改TimePickerZTE的onValueChange方法
        timePickerClass.toClass().apply {
            method {
                name = "onValueChange"
                param(numberPickerClass.toClass(), IntType, IntType)
            }.hook {
                before {
                    try {
                        val picker = this.args(0).any()
                        val oldVal = this.args(1).int()
                        val newVal = this.args(2).int()

                        // 检查是否是AM/PM Spinner
                        val mIdField = picker?.current()?.field {
                            name = "mId"
                        }?.any()

                        if (mIdField != null) {
                            val fieldName = mIdField.toString()
                            if (fieldName.contains("amPm", ignoreCase = true)) {
                                YLog.debug("[TimePickerPeriod] onValueChange - AM/PM Spinner变化: $oldVal -> $newVal")

                                // 根据选择的时段索引设置对应的小时数
                                val targetHour = when (newVal) {
                                    0 -> 2   // 凌晨 -> 2点
                                    1 -> 7   // 早上 -> 7点
                                    2 -> 10  // 上午 -> 10点
                                    3 -> 12  // 中午 -> 12点
                                    4 -> 15  // 下午 -> 15点
                                    5 -> 18  // 傍晚 -> 18点
                                    6 -> 21  // 晚上 -> 21点
                                    else -> 12
                                }

                                // 设置小时数
                                val instance = this.instance
                                instance.current().method {
                                    name = "setHour"
                                    param(IntType)
                                }.call(targetHour)

                                YLog.debug("[TimePickerPeriod] onValueChange - 设置小时数为: $targetHour")

                                // 阻止原方法执行
                                this.result = null
                            }
                        }
                    } catch (e: Exception) {
                        YLog.error("[TimePickerPeriod] onValueChange钩子异常: ${e.message}")
                    }
                }
            }
        }

        YLog.info("[TimePickerPeriod] AM/PM Spinner钩子安装完成")
    }

    // 根据小时获取时段索引
    private fun getPeriodIndex(hour: Int): Int {
        return when (hour) {
            in 0..5 -> 0   // 凌晨
            in 6..8 -> 1   // 早上
            in 9..11 -> 2  // 上午
            12 -> 3        // 中午
            in 13..17 -> 4 // 下午
            18 -> 5        // 傍晚
            in 19..23 -> 6 // 晚上
            else -> 3      // 默认中午
        }
    }

    /**
     * 处理时间选择器的标题更新
     */
//    private fun handleTimePickerUpdate(dialog: Any, timePicker: Any, hour24: Int, minute: Int) {
//        try {
//            // 判断是否是24小时制
//            val is24HourView = dialog.current().field {
//                name = "mIs24HourView"
//            }.boolean()
//
//            // 判断是否需要自定义副标题
//            val isNeedCustomSubTitle = dialog.current().field {
//                name = "mIsNeedCustomSubTitle"
//            }.boolean()
//
//            if (isNeedCustomSubTitle) {
//                return // 如果需要自定义副标题，不进行处理
//            }
//
//            // 获取副标题 TextView
//            val tvSubTitle = dialog.current().field {
//                name = "tvSubTitle"
//            }.any() as? android.widget.TextView
//
//            if (tvSubTitle != null) {
//                // 获取前缀
//                val prefixSubTitle = dialog.current().field {
//                    name = "mPrefixSubTitle"
//                }.string() ?: ""
//
//                // 构建新的副标题
//                val newSubTitle = buildSubTitle(is24HourView, hour24, minute, prefixSubTitle, timePicker)
//
//                // 设置副标题
//                tvSubTitle.text = newSubTitle
//                YLog.debug("[TimePickerPeriod] 设置标题: is24Hour=$is24HourView, hour24=$hour24, newSubTitle=$newSubTitle")
//            }
//        } catch (e: Exception) {
//            YLog.error("[TimePickerPeriod] 处理时间选择器更新时出错: ${e.message}")
//        }
//    }

    private fun handleTimePickerUpdate(dialog: Any, timePicker: Any, hour24: Int, minute: Int) {
        try {
            // 判断是否是24小时制
            val is24HourView = dialog.current().field {
                name = "mIs24HourView"
            }.boolean()

            // 判断是否需要自定义副标题
            val isNeedCustomSubTitle = dialog.current().field {
                name = "mIsNeedCustomSubTitle"
            }.boolean()

            if (isNeedCustomSubTitle) {
                return // 如果需要自定义副标题，不进行处理
            }

            // 获取副标题 TextView
            val tvSubTitle = dialog.current().field {
                name = "tvSubTitle"
            }.any() as? android.widget.TextView

            if (tvSubTitle != null) {
                // 获取前缀
                val prefixSubTitle = dialog.current().field {
                    name = "mPrefixSubTitle"
                }.string() ?: ""

                // 构建新的副标题
                val newSubTitle = buildSubTitle(is24HourView, hour24, minute, prefixSubTitle, timePicker)

                // 设置副标题
                tvSubTitle.text = newSubTitle
                YLog.debug("[TimePickerPeriod] 设置标题: is24Hour=$is24HourView, hour24=$hour24, newSubTitle=$newSubTitle")
            }

            // ========================== 新增：同步更新左侧时段Spinner ==========================
            try {
                // 从TimePicker实例获取mAmPmSpinner字段
                val mAmPmSpinner = timePicker.current().field {
                    name = "mAmPmSpinner"
                }.any()

                if (mAmPmSpinner != null) {
                    // 计算当前小时对应的时段索引
                    val periodIndex = getPeriodIndex(hour24)
                    YLog.debug("[TimePickerPeriod] updateTitle - 小时对应的时段索引: $periodIndex")

                    // 获取当前Spinner的值
                    val currentSpinnerValue = try {
                        mAmPmSpinner.current().method {
                            name = "getValue"
                            emptyParam()
                        }.int()
                    } catch (e: Exception) {
                        YLog.debug("[TimePickerPeriod] 无法获取当前Spinner值: ${e.message}")
                        -1
                    }

                    YLog.debug("[TimePickerPeriod] updateTitle - 当前Spinner值: $currentSpinnerValue")

                    // 只有当值不同时才更新，避免无限循环
                    if (currentSpinnerValue != periodIndex) {
                        mAmPmSpinner.current().method {
                            name = "setValue"
                            param(IntType)
                        }.call(periodIndex)
                        YLog.debug("[TimePickerPeriod] updateTitle - 已更新左侧Spinner值为: $periodIndex")
                    } else {
                        YLog.debug("[TimePickerPeriod] updateTitle - Spinner值未变化，跳过更新")
                    }
                } else {
                    YLog.warn("[TimePickerPeriod] updateTitle - 未找到mAmPmSpinner字段")
                }
            } catch (e: Exception) {
                YLog.error("[TimePickerPeriod] updateTitle - 更新左侧Spinner时发生异常: ${e.message}")
            }

        } catch (e: Exception) {
            YLog.error("[TimePickerPeriod] 处理时间选择器更新时出错: ${e.message}")
        }
    }

    /**
     * 构建副标题字符串
     */
    private fun buildSubTitle(is24HourView: Boolean, hour24: Int, minute: Int,
                              prefix: String, timePicker: Any?): String {
        return if (is24HourView) {
            // 24小时制
            if (is24HourDisplayPeriod) {
                // 显示时段
                build24HourTimeStringWithPeriod(hour24, minute, prefix)
            } else {
                // 不显示时段
                build24HourTimeString(hour24, minute, prefix)
            }
        } else {
            // 12小时制
            if (is12HourDisplayPeriod) {
                // 显示带中文时段的12小时制时间
                build12HourTimeStringWithPeriod(hour24, minute, prefix, timePicker)
            } else {
                // 不显示时段
                build12HourTimeString(hour24, minute, prefix, timePicker)
            }
        }
    }

    /**
     * 构建24小时制时间字符串（不显示时段）
     */
    private fun build24HourTimeString(hour24: Int, minute: Int, prefix: String): String {
        val hourStr = if (hour24 < 10) "0$hour24" else hour24.toString()
        val minuteStr = if (minute < 10) "0$minute" else minute.toString()
        return "$prefix$hourStr:$minuteStr"
    }

    /**
     * 构建24小时制时间字符串（显示时段）
     */
    private fun build24HourTimeStringWithPeriod(hour24: Int, minute: Int, prefix: String): String {
        // 根据24小时制的小时获取中文时段
        val period = getPeriodFromHour24(hour24)

        // 构建时间字符串（小时补零，分钟补零）
        val hourStr = if (hour24 < 10) "0$hour24" else hour24.toString()
        val minuteStr = if (minute < 10) "0$minute" else minute.toString()

        return if (period.isNotEmpty()) {
            "$prefix$period $hourStr:$minuteStr"
        } else {
            "$prefix$hourStr:$minuteStr"
        }
    }

    /**
     * 根据24小时制小时获取中文时段
     */
    private fun getPeriodFromHour24(hour24: Int): String {
        return when (hour24) {
            0 -> "凌晨"
            1, 2, 3, 4, 5 -> "凌晨"
            6, 7, 8 -> "早上"
            9, 10, 11 -> "上午"
            12 -> "中午"
            13, 14, 15, 16, 17 -> "下午"
            18 -> "傍晚"
            19, 20, 21, 22, 23 -> "晚上"
            else -> ""
        }
    }

    /**
     * 构建12小时制时间字符串（不显示时段）
     */
    private fun build12HourTimeString(hour24: Int, minute: Int, prefix: String, timePicker: Any?): String {
        // 将24小时制转换为12小时制
        val (hour12, _) = convert24To12Hour(hour24)

        // 获取AM/PM字符串
        val amPmString = if (hour24 >= 12) "PM" else "AM"

        // 构建时间字符串（小时不补零，分钟补零）
        val hourStr = hour12.toString()
        val minuteStr = if (minute < 10) "0$minute" else minute.toString()

        return "$prefix$amPmString $hourStr:$minuteStr"
    }

    /**
     * 构建12小时制时间字符串（显示中文时段）
     */
    private fun build12HourTimeStringWithPeriod(hour24: Int, minute: Int, prefix: String, timePicker: Any?): String {
        // 将24小时制转换为12小时制
        val (hour12, isPm) = convert24To12Hour(hour24)

        // 根据12小时制的小时和AM/PM状态获取中文时段
        val period = getPeriodFromHour12(hour12, isPm)

        // 构建时间字符串（小时不补零，分钟补零）
        val hourStr = hour12.toString()
        val minuteStr = if (minute < 10) "0$minute" else minute.toString()

        return if (period.isNotEmpty()) {
            "$prefix$period $hourStr:$minuteStr"
        } else {
            // 如果没有时段，回退到AM/PM显示
            val amPmString = if (isPm) "PM" else "AM"
            "$prefix$amPmString $hourStr:$minuteStr"
        }
    }

    /**
     * 将24小时制转换为12小时制
     * @return Pair<12小时制小时, 是否为下午>
     */
    private fun convert24To12Hour(hour24: Int): Pair<Int, Boolean> {
        return when (hour24) {
            0 -> Pair(12, false) // 0点 -> 12点上午
            in 1..11 -> Pair(hour24, false) // 1-11点 -> 1-11点上午
            12 -> Pair(12, true) // 12点 -> 12点下午
            else -> Pair(hour24 - 12, true) // 13-23点 -> 1-11点下午
        }
    }

    /**
     * 根据12小时制的小时和AM/PM状态获取中文时段
     */
    private fun getPeriodFromHour12(hour12: Int, isPm: Boolean): String {
        return if (!isPm) {
            // 上午时段
            when (hour12) {
                12 -> "凌晨" // 凌晨12点
                in 0..5 -> "凌晨" // 0-5点
                in 6..8 -> "早上" // 6-8点
                in 9..11 -> "上午" // 9-11点
                else -> ""
            }
        } else {
            // 下午时段
            when (hour12) {
                12 -> "中午" // 中午12点
                1 -> "下午" // 下午1点
                in 2..5 -> "下午" // 下午2-5点
                6 -> "傍晚" // 傍晚6点
                in 7..11 -> "晚上" // 晚上7-11点
                else -> ""
            }
        }
    }
}