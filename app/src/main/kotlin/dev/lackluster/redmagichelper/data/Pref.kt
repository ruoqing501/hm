package dev.lackluster.redmagichelper.data

object Pref {
    const val VERSION = 5
    object Key {
        object App {
            const val SPLIT_VIEW = "app_disable_split"
            const val HAZE_BLUR = "app_haze_blur"
            const val HAZE_TINT_ALPHA_LIGHT = "app_haze_tint_alpha_light"
            const val HAZE_TINT_ALPHA_DARK = "app_haze_tint_alpha_dark"
            const val SKIP_ROOT_CHECK = "app_ignore_root"
        }
        object Other{
            const val MUTE_NFC_SOUND = "mute_nfc_sound"
            const val SCREENSHOT_HIDE_STATUS_BAR = "screenshot_hide_status_bar"
            const val RERCORD_SCREEN_HIDE_STATUS_BAR = "rercord_screen_hide_status_bar"
            const val NFC_ALLOW_SCREEN_OFF_RECOGNITION = "nfc_allow_screen_off_recognition"
            const val ALLOW_THIRDPARTY_LAUNCHER = "allow_thirdparty_launcher"
            const val DOUBLE_ANY_APP = "double_any_app"
            const val RM_LOW_MEMORY_LIMIT = "rm_low_memory_limit"
            const val HIDE_MTP_CATEGORY_BROWSE = "hide_mtp_category_browse"
            const val MTP_RENAME_ROOT_NAME_SWITCH = "mtp_rename_root_name_switch"
            const val MTP_RENAME_ROOT_NAME = "mtp_rename_root_name"
        }
        object Module {
            const val ENABLED = "enable_module"
            const val DEX_KIT_CACHE = "dexkit_cache"
            const val HIDE_ICON = "hide_icon"
            const val SHOW_IN_SETTINGS = "entry_in_settings"
            const val SETTINGS_ICON_STYLE = "entry_icon_style"
            const val SETTINGS_ICON_COLOR = "entry_icon_color"
            const val SETTINGS_NAME = "entry_name"
            const val SETTINGS_NAME_CUSTOM = "entry_name_custom"
            const val SP_VERSION = "sp_version"
        }
        object Android {
            const val DISABLE_FREEFORM_RESTRICT = "android_freeform_restriction"
            const val SYSTEM_FRAMEWORK_LOCK_SCREEN_TIMEOUT_VALUE = "system_framework_lock_screen_timeout_value"
            const val SYSTEM_FRAMEWORK_LOCK_SCREEN_TIMEOUT = "system_framework_lock_screen_timeout"
            const val SYSTEM_SETTINGS_ANTI_QUES = "system_settings_anti_ques"
            const val ANDROID_FORCE_CUSTOM_THEME = "android_force_custom_theme"
            const val ALLOW_MORE_FREEFORM = "android_freeform_allow_more"
            const val MULTI_TASK = "android_freeform_multi_task"
            const val HIDE_WINDOW_TOP_BAR = "android_freeform_hide_top_bar"
            const val BLOCK_FIXED_ORIENTATION = "android_no_fixed_orientation"
            const val BLOCK_FIXED_ORIENTATION_LIST = "android_no_fixed_orientation_list"
            const val WALLPAPER_SCALE_RATIO = "android_wallpaper_scale"
            const val BLOCK_FORCE_DARK_WHITELIST = "android_dark_mode_all"
            const val FONT_SCALE = "android_font_scale"
            const val FONT_SCALE_VAL = "android_font_scale_val"
            const val DISABLE_FLAG_SECURE_ENHANCED = "disable_flag_secure_enhanced"
            const val SYSTEM_FRAMEWORK_DISABLE_72H_VERIFY = "system_framework_disable_72h_verify"
            const val SYSTEM_FRAMEWORK_OTHER_DISABLE_THERMAL = "system_framework_other_disable_thermal"
            const val REMOVE_RESTRICTIONS_WINDOW = "remove_restrictions_window"
            const val REMOVE_RESTRICTIONS_WINDOW_NUMBER = "remove_restrictions_window_number"
            const val REMOVE_ALERT_WINDOWS_NOTIFICATION = "remove_alert_windows_notification"
            const val DISABLE_SOUND_WHEN_UNLOCKED = "disable_sound_when_unlocked"
            const val ANDROID_REMOVE_INTENT_HIJACK_CONTENT = "android_remove_intent_hijack_content"
            const val ANDROID_ALLOW_UNTRUSTED_TOUCHES = "android_allow_untrusted_touches"
            const val ANDROID_DISABLE_SYSTEM_SIGNATURE_VERIFICATION = "android_disable_system_signature_verification"
            const val ANDROID_LONG_POWER_KEY_WAKEUP_ASSIST = "android_long_power_key_wakeup_assist"
            const val ANDROID_BLOCK_TELEMETRY_SERVICE = "android_block_telemetry_service"
            const val ANDROID_AIRPLANE_MODE_KEEP_BLUETOOTH = "android_airplane_mode_keep_bluetooth"
            const val ANDROID_AIRPLANE_MODE_KEEP_WLAN = "android_airplane_mode_keep_wlan"
            const val TELECOM_WLAN_CC = "telecom_wlan_cc"
            const val TELECOM_WLAN_CC_DIALOG = "telecom_wlan_cc_dialog"
            const val PIN_STA_MAC = "pin_sta_mac"
            const val PIN_AP_BSSID = "pin_ap_bssid"
            const val PIN_STA_MAC_DIALOG = "pin_sta_mac_dialog"
            const val PIN_AP_BSSID_DIALOG = "pin_ap_bssid_dialog"

            const val ALARM_CLOCK_VOLUME_LEVEL_SWITCH = "alarm_clock_volume_level_switch"
            const val MEDIA_CLOCK_VOLUME_LEVEL_SWITCH = "media_clock_volume_level_switch"
            const val NOTIFICATION_CLOCK_VOLUME_LEVEL_SWITCH = "notification_clock_volume_level_switch"
            const val RING_CLOCK_VOLUME_LEVEL_SWITCH = "ring_clock_volume_level_switch"
            const val VOICE_CLOCK_VOLUME_LEVEL_SWITCH = "voice_clock_volume_level_switch"



