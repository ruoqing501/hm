@file:Suppress("unused", "UNCHECKED_CAST")

package dev.lackluster.redmagichelper.hook.compat.type.java

val AnyClass: Class<*> = Any::class.java
val IntType: Class<*> = Integer.TYPE
val LongType: Class<*> = java.lang.Long.TYPE
val FloatType: Class<*> = java.lang.Float.TYPE
val DoubleType: Class<*> = java.lang.Double.TYPE
val BooleanType: Class<*> = java.lang.Boolean.TYPE
val ByteType: Class<*> = java.lang.Byte.TYPE
val CharType: Class<*> = Character.TYPE
val ShortType: Class<*> = java.lang.Short.TYPE
val UnitType: Class<*> = Void.TYPE
val StringType: Class<*> = String::class.java
val StringClass: Class<*> = String::class.java
val IntClass: Class<*> = Integer::class.java
val LongClass: Class<*> = java.lang.Long::class.java
val FloatClass: Class<*> = java.lang.Float::class.java
val BooleanClass: Class<*> = java.lang.Boolean::class.java
val CharSequenceClass: Class<*> = CharSequence::class.java
val ArrayClass: Class<*> = Array<Any?>::class.java

/** Builds an array type: `ArrayClass(StringClass)` -> `Array<String>`. */
fun ArrayClass(componentType: Any): Class<*> {
    val component = when (componentType) {
        is Class<*> -> componentType
        is kotlin.reflect.KClass<*> -> componentType.java
        is String -> runCatching { dev.lackluster.redmagichelper.hook.compat.XposedEnv.classLoader.loadClass(componentType) }.getOrNull()
        else -> null
    } ?: Any::class.java
    return java.lang.reflect.Array.newInstance(component, 0).javaClass
}
val ArrayListClass: Class<*> = ArrayList::class.java
val ListClass: Class<*> = List::class.java
val MapClass: Class<*> = Map::class.java
val HashMapClass: Class<*> = HashMap::class.java
