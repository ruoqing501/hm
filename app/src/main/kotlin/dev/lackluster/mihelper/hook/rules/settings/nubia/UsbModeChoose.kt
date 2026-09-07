package dev.lackluster.mihelper.hook.rules.settings.nubia

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.constructor
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.Prefs

object UsbModeChoose : YukiBaseHooker() {
    private const val TAG = "UsbModeChoose"

    // 接入 USB时不弹窗
    private val system_settings_usb_mode by lazy {
        Prefs.getBoolean(Pref.Key.NubiaSystemSettings.SYSTEM_SETTINGS_USB_MODE, false)
    }

    // USB默认选项（UI选择的值）
    private val system_settings_usb_mode_choose by lazy {
        Prefs.getInt(Pref.Key.NubiaSystemSettings.SYSTEM_SETTINGS_USB_MODE_CHOOSE, 0)
    }

    // 模式名称映射（仅用于日志）
    private val modeNames = mapOf(
        0 to "仅充电",
        1 to "多屏协同",
        2 to "传输文件",
        3 to "图片传输",
        4 to "多屏投屏",
    )

    override fun onHook() {
        // 读取用户设置
        val uiMode = system_settings_usb_mode_choose
        val skipShowWindow = system_settings_usb_mode

        // 将UI模式转换为内部实际模式值
        val internalMode = when (uiMode) {
            1 -> 0   // 仅限充电 → 仅充电
            2 -> 2   // 传输文件 → 传输文件
            3 -> 3   // 传输照片 → 图片传输
            4 -> 4   // 多屏投屏 → 多屏投屏
            else -> -1  // 默认或无效值，表示不干预
        }

        YLog.info(tag = TAG, msg = "uiMode=$uiMode, internalMode=$internalMode (${modeNames[internalMode]}), skipShowWindow=$skipShowWindow")

        // 记录模式设置（仅用于日志）
        "com.zte.settings.connecteddevice.UsbModeUtils".toClass().method {
            name = "setCurrentMode"
            paramCount = 2
        }.hook {
            before {
                val mode = args[1] as? Int
                YLog.info(tag = TAG, msg = "setCurrentMode called with mode=$mode (${modeNames[mode]})")
            }
            after {
                val mode = args[1] as? Int
                YLog.info(tag = TAG, msg = "setCurrentMode completed, mode=$mode")
            }
        }

        // 核心逻辑：拦截UsbModeChooserActivity
        "com.zte.settings.connecteddevice.UsbModeChooserActivity".toClass().method {
            name = "onCreate"
            param(Bundle::class.java)
        }.hook {
            after {
                val activity = instance as? Activity ?: return@after
                YLog.info(tag = TAG, msg = "UsbModeChooserActivity created")

                // 读取新开关：通知栏点击时总是显示弹窗
                val notificationAlwaysShow = Prefs.getBoolean(
                    Pref.Key.NubiaSystemSettings.SYSTEM_SETTINGS_USB_MODE_NOTIFICATION_SHOW,
                    false
                )

                // 判断是否从通知栏启动
                fun isFromNotification(): Boolean {
                    // getLaunchedFromPackage 需要 API 30+ (红魔11满足)
                    activity.getLaunchedFromPackage()?.let {
                        if (it == "com.android.systemui") return true
                    }
                    // 备选：referrer
                    activity.referrer?.let { uri ->
                        if (uri.scheme == "android-app" && uri.host == "com.android.systemui") return true
                    }
                    return false
                }

                if (skipShowWindow) {
                    val fromNotification = isFromNotification()
                    // 如果来自通知栏且用户要求总是显示，则正常显示弹窗
                    if (fromNotification && notificationAlwaysShow) {
                        YLog.info(tag = TAG, msg = "Notification launch and notificationAlwaysShow=true, showing dialog normally.")
                        return@after // 让 Activity 继续执行，不执行跳过逻辑
                    }

                    // 否则执行原有的跳过逻辑
                    try {
                        // 1. 取消延迟显示对话框的任务
                        val handlerField = activity.javaClass.getDeclaredField("mWeakHandler")
                        handlerField.isAccessible = true
                        val handler = handlerField.get(activity) as? Handler
                        handler?.removeCallbacksAndMessages(null)
                        YLog.info(tag = TAG, msg = "Cancelled dialog display task")
                    } catch (e: Exception) {
                        YLog.error(tag = TAG, msg = "Failed to cancel handler", e = e)
                    }

                    // 2. 如果用户选择了具体模式（非默认），则自动设置该模式
                    if (internalMode != -1) {
                        try {
                            val usbManager = activity.getSystemService("usb")
                            "com.zte.settings.connecteddevice.UsbModeUtils".toClass().method {
                                name = "setCurrentMode"
                                paramCount = 2
                            }.get().call(usbManager, internalMode)
                            YLog.info(tag = TAG, msg = "Set USB mode to $internalMode (${modeNames[internalMode]})")
                        } catch (e: Exception) {
                            YLog.error(tag = TAG, msg = "Failed to set USB mode", e = e)
                        }
                    } else {
                        YLog.info(tag = TAG, msg = "User selected default mode, not setting any specific mode")
                    }

                    // 3. 关闭Activity，避免对话框显示
                    activity.finish()
                    YLog.info(tag = TAG, msg = "Activity finished, dialog never shown")
                } else {
                    // 有弹窗的逻辑：不关闭弹窗，直接如果不为默认，使用指定模式
                    YLog.info(tag = TAG, msg = "Skip window disabled, dialog will show normally")
                    if (internalMode != -1) {
                        try {
                            val usbManager = activity.getSystemService("usb")
                            "com.zte.settings.connecteddevice.UsbModeUtils".toClass().method {
                                name = "setCurrentMode"
                                paramCount = 2
                            }.get().call(usbManager, internalMode)
                            YLog.info(tag = TAG, msg = "Set USB mode to $internalMode (${modeNames[internalMode]})")
                        } catch (e: Exception) {
                            YLog.error(tag = TAG, msg = "Failed to set USB mode", e = e)
                        }
                    } else {
                        YLog.info(tag = TAG, msg = "User selected default mode, not setting any specific mode")
                    }
                }
            }
        }

        // 可选：记录用户手动点击（保留原日志钩子）
        "com.zte.settings.connecteddevice.UsbModeChooserActivity\$5".toClass().apply {
            constructor {
                paramCount = 3
            }.ignored().give()?.hook {
                after {
                    val modeField = this.instance.javaClass.declaredFields.find { it.name.contains("val\$mode") }
                    modeField?.let {
                        it.isAccessible = true
                        val selectedMode = it.get(this.instance) as? Int
                        YLog.info(tag = TAG, msg = "User manually selected mode=$selectedMode (${modeNames[selectedMode]})")
                    }
                }
            }
        }
    }

