package dev.lackluster.redmagichelper.hook.rules.android.nubia


import android.os.PowerManager
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.android.ContextClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.hasEnable

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