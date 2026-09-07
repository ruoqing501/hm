package dev.lackluster.mihelper.hook.rules.themes.nubia

import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.constructor
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.AnyClass
import dev.lackluster.mihelper.hook.compat.type.java.BooleanType
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.hook.compat.type.java.StringClass
import dev.lackluster.mihelper.hook.compat.type.java.UnitType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs

object ScopeThemeUpdate : YukiBaseHooker() {
    private const val TAG = "ScopeThemeUpdate"

    override fun onHook() {
        if (!Prefs.getBoolean(Pref.Key.NubiaTheme.CANCEL_TRIAL_LOGIN, false)) return
        YLog.debug("$TAG: 试用免登录开关已开启，开始Hook")

        // 统一处理主题下载（包括付费试用和免费主题）——可根据需要启用
         hookThemeDownloadUnified()

        // 其他辅助Hook
        hookWallpaperDownload() // 增强后的壁纸下载免登录处理



        hookBeanIsCharge() // 判断资源是否付费

        // 熄屏免登录下载
        hookAodDownload()
    }





    private fun hookAodDownload() {
        val fragmentClass = "com.zte.beautify.view.common.preview.online.AodPreviewFragment".toClassOrNull() ?: return
        val accountManagerClass = "com.zte.beautify.model.remote.zteaccount.ZteAccountManager".toClassOrNull() ?: return
        val downloadManagerClass = "com.zte.beautify.model.download.DownloadManager".toClassOrNull() ?: return
        val downloadListenerClass = "com.lzy.okserver.custom.download.DownloadListener".toClassOrNull() ?: return // 注意实际包名
        val uploadActionUtilsClass = "com.zte.beautify.view.common.tools.UploadActionUtils".toClassOrNull() ?: return
        val zteTrackUtilsClass = "com.zte.beautify.model.remote.analytics.ZteTrackUtils".toClassOrNull() ?: return
        val commonExecUtilClass = "com.zte.beautify.util.CommonExecUtil".toClassOrNull() ?: return
        val saveAodPreviewTaskClass = "com.zte.beautify.view.common.tools.SaveAodPreviewTask".toClassOrNull() ?: return
        val aodDownloadListenerClass = "com.zte.beautify.view.common.preview.online.AodPreviewFragment\$AodDownloadListener".toClassOrNull() ?: return
        val aodTryDownloadListenerClass = "com.zte.beautify.view.common.preview.online.AodPreviewFragment\$AodTryDownloadListener".toClassOrNull() ?: return

        fragmentClass.method {
            name = "aodDownload"
            emptyParam()
            returnType = BooleanType
        }.hook {
            before {
                val accountInstance = accountManagerClass.method {
                    name = "getInstance"
                    returnType = accountManagerClass
                }.get().call() ?: return@before
                val currentUser = accountManagerClass.method {
                    name = "getCurrentUserInfo"
                }.get(accountInstance).call()
                if (currentUser != null) return@before // 已登录不干预

                YLog.debug("$TAG: AOD下载 - 用户未登录，启动免登录下载")

                val mBean = fragmentClass.field { name = "mBean" }.get(instance).any() ?: run {
                    YLog.debug("$TAG: mBean 字段为空")
                    return@before
                }
                val beanClass = mBean.javaClass

                val srcId = beanClass.method {
                    name = "getSrcId"
                    superClass()
                    emptyParam()
                }.get(mBean).call() as? String ?: run {
                    YLog.debug("$TAG: 获取 srcId 失败")
                    return@before
                }

                // 保存预览（可选，原方法中会执行）
                try {
                    val saveTask = saveAodPreviewTaskClass.constructor {
                        param(IntType, beanClass)
                    }.get().newInstance(0, mBean) as? Runnable
                    if (saveTask != null) {
                        val util = commonExecUtilClass.newInstance()
                        commonExecUtilClass.method {
                            name = "setFuncAndExec"
                            param(Runnable::class.java, StringClass)
                        }.get(util).call(saveTask, "SaveAodPreviewTask")
                    }
                } catch (e: Throwable) {
                    YLog.error("$TAG: SaveAodPreviewTask 执行失败", e)
                }

                // 设置下载时间
                beanClass.method {
                    name = "setDownloadTime"
                    superClass()
                    param(StringClass)
                }.get(mBean).call(System.currentTimeMillis().toString())

                val isFree = beanClass.method {
                    name = "isFree"
                    emptyParam()
                }.get(mBean).call() as? Boolean ?: false
                val isCharge = beanClass.method {
                    name = "getIsCharge"
                    emptyParam()
                }.get(mBean).call() as? Boolean ?: false
                val isTimeLimitFree = beanClass.method {
                    name = "isTimeLimitFree"
                    emptyParam()
                }.get(mBean).call() as? Boolean ?: false

                val isFreeOrPaid = isFree || isCharge || isTimeLimitFree

                val downloadManagerInstance = downloadManagerClass.method {
                    name = "getInstance"
                    returnType = downloadManagerClass
                }.get().call() ?: run {
                    YLog.debug("$TAG: 获取 DownloadManager 实例失败")
                    return@before
                }



                if (isFreeOrPaid) {
                    YLog.debug("$TAG: AOD免费下载，srcId=$srcId")
                    val listener = aodDownloadListenerClass.constructor {
                        param(AnyClass, fragmentClass)
                    }.give()?.newInstance("AodPreviewFragment", instance)
                    if (listener != null) {
                        downloadManagerClass.method {
                            name = "startDownloadTask"
                            param(beanClass, downloadListenerClass) // 使用父类
                        }.get(downloadManagerInstance).call(mBean, listener)
                    }

                    uploadActionUtilsClass.method {
                        name = "downloadResource"
                        param(StringClass)
                    }.get().call(srcId)
                    zteTrackUtilsClass.method {
                        name = "trackResourceClickEvent"
                        param(IntType, StringClass)
                    }.get().call(9, srcId) // FREE_AOD_DOWNLOAD_CLICK_EVENT
                } else {
                    YLog.debug("$TAG: AOD试用下载，srcId=$srcId")
                    val tryListener = aodTryDownloadListenerClass.constructor {
                        param(AnyClass, fragmentClass)
                    }.give()?.newInstance("AodPreviewFragment", instance)
                    if (tryListener != null) {
                        downloadManagerClass.method {
                            name = "startDownloadTask"
                            param(beanClass, downloadListenerClass, StringClass) // 三参数，参数类型为父类
                        }.get(downloadManagerInstance).call(mBean, tryListener, "try")
                    }

                    uploadActionUtilsClass.method {
                        name = "downloadResource"
                        param(StringClass)
                    }.get().call(srcId)
                    zteTrackUtilsClass.method {
                        name = "trackResourceClickEvent"
                        param(IntType, StringClass)
                    }.get().call(15, srcId) // TRIAL_USE_CLICK_EVENT
                }

                result = true // 拦截原方法
            }
        }
    }

