@file:Suppress("UNCHECKED_CAST", "unused")

package dev.lackluster.mihelper.hook.compat.factory

import dev.lackluster.mihelper.hook.compat.XposedEnv
import dev.lackluster.mihelper.hook.compat.log.YLog
import dev.lackluster.mihelper.hook.compat.param.HookParam
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

internal fun resolveType(type: Any?): Class<*>? = when (type) {
    null -> null
    is Class<*> -> type
    is KClass<*> -> type.java
    is String -> runCatching { XposedEnv.classLoader.loadClass(type) }.getOrNull()
    else -> null
}

class ModifierConditions {
    var isPublic = false
    var isPrivate = false
    var isProtected = false
    var isStatic = false
    var isFinal = false
    var isAbstract = false
    var isSynchronized = false
    var isNative = false
    var isTransient = false
    var isVolatile = false

    fun matches(modifiers: Int): Boolean {
        if (isPublic && !Modifier.isPublic(modifiers)) return false
        if (isPrivate && !Modifier.isPrivate(modifiers)) return false
        if (isProtected && !Modifier.isProtected(modifiers)) return false
        if (isStatic && !Modifier.isStatic(modifiers)) return false
        if (isFinal && !Modifier.isFinal(modifiers)) return false
        if (isAbstract && !Modifier.isAbstract(modifiers)) return false
        if (isSynchronized && !Modifier.isSynchronized(modifiers)) return false
        if (isNative && !Modifier.isNative(modifiers)) return false
        if (isTransient && !Modifier.isTransient(modifiers)) return false
        if (isVolatile && !Modifier.isVolatile(modifiers)) return false
        return true
    }
}

/** Hook callback DSL scope: `hook { before { ... } after { ... } }`. */
class HookScope internal constructor() {
    internal var beforeBlock: (HookParam.() -> Unit)? = null
    internal var afterBlock: (HookParam.() -> Unit)? = null

    fun before(init: HookParam.() -> Unit) {
        beforeBlock = init
    }

    fun after(init: HookParam.() -> Unit) {
        afterBlock = init
    }

    /** Replaces the whole method body with a constant result. */
    fun replaceTo(value: Any?) {
        before { result = value }
    }

    fun replaceToTrue() = replaceTo(true)
    fun replaceToFalse() = replaceTo(false)
    fun replaceToNull() = replaceTo(null)

    /** Skips the original method unconditionally (result stays null). */
    fun intercept() {
        before { intercept() }
    }

    internal fun install(executable: Executable) {
        val before = beforeBlock
        val after = afterBlock
        val handleRef = java.util.concurrent.atomic.AtomicReference<XposedInterface.HookHandle?>()
        val handle = XposedEnv.module.hook(executable).intercept { chain ->
            val param = HookParam(chain)
            param.handleProvider = { handleRef.get() }
            if (before != null) {
                runCatching { before(param) }
                    .onFailure { YLog.error("before-hook failure in $executable", it) }
            }
            if (!param.hasResult) {
                try {
                    param.resultInternal =
                        if (param.args.modified) chain.proceed(param.args.backing)
                        else chain.proceed()
                    param.hasResult = true
                } catch (t: Throwable) {
                    param.thrown = t
                }
            }
            if (after != null) {
                runCatching { after(param) }
                    .onFailure { YLog.error("after-hook failure in $executable", it) }
            }
            param.thrown?.let { throw it }
            param.resultInternal
        }
        handleRef.set(handle)
    }
}

/** Fallback finders declared in `remedys { }`. */
class RemedyScope<M : Member> internal constructor(private val clazz: Class<*>) {
    internal val finders = mutableListOf<BaseFinder<M>>()

    fun method(init: MethodFinder.() -> Unit) {
        finders.add(MethodFinder(clazz).apply(init) as BaseFinder<M>)
    }

    fun constructor(init: ConstructorFinder.() -> Unit) {
        finders.add(ConstructorFinder(clazz).apply(init) as BaseFinder<M>)
    }

    fun field(init: FieldFinder.() -> Unit) {
        finders.add(FieldFinder(clazz).apply(init) as BaseFinder<M>)
    }
}

abstract class BaseFinder<M : Member>(internal val clazz: Class<*>) {
    internal var searchSuperClass = false
    internal var onlySuperClass = false
    private var ignored = false
    private val remedies = mutableListOf<BaseFinder<M>>()

    fun superClass(isOnlySuperClass: Boolean = false) {
        searchSuperClass = true
        onlySuperClass = isOnlySuperClass
    }

    fun ignored(): BaseFinder<M> = apply { ignored = true }

    fun remedys(init: RemedyScope<M>.() -> Unit): BaseFinder<M> = apply {
        val scope = RemedyScope<M>(clazz)
        scope.init()
        remedies.addAll(scope.finders)
    }

    internal fun classHierarchy(): Sequence<Class<*>> {
        if (!searchSuperClass) return sequenceOf(clazz)
        return generateSequence(if (onlySuperClass) clazz.superclass else clazz) { it.superclass }
    }

    protected abstract fun findDeclared(target: Class<*>): List<M>

