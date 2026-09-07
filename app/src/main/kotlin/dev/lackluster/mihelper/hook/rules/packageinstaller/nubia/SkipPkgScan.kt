package dev.lackluster.mihelper.hook.rules.packageinstaller.nubia

import android.annotation.SuppressLint
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs

object SkipApkScan : YukiBaseHooker() {
    private val skipPkgScan by lazy {
        Prefs.getBoolean(Pref.Key.NubiaPackageInstaller.SKIP_PKG_INSTALLER_SCAN, false)
    }
    @SuppressLint("PrivateApi")
    override fun onHook() {
        if (!skipPkgScan){
            YLog.debug("[SkipApkScan] 跳过APK扫描功能已关闭")
            return
        }

        YLog.debug("[SkipApkScan] 开始Hook跳过APK扫描功能")
        try {
            // Hook InstallStaging.StagingAsyncTask 的 onPostExecute 方法
            "com.android.packageinstaller.InstallStaging\$StagingAsyncTask".toClassOrNull()?.apply {
                method {
                    name = "onPostExecute"
                    paramCount = 1
                }.hook {
                    before {
                        YLog.debug("[SkipApkScan] 尝试跳过APK扫描")

                        // 设置 PackageUtil 中的 ZTE_FEATURE_ODM_VERTU 字段为 true
                        val packageUtilClass = "com.android.packageinstaller.PackageUtil".toClassOrNull()
                        packageUtilClass?.apply {
                            // 使用 YukiHookAPI 的方式设置静态字段
                            field {
                                name = "ZTE_FEATURE_ODM_VERTU"
                                modifiers { isStatic }
                            }.get().set(true)
                        } ?: run {
                            YLog.debug("[SkipApkScan] 未找到 PackageUtil 类")
                        }
                    }
                }
            } ?: run {
                YLog.debug("[SkipApkScan] 未找到 InstallStaging.StagingAsyncTask 类")
            }
        }catch (e: Exception){
            YLog.debug("[SkipApkScan] Hook跳过APK扫描异常: ${e.message}", e)
        }

    }
}