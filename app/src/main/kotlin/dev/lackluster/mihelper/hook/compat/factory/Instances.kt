@file:Suppress("UNCHECKED_CAST", "unused")

package dev.lackluster.mihelper.hook.compat.factory

import dev.lackluster.mihelper.hook.compat.log.YLog
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/** Null-safe bound field accessor returned by `field { }.get(instance)`. */
class FieldInstance internal constructor(
    private val field: Field?,
    private val instance: Any?,
) {
    fun any(): Any? = runCatching {
        val f = field ?: return@runCatching null
        f.get(instance)
    }
        .onFailure { YLog.error("FieldInstance#get failure: $field", it) }
        .getOrNull()

    fun set(value: Any?) {
        val f = field ?: return
        runCatching { f.set(instance, value) }
            .onFailure { YLog.error("FieldInstance#set failure: $f", it) }
    }

    fun setTrue() = set(true)
    fun setFalse() = set(false)
    fun setNull() = set(null)

    fun string(): String? = any() as? String
    fun int(): Int = any() as? Int ?: 0
    fun long(): Long = any() as? Long ?: 0L
    fun boolean(): Boolean = any() as? Boolean ?: false
    fun <T> cast(): T? = any() as? T
}

/** Null-safe method invoker returned by `method { }.get(...)` or `current().method { }`. */
class MethodInvoker internal constructor(
    private val method: Method?,
    private val instance: Any?,
) {
    fun call(vararg args: Any?): Any? {
        val m = method ?: return null
        return runCatching { m.invoke(instance, *args) }
            .onFailure { YLog.error("MethodInvoker#call failure: $m", it) }
            .getOrNull()
    }

    fun any(): Any? = call()
    fun string(): String? = call() as? String
    fun int(): Int = call() as? Int ?: 0
    fun long(): Long = call() as? Long ?: 0L
    fun float(): Float = call() as? Float ?: 0f
    fun boolean(): Boolean = call() as? Boolean ?: false
    fun <T> cast(): T? = call() as? T
}

/** Null-safe constructor invoker returned by `constructor { }.get()`. */
class ConstructorInvoker internal constructor(
    private val constructor: Constructor<*>?,
) {
    fun newInstance(vararg args: Any?): Any? {
        val c = constructor ?: return null
        return runCatching { c.newInstance(*args) }
            .onFailure { YLog.error("ConstructorInvoker#newInstance failure: $c", it) }
            .getOrNull()
    }

    fun call(vararg args: Any?): Any? = newInstance(*args)
}

/** `anyInstance.current()` wrapper for reflective member access on a live object. */
class CurrentInstance internal constructor(val instance: Any?) {
    private val clazz: Class<*>? = instance?.javaClass

    fun field(init: FieldFinder.() -> Unit): FieldInstance {
        val c = clazz ?: return FieldInstance(null, null)
        return FieldFinder(c).apply(init).get(instance)
    }

    fun method(init: MethodFinder.() -> Unit): MethodInvoker {
        val c = clazz ?: return MethodInvoker(null, null)
        return MethodFinder(c).apply(init).get(instance)
    }

    fun constructor(init: ConstructorFinder.() -> Unit): ConstructorInvoker {
        val c = clazz ?: return ConstructorInvoker(null)
        return ConstructorFinder(c).apply(init).get()
    }

    fun any(): Any? = instance
    fun <T> cast(): T? = instance as? T
}

fun Any?.current(): CurrentInstance = CurrentInstance(this)
