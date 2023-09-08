package top.kkoishi.stg.localization

import java.util.*

class MapLocalization private constructor(private val localizationName: String, override val locale: Locale) :
    Localization<Any, Any> {
    init {
        registerThis()
    }

    override fun get(key: Any): Any? {
        TODO("Not yet implemented")
    }

    override fun set(key: Any, value: Any): Any? {
        TODO("Not yet implemented")
    }

    override fun registerThis() = LocalizationControl.register(localizationName, this)
}