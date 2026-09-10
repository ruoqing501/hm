package dev.lackluster.redmagichelper.hook.apps


import android.annotation.SuppressLint
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.rules.desktop.AppIconCustomizationHook
import dev.lackluster.redmagichelper.hook.rules.desktop.CustomTextClockHook
import dev.lackluster.redmagichelper.hook.rules.desktop.RecentTasksHook


object DeskTop : YukiBaseHooker() {
    override fun onHook() {
       loadHooker(CustomTextClockHook)
        //loadHooker(TaskViewHook)
        loadHooker(RecentTasksHook)
        loadHooker(AppIconCustomizationHook)

    }
}