            const val ANDROID_CLOCK_AUDIO = "android_clock_audio" //标题：声音
            const val ALARM_CLOCK_VOLUME_LEVEL = "alarm_clock_volume_level"
            const val MEDIA_CLOCK_VOLUME_LEVEL = "media_clock_volume_level"
            const val NOTIFICATION_CLOCK_VOLUME_LEVEL = "notification_clock_volume_level"
            const val RING_CLOCK_VOLUME_LEVEL = "ring_clock_volume_level"
            const val VOICE_CLOCK_VOLUME_LEVEL = "voice_clock_volume_level"
        }
        object Browser {
            const val AD_BLOCKER = "browser_ad_block"
            const val DEBUG_MODE = "browser_debug_mode"
            const val SWITCH_ENV = "browser_switch_env"
            const val BLOCK_UPDATE = "browser_no_update"
            const val SKIP_SPLASH = "browser_skip_splash"
            const val REMOVE_APP_REC = "browser_remove_app_rec"
            const val HIDE_HOMEPAGE_TOP_BAR = "browser_hide_home_top_bar"
            const val HIDE_AI_SEARCH_ENTRY = "browser_hide_ai_search"
        }
        object Download {
            const val FUCK_XL = "download_remove_xl"
        }
        object DownloadUI {
            const val HIDE_XL = "downloadui_remove_xl"
        }
        object GuardProvider {
            const val BLOCK_UPLOAD_APP = "guard_forbid_upload_app"
            const val BLOCK_ENV_CHECK = "guard_block_env_check"
        }
        object InCallUI {
            const val HIDE_CRBT = "incallui_hide_crbt"
        }
        object LBE {
            const val BLOCK_REMOVE_AUTO_STARTUP = "lbe_block_rec_auto_startup"
            const val CLIPBOARD_TOAST = "lbe_clipboard_toast"
        }
        object Market {
            const val AD_BLOCKER = "market_ad_block"
            const val SKIP_SPLASH = "market_skip_splash"
            const val TAB_BLUR = "market_tab_blur"
            const val FILTER_TAB = "market_filter_tab"
            const val FILTER_TAB_IGNORE_RESTRICT = "market_tab_ignore_restrict"
            const val HIDE_TAB_HOME = "market_hide_tab_home"
            const val HIDE_TAB_GAME = "market_hide_tab_game"
            const val HIDE_TAB_RANK = "market_hide_tab_rank"
            const val HIDE_TAB_AGENT = "market_hide_tab_agent"
            const val HIDE_TAB_APP_ASSEMBLE = "market_hide_tab_assemble"
            const val HIDE_TAB_MINI_GAME = "market_hide_tab_mini_game"
            const val HIDE_TAB_MINE = "market_hide_tab_mine"
            const val HIDE_TAB_OTHERS = "market_hide_tab_others"
            const val BLOCK_UPDATE_DIALOG = "market_block_up_dialog"
            const val HIDE_APP_SECURITY = "market_hide_app_security"
        }
        object MiAi {
            const val SEARCH_USE_BROWSER = "xiaoai_use_browser"
            const val SEARCH_ENGINE = "xiaoai_search_engine"
            const val SEARCH_URL = "xiaoai_search_url"
            const val HIDE_WATERMARK = "xiaoai_hide_watermark"
        }
        object MiLink {
            const val FUCK_HPPLAY = "milink_fuck_hpplay"
        }
        object MiMirror {
            const val CONTINUE_ALL_TASKS = "mismarthub_all_app"
            const val ENHANCE_CONTINUE_TASKS = "mismarthub_enhance_continue"
        }
        object MiTrust {
            const val DISABLE_RISK_CHECK = "mitrust_skip_risk_check"
        }
        object MiuiHome {
            const val REMOVE_REPORT = "home_remove_report"
            const val DOUBLE_TAP_TO_SLEEP = "home_double_tap_sleep"
            const val BACK_HAPTIC = "home_back_haptic"
            const val QUICK_SWITCH = "home_quick_back"
            const val QUICK_SWITCH_LEFT = "home_quick_back_left"
            const val QUICK_SWITCH_RIGHT = "home_quick_back_right"
            const val LINE_GESTURE_DOUBLE_TAP = "home_line_double_tap"
            const val LINE_GESTURE_LONG_PRESS = "home_line_long_press"
            const val ANIM_ICON_ZOOM = "home_anim_icon_zoom"
            const val ANIM_ICON_DARKEN = " home_anim_icon_darken"
            const val ANIM_FOLDER_ZOOM = "home_anim_folder_zoom"
            const val ANIM_FOLDER_ICON_DARKEN = "home_anim_folder_icon_darken"
            const val FOLDER_ADAPT_SIZE ="home_folder_aapt_size"
            const val PAD_RECENT_SHOW_MEMORY = "home_recent_pad_memory"
            const val PAD_RECENT_HIDE_WORLD = "home_recent_pad_world"
            const val RECENT_SHOW_REAL_MEMORY = "home_recent_real_memory"
            const val RECENT_CARD_ANIM = "home_recent_anim"
            const val RECENT_HIDE_CLEAR_BUTTON = "home_recent_hide_clear_button"
            const val RECENT_MEM_INFO_CLEAR = "home_recent_mem_info_clear"
            const val RECENT_DISABLE_FAKE_NAVBAR = "home_disable_fake_navbar"
            const val MINUS_RESTORE_SETTING = "home_minus_restore"
            const val FORCE_COLOR_STATUS_BAR = "home_force_color_status_bar"
            const val FORCE_COLOR_TEXT_ICON = "home_force_color_text_icon"
            const val FORCE_COLOR_MINUS = "home_force_color_minus"
            const val DOCK_REMOVE_NUM_LIMIT = "home_dock_remove_num_limit"
        }
        object MMS {
            const val AD_BLOCKER = "mms_ad_block"
        }
        object Music {
            const val AD_BLOCKER = "music_ad_block"
            const val SKIP_SPLASH = "music_skip_splash"
            const val HIDE_KARAOKE = "music_hide_karaoke"
            const val HIDE_LONG_AUDIO = "music_hide_long_audio"
            const val HIDE_DISCOVER = "music_hide_discover"
            const val MY_HIDE_BANNER = "music_my_hide_banner"
            const val MY_HIDE_REC_PLAYLIST = "music_my_hide_rec"
            const val HIDE_FAV_NUM = "music_hide_fav_num"
        }
        object PackageInstaller {
            const val BLOCK_UPLOAD_INFO = "package_block_upload"
            const val REMOVE_ELEMENT = "package_ad_block"
            const val DISABLE_COUNT_CHECK = "package_count_check"
            const val DISABLE_RISK_CHECK = "package_skip_risk_check"
            const val INSTALL_SOURCE = "package_install_source"
            const val SOURCE_PKG_NAME = "package_source_pkg"
            const val DISGUISE_NO_NETWORK = "package_no_network"
        }
        object PowerKeeper {
            const val DO_NOT_KILL_APP = "power_donot_kill_app"
            const val BLOCK_BATTERY_WHITELIST = "power_battery_whitelist"
            const val GMS_BG_RUNNING = "power_gms_bg_running"
            const val UNLOCK_CUSTOM_REFRESH = "power_custom_refresh"
        }
        object RemoteController {
            const val AD_BLOCKER = "remote_ad_block"
        }
        object Search {
            const val MORE_SEARCH_ENGINE = "search_more_engine"
            const val CUSTOM_SEARCH_ENGINE = "search_custom_engine"
            const val CUSTOM_SEARCH_ENGINE_ENTITY = "search_custom_engine_entity"
        }
        object SecurityCenter {
            const val SKIP_SPLASH = "security_skip_splash"
            const val LOCK_SCORE = "security_lock_score"
            const val HIDE_RED_DOT = "security_hide_red_dot"
            const val HIDE_HOME_REC = "security_hide_home_rec"
            const val HIDE_HOME_COMMON = "security_hide_home_common"
            const val HIDE_HOME_POPULAR = "security_hide_home_popular"
            const val DISABLE_RISK_APP_NOTIF = "security_no_risk_notification"
            const val REMOVE_REPORT = "security_remove_report"
            const val SKIP_WARNING = "security_skip_warn"
            const val LINK_START = "security_link_start"
            const val SHOW_SCREEN_BATTERY = "security_screen_battery"
            const val SHOW_SYSTEM_BATTERY = "security_system_battery"
            const val DISABLE_BUBBLE_RESTRICT = "security_bubble_restriction"
            const val CTRL_SYSTEM_APP_WIFI = "security_system_app_wifi"
            const val CLICK_ICON_TO_OPEN = "security_click_icon_open"
        }
        object Settings {
            const val SHOE_GOOGLE = "settings_show_google"
            const val UNLOCK_TAPLUS_FOR_PAD = "taplus_unlock_pad"
            const val QUICK_PER_OVERLAY = "settings_quick_per_overlay"
            const val QUICK_PER_INSTALL_SOURCE = "settings_quick_per_install"
        }
        object GameSpace {

