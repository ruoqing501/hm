package dev.lackluster.redmagichelper.hook.rules.android.nubia

import android.hardware.display.DisplayManager
import android.os.Build
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.field
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.factory.hasEnable

object DisableFlagSecureHooker : YukiBaseHooker() {

    override fun onHook() {
        YLog.debug("[DisableFlagSecureHooker:开始Hook禁用FLAG_SECURE功能]")

        // 总开关
        hasEnable(Pref.Key.Android.DISABLE_FLAG_SECURE_ENHANCED) {
            YLog.debug("[DisableFlagSecureHooker:总开关已启用]")

            // 1. hook WindowState.isSecureLocked
            val windowStateClass = "com.android.server.wm.WindowState".toClassOrNull()
            if (windowStateClass == null) {
                YLog.debug("[DisableFlagSecureHooker:WindowState类未找到，跳过hook isSecureLocked]")
            } else {
                YLog.debug("[DisableFlagSecureHooker:找到WindowState类，准备hook isSecureLocked方法]")
                windowStateClass.method {
                    name = "isSecureLocked"
                }.hook {
                    before {
                        YLog.debug("[DisableFlagSecureHooker:进入isSecureLocked方法hook]")

                        // 原代码中的逻辑
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            YLog.debug("[DisableFlagSecureHooker:Android U+版本，执行堆栈检查]")
                            val stackTrace = Throwable().stackTrace
                            var match = false
                            for (i in 2 until minOf(stackTrace.size, 8)) {
                                val methodName = stackTrace[i].methodName
                                if (methodName == "setInitialSurfaceControlProperties" || methodName == "createSurfaceLocked") {
                                    YLog.debug("[DisableFlagSecureHooker:匹配到阻止方法，跳过hook]")
                                    match = true
                                    break
                                }
                            }
                            if (match) {
                                return@before
                            }
                        } else {
                            YLog.debug("[DisableFlagSecureHooker:Android T及以下版本，执行堆栈检查]")
                            val stackTrace = Throwable().stackTrace
                            for (i in 4 until minOf(stackTrace.size, 8)) {
                                val methodName = stackTrace[i].methodName
                                if (methodName == "setInitialSurfaceControlProperties" || methodName == "createSurfaceLocked") {
                                    YLog.debug("[DisableFlagSecureHooker:匹配到阻止方法，跳过hook]")
                                    return@before
                                }
                            }
                        }
                        YLog.debug("[DisableFlagSecureHooker:堆栈检查通过，设置isSecureLocked返回false]")
                        result = false
                    }
                }
            }

            // 2. hook ScreenCapture相关方法
            // Android 16(Vanilla Ice Cream)及以上版本使用新的字段名和类结构
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                YLog.debug("[DisableFlagSecureHooker:Android V+版本，处理ScreenCaptureInternal]")
                // Android 16+ 使用 ScreenCaptureInternal
                hookScreenCaptureInternal()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                YLog.debug("[DisableFlagSecureHooker:Android U版本，处理ScreenCapture]")
                // Android 14-15 使用 ScreenCapture
                hookScreenCaptureU()
            } else {
                YLog.debug("[DisableFlagSecureHooker:Android T及以下版本，处理SurfaceControl]")
                // Android 13及以下使用 SurfaceControl
                hookScreenCaptureT()
            }

