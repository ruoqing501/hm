package dev.lackluster.redmagichelper.hook.rules.doubleApp

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker

object DoubleApp: YukiBaseHooker(){
    override fun onHook() {
        //去除低内存设备两个双开应用限制
        loadHooker(RmLowMemoryLimit)
        // 双开任意应用
        loadHooker(DoubleAnyApp)
    }
}