package dev.lackluster.redmagichelper.hook.rules.mihealth

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Named methods plus exact argument compatibility; never invokes a random overload. */
internal object TargetReflection {

    fun call(owner: Any, name: String, vararg args: Any?): Any? {
        val type = if (owner is Class<*>) owner else owner.javaClass
        val methods = ArrayList<Method>()
        var c: Class<*>? = type
        while (c != null) {
            methods.addAll(c.declaredMethods)
            c = c.superclass
        }
        // Room DAOs also inherit public default methods from their interfaces.
        methods.addAll(type.methods)
        for (m in methods) {
            if (m.name != name || m.parameterCount != args.size) continue
            if (owner is Class<*> && !Modifier.isStatic(m.modifiers)) continue
            val types = m.parameterTypes
            var match = true
            for (i in types.indices) {
                val arg = args[i]
                if (arg == null) {
                    if (types[i].isPrimitive) match = false
                } else if (!box(types[i]).isInstance(arg)) {
                    match = false
                }
            }
            if (!match) continue
            m.isAccessible = true
            return m.invoke(if (owner is Class<*>) null else owner, *args)
        }
        throw NoSuchMethodException("${type.name}.$name")
    }

    fun field(owner: Any, name: String): Any? {
        var c: Class<*>? = if (owner is Class<*>) owner else owner.javaClass
        while (c != null) {
            try {
                val f = c.getDeclaredField(name)
                f.isAccessible = true
                return f.get(if (owner is Class<*>) null else owner)
            } catch (ignored: NoSuchFieldException) {
            }
            c = c.superclass
        }
        throw NoSuchFieldException(name)
    }

    fun singleton(type: Class<*>): Any {
        for (name in arrayOf("INSTANCE", "Companion")) {
            try {
                return field(type, name)!!
            } catch (ignored: NoSuchFieldException) {
            }
        }
        throw NoSuchFieldException("${type.name} singleton")
    }

    private fun box(t: Class<*>): Class<*> = when (t) {
        java.lang.Integer.TYPE -> java.lang.Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        java.lang.Character.TYPE -> java.lang.Character::class.java
        else -> t
    }
}
