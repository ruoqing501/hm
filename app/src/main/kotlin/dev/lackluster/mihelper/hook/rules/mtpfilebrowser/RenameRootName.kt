package dev.lackluster.mihelper.hook.rules.mtpfilebrowser

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.hasEnable
//重命名路径浏览名称
object RenameRootName : YukiBaseHooker() {
    private const val TAG = "NubiaMtpFileBrowser"

    override fun onHook() = hasEnable(Pref.Key.Other.MTP_RENAME_ROOT_NAME_SWITCH) {
        val nubiaMtpStorage = "cn.nubia.filebrowser.mtpserver.mtp.MtpStorage".toClass()

        val mDescription = nubiaMtpStorage.getDeclaredField("mDescription").apply { isAccessible=true }
        val mStorageId = nubiaMtpStorage.getDeclaredField("mStorageId").apply { isAccessible=true }

        "cn.nubia.filebrowser.mtpserver.mtp.MtpServer".toClass().method {
            name = "addStorage"
            param("cn.nubia.filebrowser.mtpserver.mtp.MtpStorage".toClass())
        }.hook {
            before {
                val volume = nubiaMtpStorage.cast(args[0])

                if (mStorageId.getInt(volume) > 65537) {
                    return@before
                }
                val description = Prefs.getString(Pref.Key.Other.MTP_RENAME_ROOT_NAME, "内部存储设备") ?: "内部存储设备"
                mDescription.set(volume, description)
                YLog.debug(tag = TAG, msg = "MtpStorage: $description")
            }
        }
    }
}
