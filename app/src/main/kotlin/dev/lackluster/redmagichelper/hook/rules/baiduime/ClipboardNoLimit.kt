package dev.lackluster.redmagichelper.hook.rules.baiduime

import android.text.InputFilter
import android.text.Spanned
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.CharSequenceClass
import dev.lackluster.redmagichelper.hook.compat.type.java.IntType
import dev.lackluster.redmagichelper.hook.compat.type.java.StringType
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.utils.factory.hasEnable

// 百度输入法定制版(com.baidu.input_oem) 剪贴板复制/粘贴字数与条数限制解除
// 逆向结论（v12.9.800.41）：复制入库按 7000 字截断、历史上限 300 条、编辑超 7000 字拒绝保存，
// 均为硬编码，粘贴上屏本身无限制
object ClipboardNoLimit : YukiBaseHooker() {
    private const val TAG = "BaiduImeClipboardNoLimit"
    override fun onHook() {
        hasEnable(Pref.Key.Other.BAIDU_IME_NO_CLIPBOARD_LIMIT) {
            // 复制入库截断：原样返回，不入 LengthFilter
            "com.baidu.input.clipboard.manager.BDClipboardManager".toClass().method {
                name = "c"
                param(StringType)
            }.hook {
                before {
                    result = args[0]
                }
            }
            // 兜底：禁用 max=7000 的 LengthFilter 截断（覆盖灵感语录同步与云端同步强制截断）
            InputFilter.LengthFilter::class.java.method {
                name = "filter"
                param(
                    CharSequenceClass, IntType, IntType,
                    Spanned::class.java, IntType, IntType
                )
            }.hook {
                before {
                    val max = InputFilter.LengthFilter::class.java.field {
                        name = "mMax"
                    }.get(instance).int()
                    if (max == 7000) {
                        result = args[0]
                    }
                }
            }
            // 硬编码上限调大：300 条 / 7000 字
            val syncConst = "com.baidu.input.sync.clipboard.ClipboardSyncHelperKt".toClass()
            syncConst.field { name = "f49081a" }.get(null).set(100000)
            syncConst.field { name = "f49082b" }.get(null).set(Int.MAX_VALUE)
            // 编辑剪贴板条目超 7000 字拒绝保存：仅对超长文本放行，空内容校验保持原样
            "com.baidu.input.clipboard.panel.view.EditClipboardItemActivity".toClass().method {
                name = "a"
                param(StringType)
            }.hook {
                before {
                    val text = args[0] as? String
                    if (!text.isNullOrEmpty() && text.length > 7000) {
                        result = true
                    }
                }
            }
            YLog.debug(tag = TAG, msg = "clipboard limit hooks installed")
        }
    }
}
