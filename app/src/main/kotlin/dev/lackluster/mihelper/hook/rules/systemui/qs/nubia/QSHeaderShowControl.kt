package dev.lackluster.mihelper.hook.rules.systemui.qs.nubia



import android.annotation.SuppressLint
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import dev.lackluster.mihelper.utils.factory.getResID
import dev.lackluster.mihelper.utils.factory.hasEnable

object QSHeaderShowControl : YukiBaseHooker() {
    private const val TAG = "QSHeaderShowControl"
    // 配置项：快速设置显示运营商
    private val qs_show_carrier by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.QS_SHOW_CARRIER, false)
    }
    // 配置项：快速设置显示搜索按钮
    private val qs_show_search by lazy {
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.QS_SHOW_SEARCH, false)
    }

    override fun onHook() {
      val ccHeaderClazz =  "com.zte.controlcenter.widget.CCHeaderView".toClassOrNull()
         ccHeaderClazz?.method {
            name = "shouldQsCarrierVisible"
        }?.hook{
          before {
              result =qs_show_carrier
          }
        }
        ccHeaderClazz?.method {
            name = "showSearchButton"
        }?.hook{
            before {
                result =qs_show_search
            }
        }
    }
}


