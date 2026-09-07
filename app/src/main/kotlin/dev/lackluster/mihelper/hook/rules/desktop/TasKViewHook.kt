package dev.lackluster.mihelper.hook.rules.desktop

import android.annotation.SuppressLint
import android.widget.TextClock
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs
import java.text.SimpleDateFormat
import java.util.*

object TaskViewHook : YukiBaseHooker() {
    private const val TAG = "TaskViewHook"
    // 如果值为0 则显示标准模式，否则为宫格模式
    private val showMode = 0
    override fun onHook() {
    }
}