package dev.lackluster.mihelper.hook.rules.screenshot

import android.content.Context
import android.provider.Settings
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable
import de.robv.android.xposed.XposedHelpers

object ScreenRefreshRateHook : YukiBaseHooker() {
    private const val TAG = "ScreenRefreshRateHook"
    private const val FLAG_SELECTED_165 = "mihelper_selected_165" // 用于记录用户是否刚刚选择了165Hz

    override fun onHook() {
        hasEnable(Pref.Key.NubiaSystemSettings.UNLOCK_165HZ) {
            // 1. 让系统认为设备支持165Hz（修改工具类返回值）
            "com.zte.settings.utils.ZteFeatureUtils".toClassOrNull()?.method {
                name = "getCustomRefreshRate"
                paramCount = 0
            }?.hook {
                after {
                    val original = result as? String ?: return@after
                    if (original == "NA") return@after
                    val list = original.split(",").map { it.trim() }.toMutableList()
                    if ("165" !in list) {
                        list.add("165")
                        result = list.joinToString(",")
                        YLog.debug(tag = TAG, msg = "Added 165Hz to custom refresh rate: $result")
                    }
                }
            }

            // 修改获取支持刷新率列表的方法，增加165Hz
            "com.zte.settings.display.screerefresh.ScreenUtils".toClassOrNull()?.method {
                name = "getSupportedRSS"
                paramCount = 1
            }?.hook {
                after {
                    val original = result as? IntArray ?: return@after
                    if (original.contains(165)) return@after
                    val newArray = original + 165
                    result = newArray
                    YLog.debug(tag = TAG, msg = "Added 165Hz to supported refresh rates: ${newArray.joinToString()}")
                }
            }

            // 2. Hook 点击事件和 UI 更新
            val fragmentClass = "com.zte.settings.display.screerefresh.ScreenRefreshRateSettingsFragment".toClassOrNull()
            fragmentClass?.apply {
                // 2.1 处理点击165Hz选项
                method {
                    name = "onRadioButtonClicked"
                    paramCount = 1
                    param("com.android.settings.widget.RadioButtonPreference".toClass())
                }?.hook {
                    before {
                        val emitter = args[0] as Any
                        val key = emitter.javaClass.getMethod("getKey").invoke(emitter) as? String
                        val fragment = instance
                        if (key == "rate_165") {
                            // 记录用户点击了165Hz
                            XposedHelpers.setAdditionalInstanceField(fragment, FLAG_SELECTED_165, true)

                            // 修改写入的设置：将实际刷新率模式改为144Hz对应的值（4）
                            val context = fragment.javaClass.getMethod("getContext").invoke(fragment) as? Context ?: return@before
                            val resolver = context.contentResolver
                            Settings.System.putInt(resolver, "refresh_rate_mode", 4) // 144Hz mode = 4
                            YLog.debug(tag = TAG, msg = "User clicked 165Hz, actual set to 144Hz (mode=4)")

                            // 阻止原方法继续执行，避免重复写入和触发updateUI
                            //result = null
                        } else {
                            // 点击其他选项时清除标志
                            XposedHelpers.setAdditionalInstanceField(fragment, FLAG_SELECTED_165, false)
                        }
                    }
                }

                // 2.2 在updateUI后强制选中165Hz（如果标志存在）
                method {
                    name = "updateUI"
                    paramCount = 0
                }?.hook {
                    after {
                        val fragment = instance
                        val flag = XposedHelpers.getAdditionalInstanceField(fragment, FLAG_SELECTED_165) as? Boolean ?: false
                        if (flag) {
                            // 获取165Hz和144Hz选项的引用
                            val rate165Pref = try {
                                fragment.javaClass.getDeclaredField("mRate165Preference").apply { isAccessible = true }.get(fragment) as? Any
                            } catch (e: Exception) { null }
                            val rate144Pref = try {
                                fragment.javaClass.getDeclaredField("mRate144Preference").apply { isAccessible = true }.get(fragment) as? Any
                            } catch (e: Exception) { null }

                            // 强制选中165Hz，取消144Hz选中
                            rate165Pref?.let {
                                val setCheckedMethod = it.javaClass.getMethod("setChecked", Boolean::class.javaPrimitiveType)
                                setCheckedMethod.invoke(it, true)
                            }
                            rate144Pref?.let {
                                val setCheckedMethod = it.javaClass.getMethod("setChecked", Boolean::class.javaPrimitiveType)
                                setCheckedMethod.invoke(it, false)
                            }

                            // 清除标志，避免后续刷新时再次覆盖
                            XposedHelpers.setAdditionalInstanceField(fragment, FLAG_SELECTED_165, false)
                            YLog.debug(tag = TAG, msg = "Forced UI to select 165Hz")
                        }
                    }
                }
            }
        }
    }
}