            const val GAME_SPACE_SUPER_RESOLUTION_SWITCH = "game_space_super_resolution_switch"
            const val ACIVE_MODE_SWITCH = "acive_mode_switch"
            const val GAME_FUCTION_UNFREEZE_SWITCH = "game_fuction_unfreeze_switch"
            const val GAME_SPACE_DEVIL_MODE_SWITCH = "game_space_devil_mode_switch"
            const val GAME_SPACE_PREVENT_COLLAPSE = "game_space_prevent_collapse"
            const val GAME_SPACE_DEVIL_MODE_ENABLE_SUPER_RESOLUTION = "game_space_devil_mode_enable_super_resolution"
            const val GAME_SPACE_DEVIL_MODE_HIDE_PROMPT = "game_space_devil_mode_hide_prompt"
            const val GAME_SPACE_ENABLE_SUPER_RESOLUTION_LOW = "game_space_enable_super_resolution_low"
            const val GAME_SPACE_DEVIL_MODE_ENABLE_SUPER_RESOLUTION_LOW_PROP_PROMPT = "game_space_devil_mode_enable_super_resolution_low_prop_prompt"
        }
        object SystemUI {
            object Volume {
                const val DISABLE_SAFETY_WARNING = "volume_disable_safety_warning"
            }
            object StatusBar {
                const val HIDE_BATTERY_PERCENTAGE_ICON = "hide_battery_percentage_icon"
                const val HOLES_OFTEN_SHOW = "holes_often_show"
                const val HOME_RECENT_HIDE_STATUS_BAR = "home_recent_hide_status_bar"
                const val SETSTATUSBARMAXNOTIFICATIONICONS_TITLE = "setstatusbarmaxnotificationicons_title"
                const val SETSTATUSBARMAXNOTIFICATIONICONS_TITLE_SWITCH = "setstatusbarmaxnotificationicons_title_switch"
                const val STATUS_BAR_LAYOUT_SWITCH = "status_bar_layout_switch"
                const val STATUS_BAR_HEIGHT = "status_bar_height"
                const val STATUS_BAR_SYSTEM_ICON_HEIGHT = "status_bar_system_icon_height"
                const val STATUS_BAR_LEFT_CONTAINER_TOP_MARGIN = "status_bar_left_container_top_margin"
                const val STATUS_BAR_LEFT_CONTAINER_DOWN_MARGIN = "status_bar_left_container_down_margin"
                const val STATUS_BAR_LEFT_CONTAINER_LEFT_MARGIN = "status_bar_left_container_left_margin"
                const val STATUS_BAR_LEFT_CONTAINER_RIGHT_MARGIN = "status_bar_left_container_right_margin"

                const val STATUS_BAR_RIGHT_CONTAINER_TOP_MARGIN = "status_bar_right_container_top_margin"
                const val STATUS_BAR_RIGHT_CONTAINER_DOWN_MARGIN = "status_bar_right_container_down_margin"
                const val STATUS_BAR_RIGHT_CONTAINER_LEFT_MARGIN = "status_bar_right_container_left_margin"
                const val STATUS_BAR_RIGHT_CONTAINER_RIGHT_MARGIN = "status_bar_right_container_right_margin"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO = "status_bar_display_battery_info"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_DUAL_LINE = "status_bar_display_battery_info_dual_line"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_LAYOUT = "status_bar_display_battery_info_layout"
                const val STATUS_BAR_HORIZONTAL_ALIGNMENT = "status_bar_horizontal_alignment"
                const val STATUS_BAR_DISPLAY_TEMPERATURE_SWITCH = "status_bar_display_temperature_switch"
                const val STATUS_BAR_DUAL_ROW = "status_bar_dual_row"
                const val STATUS_BAR_DUAL_ROW_LEFT = "status_bar_dual_row_left"
                const val STATUS_BAR_DISPLAY_TEMP_BATTERY = "status_bar_display_temp_battery"
                const val STATUS_BAR_DISPLAY_TEMP_CPU = "status_bar_display_temp_cpu"
                const val STATUS_BAR_DISPLAY_TEMP_GPU = "status_bar_display_temp_gpu"
                const val STATUS_BAR_DISPLAY_TEMP_HIDE_UNIT = "status_bar_display_temp_hide_unit"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_FONT_SIZE = "status_bar_display_battery_info_font_size"
                const val STATUS_BAR_BATTERY_INFO_FIXED_WIDTH_SINGLE = "status_bar_battery_info_fixed_width_single"
                const val STATUS_BAR_BATTERY_INFO_FIXED_WIDTH_DUAL = "status_bar_battery_info_fixed_width_dual"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_DUAL_DISPLAY_MODE = "status_bar_display_battery_info_dual_display_mode"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_HIDE_TITLE_DUAL = "status_bar_display_battery_info_hide_title_dual"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_TEMP_MODE_DUAL = "status_bar_display_battery_info_temp_mode_dual"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_HIDE_TITLE_SINGLE = "status_bar_display_battery_info_hide_title_single"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_TEMP_MODE_SINGLE = "status_bar_display_battery_info_temp_mode_single"
                const val STATUS_BAR_BATTERY_INFO_LOCATION_DUAL = "status_bar_battery_info_location_dual"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_FONT_SIZE_DUAL = "status_bar_display_battery_info_font_size_dual"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION1 = "status_bar_display_battery_info_option1"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION2 = "status_bar_display_battery_info_option2"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_OPTION3 = "status_bar_display_battery_info_option3"
                const val STATUS_BAR_TEMPERATURE_LOCATION = "status_bar_temperature_location"
                const val STATUS_BAR_BATTERY_INFO_LOCATION = "status_bar_battery_info_location"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_IS_CHARGE_MODE_SINGLE = "status_bar_display_battery_info_is_charge_mode_single"
                const val STATUS_BAR_DISPLAY_BATTERY_INFO_IS_CHARGE_MODE_DUAL = "status_bar_display_battery_info_is_charge_mode_dual"
                const val STATUS_BAR_DISPLAY_TEMP_DISPLAY_MODE = "status_bar_display_temp_display_mode"
                const val STATUS_BAR_DISPLAY_TEMP_DISPLAY_ELECTRICITY = "status_bar_display_temp_display_electricity"
                const val STATUS_BAR_DISPLAY_TEMP_DISPLAY_POWER = "status_bar_display_temp_display_power"
                const val STATUS_BAR_DISPLAY_TEMP_FONT_SIZE = "status_bar_display_temp_font_size"
                const val STATUS_BAR_DUAL_ROW_RIGHT = "status_bar_dual_row_right"
                const val STATUS_BAR_DUAL_CLOCK_ACROSS = "status_bar_dual_clock_across"
                const val CLOCK_SIZE = "clock_size"
                const val NUBIA_CLOCK_PADDING_TOP = "nubia_clock_padding_top"
                const val NUBIA_CLOCK_PADDING_DOWN = "nubia_clock_padding_down"
                const val NUBIA_CLOCK_PADDING_LEFT = "nubia_clock_padding_left"
                const val NUBIA_CLOCK_PADDING_RIGHT = "nubia_clock_padding_right"
                const val ALIGNMENT = "alignment"
                const val SHOW_YEARS = "show_years"
                const val SHOW_MONTH = "show_month"
                const val SHOW_DAY = "show_day"
                const val SHOW_WEEK = "show_week"
                const val SHOW_CN_HOUR = "show_cn_hour"
                const val SHOW_PERIOD = "show_period"
                const val SHOW_SECONDS = "show_seconds"
                const val SHOW_MILLISECOND = "show_millisecond"
                const val HIDE_SPACE = "hide_space"
                const val DUAL_ROW = "dual_row"