    /**
     * 统一处理所有主题下载的免登录逻辑（包括付费试用和免费主题）
     */
    private fun hookThemeDownloadUnified() {
        val fragmentClass = "com.zte.beautify.view.common.preview.online.OnlineThemePreviewFragment".toClassOrNull() ?: return
        val accountManagerClass = "com.zte.beautify.model.remote.zteaccount.ZteAccountManager".toClassOrNull() ?: return

        fragmentClass.method {
            name = "themeDownload"
            param(BooleanType)
            returnType = BooleanType
        }.hook {
            before {
                val isTrial = args[0] as Boolean

                // 获取当前资源 Bean
                val mBean = fragmentClass.field { name = "mBean" }.get(instance).any() ?: run {
                    YLog.warn("$TAG: mBean 字段为空")
                    return@before
                }

                // 获取资源ID（注意 getSrcId 在父类 DBBean 中，需要 superClass）
                val srcId = mBean.javaClass.method {
                    name = "getSrcId"
                    superClass()
                    emptyParam()
                }.get(mBean).call() as? String ?: run {
                    YLog.warn("$TAG: 获取 srcId 失败")
                    return@before
                }

                // 判断是否为免费主题（isFree 在 Bean 自身，不需要 superClass）
                val isFree = mBean.javaClass.method {
                    name = "isFree"
                    superClass()
                    emptyParam()
                }.get(mBean).call() as? Boolean ?: false

                val isCharge = mBean.javaClass.method {
                    name = "getIsCharge"
                    superClass()
                    emptyParam()
                }.get(mBean).call() as? Boolean ?: false
                val isTimeLimitFree = mBean.javaClass.method {
                    name = "isTimeLimitFree"
                    superClass()
                    emptyParam()
                }.get(mBean).call() as? Boolean ?: false


                YLog.debug("$TAG: isFree=$isFree, isCharge=$isCharge, isTimeLimitFree=$isTimeLimitFree" )

                // 检查登录状态
                val accountInstance = accountManagerClass.method {
                    name = "getInstance"
                    returnType = accountManagerClass
                }.get().call()
                val currentUser = accountManagerClass.method {
                    name = "getCurrentUserInfo"
                }.get(accountInstance).call()
                if (currentUser != null) {
                    YLog.debug("$TAG: 用户已登录，交由原方法处理")
                    return@before
                }

                // 根据场景处理
                when {
                    isTrial -> {
                        // 付费主题试用下载
                        YLog.debug("$TAG: 试用免登录触发，srcId=$srcId")
                        fragmentClass.method {
                            name = "doDownload"
                            param(StringClass, BooleanType)
                            returnType = UnitType
                        }.get(instance).call(srcId, true)  // 试用标志为 true
                        result = true
                    }
                    isFree -> {
                        // 免费主题下载
                        YLog.debug("$TAG: 免费主题免登录触发，srcId=$srcId")
                        fragmentClass.method {
                            name = "doDownload"
                            param(StringClass, BooleanType)
                            returnType = UnitType
                        }.get(instance).call(srcId, false) // 非试用
                        result = true
                    }
                    else -> {
                        // 其他情况（如已付费但未下载）不处理，留给原逻辑
                        YLog.debug("$TAG: 非免费且非试用，不干预-点击了购买按钮？")
                        // 付费资源：转为试用下载，调用 doDownload(srcId, true)
                        fragmentClass.method { name = "doDownload"; param(StringClass, BooleanType) }
                            .get(instance).call(srcId, true)
                        result = true
                    }
                }
            }
        }
    }

