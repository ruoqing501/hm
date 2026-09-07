package dev.lackluster.mihelper.hook.rules.android.nubia


import android.os.PowerManager
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.hasEnable

object BlockTelemetryService : YukiBaseHooker() {
    private const val TAG = "BlockTelemetryService"


    override fun onHook() {
        hasEnable(Pref.Key.Android.ANDROID_BLOCK_TELEMETRY_SERVICE) {
            YLog.debug("$TAG enabled")
            val gameZteTMClazz =
                "com.android.server.redmagic.trackclient.ZteTrackManager".toClassOrNull()
            if (gameZteTMClazz == null) {
                YLog.error("$TAG ZteTrackManager not found")
                return@hasEnable
            }
            val zteTMClazz = "com.android.server.datacollection.ZteTrackManager".toClassOrNull()
            if (zteTMClazz == null) {
                YLog.error("$TAG }ZteTrackManager not found")
                return@hasEnable
            }
             gameZteTMClazz.method {
                name = "init"
                param(ContextClass)
            }.hook{
                before {
                    result = null
                }
                 YLog.debug("$TAG ZteTrackManager init called")
            }
            zteTMClazz.method {
                name = "init"
                param(ContextClass)
            }.hook{
                before {
                    result = null
                }
                YLog.debug("$TAG ZteTrackManager init called")
            }

        }
    }
}