    fun find(): List<M> {
        val direct = classHierarchy().flatMap { findDeclared(it).asSequence() }
            .onEach { (it as? java.lang.reflect.AccessibleObject)?.isAccessible = true }
            .toList()
        if (direct.isNotEmpty()) return direct
        for (remedy in remedies) {
            val r = remedy.find()
            if (r.isNotEmpty()) return r
        }
        if (!ignored) YLog.error("Failed to find member in ${clazz.name} ($this)")
        return emptyList()
    }

    /** First matched member, or null. */
    fun give(): M? = find().firstOrNull()

    /** Hooks all matched members. */
    open fun hook(init: HookScope.() -> Unit) {
        val scope = HookScope().apply(init)
        val members = find()
        members.filterIsInstance<Executable>().forEach { executable ->
            runCatching { scope.install(executable) }
                .onFailure { YLog.error("Failed to hook $executable", it) }
        }
    }

    /** Alias of [hook] — explicitly hooks every matched member. */
    fun hookAll(init: HookScope.() -> Unit) = hook(init)

    fun replaceTo(value: Any?) = hook {
        before { result = value }
    }

    fun replaceToTrue() = replaceTo(true)
    fun replaceToFalse() = replaceTo(false)
    fun replaceToNull() = replaceTo(null)
}

class MethodFinder(clazz: Class<*>) : BaseFinder<Method>(clazz) {
    var name: String = ""
    private var names: List<String>? = null
    private var paramTypes: List<Any?>? = null
    var paramCount: Int = -1
    var returnType: Any? = null
    private var modifiersCondition: ModifierConditions? = null

    fun name(vararg names: String) {
        this.names = names.toList()
    }

    fun param(vararg types: Any?) {
        paramTypes = types.toList()
    }

    fun emptyParam() {
        paramTypes = emptyList()
    }

    fun modifiers(init: ModifierConditions.() -> Unit) {
        modifiersCondition = ModifierConditions().apply(init)
    }

    override fun findDeclared(target: Class<*>): List<Method> =
        target.declaredMethods.filter { method ->
            if (name.isNotEmpty() && method.name != name) return@filter false
            if (names != null && method.name !in names!!) return@filter false
            if (paramCount >= 0 && method.parameterCount != paramCount) return@filter false
            paramTypes?.let { conds ->
                if (method.parameterTypes.size != conds.size) return@filter false
                method.parameterTypes.zip(conds).forEach { (actual, cond) ->
                    if (cond != null && actual != resolveType(cond)) return@filter false
                }
            }
            returnType?.let { if (method.returnType != resolveType(it)) return@filter false }
            if (modifiersCondition?.matches(method.modifiers) == false) return@filter false
            true
        }

    /** Static invocation: `method { }.get().call(...)`. Null-safe when not found. */
    fun get(): MethodInvoker = MethodInvoker(find().firstOrNull(), null)

    /** Instance invocation: `method { }.get(instance).call(...)`. */
    fun get(instance: Any?): MethodInvoker = MethodInvoker(find().firstOrNull(), instance)
}

class ConstructorFinder(clazz: Class<*>) : BaseFinder<Constructor<*>>(clazz) {
    private var paramTypes: List<Any?>? = null
    var paramCount: Int = -1
    private var modifiersCondition: ModifierConditions? = null

    fun param(vararg types: Any?) {
        paramTypes = types.toList()
    }

    fun emptyParam() {
        paramTypes = emptyList()
    }

    fun modifiers(init: ModifierConditions.() -> Unit) {
        modifiersCondition = ModifierConditions().apply(init)
    }

    override fun findDeclared(target: Class<*>): List<Constructor<*>> =
        target.declaredConstructors.filter { ctor ->
            if (paramCount >= 0 && ctor.parameterCount != paramCount) return@filter false
            paramTypes?.let { conds ->
                if (ctor.parameterTypes.size != conds.size) return@filter false
                ctor.parameterTypes.zip(conds).forEach { (actual, cond) ->
                    if (cond != null && actual != resolveType(cond)) return@filter false
                }
            }
            if (modifiersCondition?.matches(ctor.modifiers) == false) return@filter false
            true
        }

    fun get(): ConstructorInvoker = ConstructorInvoker(find().firstOrNull())
}

class FieldFinder(clazz: Class<*>) : BaseFinder<Field>(clazz) {
    var name: String = ""
    var type: Any? = null
    private var modifiersCondition: ModifierConditions? = null

    fun modifiers(init: ModifierConditions.() -> Unit) {
        modifiersCondition = ModifierConditions().apply(init)
    }

    override fun findDeclared(target: Class<*>): List<Field> =
        target.declaredFields.filter { field ->
            if (name.isNotEmpty() && field.name != name) return@filter false
            type?.let { if (field.type != resolveType(it)) return@filter false }
            if (modifiersCondition?.matches(field.modifiers) == false) return@filter false
            true
        }

    /** Bound field access: `field { }.get(instance).set(v)` / `.any()`. Null-safe when not found. */
    fun get(instance: Any?): FieldInstance = FieldInstance(find().firstOrNull(), instance)

    /** Static field access. */
    fun get(): FieldInstance = FieldInstance(find().firstOrNull(), null)

    fun getOrNull(instance: Any?): FieldInstance? =
        find().firstOrNull()?.let { FieldInstance(it, instance) }

    fun set(instance: Any?, value: Any?) {
        get(instance).set(value)
    }

    override fun hook(init: HookScope.() -> Unit) {
        YLog.error("FieldFinder does not support hook()")
    }
}
