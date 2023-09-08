package top.kkoishi.stg.localization

import java.util.Locale

interface Localization<KeyType: Any, ValueType: Any> {
    val locale: Locale
    operator fun get(key: KeyType): ValueType?
    operator fun set(key: KeyType, value: ValueType): ValueType?

    fun registerThis()
}