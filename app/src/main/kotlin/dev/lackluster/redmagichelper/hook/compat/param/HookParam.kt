@file:Suppress("UNCHECKED_CAST")

package dev.lackluster.redmagichelper.hook.compat.param

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Member

/**
 * YukiHookAPI-style hook callback param wrapping a libxposed [XposedInterface.Chain].
 */
class HookParam internal constructor(internal val chain: XposedInterface.Chain) {

    internal var resultInternal: Any? = null
    internal var hasResult = false
    internal var thrown: Throwable? = null

    val member: Member get() = chain.executable

    val instance: Any
        get() = chain.thisObject ?: error("HookParam#instance is null (hooking a static method?)")

    val instanceOrNull: Any? get() = chain.thisObject

    fun <T> instance(): T = instance as T

    val args = ArgsWrapper()

    var result: Any?
        get() = resultInternal
        set(value) {
            resultInternal = value
            hasResult = true
        }

    @Suppress("UNCHECKED_CAST")
    fun <T> result(): T? = resultInternal as? T

    val method: java.lang.reflect.Method get() = member as java.lang.reflect.Method

    internal var handleProvider: (() -> XposedInterface.HookHandle?)? = null

    /** Removes this hook itself. */
    fun removeSelf() {
        handleProvider?.invoke()?.unhook()
    }

    /** Skips the original method without setting a result (result stays null). */
    fun intercept() {
        hasResult = true
    }

    inner class ArgsWrapper internal constructor() {
        internal val backing: Array<Any?> = chain.args.toTypedArray()
        internal var modified = false

        val size: Int get() = backing.size

        operator fun get(index: Int): Any? = backing[index]

        operator fun set(index: Int, value: Any?) {
            backing[index] = value
            modified = true
        }

        operator fun invoke(): Array<Any?> = backing

        operator fun invoke(index: Int) = ArgsIndexer(index)

        fun first(): Any? = backing.firstOrNull()

        fun firstOrNull(): Any? = backing.firstOrNull()

        fun firstOrNull(predicate: (Any?) -> Boolean): Any? = backing.firstOrNull(predicate)

        fun joinToString(
            separator: CharSequence = ", ",
            prefix: CharSequence = "",
            postfix: CharSequence = "",
        ): String = backing.joinToString(separator, prefix, postfix)
    }

    inner class ArgsIndexer internal constructor(private val index: Int) {
        fun any(): Any? = args.backing[index]

        fun set(value: Any?) {
            args.backing[index] = value
            args.modified = true
        }

        fun setTrue() = set(true)
        fun setFalse() = set(false)
        fun setNull() = set(null)

        fun int(): Int = any() as Int
        fun long(): Long = any() as Long
        fun float(): Float = any() as Float
        fun double(): Double = any() as Double
        fun boolean(): Boolean = any() as Boolean
        fun string(): String = any() as String

        fun <T> cast(): T? = any() as? T

        fun <T> array(): Array<T> = any() as Array<T>

        fun first(): Any? = when (val v = any()) {
            is Array<*> -> v.firstOrNull()
            is List<*> -> v.firstOrNull()
            is ByteArray -> v.firstOrNull()
            is IntArray -> v.firstOrNull()
            else -> v
        }
    }
}