                const val TIME_INDICATOR = "time_indicator"
                const val BATTERY_COLOR_Phase1 = "battery_color_phase1"
                const val BATTERY_COLOR_Phase2 = "battery_color_phase2"
                const val BATTERY_COLOR_Phase3 = "battery_color_phase3"
                const val BATTERY_COLOR_Phase4 = "battery_color_phase4"
                const val BATTERY_STYLE_ALPHA = "battery_style_alpha"
                const val INPUT_INT = "input_int"
                const val QS_SHORTCUT_REDIR_CALENDAR = "qs_shortcut_redir_calendar"
                const val QS_SHORTCUT_REDIR_SEARCH = "qs_shortcut_redir_search"
                const val QS_SHOW_CARRIER = "qs_show_carrier"
                const val QS_SHOW_SEARCH = "qs_show_search"
                const val QS_CUSTOM_BROWSER_PACKAGE = "qs_custom_browser_package"
                const val QS_CUSTOM_ROW_COLUMN_SWITCH = "qs_custom_row_column_switch"
                const val QS_CUSTOM_ROW = "qs_custom_row"
                const val QS_CUSTOM_ROW_LANDSCAPE = "qs_custom_row_landscape"
                const val QS_CUSTOM_COLUMN = "qs_custom_column"
                const val QS_CUSTOM_COLUMN_LANDSCAPE = "qs_custom_column_landscape"
                const val QS_CUSTOM_COLUMN_EDIT = "qs_custom_column_edit"
                const val QS_CUSTOM_COLUMN_EDIT_LANDSCAPE = "qs_custom_column_edit_landscape"
                const val AOSP_SINGLEHANDMODE_ADJUST = "aosp_singlehandmode_adjust"
                const val UNHIDE_CLIPBOARD_OVERLAY = "unhide_clipboard_overlay"
                const val NO_VIBRATE_VOLKEY_LONG_PRESS = "no_vibrate_volKey_Long_press"
                const val GESTURE_USE_DEFAULT_DIGITAL_ASSIST = "gesture_use_default_digital_assist"
                const val STATUS_BAR_DUAL_ROW_NETWORK_SPEED = "status_bar_dual_row_network_speed"
                const val SPEED_UNIT_HIDE_PER_SECOND = "speed_unit_hide_per_second"
                const val STATUS_BAR_NETWORK_SPEED_DUAL_ROW_SIZE = "status_bar_network_speed_dual_row_size"
                const val STATUS_BAR_NETWORK_SPEED_DUAL_ROW_WIDTH = "status_bar_network_speed_dual_row_width"
                const val STATUS_BAR_NETWORK_SPEED_DUAL_ROW_DIGIT_LEN = "status_bar_network_speed_dual_row_digit_len"
                const val LOW_SPEED_HIDE_KILO_BYTES = "low_speed_hide_kilo_bytes"
                const val CLOCK_LAYOUT_CUSTOM = "statusbar_clock_custom"
                const val CLOCK_PADDING_LEFT = "statusbar_clock_padding_left"
                const val CLOCK_PADDING_RIGHT = "statusbar_clock_padding_right"
                const val CLOCK_GEEK = "statusbar_clock_geek"
                const val CLOCK_GEEK_FORMAT = "statusbar_clock_pattern"
                const val CLOCK_GEEK_FORMAT_HORIZON = "statusbar_clock_pattern_horizon"
                const val CLOCK_GEEK_FORMAT_PAD = "statusbar_clock_pattern_pad"
                const val CLOCK_SHOW_PERIOD = "statusbar_clock_show_period"
                const val CLOCK_SHOW_WEEK = "statusbar_clock_show_week"
                const val STATUS_BAR_DOUBLE_CLICKED_LOCKED_SCREEN = "status_bar_double_clicked_locked_screen"
                const val STATUS_BAR_NETWORK_SPEED_REFRESH_SPEED = "status_bar_network_speed_refresh_speed"
                const val RESTORE_THE_FONT_OF_THE_CLOCK_DATE_ICON = "restore_the_font_of_the_clock_date_icon"
                const val STATUS_BAR_USE_THE_NATIVE_NOTIFICATION_ICON = "status_bar_use_the_native_notification_icon"
                const val CLOCK_SHOW_MONTH_DAY = "statusbar_clock_show_month_day"
                const val CLOCK_SHOW_PULL_DOWN = "statusbar_clock_pull_down_show"
                const val STATUS_BAR_DISPLAY_TEMPERATURE = "status_bar_display_temperature"
                const val CLOCK_SHOW_PULL_DOWN_PERIOD = "statusbar_clock_pull_down_show_period"

