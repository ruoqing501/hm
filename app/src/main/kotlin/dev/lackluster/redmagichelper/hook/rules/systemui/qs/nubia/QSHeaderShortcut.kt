package dev.lackluster.redmagichelper.hook.rules.systemui.qs.nubia

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.widget.RelativeLayout
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import androidx.core.net.toUri

object QSHeaderShortcut : YukiBaseHooker() {
    private const val TAG = "QSHeaderShortcut"

    // 配置项：点击日期跳转日历
    private val qsShortcutRedirCalendar get() =
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.QS_SHORTCUT_REDIR_CALENDAR, false)
    // 配置项：点击搜索打开浏览器
    private val qsShortcutRedirSearch get() =
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.QS_SHORTCUT_REDIR_SEARCH, false)
    private val customBrowserPackage get() =
        Prefs.getString(Pref.Key.SystemUI.StatusBar.QS_CUSTOM_BROWSER_PACKAGE, "cn.nubia.browser")

    override fun onHook() {
        // 获取 CCHeaderView 类
        val ccHeaderClass = "com.zte.controlcenter.widget.CCHeaderView".toClassOrNull()


        // 获取 postStartActivityDismissingKeyguard 方法引用（用于后续调用）
        val postStartMethod = ccHeaderClass?.method {
            name = "postStartActivityDismissingKeyguard"
            param(Intent::class.java)
        }


        // 1. 点击日期跳转日历
        ccHeaderClass?.method {
            name = "handleClickDate"
        }?.hook {
            before {
                if (!qsShortcutRedirCalendar) return@before
                YLog.debug("$TAG： 拦截 handleClickDate，跳转日历")
                // 构建日历 Intent
                val intent = Intent(Intent.ACTION_VIEW,
                    "content://com.android.calendar/time".toUri())
                // 调用原类的 postStartActivityDismissingKeyguard 方法
                postStartMethod?.get( instance)?.call(intent)
                // 阻止原方法执行
                result = null
                YLog.debug("$TAG： 跳转日历完成")
            }
        }
//        if (qsShortcutRedirSearch) {
//            ccHeaderClass?.method {
//                name = "handleClickSearch"
//            }?.hook {
//                before {
//                    YLog.debug("$TAG： 拦截 handleClickSearch，打开浏览器")
//
//                    // 创建默认浏览器 Intent
////                    val intent = Intent(Intent.ACTION_VIEW, "http://".toUri())
//                    val intent = Intent(Intent.ACTION_VIEW, "http://".toUri())
//                    val resolveInfo: ResolveInfo? = (instance as RelativeLayout).context.packageManager.resolveActivity(
//                        intent,
//                        PackageManager.MATCH_DEFAULT_ONLY
//                    )
//
//                    if (resolveInfo != null) {
//                        val packageName = resolveInfo.activityInfo.packageName
//                        val className = resolveInfo.activityInfo.name
//                        YLog.debug("$TAG： 默认浏览器包名：$packageName")
//                        val browserIntent = Intent().setClassName(packageName, className)
//                        postStartMethod?.get( instance)?.call(browserIntent)
//                    } else {
//                        YLog.warn("$TAG： 未找到默认浏览器")
//                    }
//
//                    result = null
//                    YLog.debug("$TAG： 打开浏览器完成")
//                }
//            }
//        }


        ccHeaderClass?.method {
            name = "handleClickSearch"
        }?.hook {
            before {
                if (!qsShortcutRedirSearch) return@before
                YLog.debug("$TAG：拦截 handleClickSearch，尝试打开自定义浏览器：$customBrowserPackage")

                    val context = (instance as RelativeLayout).context
//                    val intent = Intent(Intent.ACTION_VIEW, "http://".toUri()).apply {
                    val intent = Intent(Intent.ACTION_VIEW, "http://baidu.com".toUri()).apply {
                        setPackage(customBrowserPackage)  // 指定目标包名
                    }




                    // 检查该包是否有 Activity 能处理该 Intent
                    val resolveInfo = context.packageManager.resolveActivity(intent, 0)
                    if (resolveInfo != null) {
                        // 包名有效，直接使用该 Intent
                        postStartMethod?.get(instance)?.call(intent)
                        YLog.debug("$TAG：成功启动浏览器：$customBrowserPackage")
                    }
//                    else {
//                        // 包名无效，可选的回退策略
//                        YLog.warn("$TAG：指定的浏览器未安装：$customBrowserPackage，使用默认浏览器")
//                        // 方案一：静默失败（不执行任何跳转）
//                        // 方案二：回退到默认浏览器（去掉 setPackage 再试）
//                        val fallbackIntent = Intent(Intent.ACTION_VIEW, "http://".toUri())
//                        postStartMethod?.get(instance)?.call(fallbackIntent)
//                        // 方案三：显示 Toast 提示用户
//                        // Toast.makeText(context, "浏览器未安装", Toast.LENGTH_SHORT).show()
//                    }

                result = null  // 阻止原方法执行
                YLog.debug("$TAG：处理完成")
            }
        }



    }
}