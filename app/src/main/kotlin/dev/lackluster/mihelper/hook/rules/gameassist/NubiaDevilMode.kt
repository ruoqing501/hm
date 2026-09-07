package dev.lackluster.mihelper.hook.rules.gameassist


import android.app.Dialog
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.highcapable.yukihookapi.hook.type.android.ViewClass
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.hook.rules.gamespace.NubiaGameSpace
import dev.lackluster.mihelper.utils.factory.hasEnable
import java.util.concurrent.atomic.AtomicBoolean

// 包名：com.zte.gameassist
object NubiaDevilMode : YukiBaseHooker() {

    private const val TAG = "NubiaDevilModes"

    override fun onHook() {

    }
}

