package dev.lackluster.redmagichelper.hook.rules.mtpfilebrowser

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
//重命名路径浏览名称
object RenameRootName : YukiBaseHooker() {
    private const val TAG = "NubiaMtpFileBrowser"

    override fun onHook() {
        val nubiaMtpStorage = "cn.nubia.filebrowser.mtpserver.mtp.MtpStorage".toClass()

        val mDescription = nubiaMtpStorage.getDeclaredField("mDescription").apply { isAccessible=true }
        val mStorageId = nubiaMtpStorage.getDeclaredField("mStorageId").apply { isAccessible=true }

        "cn.nubia.filebrowser.mtpserver.mtp.MtpServer".toClass().method {
            name = "addStorage"
            param("cn.nubia.filebrowser.mtpserver.mtp.MtpStorage".toClass())
        }.hook {
            before {
                if (!Prefs.getBoolean(Pref.Key.Other.MTP_RENAME_ROOT_NAME_SWITCH, false)) return@before
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
