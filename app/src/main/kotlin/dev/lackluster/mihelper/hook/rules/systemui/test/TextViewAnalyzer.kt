package dev.lackluster.mihelper.hook.rules.systemui.test

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of WooboxForRedmagicOS project
 * Copyright (C) 2024 YourName, your.email@example.com

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextClock
import android.widget.TextView
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.constructor
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale


/**
 * 调试工具：监控所有包含 "am"/"pm" 的 TextView，记录其设置文本、可见性变化等信息。
 * 使用 YukiHookAPI 语法重写。
 */
object TextViewAnalyzer : YukiBaseHooker() {

    // 开关状态（从 XSP 读取，可自行替换为 Prefs）
    private val isEnable = true
    private const val TAG = "TextViewAnalyzer"

    // 存储已发现的 TextView 信息
    private val foundTextViews = Collections.synchronizedMap(mutableMapOf<String, TextViewInfo>())

    // 记录日志历史
    private val logHistory = mutableListOf<String>()

    data class TextViewInfo(
        val resourceId: String,
        val className: String,
        val firstSeen: Long = System.currentTimeMillis(),
        var lastSeen: Long = System.currentTimeMillis(),
        var setTextCount: Int = 0,
        var visibilityChanges: Int = 0
    )

    @SuppressLint("SimpleDateFormat")
    override fun onHook() {
        // 如果功能未启用，直接返回
        if (!isEnable) {
            Log.d("TextViewAnalyzer", "功能未启用，跳过 Hook")
            return
        }

        YLog.debug(tag = TAG, msg = "开始 Hook TextView 分析功能")
        YLog.debug(tag = TAG, msg = "调试模式已开启，将记录所有包含 'am' 或 'pm' 的 TextView")

        // 添加关闭时的统计日志（进程退出时执行）
        Runtime.getRuntime().addShutdownHook(Thread {
            logStatistics()
        })

        try {
            // 获取 TextView 类
            val textViewClass = "android.widget.TextView".toClass()

////            // 1. Hook 构造函数
            hookTextViewConstructors(textViewClass)
//
//            // 2. Hook setText 方法
            hookSetTextMethod(textViewClass)
//
//
//            // 3. Hook setVisibility 方法（可选）
            hookSetVisibilityMethod(textViewClass)
//
//            // 4. Hook onVisibilityChanged 方法（可选）
            hookOnVisibilityChangedMethod(textViewClass)

//            hookSetTextMethod2(textViewClass)



            YLog.debug(tag = TAG, msg = "TextView 分析器安装完成")
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg =  "安装分析器时出错: ${e.message}")
        }
    }

    // ---------- Hook 实现 ----------

    /**
     * Hook TextView 的所有构造函数
     */
    private fun hookTextViewConstructors(textViewClass: Class<*>) {
        // 使用 constructor {}.hookAll 来钩住所有构造函数
        textViewClass.constructor {
            // 不指定参数，匹配所有构造函数
        }.hookAll {
            after {
                val textView = instance as? TextView ?: return@after
                val context = textView.context

                // 只处理 SystemUI
                if (context.packageName != "com.android.systemui") {
                    return@after
                }

                val resourceId = getResourceIdName(textView)

                // 检查资源 ID 是否包含 "am"/"pm"
                if (isAmPmTextView(resourceId)) {
                    logTextViewInfo("构造函数", textView, resourceId, null, null)

                    // 记录发现的 TextView
                    if (!foundTextViews.containsKey(resourceId)) {
                        foundTextViews[resourceId] = TextViewInfo(
                            resourceId = resourceId,
                            className = textView.javaClass.simpleName
                        )
                    }
                }
            }
        }

        YLog.debug(tag = TAG, msg = "已安装构造函数钩子")
    }

    /**
     * Hook setText(CharSequence) 方法
     */
    private fun hookSetTextMethod(textViewClass: Class<*>) {
        textViewClass.method {
            name = "setText"
            param(CharSequence::class.java)
        }.hook {
            before {
                val textView = instance as? TextView ?: return@before
                val context = textView.context

                // 只处理 SystemUI
                if (context.packageName != "com.android.systemui") {
                    return@before
                }

                val resourceId = getResourceIdName(textView)
                val newText = args[0] as? String
                val currentText = textView.text?.toString()

                // 检查资源 ID 或文本内容是否与 AM/PM 相关
                if (isAmPmTextView(resourceId) || (newText != null && isAmPmText(newText))) {
                    val logEntry = logTextViewInfo("setText", textView, resourceId, currentText, newText)

                    // 更新统计信息
                    val info = foundTextViews[resourceId]
                    if (info != null) {
                        info.lastSeen = System.currentTimeMillis()
                        info.setTextCount++
                    } else {
                        foundTextViews[resourceId] = TextViewInfo(
                            resourceId = resourceId,
                            className = textView.javaClass.simpleName,
                            setTextCount = 1
                        )
                    }

                    // 保存日志历史
                    saveToLogHistory(logEntry)
                }
            }
        }

        YLog.debug(tag = TAG, msg = "已安装 setText 方法钩子")
    }

    /**
     * Hook setVisibility(int) 方法
     */
    private fun hookSetVisibilityMethod(textViewClass: Class<*>) {
        try {
            textViewClass.method {
                name = "setVisibility"
                param(IntType)
            }.hook {
                before {
                    val textView = instance as? TextView ?: return@before
                    val context = textView.context

                    // 只处理 SystemUI
                    if (context.packageName != "com.android.systemui") {
                        return@before
                    }

                    val resourceId = getResourceIdName(textView)
                    val newVisibility = args[0] as Int

                    if (isAmPmTextView(resourceId)) {
                        val visibilityName = getVisibilityName(newVisibility)
                        val currentVisibility = getVisibilityName(textView.visibility)

                        YLog.debug(tag = TAG, msg = "TextView 可见性变化:")
                        YLog.debug(tag = TAG, msg = "资源ID: $resourceId")
                        YLog.debug(tag = TAG, msg = "当前可见性: $currentVisibility")
                        YLog.debug(tag = TAG, msg = "新可见性: $visibilityName")
                        YLog.debug(tag = TAG, msg = "当前文本: ${textView.text}")
                        YLog.debug(tag = TAG, msg = "类名: ${textView.javaClass.simpleName}")

                        // 记录视图层级
                        val hierarchy = getViewHierarchy(textView)
                        YLog.debug(tag = TAG, msg = "视图层级:")
                        for ((index, view) in hierarchy.withIndex()) {
                            val indent = "  ".repeat(index)
                            val className = view.javaClass.simpleName
                            val resId = getResourceIdName(view)
                            YLog.debug(tag = TAG, msg = "$indent$className (ID: $resId)")
                        }

                        // 更新统计信息
                        val info = foundTextViews[resourceId]
                        if (info != null) {
                            info.lastSeen = System.currentTimeMillis()
                            info.visibilityChanges++
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            YLog.error(tag = TAG, msg = "安装 setVisibility 钩子失败: ${e.message}")
        }


        YLog.debug(tag = TAG, msg = "已安装 setVisibility 方法钩子")
    }

    /**
     * Hook onVisibilityChanged(View, int) 方法（protected，可能不存在）
     */
    private fun hookOnVisibilityChangedMethod(textViewClass: Class<*>) {
        // 使用 try-catch 防止方法不存在导致崩溃
        try {
            textViewClass.method {
                name = "onVisibilityChanged"
                param(View::class.java, IntType)
            }.hook {
                after {
                    val textView = instance as? TextView ?: return@after
                    val context = textView.context

                    if (context.packageName != "com.android.systemui") {
                        return@after
                    }

                    val resourceId = getResourceIdName(textView)
                    val changedView = args[0] as? View
                    val visibility = args[1] as Int

                    if (isAmPmTextView(resourceId)) {
                        val visibilityName = getVisibilityName(visibility)
                        YLog.debug(tag = TAG, msg = "TextView onVisibilityChanged:")
                        YLog.debug(tag = TAG, msg = "资源ID: $resourceId")
                        YLog.debug(tag = TAG, msg = "可见性: $visibilityName")
                        YLog.debug(tag = TAG, msg = "文本: ${textView.text}")
                        YLog.debug(tag = TAG, msg = "改变的视图: ${changedView?.javaClass?.simpleName}")
                    }
                }
            }
            YLog.debug(tag = TAG, msg = "已安装 onVisibilityChanged 方法钩子")
        } catch (e: Exception) {
            Log.d("TextViewAnalyzer", "无法 Hook onVisibilityChanged 方法: ${e.message}")
        }
    }

    // ---------- 辅助方法 ----------

    /**
     * 检查资源 ID 是否与 AM/PM 相关
     */
    private fun isAmPmTextView(resourceId: String): Boolean {
        if (resourceId == "NO_ID" || resourceId == "UNKNOWN_ID") {
            return false
        }
        val lowerId = resourceId.lowercase(Locale.getDefault())
        return lowerId.contains("clock")
    }

    /**
     * 检查文本内容是否与 AM/PM 相关
     */
    private fun isAmPmText(text: String): Boolean {
        if (text.isEmpty()) return false
        val lowerText = text.lowercase(Locale.getDefault())
        return lowerText == "clock"
    }

    /**
     * 记录 TextView 信息到日志，并返回日志条目
     */
    @SuppressLint("SimpleDateFormat")
    private fun logTextViewInfo(
        method: String,
        textView: TextView,
        resourceId: String,
        currentText: String?,
        newText: String?
    ): String {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS").format(Date())
        val logEntry = buildString {
            append("[$timestamp] $method: ")
            append("资源ID='$resourceId', ")
            append("类名='${textView.javaClass.simpleName}', ")
            append("可见性='${getVisibilityName(textView.visibility)}', ")
            if (currentText != null) append("当前文本='$currentText', ")
            if (newText != null) append("新文本='$newText', ")
            append("位置=(${textView.left},${textView.top},${textView.right},${textView.bottom})")
        }

        YLog.debug(tag = TAG, msg = logEntry)

        // 记录视图层级
        val hierarchy = getViewHierarchy(textView)
        YLog.debug(tag = TAG, msg = "视图层级:")
        for ((index, view) in hierarchy.withIndex()) {
            val indent = "  ".repeat(index)
            val className = view.javaClass.simpleName
            val resId = getResourceIdName(view)
            YLog.debug(tag = TAG, msg = "$indent$className (ID: $resId)")
        }

        // 记录调用堆栈（前5个）
        YLog.debug(tag = TAG, msg = "调用堆栈:")
        Thread.currentThread().stackTrace.take(6).drop(2).forEach {
            YLog.debug(tag = TAG, msg = "  at $it")
        }

        return logEntry
    }

    /**
     * 获取视图从根到当前的层级列表
     */
    private fun getViewHierarchy(view: View): List<View> {
        val hierarchy = mutableListOf<View>()
        var current: View? = view
        var depth = 0
        val maxDepth = 10

        while (current != null && depth < maxDepth) {
            hierarchy.add(current)
            val parent = current.parent
            current = if (parent is View) parent else null
            depth++
        }

        return hierarchy.reversed()
    }

    /**
     * 将可见性整数转换为可读字符串
     */
    private fun getVisibilityName(visibility: Int): String {
        return when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN($visibility)"
        }
    }



    /**
     * 保存日志到历史记录（最多100条）
     */
    private fun saveToLogHistory(logEntry: String) {
        synchronized(logHistory) {
            if (logHistory.size >= 100) {
                logHistory.removeAt(0)
            }
            logHistory.add(logEntry)
        }
    }

    /**
     * 输出统计信息
     */
    @SuppressLint("SimpleDateFormat")
    private fun logStatistics() {
        YLog.debug(tag = TAG, msg = "====== 统计信息 ======")
        YLog.debug(tag = TAG, msg = "发现的 TextView 数量: ${foundTextViews.size}")

        if (foundTextViews.isNotEmpty()) {
            YLog.debug(tag = TAG, msg = "发现的 TextView 列表:")
            foundTextViews.values.sortedBy { it.firstSeen }.forEach { info ->
                val firstSeenTime = SimpleDateFormat("HH:mm:ss").format(Date(info.firstSeen))
                val lastSeenTime = SimpleDateFormat("HH:mm:ss").format(Date(info.lastSeen))

                YLog.debug(tag = TAG, msg = "资源ID: ${info.resourceId}")
                YLog.debug(tag = TAG, msg = "  类名: ${info.className}")
                YLog.debug(tag = TAG, msg = "  首次发现: $firstSeenTime")
                YLog.debug(tag = TAG, msg = "  最后发现: $lastSeenTime")
                YLog.debug(tag = TAG, msg = "  setText 调用次数: ${info.setTextCount}")
                YLog.debug(tag = TAG, msg = "  可见性变化次数: ${info.visibilityChanges}")
            }
        }

        YLog.debug(tag = TAG, msg = "日志记录数量: ${logHistory.size}")
        YLog.debug(tag = TAG, msg = "====== 统计结束 ======")
    }
//    private var lockScreenFound = false
//    private fun hookSetTextMethod2(textViewClass: Class<*>) {
//        textViewClass.method {
//            name = "setText"
//            param(CharSequence::class.java)
//        }.hook {
//            after {
//                // 如果已经处理过，直接返回
//                if (lockScreenFound) return@after
//                val textView = instance as? TextView ?: return@after
//                val context = textView.context
//                if (context.packageName != "com.android.systemui") return@after
//
//                val resourceId = getResourceIdName(textView)
//                val newText = args[0] as? String
//                val currentText = textView.text?.toString()
//
//                // 先判断是否为锁屏时钟
//                val isLockScreen = isLockScreenClock(textView)
//                if (isLockScreen) {
//                    lockScreenFound = true
//                    // 锁屏时钟的专属处理：可打印特殊标记，或存储实例等
//                    YLog.debug(tag = TAG, msg = "【锁屏时钟】检测到锁屏时钟文本变化")
//
//
//                    // 1. 保存原始字体大小（像素）
//                    val originalSizePx = textView.textSize
//                    // 为锁屏时钟设置字体
//                    // 设置之前，先得到该字体的所有属性
//                    // 保留原属性，仅修改字体风格
//                    // 或者原属性的字体大小
//                    // 为锁屏时钟设置字体风格
////                    textView.typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
//                    textView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
//                    // 字体大小 和原来的一致
////                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f)
//                    // 3. 恢复原始字体大小（必须用像素单位）
//                    textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, originalSizePx*0.8f)
//
//
//
//                }
//            }
//        }
//    }
    private var lockScreenFound = false
    private fun hookSetTextMethod2(textViewClass: Class<*>) {
        textViewClass.method {
            name = "setText"
            param(CharSequence::class.java)
        }.hook {
            after {
                // 如果已经处理过，直接返回
                if (lockScreenFound) return@after
                val textView = instance as? TextView ?: return@after
                val context = textView.context
                if (context.packageName != "com.android.systemui") return@after

                val resourceId = getResourceIdName(textView)
                val newText = args[0] as? String
                val currentText = textView.text?.toString()

                // 先判断是否为锁屏时钟
                val isLockScreen = isLockScreenClock(textView)
                if (isLockScreen) {
                    lockScreenFound = true
                    // 锁屏时钟的专属处理：可打印特殊标记，或存储实例等
                    YLog.debug(tag = TAG, msg = "【锁屏时钟】检测到锁屏时钟文本变化")


                    // 1. 保存原始字体大小（像素）
                    val originalSizePx = textView.textSize
                    val originalWidth = textView.width   // 注意：可能为0（尚未布局）
                    // 为锁屏时钟设置字体
                    // 设置之前，先得到该字体的所有属性
                    // 保留原属性，仅修改字体风格
                    // 或者原属性的字体大小
                    // 为锁屏时钟设置字体风格
//                    textView.typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                    val typeface = Typeface.createFromFile("/system/fonts/DancingScript-Regular.ttf")
                    textView.typeface = typeface
                    // 恢复原始宽度
                    // 字体大小 和原来的一致
//                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f)
                    // 3. 恢复原始字体大小（必须用像素单位）
                    textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, originalSizePx*0.8f)

                    // 关键步骤：固定宽度为原始宽度，防止布局抖动
                    if (originalWidth > 0) {
                        val params = textView.layoutParams
                        params?.width = originalWidth
                        YLog.debug(tag = TAG, msg = "【锁屏时钟】宽度为：${params?.width}")
                        textView.layoutParams = params  // 重新设置 LayoutParams 会触发 requestLayout
                    }


                }
            }
        }
    }



    /**
     * 判断给定视图是否为锁屏界面中的特定时钟视图（TextClock with ID clock_view）
     * 并逐级验证所有父视图的资源ID是否与期望的层级完全一致。
     *
     * @param view 要检查的视图（应为 TextClock 实例）
     * @return 如果视图及其所有父视图的ID与锁屏时钟层级完全匹配，则返回 true；否则 false
     */
    private fun isLockScreenClock(view: View): Boolean {
        try {
            // 1. 检查当前视图本身：必须是 TextClock 且 ID 为 clock_view
            if (view !is TextView) {
                YLog.debug(tag = TAG, msg =" 当前视图不是 TextView")
                return false
            }
            val viewIdName = getResourceIdName(view)
            if (viewIdName != "clock_view") {
                YLog.debug(tag = TAG, msg ="视图ID不匹配: $viewIdName, 期望: clock_view")
                return false
            }

            // 2. 逐级检查父视图ID（从直接父容器开始向上）
            // 第1级父容器：clock_area
            val parent1 = view.parent
            if (parent1 !is View) return false
            val parent1Id = getResourceIdName(parent1)
            if (parent1Id != "clock_area") {
                YLog.debug(tag = TAG, msg ="父容器1 ID不匹配: $parent1Id, 期望: clock_area")
                return false
            }

            // 第2级父容器：depth_down_area
            val parent2 = parent1.parent
            if (parent2 !is View) return false
            val parent2Id = getResourceIdName(parent2)
            if (parent2Id != "depth_down_area") {
                YLog.debug(tag = TAG, msg ="父容器2 ID不匹配: $parent2Id, 期望: depth_down_area")
                return false
            }

            // 第3级父容器：ConstraintLayout（通常无ID，期望为 NO_ID）
            val parent3 = parent2.parent
            if (parent3 !is View) return false
            val parent3Id = getResourceIdName(parent3)
            if (parent3Id != "NO_ID" && parent3Id != "UNKNOWN_ID") {
                YLog.debug(tag = TAG, msg ="父容器3 ID不匹配: $parent3Id, 期望无ID")
                return false
            }

            // 第4级父容器：keyguard_clock_style_default
            val parent4 = parent3.parent
            if (parent4 !is View) return false
            val parent4Id = getResourceIdName(parent4)
            if (parent4Id != "keyguard_clock_style_default") {
                YLog.debug(tag = TAG, msg ="父容器4 ID不匹配: $parent4Id, 期望: keyguard_clock_style_default")
                return false
            }

            // 第5级父容器：keyguard_clock_container
            val parent5 = parent4.parent
            if (parent5 !is View) return false
            val parent5Id = getResourceIdName(parent5)
            if (parent5Id != "keyguard_clock_container") {
                YLog.debug(tag = TAG, msg ="父容器5 ID不匹配: $parent5Id, 期望: keyguard_clock_container")
                return false
            }

            // 第6级父容器：status_view_container
            val parent6 = parent5.parent
            if (parent6 !is View) return false
            val parent6Id = getResourceIdName(parent6)
            if (parent6Id != "status_view_container") {
                YLog.debug(tag = TAG, msg ="父容器6 ID不匹配: $parent6Id, 期望: status_view_container")
                return false
            }

            // 第7级父容器：screen_on_area
            val parent7 = parent6.parent
            if (parent7 !is View) return false
            val parent7Id = getResourceIdName(parent7)
            if (parent7Id != "screen_on_area") {
                YLog.debug(tag = TAG, msg ="父容器7 ID不匹配: $parent7Id, 期望: screen_on_area")
                return false
            }

            // 第8级父容器：keyguard_status_view
            val parent8 = parent7.parent
            if (parent8 !is View) return false
            val parent8Id = getResourceIdName(parent8)
            if (parent8Id != "keyguard_status_view") {
                YLog.debug(tag = TAG, msg ="父容器8 ID不匹配: $parent8Id, 期望: keyguard_status_view")
                return false
            }

            // 第9级父容器：notification_panel
            val parent9 = parent8.parent
            if (parent9 !is View) return false
            val parent9Id = getResourceIdName(parent9)
            if (parent9Id != "notification_panel") {
                YLog.debug(tag = TAG, msg ="父容器9 ID不匹配: $parent9Id, 期望: notification_panel")
                return false
            }

            // 如果所有层级都匹配
            YLog.debug(tag = TAG, msg ="找到锁屏时钟，视图层级验证通过")
            return true

        } catch (e: Exception) {
            YLog.debug(tag = TAG, msg ="检查锁屏时钟时出错: ${e.message}")
            return false
        }
    }

    /**
     * 根据视图获取其资源ID的字符串名称。
     * 参考状态栏时钟中的实现。
     */
    private fun getResourceIdName(view: View): String {
        return try {
            val resId = view.id
            if (resId <= 0) {
                "NO_ID"
            } else {
                try {
                    view.resources.getResourceEntryName(resId)
                } catch (e: Resources.NotFoundException) {
                    "0x${Integer.toHexString(resId)}"
                }
            }
        } catch (e: Exception) {
            "UNKNOWN_ID"
        }
    }
}