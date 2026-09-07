package dev.lackluster.mihelper.hook.rules.doubleApp


import android.content.Context
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

//去除低内存设备两个双开应用限制
object RmLowMemoryLimit: YukiBaseHooker(){
    private const val TAG = "NubiaDoubleApp"

    override fun onHook() {
        hasEnable(Pref.Key.Other.RM_LOW_MEMORY_LIMIT){
          "com.zte.cn.doubleapp.common.Utils".toClass().method {
              name = "showLimitedApps"
              param(Context::class.java)
          }.hook(){
              before {
                  result =false
                  YLog.debug(tag = TAG, msg = "showLimitedApps$：${result}")
              }
          }
        }
    }
}