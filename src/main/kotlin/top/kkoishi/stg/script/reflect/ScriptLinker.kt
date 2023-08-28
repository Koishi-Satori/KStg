package top.kkoishi.stg.script.reflect

import top.kkoishi.stg.exceptions.ScriptException
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

    @Suppress("UNCHECKED_CAST")
    fun <ParameterType, ReturnType> function1Allocator(): (MethodHandle?, Method?) -> ((ParameterType) -> ReturnType) {
        return { methodHandle: MethodHandle?, method: Method? ->
            if (methodHandle != null) { parameter: ParameterType ->
                methodHandle(parameter) as ReturnType
            } else if (method != null) { parameter: ParameterType ->
                method(null, parameter) as ReturnType
            } else {
                throw ScriptException("Can not link to method, warnings can be found in last log.")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <ParameterType1, ParameterType2, ReturnType> function2Allocator():
                (MethodHandle?, Method?) -> ((ParameterType1, ParameterType2) -> ReturnType) {
        return { methodHandle: MethodHandle?, method: Method? ->
            if (methodHandle != null) { parameter1: ParameterType1, parameter2: ParameterType2 ->
                methodHandle(parameter1, parameter2) as ReturnType
            } else if (method != null) { parameter1: ParameterType1, parameter2: ParameterType2 ->
                method(null, parameter1, parameter2) as ReturnType
            } else {
                throw ScriptException("Can not link to method, warnings can be found in last log.")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <ReturnType> noParameterFunctionAllocator(): (MethodHandle?, Method?) -> (() -> ReturnType) {
        return { methodHandle: MethodHandle?, method: Method? ->
            if (methodHandle != null) { ->
                methodHandle() as ReturnType
            } else if (method != null) { ->
                method(null) as ReturnType
            } else {
                throw ScriptException("Can not link to method, warnings can be found in last log.")
            }
        }
    }
}