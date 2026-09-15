package dev.lackluster.redmagichelper.hook.rules.screenshot


import java.util.concurrent.atomic.AtomicBoolean

object ScreenCaptureState {
    val isRecording = AtomicBoolean(false)
}
