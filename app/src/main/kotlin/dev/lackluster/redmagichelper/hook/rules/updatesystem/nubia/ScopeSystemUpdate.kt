package dev.lackluster.redmagichelper.hook.rules.updatesystem.nubia

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.app.Application
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.ArrayClass
import dev.lackluster.redmagichelper.hook.compat.type.java.LongType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

object ScopeSystemUpdate : YukiBaseHooker() {
    private const val TAG = "ScopeSystemUpdate"

    // 两个开关的值（每次 before 中读取，开关切换即时生效）
    private val disableUpdate
        get() = Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.DISABLE_SYSTEM_UPDATE, false)
    private val copyUrl
        get() = Prefs.getBoolean(Pref.Key.NubiaSystemUpdate.UPDATE_PACKAGE_ADDRESS, false)

    override fun onHook() {
        hookApplyPayload()
    }

    private fun hookApplyPayload() {
        val updateEngine = "android.os.UpdateEngine".toClassOrNull() ?: run {
            YLog.error("$TAG 未找到 UpdateEngine 类")
            return
        }

        updateEngine.method {
            name = "applyPayload"
            param(StringClass, LongType, LongType, ArrayClass(StringClass))
        }.hook {
            before {
                val url = args[0] as String
                val offset = args[1] as Long
                val size = args[2] as Long
                val headerKeyValuePairs = args[3] as Array<*>
                // 1. 复制 URL 到剪贴板（仅当对应开关开启）
                if (copyUrl) {
                    val context = getSystemUIContext()
                    if (context != null) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SystemUpdateURL", url)
                        clipboard.setPrimaryClip(clip)
                        YLog.debug("$TAG 已将 URL 复制到剪贴板: $url")
                    } else {
                        YLog.warn("$TAG 无法获取 Context，未能复制 URL 到剪贴板")
                    }
                }

                // 2. 拦截更新（仅当对应开关开启）
                if (disableUpdate) {
                    result = null
                    YLog.info(
                        "$TAG 已经拦截了一次系统更新：\n- URL: $url\n- Offset: $offset\n- Size: $size\n- headerKeyValuePairs: ${
                            headerKeyValuePairs.joinToString(", ")
                        }"
                    )
                } else {
                    // 不拦截，仅记录日志（可选）
                    YLog.debug("$TAG 检测到系统更新但未拦截 (URL: $url)")
                }
            }
        }
    }

    /**
     * 尝试获取 SystemUI 的 Context，用于访问剪贴板服务
     */
    private fun getSystemUIContext(): Context? {
        return try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApp = activityThread.getMethod("currentApplication").invoke(null) as Application
            currentApp.createPackageContext("com.android.systemui", Context.CONTEXT_IGNORE_SECURITY)
        } catch (e: Exception) {
            null
        }
    }
}