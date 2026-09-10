package dev.lackluster.redmagichelper.hook.rules.neostore

import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

// 努比亚应用中心：自定义同时下载数量
object StoreDownloadHook : YukiBaseHooker() {

    private const val TAG = "StoreDownloadHook"

    override fun onHook() {
        hasEnable(Pref.Key.NeoStore.STORE_DOWNLOAD_ENABLED) {
            hookConfigMgrCount()
            hookDownloadServiceResize()
        }
    }

    private val downloadCount: Int
        get() = Prefs.getInt(Pref.Key.NeoStore.STORE_DOWNLOAD_COUNT, 5).coerceIn(1, 50)

    /**
     * ConfigMgrImp.H0() 返回同时下载数量，直接替换为用户设置的值
     */
    private fun hookConfigMgrCount() {
        "cn.nubia.neostore.model.ConfigMgrImp".toClassOrNull()?.apply {
            method {
                name = "H0"
                emptyParam()
                returnType = IntType
            }.hook {
                before {
                    result = downloadCount
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 ConfigMgrImp 类，同时下载数量 Hook 失败")
    }

    /**
     * DownloadService.f(int) 调整下载队列大小，将参数改为用户设置的值
     */
    private fun hookDownloadServiceResize() {
        "cn.nubia.neostore.service.DownloadService".toClassOrNull()?.apply {
            method {
                name = "f"
                param(IntType)
            }.hook {
                before {
                    args[0] = downloadCount
                }
            }
        } ?: YLog.error(tag = TAG, msg = "未找到 DownloadService 类，下载队列 Hook 失败")
    }
}
