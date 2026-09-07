package dev.lackluster.mihelper.hook.rules.permissioncontroller



import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object PermissionController: YukiBaseHooker() {
    private const val TAG = "NubiaPermissionController"
    override fun onHook() {
        // 允许使用第三方桌面启动器
        loadHooker(AllowThirdpartyLauncher)
    }
}