package dev.lackluster.redmagichelper.hook.rules.systemui.qs.nubia



import android.annotation.SuppressLint
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.factory.current
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.factory.getResID
import dev.lackluster.redmagichelper.utils.factory.hasEnable

object QSHeaderShowControl : YukiBaseHooker() {
    private const val TAG = "QSHeaderShowControl"
    // 配置项：快速设置显示运营商
    private val qs_show_carrier get() =
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.QS_SHOW_CARRIER, false)
    // 配置项：快速设置显示搜索按钮
    private val qs_show_search get() =
        Prefs.getBoolean(Pref.Key.SystemUI.StatusBar.QS_SHOW_SEARCH, false)

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