                const val CLOCK_SHOW_AMPM = "statusbar_clock_show_ampm"
                const val CLOCK_SHOW_SECONDS = "statusbar_clock_show_seconds"
                const val CLOCK_SHOW_LEADING_ZERO = "statusbar_clock_show_zero"
                const val CLOCK_FIXED_WIDTH = "statusbar_clock_fixed_width"
                const val CLOCK_TNUM = "statusbar_clock_tnum"
                const val NOTIFICATION_COUNT = "statusbar_notif_max"
                const val NOTIFICATION_COUNT_ICON = "statusbar_notif_icon_max"
                const val DOUBLE_TAP_TO_SLEEP = "statusbar_double_tap_sleep"
                const val DISABLE_SMART_DARK = "systemui_disable_smart_dark"
            }
            object ScreenOff{
                const val SCREEN_OFF_PERIOD_FONT_SIZE = "screen_off_period_font_size"
                const val SCREEN_OFF_PERIOD_FONT_SIZE_SETTINGS = "screen_off_period_font_size_settings"
            }
            object FontWeight {
                const val FONT_PATH = "sb_font_path"
                const val AOD_FONT_PATH = "aod_font_path"
                const val AOD_FONT_ZOOM = "aod_font_zoom"
                const val AOD_CLOCK = "aod_clock"
                const val AOD_SHOW_SECONDS = "aod_show_seconds"
                const val LOCK_SCREEN_FONT_SIZE_ZOOM = "lock_screen_font_size_zoom"
                const val CLOCK = "sb_font_clock"
                const val CLOCK_WEIGHT = "sb_font_clock_weight"
                const val CLOCK_NOTIFICATION = "sb_font_clock_notif"
                const val CLOCK_NOTIFICATION_WEIGHT = "sb_font_clock_notif_weight"
                const val FOCUS_NOTIFICATION = "sb_font_focus"
                const val FOCUS_NOTIFICATION_WEIGHT = "sb_font_focus_weight"
                const val CARRIER = "sb_font_carrier"
                const val LOCKSCREEN_CLOCK = "sb_font_lockscreen_clock"
                const val CARRIER_WEIGHT = "sb_font_carrier_weight"
                const val NET_SPEED_NUMBER = "sb_font_speed_num"
                const val NET_SPEED_NUMBER_WEIGHT = "sb_font_speed_num_weight"
                const val NET_SPEED_UNIT = "sb_font_speed_unit"
                const val NET_SPEED_UNIT_WEIGHT = "sb_font_speed_unit_weight"
                const val MOBILE_TYPE = "sb_font_mobile_type"
                const val MOBILE_TYPE_WEIGHT = "sb_font_mobile_type_weight"
                const val BATTERY_PERCENTAGE_IN = "sb_font_battery_pct_in"
                const val BATTERY_PERCENTAGE_IN_WEIGHT = "sb_font_bat_pct_in_weight"
                const val BATTERY_PERCENTAGE_OUT = "sb_font_bat_pct_out"
                const val BATTERY_PERCENTAGE_OUT_WEIGHT = "sb_font_bat_pct_out_weight"
                const val BATTERY_PERCENTAGE_MARK = "sb_font_bat_pct_mark"
                const val BATTERY_PERCENTAGE_MARK_WEIGHT = "sb_font_bat_pct_mark_weight"
            }
            object IconTurner {
                const val DISABLE_ATTERY_PERCENTAGE_DISPLAY_SETTING_OPTION = "disable_attery_percentage_display_setting_option"
                const val BATTERY_ICON_COLOR_SWITCH = "battery_icon_color_switch"
                const val IGNORE_SYS_HIDE = "statusbar_ignore_sys_hide"
                const val NUBIA_IGNORE_SYS_HIDE = "nubia_statusbar_ignore_sys_hide"
                const val NUBIA_ICON_TUNER_WIFI_HIDE_WIFI_TYPE = "nubia_icon_tuner_wifi_hide_wifi_type"
                const val NUBIA_HIDE_WIFI_ACTIVITY = "nubia_hide_wifi_activity"
                const val MOBILE = "statusbar_hide_mobile"
                const val NUBIA_MOBILE = "nubia_statusbar_hide_mobile"
                const val HIDE_SIM_ONE = "statusbar_hide_sim_one"
                const val HIDE_SIM_TWO = "statusbar_hide_sim_two"
                const val NO_SIM = "statusbar_hide_no_sim"
                const val HIDE_MOBILE_ACTIVITY = "statusbar_hide_mobile_activity"
                const val HIDE_MOBILE_TYPE = "statusbar_hide_mobile_type"
                const val HD_NEW = "statusbar_hide_hd_new"
                const val HIDE_HD_SMALL = "statusbar_hide_hd_small"
                @Deprecated("Use HD_NEW")
                const val HIDE_HD_LARGE = "statusbar_hide_hd_large"
                @Deprecated("")
                const val HIDE_HD_NO_SERVICE = "statusbar_hide_hd_no_service"
                const val HIDE_ROAM = "statusbar_hide_roam"
                const val HIDE_ROAM_SMALL = "statusbar_hide_roam_small"
                const val HIDE_VOLTE = "statusbar_hide_volte"
                const val HIDE_VOWIFI = "statusbar_hide_vowifi"
                const val WIFI = "statusbar_hide_wifi"
                const val NUBIA_WIFI = "nubia_statusbar_hide_wifi"
                const val HIDE_WIFI_ACTIVITY = "statusbar_hide_wifi_activity"
                const val HIDE_WIFI_STANDARD = "statusbar_hide_wifi_type"
                const val HOTSPOT = "statusbar_hide_hotspot"
                const val NUBIA_HOTSPOT = "nubia_statusbar_hide_hotspot"
                const val BATTERY_STYLE = "statusbar_battery_style"
                const val BATTERY_PERCENTAGE_SYMBOL_STYLE = "statusbar_battery_percent_symbol_style"
                const val STATUS_BAR_BATTERY_LAYOUT_WIDTH = "status_bar_battery_layout_width"
                const val HIDE_CHARGE = "statusbar_hide_charge"
                const val BATTERY_MODIFY_PERCENTAGE_TEXT_SIZE = "statusbar_change_battery_percent_size"
                const val BATTERY_PERCENTAGE_TEXT_SIZE = "statusbar_battery_percent_size"
                const val BATTERY_PERCENTAGE_TNUM = "statusbar_battery_percent_tnum"
                const val SWAP_BATTERY_PERCENT = "statusbar_swap_battery_percent"
                const val BATTERY_MODIFY_PADDING = "statusbar_battery_custom"
                const val BATTERY_PADDING_LEFT = "statusbar_battery_custom_padding_left"
                const val BATTERY_PADDING_RIGHT = "statusbar_battery_custom_padding_right"
                const val FLIGHT_MODE = "statusbar_hide_flight_mode"
                const val GPS = "statusbar_hide_gps"
                const val BLUETOOTH = "statusbar_hide_bluetooth"
                const val BLUETOOTH_BATTERY = "statusbar_hide_bluetooth_battery"
                const val NFC = "statusbar_hide_nfc"
                const val VPN = "statusbar_hide_vpn"
                const val NET_SPEED = "statusbar_hide_net_speed"
                const val CAR = "statusbar_hide_car"
                const val PAD = "statusbar_hide_pad"
                const val PC = "statusbar_hide_pc"
                const val PHONE = "statusbar_hide_phone"
                const val SOUND_BOX = "statusbar_hide_sound_box"
                const val SOUND_BOX_GROUP = "statusbar_hide_sound_box_group"
                const val SOUND_BOX_SCREEN = "statusbar_hide_sound_box_screen"
                const val STEREO = "statusbar_hide_stereo"
                const val TV = "statusbar_hide_tv"
                const val WIRELESS_HEADSET = "statusbar_hide_wireless_headset"
                const val GLASSES = "statusbar_hide_glasses"
                const val CAMERA = "statusbar_hide_camera"
                const val SWAP_MOBILE_WIFI = "statusbar_swap_mobile_wifi"
                const val ALARM = "statusbar_hide_alarm"
                const val HEADSET = "statusbar_hide_headset"
                const val VOLUME = "statusbar_hide_volume"
                const val ZEN = "statusbar_hide_zen"
                const val HIDE_PRIVACY = "statusbar_hide_privacy"
            }
            object LockScreen {
                const val DOUBLE_TAP_TO_SLEEP = "systemui_double_tap_sleep"
                const val LOCK_SCREEN_BATTERY_DETAIL_FONT_SIZE = "lock_screen_battery_detail_font_size"
                const val LOCK_SCREEN_BATTERY_DETAIL_LINE_SPACING = "lock_screen_battery_detail_line_spacing"
                const val SHOW_BATTERY_TEMPERATURE = "show_battery_temperature"
                const val SHOW_CHARGING_C_MORE = "show_charging_c_more"
                const val SHOW_CHARGING_V_MORE = "show_charging_v_more"
                const val SHOW_CHARGING_P_MORE = "show_charging_p_more"
                const val SHOW_REFRESH_INTERVAL_TIME = "show_refresh_interval_time"
                const val LOCK_SCREEN_CHARGING_ANIMATION_SWITCH = "lock_screen_charging_animation_switch"
                const val LOCK_SCREEN_CHARGING_ANIMATION = "lock_screen_charging_animation"
                const val LOCK_SCREEN_DISPLAY_ANIMATION_DURATION = "lock_screen_display_animation_duration"
                const val LOCK_SCREEN_DISPLAY_DELAY_TIME = "lock_screen_display_delay_time"
                const val LOCK_SCREEN_HIDE_STATUS_BAR = "lock_screen_hide_status_bar"
                const val SHOW_CHARGING_INFO = "show_charging_info"
                const val HIDE_DISTURB = "systemui_lockscreen_hide_disturb"
                const val DISPLAY_SECONDS = "systemui_lockscreen_display_seconds"
                const val DISPLAY_PERIOD = "systemui_lockscreen_display_period"
                const val ALLOW_ADJUST_VOLUME = "systemui_lockscreen_allow_adjust_volume"
                const val CARRIER_TEXT = "systemui_lockscreen_carrier_text"
                const val KEEP_NOTIFICATION = "systemui_lockscreen_keep_notif"
                const val SCREEN_OFF_PERIOD = "systemui_screen_off_period"
                const val SCREEN_OFF_SHOW_SECONDS = "systemui_screen_off_show_period"
            }
            object NotifCenter {
                const val NOTIF_NO_WHITELIST = "systemui_notif_no_whitelist"
                const val NOTIF_FREEFORM = "systemui_notif_freeform"
                const val CLOCK_PAD_ANIM = "statusbar_clock_pad_anim"
                const val MIUIX_EXPAND_BUTTON = "systemui_notif_miuix_expand"
                const val MONET_OVERLAY = "systemui_notif_monet_overlay"
                const val MONET_OVERLAY_COLOR = "systemui_notif_monet_color"
                const val EXPAND_NOTIFICATION = "systemui_notif_expand_notifs"
            }
            object MediaControl {
                const val UNLOCK_ACTION = "media_unlock_action"

