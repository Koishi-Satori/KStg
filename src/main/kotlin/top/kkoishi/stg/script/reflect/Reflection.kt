package top.kkoishi.stg.script.reflect

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodHandles.Lookup
import java.lang.invoke.MethodType
import java.lang.reflect.*
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayDeque

object Reflection {
    @JvmStatic
    internal fun isStatic(mth: Method): Boolean = Modifier.isStatic(mth.modifiers)

    @JvmStatic
    internal fun isStatic(field: Field): Boolean = Modifier.isStatic(field.modifiers)
    @JvmStatic
    internal fun isFinal(field: Field): Boolean = Modifier.isFinal(field.modifiers)

    internal fun <T> parseMethod(descriptor: String, allocator: (MethodHandle?, Method?) -> T): T {
        val info = parseMethodDescriptor(descriptor)
        val lookup = MethodHandles.lookup()
        var methodHandle: MethodHandle? = null
        var method: Method? = null
        with(Reflection::class.logger()) {
            log(System.Logger.Level.INFO, "Parse Method Descriptor: $descriptor")
            log(System.Logger.Level.INFO, "Method Owner: ${info.ownerClass()}")
            log(System.Logger.Level.INFO, "Method Return: ${info.requiredReturnType()}")
            log(System.Logger.Level.INFO, "Method Name: ${info.methodName()}")
            log(System.Logger.Level.INFO, "Method Parameter: ${info.parameterTypes().contentToString()}")

            try {
                val type = MethodType.methodType(info.requiredReturnType(), info.parameterTypes())
                methodHandle = lookup.findStatic(info.ownerClass(), info.methodName(), type)
            } catch (e: IllegalAccessException) {
                log(System.Logger.Level.WARNING, e)
                methodHandle = tryAccessPrivate(info, lookup)
            } catch (e: Exception) {
                log(System.Logger.Level.WARNING, e)
                method = tryGetMethod(info, descriptor)
            }

            log(System.Logger.Level.INFO, "Parse method to ($methodHandle, $method)")
        }
        return allocator(methodHandle, method)
    }

    private fun tryAccessPrivate(info: ScriptLinker.LinkInfo, lookup: Lookup): MethodHandle? {
        return try {
            MethodHandles.privateLookupIn(info.ownerClass(), lookup).findStatic(
                info.ownerClass(),
                info.methodName(),
                MethodType.methodType(info.requiredReturnType(), info.parameterTypes())
            )
        } catch (e: Exception) {
            Reflection::class.logger().log(System.Logger.Level.TRACE, e)
            null
        }
    }

    private fun tryGetMethod(info: ScriptLinker.LinkInfo, descriptor: String): Method? {
        var method: Method? = null
        try {
            method = getMethod(info.ownerClass(), info.methodName(), *info.parameterTypes())
            method.isAccessible = true
            if (!isStatic(method)) {
                method = null
                Reflection::class.logger().log(System.Logger.Level.ERROR, "$descriptor should be static!")
            }
        } catch (e: Exception) {
            Reflection::class.logger().log(System.Logger.Level.TRACE, e)
        }
        return method
    }

    @JvmStatic
    fun parseMethodDescriptor(descriptor: String): ScriptLinker.LinkInfo {
        val rest = descriptor.iterator()
        var actualName = ""
        var className = ""
        val classNameBuffer = StringBuilder()
        val buffer = StringBuilder()

        // Jump all the dot before the actual name and get it.
        var lookup = '.'
        while (rest.hasNext()) {
            lookup = rest.nextChar()
            when (lookup) {
                '.' -> {
                    classNameBuffer.append(buffer.toString()).append('.')
                    buffer.clear()
                    if (!rest.hasNext())
                        throw IllegalArgumentException("Incorrect JVM descriptor $descriptor")
                }

                '(' -> {
                    if (classNameBuffer.last() == '.')
                        classNameBuffer.deleteCharAt(classNameBuffer.lastIndex)
                    className = classNameBuffer.toString()
                    actualName = buffer.toString()
                    buffer.clear()
                    classNameBuffer.clear()
                    break
                }

                else -> buffer.append(lookup)
            }
        }
        if (lookup != '(')
            throw IllegalArgumentException("Incorrect JVM descriptor $descriptor")

        val parameters = ArrayDeque<Class<*>>(4)
        while (rest.hasNext()) {
            val clz = parseClassDescriptor(rest, buffer) ?: break
            parameters.addLast(clz)
        }

        val returnType = parseClassDescriptor(rest, buffer) ?: throw IllegalArgumentException()

        return MethodInfo(Class.forName(className), actualName, returnType, parameters.toTypedArray())
    }

