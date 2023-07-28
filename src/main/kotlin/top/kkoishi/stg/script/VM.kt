package top.kkoishi.stg.script

import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.logic.Parser
import top.kkoishi.stg.logic.Type
import top.kkoishi.stg.logic.isVarChar
import java.math.BigDecimal
import java.math.BigInteger

object VM {
    @JvmStatic
    private val DEC_FLOAT_MAX = BigDecimal(Float.MAX_VALUE.toDouble())

    @JvmStatic
    private val DEC_DOUBLE_MAX = BigDecimal(Double.MAX_VALUE)

    @JvmStatic
    private val INT_SHORT_MAX = BigInteger("7fff", 16)

    @JvmStatic
    private val INT_SHORT_MIN = BigInteger("-8000", 16)

    @JvmStatic
    private val INT_INT_MAX = BigInteger("7fffffff", 16)

    @JvmStatic
    private val INT_INT_MIN = BigInteger("-80000000", 16)

    @JvmStatic
    private val INT_LONG_MAX = BigInteger("7fffffffffffffff", 16)

    @JvmStatic
    private val INT_LONG_MIN = BigInteger("-8000000000000000", 16)

    internal fun Parser.parseVMInstructions(key: String, var_scope: String): Instruction {
        return when (key) {
            "set_var" -> set_var(this, var_scope)
            "mul_var" -> mul_var(this, var_scope)
            else -> throw ScriptException("Incorrect key: $key at ${lexer.line}:${lexer.col}")
        }
    }

    private fun set_var(parser: Parser, var_scope: String): SNInstruction.Companion.set_var {
        with(parser) {
            val (key1, value1: String) = checkProperty()
            val (_, value2: String) = checkProperty()
            check(Type.L_BLANKET_R)

            return if (key1 == "name") {
                SNInstruction.Companion.set_var(value1, value2)
            } else {
                SNInstruction.Companion.set_var(value2, value1)
            }
        }
    }

    private fun mul_var(parser: Parser, var_scope: String): SNInstruction.Companion.mul_var {
        with(parser) {
            val (key1, value1: String) = checkProperty()
            val (_, value2: String) = checkProperty()
            check(Type.L_BLANKET_R)
            return if (key1 == "name")
                SNInstruction.Companion.mul_var(value1, value2)
            else
                SNInstruction.Companion.mul_var(value2, value1)
        }
    }

    internal fun parseVarName(name: String): Pair<String?, String> {
        val arr = name.removePrefix("$").split("::")
        return if (arr.size != 2)
            null to arr[0]
        else
            arr[0] to arr[1]
    }

    internal inline fun <reified T> processVars(string: String, defaultScope: String): T {
        when (T::class) {
            String::class -> {
                val sb = StringBuilder()
                val varBuf = StringBuilder()
                val iter = string.removePrefix("$").iterator()
                var flag = 0

                while (iter.hasNext()) {
                    val c = iter.nextChar()
                    if (c == '$')
                        flag = 1
                    else if (flag == 1 && c == '(')
                        flag = 2
                    else if ((flag == 2 && c == ')') || (flag == 1 && !c.isVarChar())) {
                        var (scope, name) = parseVarName(varBuf.toString())
                        if (scope == null)
                            scope = defaultScope
                        sb.append(LocalVariables.get<LocalVariables>(scope).getVar(name).value.toString())

                        flag = 0
                        varBuf.clear()
                    } else {
                        if (flag > 0)
                            varBuf.append(c)
                        else
                            sb.append(c)
                    }
                }
                if (flag > 0 && varBuf.isNotEmpty()) {
                    var (scope, name) = parseVarName(varBuf.toString())
                    if (scope == null)
                        scope = defaultScope
                    sb.append(LocalVariables.get<LocalVariables>(scope).getVar(name).value.toString())
                    varBuf.clear()
                }
                return sb.toString() as T
            }

            Int::class -> {
                return if (string.startsWith("$")) {
                    var (scope, name) = parseVarName(string.removePrefix("$"))
                    if (scope == null)
                        scope = defaultScope

                    (LocalVariables.get<LocalVariables>(scope).getVar(name).value as Number).toInt() as T
                } else
                    string.toInt() as T
            }

            Number::class -> {
                return if (string.startsWith("$")) {
                    var (scope, name) = parseVarName(string)
                    if (scope == null)
                        scope = defaultScope

                    (LocalVariables.get<LocalVariables>(scope).getVar(name).value as Number) as T
                } else string.toNumber() as T
            }

            else -> throw ScriptException("This should not happen")
        }
    }

