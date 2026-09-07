package dev.lackluster.mihelper.hook.rules.android


import com.highcapable.kavaref.KavaRef
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

// 禁用温控
object AntiQues : YukiBaseHooker() {
    private const val TAG = "AntiQues"
    override fun onHook() {
        hasEnable(Pref.Key.Android.SYSTEM_SETTINGS_ANTI_QUES){
            // Hook SystemServiceManager.loadClassFromLoader 方法
            "com.android.server.SystemServiceManager".toClass().resolve().apply {
                firstMethod {
                    name = "loadClassFromLoader"
                    parameters(String::class.java, ClassLoader::class.java)
                }.hook {
                    before {
                        val clzName = args[0] as String?
                        val classLoader = args[1] as ClassLoader?
                        //YLog.debug("$TAG 🔍 搜索目标类：$clzName")
                        //YLog.debug("$TAG 🔍 Jar 包路径：${classLoader}")
                        // 当加载 WifiService 时
                        // /apex/com.android.wifi/javalib/service-wifi.jar
                        if (clzName == "com.android.server.wifi.WifiService") {
                            // 使用动态获取的 ClassLoader 来 Hook Utils.checkDeviceNameIsIllegalSync
                            classLoader?.let { cl ->
                                "com.android.server.wifi.Utils".toClass(cl).resolve().apply {
                                    firstMethod {
                                        name = "checkDeviceNameIsIllegalSync"
                                        parameters(android.content.Context::class.java, Int::class.java, String::class.java)
                                    }.hook {
                                        before {
                                            result =false
                                            YLog.debug(tag = TAG, msg = "WifiService.checkDeviceNameIsIllegalSync hook success")
                                        }
                                    }
                                }
                            }
                            // 取消本次 hook，避免重复执行
                            removeSelf()
                        }
                    }
                }
            }
        }
    }
}
