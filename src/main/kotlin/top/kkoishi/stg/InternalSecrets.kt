@file:JvmName("  InternalSecrets  ")
@file:Suppress("NOTHING_TO_INLINE")

package top.kkoishi.stg

import sun.misc.Unsafe
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.lang.reflect.Field

internal var canAccessUnsafe: Boolean = false

private val UNSAFE: Unsafe = accessUnsafe()

private fun accessUnsafe(): Unsafe {
    return try {
        val field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
        field.isAccessible = true
        canAccessUnsafe = true
        field.get(null) as Unsafe
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

internal inline fun getCallerClass(): Class<*> {
    try {
        throw RuntimeException()
    } catch (e: Exception) {
        return Class.forName(e.stackTrace[0].className)
    }
}

internal fun setField(field: Field, isStatic: Boolean, obj: Any?, value: Any?): Any? {
    if (isStatic)
        return setStaticField(field, value)
    if (obj == null)
        throw NullPointerException()
    return setObjectField(field, obj, value)
}

private fun setStaticField(field: Field, value: Any?): Any? {
    val base = UNSAFE.staticFieldBase(field)
    val offset = UNSAFE.staticFieldOffset(field)
    return UNSAFE.getAndSetObject(base, offset, value)
}

private fun setObjectField(field: Field, obj: Any, value: Any?): Any? {
    val offset = UNSAFE.objectFieldOffset(field)
    return UNSAFE.getAndSetObject(obj, offset, value)
}

internal fun getField(field: Field, isStatic: Boolean, obj: Any?): Any {
    if (isStatic)
        return getStaticField(field)
    if (obj == null)
        throw NullPointerException()
    return getObjectField(field, obj)
}

private fun getStaticField(field: Field): Any {
    val base = UNSAFE.staticFieldBase(field)
    val offset = UNSAFE.staticFieldOffset(field)
    return UNSAFE.getObject(base, offset)
}

private fun getObjectField(field: Field, obj: Any): Any {
    val offset = UNSAFE.objectFieldOffset(field)
    return UNSAFE.getObject(obj, offset)
}
