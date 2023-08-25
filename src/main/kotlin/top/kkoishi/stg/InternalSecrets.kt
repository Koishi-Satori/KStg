@file:JvmName("  InternalSecrets  ")
@file:Suppress("NOTHING_TO_INLINE")

package top.kkoishi.stg

import sun.misc.Unsafe
import java.lang.RuntimeException

private val UNSAFE: Unsafe? = accessUnsafe()

private fun accessUnsafe(): Unsafe? {
    return try{
        val field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
        field.isAccessible = true
        field.get(null) as Unsafe
    } catch (e: Exception) {
        null
    }
}

internal inline fun getCallerClass(): Class<*> {
    try {
        throw RuntimeException()
    } catch (e: Exception) {
        return Class.forName(e.stackTrace[0].className)
    }
}
