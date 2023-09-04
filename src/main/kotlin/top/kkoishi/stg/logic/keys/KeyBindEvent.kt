package top.kkoishi.stg.logic.keys

import top.kkoishi.stg.common.entities.Object

@FunctionalInterface
interface KeyBindEvent<ObjectType, ReturnType> where ObjectType : Object {
    fun invoke(it: ObjectType): ReturnType
}

class KeyBindInvoker<ObjectType : Object>(val action: (ObjectType) -> Unit) : KeyBindEvent<ObjectType, Unit> {
    override fun invoke(it: ObjectType) = action(it)
}

class KeyBindEventWithCaller<ObjectType : Object, ReturnType>(
    private val caller: ObjectType?,
    val action: (ObjectType?) -> ReturnType,
) : KeyBindEvent<ObjectType, ReturnType> {
    override fun invoke(it: ObjectType): ReturnType {
        return action(caller)
    }
}