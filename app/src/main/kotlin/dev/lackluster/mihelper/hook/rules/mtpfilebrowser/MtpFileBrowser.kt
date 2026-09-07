package dev.lackluster.mihelper.hook.rules.mtpfilebrowser

import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker

// MTP浏览
object MtpFileBrowser: YukiBaseHooker(){
    override fun onHook() {
        //隐藏MTP分类浏览
        loadHooker(HideMTPCategoryBrowse)
        //重命名路径浏览名称
        loadHooker(RenameRootName)
    }
}