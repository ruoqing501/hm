package dev.lackluster.redmagichelper.hook.rules.permissioncontroller



import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.factory.hasEnable

object PermissionController: YukiBaseHooker() {
    private const val TAG = "NubiaPermissionController"
    override fun onHook() {
        // 允许使用第三方桌面启动器
        loadHooker(AllowThirdpartyLauncher)
    }
}