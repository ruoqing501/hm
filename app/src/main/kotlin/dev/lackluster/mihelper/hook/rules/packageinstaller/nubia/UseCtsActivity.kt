package dev.lackluster.mihelper.hook.rules.packageinstaller.nubia

import android.annotation.SuppressLint
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object UseCtsActivity : YukiBaseHooker() {
    @SuppressLint("PrivateApi")
    override fun onHook() {
        // 检测是否启用CTS测试安装
        hasEnable(Pref.Key.NubiaPackageInstaller.CTS_TEST_INSTALLER){
            YLog.debug("[UseCtsActivity] 开始 Hook CTS Activity 功能")

            val targetClass = "com.android.packageinstaller.InstallStart".toClassOrNull()

            if (targetClass == null) {
                YLog.error("[UseCtsActivity] 无法加载 InstallStart 类")
                return@hasEnable
            }

            YLog.debug("[UseCtsActivity] 成功加载 InstallStart 类")

            // 使用 method 查找方法，指定参数类型
            val targetMethod = targetClass.method {
                name = "getCallingPackageNameForUid"
                paramCount = 1
                param(IntType)
            }.ignored()  // 使用 ignored() 以防找不到方法

            targetMethod.hook {
                before {
                    YLog.debug("[UseCtsActivity] getCallingPackageNameForUid 方法被调用，参数: uid=${this.args(0).int()}")

                    // 设置固定返回值
//                    this.result = "u9521.cts"
                    this.result = "zuji.cts"

                    YLog.debug("[UseCtsActivity] 已修改返回值为: zuji.cts")
                }

                after {
                    YLog.debug("[UseCtsActivity] getCallingPackageNameForUid 方法执行完成，返回值: ${this.result}")
                }
            }

            YLog.debug("[UseCtsActivity] CTS Activity Hook 设置完成")
        }

    }
}