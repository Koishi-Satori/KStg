package top.kkoishi.stg.localization

import top.kkoishi.stg.script.reflect.Reflection
import java.lang.reflect.Field
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.collections.HashMap

@Suppress("MemberVisibilityCanBePrivate")
abstract class ClassLocalization<ConstantClass : ClassLocalization<ConstantClass>>(
    override val locale: Locale,
    val thisClass: Class<ConstantClass>,
    val ymlPath: String,
) : Localization<String, String> {
    private val constants = HashMap<String, String>()

    init {
        registerThis()
    }


    abstract fun constantFieldsName(): Array<String>?

    abstract fun reference(): ConstantClass

    override fun get(key: String): String? = constants[key]

    override fun set(key: String, value: String): String? {
        val old = constants[key]
        constants[key] = value
        return old
    }

    fun values() = constants.values

    final override fun registerThis() {
        val names = constantFieldsName()
        val fields: Array<Field> = if (names == null) {
            // get all fields through reflection.
            Reflection.getAllPossibleConstantFields(thisClass, String::class.java)
        } else
            Array(names.size) { thisClass.getField(names[it]) }

        val constantFields = ArrayDeque<Pair<Field, String>>(fields.size)
        fields.forEach {
            it.isAccessible = true
            val key: LocalizationKey? = Reflection.getAnnotation(it, LocalizationKey::class.java)
            if (key != null && key.name.isNotEmpty())
                constantFields.addLast(it to key.name)
            else
                constantFields.addLast(it to it.name)
        }

        // fill in fields.
        val data = LocalizationControl.readYML(Path.of(ymlPath))
        constantFields.forEach {
            val value = find(it.second, data)
            Reflection.setField(reference(), it.first, value)
        }
    }

    private fun find(name: String, data: MutableMap<String, String>): String {
        var variant = name
        var value = data[variant]
        if (value != null) {
            constants[variant] = value
            return value
        }

        variant = name.lowercase()
        value = data[variant]
        if (value != null) {
            constants[variant] = value
            return value
        }

        variant = name.uppercase()
        value = data[variant]
        if (value != null) {
            constants[variant] = value
            return value
        }

        return "${name}_value"
    }
}