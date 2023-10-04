package top.kkoishi.stg.localization

import java.io.IOException
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists

class MapLocalization private constructor(private val localizationName: String, override val locale: Locale) :
    Localization<Any, Any> {
    init {
        registerThis()
    }

    private val data: Hashtable<Any, Any> = Hashtable()

    fun load(ymlPath: String, override: Boolean = true, clear: Boolean = false): Result<MapLocalization> =
        runCatching {
            val p = Path.of(ymlPath)
            if (!p.exists())
                return Result.failure(IOException("No Such file called $ymlPath."))
            val nData = LocalizationControl.readYML(p)
            if (clear)
                data.clear()

            if (override)
                data.putAll(nData)
            else
                nData.forEach { data.putIfAbsent(it.key, it.value) }
            return Result.success(this)
        }

    override fun get(key: Any): Any? = data[key]

    override fun set(key: Any, value: Any): Any? = data.put(key, value)

    override fun registerThis() = LocalizationControl.register(localizationName, this)
}