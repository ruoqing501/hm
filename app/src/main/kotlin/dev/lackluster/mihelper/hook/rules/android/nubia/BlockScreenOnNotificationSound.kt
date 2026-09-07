package dev.lackluster.mihelper.hook.rules.android.nubia

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import dev.lackluster.mihelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.mihelper.hook.compat.factory.field
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.utils.factory.hasEnable

object BlockScreenOnNotificationSound : YukiBaseHooker() {

    @SuppressLint("PrivateApi")
    override fun onHook() {
        hasEnable(Pref.Key.Android.DISABLE_SOUND_WHEN_UNLOCKED) {
            YLog.debug("[BlockScreenOnNotificationSound] 开始Hook亮屏时屏蔽通知声音功能")

            // 获取NotificationAttentionHelper类
            val notificationAttentionHelperClass = "com.android.server.notification.NotificationAttentionHelper".toClassOrNull()
            if (notificationAttentionHelperClass == null) {
                YLog.error("[BlockScreenOnNotificationSound] NotificationAttentionHelper类未找到")
                return@hasEnable
            }

            // 获取NotificationRecord类
            val notificationRecordClass = "com.android.server.notification.NotificationRecord".toClassOrNull()
            if (notificationRecordClass == null) {
                YLog.error("[BlockScreenOnNotificationSound] NotificationRecord类未找到")
                return@hasEnable
            }

            // 获取Signals内部类
            val signalsClass = "com.android.server.notification.NotificationAttentionHelper\$Signals".toClassOrNull()
            if (signalsClass == null) {
                YLog.error("[BlockScreenOnNotificationSound] Signals内部类未找到")
                return@hasEnable
            }

            // Hook buzzBeepBlinkLocked 方法（正确的方法位置和签名）
            YLog.debug("[BlockScreenOnNotificationSound] 准备Hook buzzBeepBlinkLocked 方法")

            notificationAttentionHelperClass.method {
                name = "buzzBeepBlinkLocked"
                param(notificationRecordClass, signalsClass)
            }.hook {
                before {
                    YLog.debug("[BlockScreenOnNotificationSound] 进入buzzBeepBlinkLocked方法")

                    // 获取mAudioManager字段（现在是从NotificationAttentionHelper实例）
                    val audioManagerField = notificationAttentionHelperClass.field {
                        name = "mAudioManager"
                    }.get(this.instance).any() as? AudioManager

                    // 获取mScreenOn字段
                    val screenOnField = notificationAttentionHelperClass.field {
                        name = "mScreenOn"
                    }.get(this.instance).boolean()

                    // 获取Context
                    val contextField = notificationAttentionHelperClass.field {
                        name = "mContext"
                    }.get(this.instance).any() as? Context

                    if (contextField == null) {
                        YLog.error("[BlockScreenOnNotificationSound] 无法获取Context")
                        return@before
                    }

                    // 获取KeyguardManager
                    val keyguardManager = contextField.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                    // 检查条件：屏幕亮着且未锁屏
                    if (screenOnField && !keyguardManager.isKeyguardLocked) {
                        YLog.debug("[BlockScreenOnNotificationSound] 屏幕亮着且未锁屏，临时移除mAudioManager")

                        // 临时保存原始的AudioManager
                        val originalAudioManager = audioManagerField
                        this.args[0] = originalAudioManager // 使用args(0)保存原始AudioManager

                        // 将mAudioManager设置为null
                        notificationAttentionHelperClass.field {
                            name = "mAudioManager"
                        }.get(this.instance).set(null)
                    }
                }

                after {
                    YLog.debug("[BlockScreenOnNotificationSound] buzzBeepBlinkLocked方法执行完毕")

                    // 恢复原始的AudioManager
                    val originalAudioManager = this.args(0).any() as? AudioManager
                    if (originalAudioManager != null) {
                        YLog.debug("[BlockScreenOnNotificationSound] 恢复原始的mAudioManager")
                        notificationAttentionHelperClass.field {
                            name = "mAudioManager"
                        }.get(this.instance).set(originalAudioManager)
                    }
                }
            }

            // Hook clearSoundLocked 方法（正确的方法位置）
            YLog.debug("[BlockScreenOnNotificationSound] 准备Hook clearSoundLocked 方法")

            notificationAttentionHelperClass.method {
                name = "clearSoundLocked"
            }.hook {
                before {
                    YLog.debug("[BlockScreenOnNotificationSound] 进入clearSoundLocked方法")

                    // 获取当前的mAudioManager
                    val audioManagerField = notificationAttentionHelperClass.field {
                        name = "mAudioManager"
                    }.get(this.instance).any() as? AudioManager

                    // 如果mAudioManager为null，则恢复它
                    if (audioManagerField == null) {
                        YLog.debug("[BlockScreenOnNotificationSound] mAudioManager为null，正在恢复")

                        // 通过mContext字段获取Context
                        val contextField = notificationAttentionHelperClass.field {
                            name = "mContext"
                        }.get(this.instance).any() as? Context

                        if (contextField != null) {
                            // 获取新的AudioManager
                            val audioManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                contextField.getSystemService(AudioManager::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                contextField.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            }

                            // 恢复mAudioManager
                            notificationAttentionHelperClass.field {
                                name = "mAudioManager"
                            }.get(this.instance).set(audioManager)
                        }
                    }
                }
            }

            YLog.debug("[BlockScreenOnNotificationSound] Hook完成")
        } ?: YLog.debug("[BlockScreenOnNotificationSound] 总开关未启用，跳过hook")
    }
}