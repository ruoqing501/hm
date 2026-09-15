package dev.lackluster.redmagichelper.service

import android.content.Context
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.utils.IconCodec
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.Prefs
import dev.lackluster.redmagichelper.utils.ScreenOffHideExecutor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * 快捷设置磁贴(移植自 LS_Augment 的 AugmentTileService,语义改为「恢复全部隐藏应用」)。
 *
 * LS_Augment 的磁贴在全部可见时隐藏、否则恢复(双向切换);RedMagicHelper 已有
 * 熄屏自动隐藏([ScreenOffHideExecutor],隐藏只发生在熄屏后且不会自动恢复),
 * 因此磁贴只作为手动恢复入口:点击即对所选目标执行 restoreAll。
 *
 * 名称/描述/图片来自模块自身 Prefs(本进程可读本地 SharedPreferences,见 [Prefs.initApp]);
 * 有目标处于隐藏状态时磁贴为 ACTIVE,否则 INACTIVE;未启用、未配置目标或 Root 不可用时
 * 为 UNAVAILABLE。
 */
class RmhTileService : TileService() {
    private val main = Handler(Looper.getMainLooper())

    override fun onTileAdded() {
        super.onTileAdded()
        refreshAsync()
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshAsync()
    }

    override fun onClick() {
        super.onClick()
        Prefs.initApp(this)
        if (!Prefs.getBoolean(Pref.Key.Module.TILE_ENABLED, true)) {
            qsTile?.let {
                it.state = Tile.STATE_UNAVAILABLE
                it.updateTile()
            }
            return
        }
        if (!IN_FLIGHT.compareAndSet(false, true)) return
        qsTile?.let {
            it.state = Tile.STATE_INACTIVE
            it.updateTile()
        }
        thread(name = "rmh-tile-action") {
            val targets = Prefs.getStringSet(Pref.Key.Other.SCREEN_OFF_HIDE_TARGETS, mutableSetOf())
            val outcome = ScreenOffHideExecutor.restoreAll(targets)
            if (outcome.success) {
                Log.i(TAG, "tile restore all done: ${outcome.message}")
            } else {
                Log.w(TAG, "tile restore all failed: ${outcome.message}")
            }
            main.post {
                IN_FLIGHT.set(false)
                refreshAsync()
            }
        }
    }

    private fun refreshAsync() {
        Prefs.initApp(this)
        thread(name = "rmh-tile-refresh") {
            val enabled = Prefs.getBoolean(Pref.Key.Module.TILE_ENABLED, true)
            val targets = ScreenOffHideExecutor.sanitize(
                Prefs.getStringSet(Pref.Key.Other.SCREEN_OFF_HIDE_TARGETS, mutableSetOf())
            )
            var hiddenCount = 0
            var usable = enabled && targets.isNotEmpty() && ScreenOffHideExecutor.rootGranted()
            if (usable) {
                val userId = ScreenOffHideExecutor.currentUserId()
                for (pkg in targets) {
                    when (ScreenOffHideExecutor.queryState(pkg, userId)) {
                        ScreenOffHideExecutor.State.HIDDEN -> hiddenCount++
                        ScreenOffHideExecutor.State.ERROR -> {
                            usable = false
                            break
                        }
                        else -> Unit
                    }
                }
            }
            val state = when {
                !usable -> Tile.STATE_UNAVAILABLE
                hiddenCount > 0 -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            main.post { applyPresentation(state) }
        }
    }

    private fun applyPresentation(state: Int) {
        val tile = qsTile ?: return
        val label = prefText(Pref.Key.Module.TILE_LABEL, getString(R.string.tile_default_label), 30)
        val description = prefText(Pref.Key.Module.TILE_DESCRIPTION, getString(R.string.tile_default_description), 60)
        val stateText = getString(
            if (state == Tile.STATE_ACTIVE) R.string.tile_state_hidden else R.string.tile_state_visible
        )
        tile.icon = resolveIcon(this)
        tile.label = label
        tile.subtitle = description
        tile.stateDescription = stateText
        tile.contentDescription = "$label，$description，$stateText"
        tile.state = state
        tile.updateTile()
    }

    companion object {
        private const val TAG = "RedMagicHelper"
        private val IN_FLIGHT = AtomicBoolean()

        /** 读取自定义磁贴图片(Base64 WebP),无则回退到默认矢量图标。 */
        fun resolveIcon(context: Context): Icon {
            val encoded = Prefs.getString(Pref.Key.Module.TILE_ICON, "") ?: ""
            if (encoded.isNotEmpty()) {
                runCatching {
                    IconCodec.decodeIcon(encoded)?.let { return Icon.createWithBitmap(it) }
                }
            }
            return Icon.createWithResource(context, R.drawable.ic_tile_restore)
        }

        /** 配置变更后请求系统回调 onStartListening,让磁贴立即刷新。 */
        fun requestRefresh(context: Context) {
            runCatching {
                TileService.requestListeningState(context, ComponentName(context, RmhTileService::class.java))
            }
        }

        /** 清理换行/控制字符并按码点截断,空串回退默认值(对应 LS_Augment TilePresentation.value)。 */
        private fun prefText(key: String, fallback: String, maxCodePoints: Int): String {
            val raw = Prefs.getString(key, null) ?: return fallback
            val value = raw.replace('\r', ' ').replace('\n', ' ').trim()
            if (value.isEmpty()) return fallback
            val count = value.codePointCount(0, value.length)
            return if (count <= maxCodePoints) value
            else value.substring(0, value.offsetByCodePoints(0, maxCodePoints))
        }
    }
}