                const val BACKGROUND_STYLE = "media_ctrl_background"
                const val BLUR_RADIUS = "media_ctrl_radius"
                const val ALLOW_REVERSE = "media_allow_reverse"

                const val LYT_ALBUM = "media_lyt_album"
                const val LYT_LEFT_ACTIONS = "media_lyt_left_actions"
                const val LYT_ACTIONS_ORDER = "media_lyt_actions_order"
                const val LYT_HIDE_TIME = "media_lyt_hide_time"
                const val LYT_HIDE_SEAMLESS = "media_lyt_hide_seamless"
                const val LYT_HEADER_MARGIN = "media_lyt_header_margin"
                const val LYT_HEADER_PADDING = "media_lyt_header_padding"

                const val ELM_TEXT_SIZE = "media_elm_text_size"
                const val ELM_TITLE_SIZE = "media_elm_title_size"
                const val ELM_ARTIST_SIZE = "media_elm_artist_size"
                const val ELM_TIME_SIZE = "media_elm_time_size"
                const val ELM_ACTIONS_RESIZE = "media_elm_actions_resize"
                const val ELM_THUMB_STYLE = "media_elm_thumb_style"
                const val ELM_PROGRESS_STYLE = "media_elm_prog_style"
                const val ELM_PROGRESS_WIDTH = "media_elm_prog_width"

                const val FIX_THUMB_CROPPED = "media_thumb_crop"
                const val USE_ANIM = "media_ctrl_anim"
            }
            object ControlCenter {
                const val HIDE_CARRIER_ONE = "statusbar_hide_carrier_one"
                const val HIDE_CARRIER_TWO = "statusbar_hide_carrier_two"
                const val HIDE_CARRIER_HD = "statusbar_hide_carrier_hd"
                const val BATTERY_PERCENTAGE = "statusbar_cc_battery_percent"
                const val BATTERY_PERCENTAGE_ANIM = "statusbar_cc_battery_percent_anim"
            }
            object Plugin {
                const val AUTO_FLASH_ON = "sys_plugin_flash_on"
            }
            const val FUCK_GESTURES_DAT = "systemui_fuck_gesture_dat"
        }


        object NubiaPackageInstaller{
            const val SKIP_PKG_INSTALLER_SCAN = "skip_pkg_scan"
            const val HIDE_EVOLUTION_MODE_TOGGLE = "hide_evolution_mode_toggle"
            const val HIDE_STORE_INSTALL_PROMPT = "hide_store_install_prompt"
            const val CTS_TEST_INSTALLER = "cts_test_installer"

        }
        object NubiaSystemSettings{
            const val DISABLE_USB_INSTALLATION_AND_SWITCH_ACCOUNT_VERIFICATION = "disable_usb_installation_and_switch_account_verification"
            const val TIME_PICKER_PERIOD = "time_picker_period"
            const val USB_DEBUGGING = "usb_debugging"
            const val USB_INSTALL = "usb_install"
            const val LOCK_REFRESH_RATE = "lock_refresh_rate"
            const val AUTOMATIC_SCREEN_OFF = "automatic_screen_off"
            const val UNLOCK_165HZ = "unlock_165hz"
            const val DISPLAY_SYSTEM_SETTINGS_DEVELOP = "display_system_settings_develop"
            const val SYSTEM_SETTINGS_USB_MODE = "system_settings_usb_mode"
            const val SYSTEM_SETTINGS_USB_MODE_CHOOSE = "system_settings_usb_mode_choose"
            const val SYSTEM_SETTINGS_USB_MODE_NOTIFICATION_SHOW = "system_settings_usb_mode_notification_show"
            const val SYSTEM_SETTINGS_USB_DEBUGGING_AUTO_ALLOW = "system_settings_usb_debugging_auto_allow"

        }
        object SystemDesktop{
            const val SYSTEM_TIME_COMPONENT_DESKTOP_SWITCH= "system_time_component_desktop_switch"
            const val LIGHT_THEME_COLOR= "light_theme_color"
            const val DARK_THEME_COLOR= "dark_theme_color"
            const val MEMORY_FONT_COLOR_OPTION= "memory_font_color_option"
            const val MEMORY_DISPLAY_STYLE= "memory_display_style"
            const val CLASSICS_PORTRAITSCREEN_MEMORY_FONT_SIZE= "classics_portraitscreen_memory_font_size"
            const val CLASSICS_PORTRAITSCREEN_COMPONENT_HEIGHT= "classics_portraitscreen_component_height"
            const val CLASSICS_LANDSACPE_MEMORY_FONT_SIZE= "classics_landsacpe_memory_font_size"
            const val CLASSICS_LANDSACPE_COMPONENT_HEIGHT= "classics_landsacpe_component_height"


