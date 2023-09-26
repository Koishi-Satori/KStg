package top.kkoishi.stg.boot

import top.kkoishi.stg.boot.jvm.KStgEngineMain
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.Threads
import java.io.IOException
import java.lang.StringBuilder
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.forEachLine
import kotlin.io.path.isRegularFile

/**
 * A simple setting processor.
 *
 * @author KKoishi_
 */
abstract class Settings<ValueType>(fileName: String) {
    val file: Path = Path.of(fileName)

    init {
        if (!file.exists() && !file.isRegularFile())
            throw ExceptionInInitializerError("File $file does not exist.")
    }

    class Handler<ValueType>(val key: String, val handle: (ValueType) -> Unit) {
        operator fun invoke(value: ValueType) = handle(value)
    }

    /**
     * Read settings.
     *
     * @return if success.
     */
    abstract fun read(): Boolean

    /**
     * Add setting entry handler.
     *
     * @param key the key
     * @param handle how to handle.
     */
    abstract fun addHandler(key: String, handle: (ValueType) -> Unit)

    fun load() = entries().forEach { (key, value) -> this[key](value) }

    abstract fun getValue(key: String): ValueType

    protected abstract fun entries(): Array<Pair<String, ValueType>>
    protected abstract operator fun get(key: String): Handler<ValueType>

    /**
     * The key of ini parameters will be "section_name::key", and if there is no section, it will be "key".
     *
     * @author KKoishi_
     */
    class INI(fileName: String) : Settings<String>(fileName) {
        private companion object {
            @JvmStatic
            val EMPTY_HANDLER = Handler<String>("") {}
        }

        private var sectionName = ""
        private val entries = LinkedHashMap<String, String>()
        private val handlers = LinkedHashMap<String, Handler<String>>()

        init {
            addHandler("engine::workdir", Threads::workdir)
            addHandler("engin::plugin_dir", KStgEngineMain::plugin_dir)
        }

        override fun read(): Boolean {
            try {
                file.forEachLine(Charsets.UTF_8, this::readLine)
            } catch (e: Exception) {
                Settings::class.logger().log(System.Logger.Level.ERROR, e)
                return false
            }
            return true
        }

        private fun readLine(line: String) {
            when {
                line.startsWith(';') -> Settings::class.logger()
                    .log(System.Logger.Level.INFO, "Skip ini comment: $line")

                line.startsWith('[') -> {
                    if (!line.matches(Regex("^\\[.+]\r?\n?")))
                        throw IOException("Invalid ini line: $line")
                    val rightIndex = line.indexOf(']')
                    val name = line.substring(1, rightIndex)
                    sectionName = name
                }

                else -> {
                    if (!line.matches(Regex("^[^\\s=]+\\s*=\\s*.+")))
                        throw IOException("Invalid ini line: $line")
                    parseParameter(line)
                }
            }
        }

        private fun parseParameter(line: String) {
            val buf = StringBuilder()
            val rest = line.iterator()
            var key = ""
            var flag = false

            var c: Char = rest.nextChar()
            buf.append(c)

            while (rest.hasNext()) {
                c = rest.nextChar()
                if (c == ' ')
                    continue
                if (!flag) {
                    if (c == '=') {
                        key = buf.toString()
                        buf.clear()
                        flag = true
                        continue
                    }
                }
                buf.append(c)
            }

            entries[if (sectionName.isEmpty()) key else "$sectionName::$key"] = buf.toString()
        }

        override fun entries(): Array<Pair<String, String>> = entries.map { it.key to it.value }.toTypedArray()

        override fun getValue(key: String) = entries[key] ?: ""

        override fun get(key: String): Handler<String> = handlers[key] ?: EMPTY_HANDLER

        override fun addHandler(key: String, handle: (String) -> Unit) {
            handlers[key] = Handler(key, handle)
        }
    }
}