            // 3. hook DisplayControl.createDisplay（或createVirtualDisplay）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                YLog.debug("[DisableFlagSecureHooker:Android V+版本，尝试hook DisplayControl.createVirtualDisplay]")
                val displayControlClass = "com.android.server.display.DisplayControl".toClassOrNull()
                if (displayControlClass == null) {
                    YLog.debug("[DisableFlagSecureHooker:DisplayControl类未找到]")
                } else {
                    displayControlClass.method {
                        name = "createVirtualDisplay"
                    }.hook {
                        before {
                            YLog.debug("[DisableFlagSecureHooker:进入createVirtualDisplay方法hook]")
                            if (this.args.size > 1) {
                                this.args(1).setTrue()
                            }
                        }
                    }
                }
            } else {
                YLog.debug("[DisableFlagSecureHooker:Android V以下版本，尝试hook SurfaceControl.createDisplay]")
                val surfaceControlClass = "android.view.SurfaceControl".toClassOrNull()
                if (surfaceControlClass == null) {
                    YLog.debug("[DisableFlagSecureHooker:SurfaceControl类未找到]")
                } else {
                    surfaceControlClass.method {
                        name = "createDisplay"
                    }.hook {
                        before {
                            YLog.debug("[DisableFlagSecureHooker:进入createDisplay方法hook]")
                            if (this.args.size > 1) {
                                this.args(1).setTrue()
                            }
                        }
                    }
                }
            }

            // 4. hook VirtualDisplayAdapter.createVirtualDisplayLocked
            YLog.debug("[DisableFlagSecureHooker:尝试hook VirtualDisplayAdapter.createVirtualDisplayLocked]")
            val virtualDisplayAdapterClass = "com.android.server.display.VirtualDisplayAdapter".toClassOrNull()
            if (virtualDisplayAdapterClass == null) {
                YLog.debug("[DisableFlagSecureHooker:VirtualDisplayAdapter类未找到]")
            } else {
                virtualDisplayAdapterClass.method {
                    name = "createVirtualDisplayLocked"
                }.hook {
                    before {
                        YLog.debug("[DisableFlagSecureHooker:进入createVirtualDisplayLocked方法hook]")
                        if (this.args.size > 2) {
                            val caller = this.args(2).int()
                            if (caller >= 10000 && this.args(1).any() == null) {
                                return@before
                            }

                            for (i in 3 until this.args.size) {
                                val arg = this.args(i).any()
                                if (arg is Int) {
                                    val flags = arg or DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE
                                    this.args(i).set(flags)
                                    return@before
                                }
                            }
                        }
                    }
                }
            }

            // 5. hook ActivityTaskManagerService.registerScreenCaptureObserver（U以上）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                YLog.debug("[DisableFlagSecureHooker:Android U+版本，尝试hook ActivityTaskManagerService.registerScreenCaptureObserver]")
                val atmsClass = "com.android.server.wm.ActivityTaskManagerService".toClassOrNull()
                if (atmsClass == null) {
                    YLog.debug("[DisableFlagSecureHooker:ActivityTaskManagerService类未找到]")
                } else {
                    atmsClass.method {
                        name = "registerScreenCaptureObserver"
                    }.hook {
                        before {
                            result = null
                        }
                    }
                }
            }

            // 6. hook WindowManagerService.registerScreenRecordingCallback（V以上）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                YLog.debug("[DisableFlagSecureHooker:Android V+版本，尝试hook WindowManagerService.registerScreenRecordingCallback]")
                val wmsClass = "com.android.server.wm.WindowManagerService".toClassOrNull()
                if (wmsClass == null) {
                    YLog.debug("[DisableFlagSecureHooker:WindowManagerService类未找到]")
                } else {
                    wmsClass.method {
                        name = "registerScreenRecordingCallback"
                    }.hook {
                        before {
                            result = false
                        }
                    }
                }
            }

            // 7. hook ScreenshotHardwareBuffer.containsSecureLayers
            hookScreenshotHardwareBuffer()

            YLog.debug("[DisableFlagSecureHooker:所有hook点处理完成]")
        } ?: YLog.debug("[DisableFlagSecureHooker:总开关未启用，跳过所有hook]")

        YLog.debug("[DisableFlagSecureHooker:Hook过程结束]")
    }

    private fun hookScreenCaptureInternal() {
        // Android 16(Vanilla Ice Cream)及以上
        val screenCaptureClass = "android.window.ScreenCaptureInternal".toClassOrNull()
        if (screenCaptureClass == null) {
            YLog.debug("[DisableFlagSecureHooker:ScreenCaptureInternal类未找到]")
            return
        }

        val captureArgsClass = "android.window.ScreenCaptureInternal\$CaptureArgs".toClassOrNull()
        if (captureArgsClass == null) {
            YLog.debug("[DisableFlagSecureHooker:CaptureArgs类未找到]")
            return
        }

        // hook nativeCaptureDisplay
        screenCaptureClass.method {
            name = "nativeCaptureDisplay"
        }.hook {
            before {
                val captureArgs = this.args().first()
                captureArgs.let { argsObj ->
                    // Android 16+ 使用 mSecureContentPolicy 字段
                    val securePolicyField = captureArgsClass.field {
                        name = "mSecureContentPolicy"
                    }.get(argsObj)

                    if (securePolicyField != null) {
                        securePolicyField.set(1) // 1 表示允许捕获安全内容
                        YLog.debug("[DisableFlagSecureHooker:设置mSecureContentPolicy为1]")
                    }
                }
            }
        }

        // hook nativeCaptureLayers
        screenCaptureClass.method {
            name = "nativeCaptureLayers"
        }.hook {
            before {
                val captureArgs = this.args().first()
                captureArgs.let { argsObj ->
                    val securePolicyField = captureArgsClass.field {
                        name = "mSecureContentPolicy"
                    }.get(argsObj)

                    if (securePolicyField != null) {
                        securePolicyField.set(1)
                        YLog.debug("[DisableFlagSecureHooker:设置mSecureContentPolicy为1]")
                    }
                }
            }
        }
    }

    private fun hookScreenCaptureU() {
        // Android 14-15 (Upside Down Cake)
        val screenCaptureClass = "android.window.ScreenCapture".toClassOrNull()
        if (screenCaptureClass == null) {
            YLog.debug("[DisableFlagSecureHooker:ScreenCapture类未找到]")
            return
        }

        val captureArgsClass = "android.window.ScreenCapture\$CaptureArgs".toClassOrNull()
        if (captureArgsClass == null) {
            YLog.debug("[DisableFlagSecureHooker:CaptureArgs类未找到]")
            return
        }

        // 尝试多个可能的字段名
        val possibleFieldNames = arrayOf("mCaptureSecureLayers", "mSecureContentPolicy")

        // hook nativeCaptureDisplay
        screenCaptureClass.method {
            name = "nativeCaptureDisplay"
        }.hook {
            before {
                val captureArgs = this.args().first()
                captureArgs.let { argsObj ->
                    for (fieldName in possibleFieldNames) {
                        try {
                            val field = captureArgsClass.field {
                                name = fieldName
                            }.get(argsObj)

                            if (field != null) {
                                if (fieldName == "mSecureContentPolicy") {
                                    field.set(1)
                                } else {
                                    field.setTrue()
                                }
                                YLog.debug("[DisableFlagSecureHooker:设置${fieldName}成功]")
                                break
                            }
                        } catch (e: Exception) {
                            // 继续尝试下一个字段名
                        }
                    }
                }
            }
        }

        // hook nativeCaptureLayers
        screenCaptureClass.method {
            name = "nativeCaptureLayers"
        }.hook {
            before {
                val captureArgs = this.args().first()
                captureArgs.let { argsObj ->
                    for (fieldName in possibleFieldNames) {
                        try {
                            val field = captureArgsClass.field {
                                name = fieldName
                            }.get(argsObj)

                            if (field != null) {
                                if (fieldName == "mSecureContentPolicy") {
                                    field.set(1)
                                } else {
                                    field.setTrue()
                                }
                                YLog.debug("[DisableFlagSecureHooker:设置${fieldName}成功]")
                                break
                            }
                        } catch (e: Exception) {
                            // 继续尝试下一个字段名
                        }
                    }
                }
            }
        }
    }

    private fun hookScreenCaptureT() {
        // Android 13及以下
        val screenCaptureClass = "android.view.SurfaceControl".toClassOrNull()
        if (screenCaptureClass == null) {
            YLog.debug("[DisableFlagSecureHooker:SurfaceControl类未找到]")
            return
        }

        val captureArgsClass = "android.view.SurfaceControl\$CaptureArgs".toClassOrNull()
        if (captureArgsClass == null) {
            YLog.debug("[DisableFlagSecureHooker:CaptureArgs类未找到]")
            return
        }

        // hook nativeCaptureDisplay
        screenCaptureClass.method {
            name = "nativeCaptureDisplay"
        }.hook {
            before {
                val captureArgs = this.args().first()
                captureArgs.let { argsObj ->
                    val captureSecureLayersField = captureArgsClass.field {
                        name = "mCaptureSecureLayers"
                    }.get(argsObj)

                    captureSecureLayersField?.setTrue()

                    // 尝试设置 mAllowProtected
                    try {
                        val allowProtectedField = captureArgsClass.field {
                            name = "mAllowProtected"
                        }.get(argsObj)
                        allowProtectedField?.setTrue()
                    } catch (e: Exception) {
                        // 忽略，可能不存在此字段
                    }
                }
            }
        }

        // hook nativeCaptureLayers
        screenCaptureClass.method {
            name = "nativeCaptureLayers"
        }.hook {
            before {
                val captureArgs = this.args().first()
                captureArgs.let { argsObj ->
                    val captureSecureLayersField = captureArgsClass.field {
                        name = "mCaptureSecureLayers"
                    }.get(argsObj)

                    captureSecureLayersField.setTrue()

                    // 尝试设置 mAllowProtected
                    try {
                        val allowProtectedField = captureArgsClass.field {
                            name = "mAllowProtected"
                        }.get(argsObj)
                        allowProtectedField.setTrue()
                    } catch (e: Exception) {
                        // 忽略，可能不存在此字段
                    }
                }
            }
        }
    }

    private fun hookScreenshotHardwareBuffer() {
        val screenshotHardwareBufferClassName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            "android.window.ScreenCapture\$ScreenshotHardwareBuffer"
        } else {
            "android.view.SurfaceControl\$ScreenshotHardwareBuffer"
        }

        val screenshotHardwareBufferClass = screenshotHardwareBufferClassName.toClassOrNull()
        if (screenshotHardwareBufferClass == null) {
            YLog.debug("[DisableFlagSecureHooker:ScreenshotHardwareBuffer类未找到]")
            return
        }

        screenshotHardwareBufferClass.method {
            name = "containsSecureLayers"
        }.hook {
            before {
                result = false
            }
        }
    }
}