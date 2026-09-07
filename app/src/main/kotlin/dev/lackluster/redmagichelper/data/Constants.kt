//package dev.lackluster.redmagichelper.data
//
//object Constants {
//    const val CMD_LSPOSED = "am broadcast -a android.telephony.action.SECRET_CODE -d android_secret_code://5776733 android"
//    const val PER_MIUI_INTERNAL_API = "miui.permission.USE_INTERNAL_GENERAL_API"
//    const val ACTION_PREFIX = "hyperhelper.action."
//    const val ACTION_NOTIFICATIONS = ACTION_PREFIX + "SYSTEM_ACTION_NOTIFICATIONS"
//    const val ACTION_QUICK_SETTINGS = ACTION_PREFIX + "SYSTEM_ACTION_QUICK_SETTINGS"
//    const val ACTION_SCREENSHOT = "android.intent.action.CAPTURE_SCREENSHOT"
//    const val ACTION_HOME = ACTION_PREFIX + "SYSTEM_ACTION_HOME"
//    const val ACTION_RECENTS = ACTION_PREFIX + "SYSTEM_ACTION_RECENTS"
//}

package dev.lackluster.redmagichelper.data

object Constants {
    const val CMD_LSPOSED = "am broadcast -a android.telephony.action.SECRET_CODE -d android_secret_code://5776733 android"
    const val PER_MIUI_INTERNAL_API = "miui.permission.USE_INTERNAL_GENERAL_API"
    const val ACTION_PREFIX = "hyperhelper.action."
    const val ACTION_NOTIFICATIONS = ACTION_PREFIX + "SYSTEM_ACTION_NOTIFICATIONS"
    const val ACTION_QUICK_SETTINGS = ACTION_PREFIX + "SYSTEM_ACTION_QUICK_SETTINGS"
    const val ACTION_SCREENSHOT = "android.intent.action.CAPTURE_SCREENSHOT"
    const val ACTION_HOME = ACTION_PREFIX + "SYSTEM_ACTION_HOME"
    //const val ACTION_RECENTS = ACTION_PREFIX + "SYSTEM_ACTION_RECENTS"


    // 截图广播
    const val ACTION_RECENTS = ACTION_PREFIX + "SYSTEM_ACTION_RECENTS"
    const val ACTION_HIDE_STATUSBAR = ACTION_PREFIX + "ACTION_HIDE_STATUSBAR"
    const val ACTION_SHOW_STATUSBAR = ACTION_PREFIX + "ACTION_SHOW_STATUSBAR"
    const val EXTRA_STATUSBAR_VISIBLE = "statusbar_visible"


    const val ACTION_HIDE_STATUSBAR_DONE = ACTION_PREFIX + "ACTION_HIDE_STATUSBAR_DONE"
    const val EXTRA_HIDE_SUCCESS = "hide_success"

}
