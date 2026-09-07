package dev.lackluster.mihelper.hook.rules.mtpfilebrowser


import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

//隐藏MTP分类浏览
object HideMTPCategoryBrowse : YukiBaseHooker() {
    private const val TAG = "NubiaMtpFileBrowser"
    override fun onHook() = hasEnable(Pref.Key.Other.HIDE_MTP_CATEGORY_BROWSE) {
        "cn.nubia.filebrowser.mtpserver.mtp.MtpDatabase".toClass().method {
            name = "addEmulatedStorage"
        }.hook() {
            before {
                result = null
                YLog.debug(tag = TAG, msg = "blocked MTP Category Browse")
            }
        }
    }
}