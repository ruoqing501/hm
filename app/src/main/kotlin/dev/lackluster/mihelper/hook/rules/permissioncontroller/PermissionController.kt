package dev.lackluster.mihelper.hook.rules.permissioncontroller



import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import io.github.kyuubiran.ezxhelper.core.misc.params

object PermissionController: YukiBaseHooker() {
    private const val TAG = "NubiaPermissionController"
    override fun onHook() {
        // 允许使用第三方桌面启动器
        loadHooker(AllowThirdpartyLauncher)
    }
}