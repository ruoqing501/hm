package dev.lackluster.mihelper.hook.apps


import android.annotation.SuppressLint
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.rules.desktop.CustomTextClockHook
import dev.lackluster.mihelper.hook.rules.desktop.RecentTasksHook


object DeskTop : YukiBaseHooker() {
    override fun onHook() {
       loadHooker(CustomTextClockHook)
        //loadHooker(TaskViewHook)
        loadHooker(RecentTasksHook)

    }
}
