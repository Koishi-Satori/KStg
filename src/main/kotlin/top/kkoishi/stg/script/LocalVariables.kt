package top.kkoishi.stg.script

import kotlin.reflect.KClass

abstract class LocalVariables(val scopeName: String) {
    init {
        // may produce bugs.
        // if you implement this and use your own constructor, please invoke super::init at last.
        scopes[scopeName] = this
    }

    protected val vars: HashMap<String, Var> = HashMap()

    fun containsVar(name: String) = vars[name] != null

    fun getVar(name: String): Var = vars[name] ?: throw NoSuchElementException("No such var called $name")

    operator fun get(name: String): Var? = vars[name]

    fun <T : Any> setVar(name: String, value: T, klz: KClass<out T> = value::class) {
        var variable = vars[name]
        if (variable == null)
            variable = Var(klz, value)
        else
            variable.value = value
        vars[name] = variable
    }

    override fun toString(): String {
        return "LocalVariables(scopeName='$scopeName', vars=$vars)"
    }


    companion object {
        @JvmStatic
        private val scopes: HashMap<String, LocalVariables> = HashMap()

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        operator fun <T> get(scopeName: String): T where T : LocalVariables {
            return scopes[scopeName] as T
        }

        @JvmStatic
        operator fun <T> set(scopeName: String, vars: T) where T : LocalVariables {
            scopes[scopeName] = vars
        }
    }

    data class Var(val type: KClass<out Any>, var value: Any) {
        override fun toString(): String = value.toString()
    }
}