            const val SIMPLE_PORTRAITSCREEN_COMPONENT_HEIGHT= "simple_portraitscreen_component_height"
            const val SIMPLE_PORTRAITSCREEN_MEMORY_FONT_SIZE= "simple_portraitscreen_memory_font_size"
            const val SIMPLE_LANDSACPE_MEMORY_FONT_SIZE= "simple_landsacpe_memory_font_size"
            const val SIMPLE_LANDSACPE_COMPONENT_HEIGHT= "simple_landsacpe_component_height"
            const val SYSTEM_DESKTOP_RECENT_TASK_DISPLAY_MEMORY= "system_desktop_recent_task_display_memory"
            const val DISPLAY_STYLE= "DISPLAY_STYLE"
            const val SYSTEM_TIME_COMPONENT_DESKTOP_SHOW_SECONDS = "system_time_component_desktop_show_seconds"
            const val SYSTEM_TIME_COMPONENT_DESKTOP_SHOW_PERIOD = "system_time_component_desktop_show_period"
        }
        object NubiaSystemUpdate{
            const val DISABLE_SYSTEM_UPDATE = "disable_system_update"
            const val UPDATE_PACKAGE_ADDRESS = "update_package_address"
            const val SYSTEM_UPDATE_MOCK_MODEL = "system_update_mock_model"
            const val SYSTEM_UPDATE_MOCK_MODEL_UPDATE = "system_update_mock_model_update"
            const val SYSTEM_UPDATE_MOCK_IMEI_SW = "system_update_mock_imei_sw"
            const val SYSTEM_UPDATE_MOCK_IMEI = "system_update_mock_imei"
            const val SYSTEM_UPDATE_MOCK_LOCAL_SW = "system_update_mock_local_sw"
            const val SYSTEM_UPDATE_MOCK_LOCAL = "system_update_mock_local"
            const val SYSTEM_UPDATE_MOCK_SIGN_SW = "system_update_mock_sign_sw"
            const val SYSTEM_UPDATE_MOCK_SIGN = "system_update_mock_sign"
            const val SYSTEM_UPDATE_MOCK_FINGERPRINT_SW = "system_update_mock_fingerprint_sw"
            const val SYSTEM_UPDATE_MOCK_FINGERPRINT = "system_update_mock_fingerprint"
            const val SYSTEM_UPDATE_MOCK_BUILD_DISPLAY_SW = "system_update_mock_build_display_sw"
            const val SYSTEM_UPDATE_MOCK_BUILD_DISPLAY = "system_update_mock_build_display"
            const val SYSTEM_UPDATE_MOCK_SYSTEM_INNER_VERSION_SW = "system_update_mock_system_inner_version_sw"
            const val SYSTEM_UPDATE_MOCK_SYSTEM_INNER_VERSION = "system_update_mock_system_inner_version"
            const val SYSTEM_UPDATE_MOCK_VARIANT_ID_SW = "system_update_mock_variant_id_sw"
            const val SYSTEM_UPDATE_MOCK_VARIANT_ID = "system_update_mock_variant_id"
            const val SYSTEM_UPDATE_MOCK_MANUFACTURE_SW = "system_update_mock_manufacture_sw"
            const val SYSTEM_UPDATE_MOCK_MANUFACTURE = "system_update_mock_manufacture"
        }
        object About{
            const val SHOW_Donation_Window = "show_donation_window"
        }
        object NubiaTheme{
            const val CANCEL_TRIAL_LOGIN = "cancel_trial_login"
        }
        object Taplus {
            const val HIDE_SHOP = "taplus_hide_shop"
            const val SEARCH_USE_BROWSER = "taplus_use_browser"
            const val SEARCH_ENGINE = "taplus_search_engine"
            const val SEARCH_URL = "taplus_search_url"
        }
        object Themes {
            const val SKIP_SPLASH = "themes_skip_splash"
        }
        object Hints {
            const val MEDIA_ADVANCED_TEXTURES = "hint_media_adv_textures"
        }
        object Updater {
            const val DISABLE_VALIDATION = "updater_no_validation"
        }
        object Weather {
            const val CARD_COLOR = "weather_card_color"
        }
    }
    @Suppress("unused")
    object OldKey {
        object Android {
            const val ROTATE_SUGGEST = "android_rotate_suggest"
        }
        object Gallery {
            const val PATH_OPTIM = "gallery_path_optim"
            const val UNLIMITED_CROP = "screenshot_unlimited_crop"
        }
        object Joyose {
            const val BLOCK_CLOUD_CONTROL = "joyose_no_cloud_control"
        }
        object MiShare {
            const val ALWAYS_ON = "mishare_no_auto_off"
        }
        object MiuiHome {
            object Refactor {
                const val EXTRA_COMPATIBILITY = "home_refactor_extra_compatibility"
                const val SYNC_WALLPAPER_SCALE = "home_refactor_wallpaper_scale_sync"
                const val EXTRA_FIX = "home_refactor_extra_fix"
                const val FIX_SMALL_WINDOW_ANIM = "home_refactor_small_window"
                const val ALL_APPS_BLUR_BG = "home_refactor_allapps_blur_bg"
                const val MINUS_MODE = "home_refactor_minus_mode"
                const val SHOW_LAUNCH_IN_RECENTS = "home_refactor_launch_show"
                const val SHOW_LAUNCH_IN_RECENTS_SCALE = "home_refactor_launch_scale"
                const val SHOW_LAUNCH_IN_FOLDER = "home_refactor_launch_folder"
                const val SHOW_LAUNCH_IN_FOLDER_SCALE = "home_refactor_launch_folder_scale"
                const val SHOW_LAUNCH_IN_MINUS_SCALE = "home_refactor_minus_launch_scale"
                const val APPS_BLUR = "home_refactor_apps_blur"
                const val APPS_BLUR_RADIUS_STR = "home_refactor_apps_radius_str"
                const val APPS_DIM = "home_refactor_apps_dim"
                const val APPS_DIM_MAX = "home_refactor_apps_dim_max"
                const val APPS_NONLINEAR_TYPE = "home_refactor_apps_nonlinear_type"
                const val APPS_NONLINEAR_DECE_FACTOR = "home_refactor_apps_nonlinear_dece_factor"
                const val APPS_NONLINEAR_PATH_X1 = "home_refactor_apps_nonlinear_path_x1"
                const val APPS_NONLINEAR_PATH_Y1 = "home_refactor_apps_nonlinear_path_y1"
                const val APPS_NONLINEAR_PATH_X2 = "home_refactor_apps_nonlinear_path_x2"
                const val APPS_NONLINEAR_PATH_Y2 = "home_refactor_apps_nonlinear_path_y2"
                const val FOLDER_BLUR = "home_refactor_folder_blur"
                const val FOLDER_BLUR_RADIUS_STR = "home_refactor_folder_radius_str"
                const val FOLDER_DIM = "home_refactor_folder_dim"
                const val FOLDER_DIM_MAX = "home_refactor_folder_dim_max"
                const val FOLDER_NONLINEAR_TYPE = "home_refactor_folder_nonlinear_type"
                const val FOLDER_NONLINEAR_DECE_FACTOR = "home_refactor_folder_nonlinear_dece_factor"
                const val FOLDER_NONLINEAR_PATH_X1 = "home_refactor_folder_nonlinear_path_x1"
                const val FOLDER_NONLINEAR_PATH_Y1 = "home_refactor_folder_nonlinear_path_y1"
                const val FOLDER_NONLINEAR_PATH_X2 = "home_refactor_folder_nonlinear_path_x2"
                const val FOLDER_NONLINEAR_PATH_Y2 = "home_refactor_folder_nonlinear_path_y2"
                const val WALLPAPER_BLUR = "home_refactor_wall_blur"
                const val WALLPAPER_BLUR_RADIUS_STR = "home_refactor_wall_radius_dp"
                const val WALLPAPER_DIM = "home_refactor_wall_dim"
                const val WALLPAPER_DIM_MAX = "home_refactor_wall_dim_max"
                const val WALLPAPER_NONLINEAR_TYPE = "home_refactor_wall_nonlinear_type"
                const val WALLPAPER_NONLINEAR_DECE_FACTOR = "home_refactor_wall_nonlinear_dece_factor"
                const val WALLPAPER_NONLINEAR_PATH_X1 = "home_refactor_wall_nonlinear_path_x1"
                const val WALLPAPER_NONLINEAR_PATH_Y1 = "home_refactor_wall_nonlinear_path_y1"
                const val WALLPAPER_NONLINEAR_PATH_X2 = "home_refactor_wall_nonlinear_path_x2"
                const val WALLPAPER_NONLINEAR_PATH_Y2 = "home_refactor_wall_nonlinear_path_y2"
                const val MINUS_BLUR = "home_refactor_minus_blur"
                const val MINUS_BLUR_RADIUS_STR = "home_refactor_minus_radius_str"
                const val MINUS_DIM = "home_refactor_minus_dim"
                const val MINUS_DIM_MAX = "home_refactor_minus_dim_max"
                const val MINUS_OVERLAP = "home_refactor_minus_overlap"
                const val SHOW_LAUNCH_IN_MINUS = "home_refactor_minus_launch"
                const val APPS_BLUR_RADIUS = "home_refactor_apps_blur_radius"
                const val WALLPAPER_BLUR_RADIUS = "home_refactor_wall_radius"
                const val MINUS_BLUR_RADIUS = "home_refactor_minus_radius"
            }
            const val MINUS_BLUR = "person_assist_blur"
            const val REFACTOR = "home_refactor"
            const val ANIM_UNLOCK = "home_anim_unlock"
            const val ANIM_WALLPAPER_ZOOM_SYNC = "home_wallpaper_sync"
            const val FOLDER_NO_PADDING = "home_folder_no_padding"
            const val FOLDER_COLUMNS = "home_folder_columns"
            const val FOLDER_BLUR = "home_blur_enhance"
            const val ICON_UNBLOCK_GOOGLE = "home_icon_unblock_google"
            const val ICON_CORNER4LARGE = "home_icon_corner4large"
            const val ICON_PERFECT = "home_icon_perfect_icon"
            const val RECENT_WALLPAPER_DARKEN = "home_recent_wallpaper"
            const val PAD_DOCK_TIME_DURATION = "home_pad_dock_time_duration"
            const val PAD_DOCK_SAFE_AREA_HEIGHT = "home_pad_dock_safe_height"
            const val WIDGET_ANIM = "home_widget_launch_anim"
            const val WIDGET_RESIZABLE = "home_widget_resizable"
            const val SHORTCUT_FREEFORM = "home_shortcut_freeform"
            const val SHORTCUT_INSTANCE = "home_shortcut_instance"
            const val MINUS_FOLD_STYLE = "home_minus_fold"
            const val MINUS_BLUR_TYPE = "person_assist_blur_type"
            const val ALWAYS_SHOW_TIME = "home_always_show_time"
            const val FAKE_PREMIUM = "home_fake_premium"
        }
        object Module {
            const val LITE_MODE = "lite_mode"
        }
        object ScreenRecorder {
            const val SAVE_TO_MOVIES = "screen_recorder_to_movies"
        }
        object Screenshot {
            const val SAVE_AS_PNG = "screenshot_save_png"
            const val SAVE_TO_PICTURE = "screenshot_to_picture"
        }
        object Settings {
            const val UNLOCK_VOIP_ASSISTANT = "settings_unlock_voip_assistant"
            const val UNLOCK_CUSTOM_REFRESH = "power_custom_refresh"
            const val UNLOCK_NET_MODE_SETTINGS = "phone_net_mode_settings"
        }
        object SystemUI {
            object IconTurner {
                const val HIDE_BATTERY_PERCENT_SYMBOL = "statusbar_hide_battery_percent"
                const val CHANGE_BATTERY_PERCENT_SYMBOL = "statusbar_change_battery_percent_mark"
                const val BATTERY_PADDING_LEFT = "statusbar_battery_padding_left"
                const val BATTERY_PADDING_RIGHT = "statusbar_battery_padding_right"
            }
            object MediaControl {
                const val SQUIGGLY_PROGRESS = "media_ctrl_squiggly_progress"
                const val HIDE_APP_ICON = "media_ctrl_hide_app"
            }
            object NotifCenter {
                const val CLOCK_COLOR_FIX = "statusbar_clock_color_fix"
            }
        }
        object PackageInstaller {
            const val UPDATE_SYSTEM_APP = "package_update_system_app"
            const val MORE_INFO = "package_more_info"
        }
        object SecurityCenter {
            const val SKIP_WARNING = "security_skip_warn"
            const val SKIP_OPEN_APP = "security_skip_open_app"
        }
    }
    object DefValue {
        object SystemUI {
            const val CLOCK_GEEK_FORMAT = "HH:mm"

            const val CLOCK_GEEK_FORMAT_HORIZON = "M d E"
            const val CLOCK_GEEK_FORMAT_PAD = "M d E"
        }
    }
}