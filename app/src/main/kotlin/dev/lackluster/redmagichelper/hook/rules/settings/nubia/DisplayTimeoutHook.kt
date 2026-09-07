package dev.lackluster.redmagichelper.hook.rules.settings.nubia

import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.factory.hasEnable
import java.util.ArrayList
// 解锁屏幕超时选项（30分钟，从不）
object DisplayTimeoutHook : YukiBaseHooker() {
    private const val TAG = "DisplayTimeoutHook"

    override fun onHook() {
        hasEnable(Pref.Key.NubiaSystemSettings.AUTOMATIC_SCREEN_OFF){
            // 方法一：尝试将限制标志置为 false
            try {
                "com.zte.settings.utils.ZteProjectUtils".toClass().apply {
                    field {
                        name = "CHANGE_SCREEN_TOMEOUT_TO_TEN"
                        type = BooleanType
                    }.get().set(false)
                    YLog.info(tag = TAG, msg = "CHANGE_SCREEN_TOMEOUT_TO_TEN set to false")
                }
            } catch (e: Exception) {
                YLog.error(tag = TAG, msg = "Failed to modify CHANGE_SCREEN_TOMEOUT_TO_TEN", e = e)
            }
            //
            //// 方法二：直接干预 getCandidates() 返回值，确保所有选项出现（兜底）
            //"com.android.settings.display.ScreenTimeoutSettings".toClass().apply {
            //    method {
            //        name = "getCandidates"
            //        paramCount = 0
            //    }.hook {
            //        after {
            //            val original = result as? List<*> ?: return@after
            //            if (original.size >= 8) return@after // 已经是完整列表
            //
            //            val instance = this.instance
            //            // 获取原始选项数组
            //            val entriesField = instance.javaClass.getDeclaredField("mInitialEntries")
            //            entriesField.isAccessible = true
            //            val entries = entriesField.get(instance) as? Array<CharSequence> ?: return@after
            //
            //            val valuesField = instance.javaClass.getDeclaredField("mInitialValues")
            //            valuesField.isAccessible = true
            //            val values = valuesField.get(instance) as? Array<CharSequence> ?: return@after
            //
            //            if (entries.size != values.size || entries.size < 2) return@after
            //
            //            // 取出被截断的最后两项（30分钟、从不）
            //            val extraLabel1 = entries[entries.size - 2]
            //            val extraValue1 = values[values.size - 2].toString()
            //            val extraLabel2 = entries[entries.size - 1]
            //            val extraValue2 = values[values.size - 1].toString()
            //
            //            // 构造 TimeoutCandidateInfo 对象
            //            val candidateClass = "com.android.settings.display.ScreenTimeoutSettings\$TimeoutCandidateInfo".toClass()
            //            val constructor = candidateClass.getDeclaredConstructor(
            //                CharSequence::class.java,
            //                String::class.java,
            //                Boolean::class.javaPrimitiveType
            //            )
            //            constructor.isAccessible = true
            //            val candidate1 = constructor.newInstance(extraLabel1, extraValue1, true)
            //            val candidate2 = constructor.newInstance(extraLabel2, extraValue2, true)
            //
            //            // 将缺失选项加入结果列表（直接创建新列表）
            //            val newList = ArrayList<Any>(original)
            //            newList.add(candidate1)
            //            newList.add(candidate2)
            //            result = newList
            //            YLog.info(tag = TAG, msg = "Missing timeout options added")
            //        }
            //    }
            //}
        }

    }
}