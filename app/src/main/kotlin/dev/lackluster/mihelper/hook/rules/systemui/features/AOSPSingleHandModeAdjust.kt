package dev.lackluster.mihelper.hook.rules.systemui.features


import android.view.WindowManager
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.getResID
import dev.lackluster.mihelper.utils.factory.hasEnable
import kotlin.compareTo
import kotlin.math.ceil

object AOSPSingleHandModeAdjust : YukiBaseHooker() {
    private const val TAG = "AOSPSingleHandModeAdjust"
    private val aosp_singlehandmode_adjust by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.AOSP_SINGLEHANDMODE_ADJUST, false)
    }
    private val upOffsetPx by lazy {
        Prefs.getInt(Pref.Key.SystemUI.StatusBar.INPUT_INT, 0)
    }

    override fun onHook() {
        hasEnable(Pref.Key.SystemUI.StatusBar.AOSP_SINGLEHANDMODE_ADJUST) {
            YLog.debug("$TAG 功能开启已开启")
            val oHDAOClazz =
                "com.android.wm.shell.onehanded.OneHandedDisplayAreaOrganizer".toClassOrNull()
            val oHTHClazz =
                "com.android.wm.shell.onehanded.OneHandedTutorialHandler".toClassOrNull()
            oHDAOClazz?.method {
                name = "scheduleOffset"
                param(IntType, IntType)
            }?.hook {
                before {
                    val rollDownPx = args[1] as Int
                    val finOffset = rollDownPx - upOffsetPx
                    if (rollDownPx == 0) {
                        YLog.debug("$TAG rollDownPx is Zero")
                        return@before
                    }
                    //up overflow , down overflow 0.4 is actually 39.999996%,quit signlehand
                    if (finOffset <= 0 || finOffset > ceil(rollDownPx / 0.4)) {
                        YLog.debug("$TAG singleHand screen overflow!! HeightOffset:$finOffset")
                        return@before
                    }
                    //raise some px on onehand mode also can bypass statusbar restrict,dont know why works
                    //this is a system bug ,fixed at RMOS 9.5.25
                    args[1] = rollDownPx - upOffsetPx
                    YLog.debug("$TAG singleHand screen adjust!! HeightOffset:$finOffset")

                }
            }
            oHTHClazz?.method {
                name = "getTutorialTargetLayoutParams"

            }?.hook {
                after {
                    val tTlp = result as WindowManager.LayoutParams
                    tTlp.apply {
                        if (height - upOffsetPx <= 0 || (height - upOffsetPx) > ceil(height / 0.4)) {
//                            YLog.debug("$TAG singleHand screen overflow!! HeightOffset:$height")
                            return@after
                        }
                        height -= upOffsetPx
                        YLog.debug("$TAG singleHand screen overflow!! HeightOffset:$height")
                    }
                }
            }
        }
    }
}


