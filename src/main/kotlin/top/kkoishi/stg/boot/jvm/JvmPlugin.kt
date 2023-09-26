package top.kkoishi.stg.boot.jvm

/**
 * To implement this class, you need an static method: ```fun getInstance(): JvmPlugin``` to allocate the instance, or the
 * engine will try to use unsafe.
 */
interface JvmPlugin {
    fun main(args: Array<String>)

    fun info(): Array<String>
}