    @JvmStatic
    private fun parseClassDescriptor(rest: CharIterator, buffer: StringBuilder): Class<*>? {
        if (!rest.hasNext())
            return null
        var lookup: Char = rest.nextChar()
        when (lookup) {
            'B' -> return Byte::class.java
            'C' -> return Char::class.java
            'D' -> return Double::class.java
            'F' -> return Float::class.java
            'I' -> return Int::class.java
            'J' -> return Long::class.java
            'S' -> return Short::class.java
            'Z' -> return Boolean::class.java
            '[' -> {
                val componentClass =
                    parseClassDescriptor(rest, buffer)
                if (componentClass == Void.TYPE || componentClass == null)
                    throw IllegalArgumentException("Illegal Array Class Descriptor.")
                return java.lang.reflect.Array.newInstance(componentClass, 0).javaClass
            }

            'L' -> {
                while (rest.hasNext()) {
                    lookup = rest.next()
                    if (lookup == ';')
                        break
                    else if (lookup == '/')
                        buffer.append('.')
                    else
                        buffer.append(lookup)
                }
                if (lookup != ';')
                    throw IllegalArgumentException("Illegal Class Descriptor: Invalid class descriptor.")
                val clz = Class.forName(buffer.toString())
                buffer.clear()
                return clz
            }

            'V' -> return Void.TYPE
            else -> {
                if (lookup == ')')
                    return null
                else throw IllegalArgumentException("Illegal Class Descriptor.")
            }
        }
    }

    private fun methodToDescriptor(clz: Class<*>, name: String, argTypes: Array<out Class<*>>): String {
        return (clz.name + '.' + name +
                if (argTypes.isEmpty()) "()" else Arrays.stream(argTypes)
                    .map { c: Class<*>? -> if (c == null) "null" else c.descriptorString() }
                    .collect(Collectors.joining("", "(", ")")))
    }

    private fun getMethod(clz: Class<*>, methodName: String, vararg parameterClasses: Class<*>): Method {
        try {
            return clz.getDeclaredMethod(methodName, *parameterClasses)
        } catch (noMethod: NoSuchMethodException) {
            if (clz == Any::class.java)
                throw NoSuchMethodException(methodToDescriptor(clz, methodName, parameterClasses))
            val superMethod = getMethodRecruit(clz.superclass, methodName, *parameterClasses)
            if (superMethod != null)
                return superMethod
        } catch (security: SecurityException) {
            Reflection::class.logger().log(System.Logger.Level.TRACE, security)
        }
        throw NoSuchMethodException(methodToDescriptor(clz, methodName, parameterClasses))
    }

    @JvmStatic
    private fun getMethodRecruit(clz: Class<*>, methodName: String, vararg parameterClasses: Class<*>): Method? {
        try {
            return clz.getDeclaredMethod(methodName, *parameterClasses)
        } catch (noMethod: NoSuchMethodException) {
            if (clz == Any::class.java)
                return null
            val superMethod = getMethodRecruit(clz.superclass, methodName, *parameterClasses)
            if (superMethod != null)
                return superMethod
        } catch (security: SecurityException) {
            Reflection::class.logger().log(System.Logger.Level.TRACE, security)
        }
        return null
    }

    private class MethodInfo(
        private val clz: Class<*>,
        private val name: String,
        private val returnType: Class<*>,
        private val parameterClasses: Array<Class<*>>,
    ) : ScriptLinker.LinkInfo {
        override fun requiredReturnType(): Class<*> = returnType

        override fun parameterTypes(): Array<Class<*>> = parameterClasses

        override fun methodName(): String = name

        override fun ownerClass(): Class<*> = clz
        override fun toString(): String {
            return "${methodToDescriptor(clz, name, parameterClasses)}${returnType.descriptorString()}"
        }
    }
}