package top.kkoishi.stg.localization

import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.script.execution.contentIterator
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.exists

internal object LocalizationControl {
    @JvmStatic
    private val storedLocalization = HashMap<String, ArrayDeque<Localization<*, *>>>()

    @JvmStatic
    @JvmName(" getLocalization ")
    @Suppress("UNCHECKED_CAST")
    internal fun <KeyType : Any, ValueType : Any> getLocalization(
        localizationName: String,
        locale: Locale,
    ): Localization<KeyType, ValueType>? {
        val localizations = storedLocalization[localizationName] ?: return null
        localizations.forEach {
            if (it.locale == locale)
                return it as Localization<KeyType, ValueType>
        }
        return null
    }

    @JvmStatic
    @JvmName(" register ")
    internal fun <KeyType : Any, ValueType : Any> register(
        localizationName: String,
        localization: Localization<KeyType, ValueType>,
    ) {
        var localizations = storedLocalization[localizationName]
        if (localizations == null) {
            localizations = ArrayDeque()
            localizations.addLast(localization)
            storedLocalization[localizationName] = localizations
        } else {
            val locale = localization.locale
            var replaceIndex = -1

            localizations.forEachIndexed { index, it ->
                if (it.locale == locale) {
                    replaceIndex = index
                    return@forEachIndexed
                }
            }

            if (replaceIndex != -1)
                localizations[replaceIndex] = localization
            else
                localizations.addLast(localization)
        }
    }

    @JvmStatic
    @JvmName(" readYML ")
    internal fun readYML(ymlPath: Path): MutableMap<String, String> {
        if (!ymlPath.exists())
            throw ScriptException("The yaml $ymlPath does not exist.")

        return SimpleYMLAnalysis(ymlPath.contentIterator()).analysis()
    }

    private class SimpleYMLAnalysis(val rest: CharIterator) {
        fun analysis(): HashMap<String, String> {
            val map = HashMap<String, String>()
            val key = StringBuilder()
            val value = StringBuilder()
            var flagIndexingKey = true
            var flagInString = false

            fun tryPutMap() {
                if (key.isNotEmpty()) {
                    map[key.toString()] = value.toString()
                    flagIndexingKey = true
                    flagInString = false
                    key.clear()
                    value.clear()
                }
            }

            while (rest.hasNext()) {
                var c = rest.nextChar()

                if (c.isWhitespace() && !flagInString)
                    continue
                else if (c == '\n' || c == '\r') {
                    flagIndexingKey = true
                    tryPutMap()
                } else if (c == ':') {
                    if (flagIndexingKey)
                        flagIndexingKey = false
                } else if (c == '#') {
                    // skip comments
                    while (rest.hasNext()) {
                        c = rest.nextChar()
                        if (c == '\n' || c == '\r') {
                            tryPutMap()
                            break
                        }
                    }
                } else {
                    if (flagIndexingKey)
                        key.append(c)
                    else {
                        flagInString = true
                        value.append(c)
                    }
                }
            }

            tryPutMap()
            return map
        }
    }
}