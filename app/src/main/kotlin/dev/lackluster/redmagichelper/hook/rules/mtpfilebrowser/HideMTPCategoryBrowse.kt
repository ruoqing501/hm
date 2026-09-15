package dev.lackluster.redmagichelper.hook.rules.mtpfilebrowser


import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs

//隐藏MTP分类浏览
object HideMTPCategoryBrowse : YukiBaseHooker() {
    private const val TAG = "NubiaMtpFileBrowser"
    override fun onHook() {
        "cn.nubia.filebrowser.mtpserver.mtp.MtpDatabase".toClass().method {
            name = "addEmulatedStorage"
        }.hook() {
            before {
                if (!Prefs.getBoolean(Pref.Key.Other.HIDE_MTP_CATEGORY_BROWSE, false)) return@before
                result = null
                YLog.debug(tag = TAG, msg = "blocked MTP Category Browse")
            }
        }
    }
}
