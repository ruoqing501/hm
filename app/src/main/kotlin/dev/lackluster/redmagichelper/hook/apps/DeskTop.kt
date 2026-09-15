package dev.lackluster.redmagichelper.hook.apps


import android.annotation.SuppressLint
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.desktop.RecentTasksHook


object DeskTop : YukiBaseHooker() {
    override fun onHook() {
        //loadHooker(TaskViewHook)
        loadHooker(RecentTasksHook)

    }
}
