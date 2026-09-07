package dev.lackluster.mihelper.utils.nubia

import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import java.lang.reflect.Method

object Deoptimizer: YukiBaseHooker() {

    @JvmStatic
    var m: Method? = null

    @JvmStatic
    var deoptimizeMethod = m

    init {
        try {
            //noinspection JavaReflectionMemberAccess
            m = XposedBridge::class.java.getDeclaredMethod(
                "deoptimizeMethod", Member::class.java
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
        deoptimizeMethod = m
    }

    @Throws(InvocationTargetException::class, IllegalAccessException::class)
    fun deoptimizeMethod(tag: String,c: Class<*>, methodName: String) {
        for (m in c.declaredMethods) {
            if (deoptimizeMethod != null && m.name == methodName) {
                deoptimizeMethod!!.invoke(null, m)
//                Log.i("Method deoptimized: $m", logInRelease = true)
//                Log.i("deoptimized", "Method deoptimized: $m")
                YLog.debug("$tag  Method deoptimized: $m")
            }
        }
    }

    @Throws(InvocationTargetException::class, IllegalAccessException::class)
    fun deoptimizeMethods(tag: String,c: Class<*>, vararg methodName: String) {
        for (name in methodName) {
            deoptimizeMethod(tag,c, name)
        }
    }

    @Throws(InvocationTargetException::class, IllegalAccessException::class)
    fun deoptimizeAllMethods(tag: String,c: Class<*>,) {
        val meArray = arrayOf<String>()
        for (m in c.declaredMethods) {
            meArray.plus(m.name)
        }
        deoptimizeMethods(tag,c, *meArray)
    }

    override fun onHook() {
        TODO("Not yet implemented")
    }

}