    // 启动车载模式 Activity（原代码保留，未修改）
    private fun startCarActivity(activity: Activity) {
        try {
            val intent = Intent().apply {
                setClassName(
                    "com.zte.carmateservice",
                    "com.zte.carmateservice.ui.UsbAttachTipsActivity"
                )
            }
            activity.startActivity(intent)
            YLog.info(tag = TAG, msg = "Started car activity")
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Failed to start car activity", e = e)
        }
    }

    // 启动投屏模式 Activity（原代码保留，未修改）
    private fun startUsbCastActivity(activity: Activity) {
        try {
            val isMultScreen = "com.zte.settings.connecteddevice.UsbModeUtils".toClass().method {
                name = "isSupportMultScreenVersion"
                paramCount = 1
            }.get().call(activity) as? Boolean ?: false

            val intent = Intent().apply {
                if (isMultScreen) {
                    setClassName(
                        "com.zte.multscr",
                        "com.zte.multscr.NewMultiScreenActivity"
                    )
                } else {
                    setClassName(
                        "cn.nubia.touping",
                        "cn.nubia.touping.USBHelpTouPingActivity"
                    )
                }
            }
            activity.startActivity(intent)
            YLog.info(tag = TAG, msg = "Started USB cast activity")
        } catch (e: Exception) {
            YLog.error(tag = TAG, msg = "Failed to start USB cast activity", e = e)
        }
    }
}