    /*
    * 将试用资源标记为已购买（辅助功能，可根据需要启用）
    */
    private fun hookBeanIsCharge() {
        val beanClass = "com.zte.beautify.model.local.data.Bean".toClassOrNull() ?: return

        beanClass.method {
            name = "getIsCharge"
            emptyParam()
            returnType = BooleanType
        }.hook {
            after {
                val original = result as Boolean
                if (!original) {
                    val fromType = beanClass.method {
                        name = "getFromType"
                        superClass()
                        emptyParam()
                    }.get(instance).call() as? String
                    if ("try" == fromType) {
                        result = true
                        YLog.debug("$TAG: 试用资源被标记为已购买，原始值为 false")
                    }else{ //所有资源都标记为已购买
                        result = true
                        YLog.debug("$TAG: 资源标记为已购买，原始值为 false")
                    }
                }
            }
        }
    }

    /*
    * 壁纸下载免登录（同时处理付费试用和免费壁纸）
    * 对应 BeautyPreviewActivity 中的 ResourceDownload(boolean) 方法
    */
    private fun hookWallpaperDownload() {
        val activityClass = "com.zte.beautify.view.common.preview.BeautyPreviewActivity".toClassOrNull() ?: return
        val accountManagerClass = "com.zte.beautify.model.remote.zteaccount.ZteAccountManager".toClassOrNull() ?: return
        val downloadManagerClass = "com.zte.beautify.model.download.DownloadManager".toClassOrNull() ?: return

        // 两个监听器类的名称
        val tryListenerClass = "com.zte.beautify.view.common.preview.BeautyPreviewActivity\$OnlineResourceTryDownloadListener".toClassOrNull()
        val downloadListenerClass = "com.zte.beautify.view.common.preview.BeautyPreviewActivity\$OnlineResourceDownloadListener".toClassOrNull()

        activityClass.method {
            name = "ResourceDownload"
            param(BooleanType)
        }.hook {
            before {
                val isTrial = args[0] as Boolean

                // 获取 mBean
                val mBeanField = activityClass.field { name = "mBean" }.get(instance).any() ?: run {
                    YLog.warn("$TAG: mBean 字段为空")
                    return@before
                }
                val beanClass = mBeanField.javaClass

                // 获取 srcId
                val srcId = beanClass.method {
                    name = "getSrcId"
                    superClass()
                    emptyParam()
                }.get(mBeanField).call() as? String ?: run {
                    YLog.warn("$TAG: 获取 srcId 失败")
                    return@before
                }

                // 判断是否为免费壁纸
                val isFree = beanClass.method {
                    name = "isFree"
                    emptyParam()
                }.get(mBeanField).call() as? Boolean ?: false

                // 检查登录状态
                val accountInstance = accountManagerClass.method {
                    name = "getInstance"
                    returnType = accountManagerClass
                }.get().call() ?: return@before
                val currentUser = accountManagerClass.method {
                    name = "getCurrentUserInfo"
                }.get(accountInstance).call()
                if (currentUser != null) {
                    YLog.debug("$TAG: 用户已登录，交由原方法处理")
                    return@before
                }

                // 需要处理的场景：试用 或 免费
                if (!isTrial && !isFree) {
                    YLog.debug("$TAG: 非试用且非免费，不干预-购买按钮？")
//                    return@before
                }

                YLog.debug("$TAG: 壁纸免登录触发，isTrial=$isTrial, isFree=$isFree, srcId=$srcId")

                // 设置下载时间
                beanClass.method {
                    name = "setDownloadTime"
                    superClass()
                    param(StringClass)
                }.get(mBeanField).call(System.currentTimeMillis().toString())

                // 获取 DownloadManager 实例
                val downloadManagerInstance = downloadManagerClass.method {
                    name = "getInstance"
                    returnType = downloadManagerClass
                }.get().call() ?: run {
                    YLog.warn("$TAG: 获取 DownloadManager 实例失败")
                    return@before
                }

                // 根据场景选择监听器
                val listener = if (isTrial) {
                    // 试用下载：使用 TryDownloadListener
                    if (tryListenerClass == null) {
                        YLog.warn("$TAG: OnlineResourceTryDownloadListener 类未找到")
                        null
                    } else {
                        val constructor = tryListenerClass.constructor {
                            paramCount = 2
                            param(AnyClass, activityClass)
                        }.give()
                        constructor?.newInstance("OnlineResourcePreviewFragment", instance)
                    }
                } else {
                    // 免费下载：使用普通的 DownloadListener
                    if (downloadListenerClass == null) {
                        YLog.warn("$TAG: OnlineResourceDownloadListener 类未找到")
                        null
                    } else {
                        val constructor = downloadListenerClass.constructor {
                            paramCount = 2
                            param(AnyClass, activityClass)
                        }.give()
                        constructor?.newInstance("OnlineResourcePreviewFragment", instance)
                    }
                }

                // 调用 startDownloadTask
                if (isTrial) {
                    // 试用下载需要第三个参数 "try"
                    downloadManagerClass.method {
                        name = "startDownloadTask"
                        param(beanClass, (tryListenerClass?.superclass ?: tryListenerClass) ?: AnyClass, StringClass)
                    }.get(downloadManagerInstance).call(mBeanField, listener, "try")
                } else {
                    // 免费下载不需要第三个参数
                    downloadManagerClass.method {
                        name = "startDownloadTask"
                        param(beanClass, (downloadListenerClass?.superclass ?: downloadListenerClass) ?: AnyClass)
                    }.get(downloadManagerInstance).call(mBeanField, listener)
                }

                YLog.debug("$TAG: 下载任务已启动")

                // 拦截原方法，阻止启动登录
                result = null
            }
        }
    }
}