    private operator fun Number.times(other: Number): Number {
        // get the data type of "this", then times them.
        // only support: Float, Double, Int, Long, Short, Byte, BigInteger, BigDecimal
        return when (this.javaClass) {
            Int::class.java, Integer::class.java -> toInt() * other.toInt()
            Long::class.java, java.lang.Long::class.java -> toLong() * other.toLong()
            Float::class.java, java.lang.Float::class.java -> toFloat() * other.toFloat()
            Double::class.java, java.lang.Double::class.java -> toDouble() * other.toDouble()
            Short::class.java, java.lang.Short::class.java -> toShort() * other.toShort()
            Byte::class.java, java.lang.Byte::class.java -> toByte() * other.toByte()
            BigInteger::class.java -> (this as BigInteger).multiply(other as BigInteger)
            BigDecimal::class.java -> (this as BigDecimal).multiply(other as BigDecimal)
            else -> throw UnsupportedOperationException("The number type that is not expected: ${this.javaClass}")
        }
    }

    fun String.toNumber(): Number {
        if (contains('.')) {
            // parse float number
            val dec = BigDecimal(this)
            val abs = dec.abs()
            return when {
                abs <= DEC_FLOAT_MAX -> dec.toFloat()
                abs <= DEC_DOUBLE_MAX -> dec.toFloat()
                else -> dec
            }
        } else {
            val int = BigInteger(this)
            return if (int.signum() == -1) when {
                int >= INT_SHORT_MIN -> int.toShort()
                int >= INT_INT_MIN -> int.toInt()
                int >= INT_LONG_MIN -> int.toLong()
                else -> int
            } else when {
                int <= INT_SHORT_MAX -> int.toShort()
                int <= INT_INT_MAX -> int.toInt()
                int <= INT_LONG_MAX -> int.toLong()
                else -> int
            }
        }
    }

    internal abstract class Instruction(val opcode: Byte) {
        open operator fun invoke() {}
        open operator fun invoke(vars: LocalVariables) {}

        abstract fun needVars(): Boolean

        companion object {
            const val SET_VAR: Byte = 0x10
            const val ADD_VAR: Byte = 0x11
            const val MUL_VAR: Byte = 0x12
            const val DIV_VAR: Byte = 0x13
        }
    }

    internal open class SNInstruction(
        opcode: Byte,
        protected val string: String,
        protected val number: String,
    ) : Instruction(opcode) {

        override fun needVars() = true

        companion object {
            internal class set_var(string: String, number: String) : SNInstruction(SET_VAR, string, number) {
                override fun invoke(vars: LocalVariables) {
                    vars.setVar(string, processVars<Number>(number, vars.scopeName))
                }
            }

            internal class add_var(string: String, number: String) : SNInstruction(ADD_VAR, string, number)
            internal class mul_var(string: String, number: String) : SNInstruction(MUL_VAR, string, number) {
                override fun invoke(vars: LocalVariables) = with(vars[string]) {
                    if (this == null)
                        vars.setVar(string, 0 as Number)
                    else
                        vars.setVar(string, (value as Number) * processVars(number, vars.scopeName))
                }
            }

            internal class div_var(string: String, number: String) : SNInstruction(DIV_VAR, string, number)
        }
    }

}

internal typealias InstructionSequence = Array<VM.Instruction>
