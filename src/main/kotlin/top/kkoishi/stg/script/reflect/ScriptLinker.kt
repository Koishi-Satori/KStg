package top.kkoishi.stg.script.reflect

import java.lang.invoke.MethodHandle
import java.lang.reflect.Method

object ScriptLinker {
    interface LinkInfo {
        fun requiredReturnType(): Class<*>
        fun parameterTypes(): Array<Class<*>>
        fun methodName(): String
        fun ownerClass(): Class<*>
    }

    fun <T> bind(descriptor: String, allocator: (MethodHandle?, Method?) -> T): T {
        return Reflection.parseMethod(descriptor, allocator)
    }
}