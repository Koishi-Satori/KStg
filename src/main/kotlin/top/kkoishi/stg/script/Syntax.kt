@file:Suppress("unused")
@file:JvmName("ScriptSyntax")

package top.kkoishi.stg.script

import top.kkoishi.stg.exceptions.InternalError
import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.script.VM.parseVMInstructions
import java.io.IOException
import java.io.Reader
import kotlin.reflect.KClass

internal enum class Type {
    KEY,
    EQUAL, BG, SM, BG_EQ, SM_EQ,
    CALL, FUNC_CALL, VAR, STRING,
    L_BLANKET_L, L_BLANKET_R, NUMBER
}

internal data class Token(val content: String, val type: Type)

internal val L_BLANKET_L = Token("", Type.L_BLANKET_L)
internal val L_BLANKET_R = Token("", Type.L_BLANKET_R)
internal val EQUAL = Token("", Type.EQUAL)
internal val BG = Token("", Type.BG)
internal val BG_EQ = Token("", Type.BG_EQ)
internal val SM = Token("", Type.SM)
internal val SM_EQ = Token("", Type.SM_EQ)

internal class ReaderIterator(private val rd: Reader) : CharIterator() {
    private var peek = 0

    init {
        read()
    }

    private fun read() {
        peek = try {
            rd.read()
        } catch (e: IOException) {
            -1
        }
    }

    override fun nextChar(): Char {
        if (peek == -1)
            throw NoSuchElementException("The iterator is end.")
        else {
            val c = peek.toChar()
            read()
            return c
        }
    }

    override fun hasNext(): Boolean = peek != -1

    fun close() = rd.close()
}

internal abstract class Lexer(protected val rest: CharIterator) {
    var line = 0
    var col = 0

    abstract fun next(): Token
    abstract fun hasNext(): Boolean
}

internal abstract class Parser(val lexer: Lexer) {
    lateinit var tk: Token

    abstract fun parse(): InstructionSequence

    open fun check(vararg types: Type): String {
        if (!lexer.hasNext())
            throw ScriptException("Illegal script format: expect ${types.contentToString()} at ${lexer.line}:${lexer.col}, but got nothing")
        tk = lexer.next()
        types.forEach { if (tk.type == it) return tk.content }
        throw ScriptException("Illegal script format: expect ${types.contentToString()} at ${lexer.line}:${lexer.col}, but got ${tk.type}")
    }

    open fun checkMixedParameters(): Pair<String, String> {
        val key = check(Type.KEY)
        check(Type.EQUAL)
        if (!lexer.hasNext())
            throw ScriptException("Unfinished parameter: at ${lexer.line}:${lexer.col}")
        tk = lexer.next()
        return when (tk.type) {
            Type.STRING, Type.NUMBER -> key to tk.content
            Type.VAR -> key to "$${tk.content}"
            else -> throw ScriptException("The right side of property must be STRING or NUMBER: at ${lexer.line}:${lexer.col}")
        }
    }

    protected fun checkMixedParameterValue(): String {
        if (!lexer.hasNext())
            throw ScriptException("Unfinished parameter: at ${lexer.line}:${lexer.col}")
        tk = lexer.next()
        return when (tk.type) {
            Type.STRING, Type.NUMBER -> tk.content
            Type.VAR -> "$${tk.content}"
            else -> throw ScriptException("The right side of property must be STRING or NUMBER: at ${lexer.line}:${lexer.col}")
        }
    }

    open fun checkParameters(): Pair<String, String> {
        val key = check(Type.KEY)
        check(Type.EQUAL)
        if (!lexer.hasNext())
            throw ScriptException("Unfinished property: at ${lexer.line}:${lexer.col}")
        tk = lexer.next()
        return when (tk.type) {
            Type.STRING, Type.NUMBER -> key to tk.content
            else -> throw ScriptException("The right side of property must be STRING or NUMBER: at ${lexer.line}:${lexer.col}")
        }
    }
}

internal enum class ParseType(val isNode: Boolean = false) {
    // Node Types
    INSTRUCTION(true), ROOT(true),

    // Parameter Types
    PARAMETER, OPTIONAL_PARAMETER, COMPLEX_PARAMETER(true)
}

internal sealed interface ParseInfo {
    val name: String
    val type: ParseType
    fun isNode() = type.isNode
    fun children(): Iterator<ParseInfo>
    fun amount(): Int

    sealed class NodeInfo(override val type: ParseType, override val name: String) : ParseInfo
    sealed class ParameterInfo(
        override val type: ParseType,
        override val name: String,
    ) : ParseInfo {
        override fun children(): Iterator<ParseInfo> = EMPTY_ITER
        final override fun amount(): Int = ONLY
    }

    /* ---------------------------------  Node Info  --------------------------------- */

    class RootInfo(name: String, vararg initialContext: InstructionInfo) : NodeInfo(ParseType.ROOT, name) {
        private val nodes = ArrayDeque<InstructionInfo>(initialContext.size)

        init {
            initialContext.forEach(nodes::addLast)
        }

        override fun children(): Iterator<InstructionInfo> = nodes.iterator()
        override fun amount(): Int = ONLY
        fun add(node: InstructionInfo) = nodes.addLast(node)
    }

    abstract class InstructionInfo(name: String) : NodeInfo(ParseType.INSTRUCTION, name) {
        private val nodes = ArrayDeque<ParameterInfo>()

        /**
         * Allocate an Instruction instance using parameters stored in the given Map [parameters].
         *
         * @param parameters where the parameters of the Instruction.
         * @return allocated instance. Or null if any parameter is wrong.
         */
        abstract fun allocate(parameters: Map<String, Any>): VM.Instruction?

        final override fun children(): Iterator<ParameterInfo> = nodes.iterator()
        final override fun amount(): Int = UNDEFINED
        fun add(node: ParameterInfo) = nodes.addLast(node)
    }

    /* ---------------------------------  Parameter Info  --------------------------------- */

    /**
     * Special.
     */
    class ComplexParameterInfo(name: String) : ParameterInfo(ParseType.COMPLEX_PARAMETER, name)
    class CommonParameterInfo(name: String) : ParameterInfo(ParseType.PARAMETER, name)
    class OptionalParameterInfo(name: String, val defaultValue: Any) : ParameterInfo(ParseType.OPTIONAL_PARAMETER, name)

    companion object {
        private val EMPTY_ITER: Iterator<ParseInfo> = object : Iterator<ParseInfo> {
            override fun hasNext(): Boolean = false
            override fun next(): ParseInfo = throw InternalError("No more ParseInfo rest!")
        }
        private const val ONLY = 1
        private const val UNDEFINED = -1
    }
}

@Suppress("MemberVisibilityCanBePrivate")
internal abstract class InfoParser(lexer: Lexer, rootName: String, val scopeName: String) : Parser(lexer) {
    protected val root: ParseInfo.RootInfo = ParseInfo.RootInfo(rootName)
    abstract fun buildParseInfo()

    @Throws(ScriptException::class)
    abstract fun parseComplexParameter(name: String): InstructionSequence

    @Throws(ScriptException::class)
    open fun parseActual(): InstructionSequence {
        // check head
        checkRoot()

        // check instructions
        val seq = parseInstructions()
        if (tk.type == Type.L_BLANKET_R)
            return seq
        throw ScriptException("Uncompleted script: at ${lexer.line}:${lexer.col}")
    }

    @Throws(ScriptException::class)
    protected fun checkRoot() {
        if (check(Type.KEY) != root.name)
            throw ScriptException("Illegal script format: Require correct ROOT_NAME '${root.name}' at ${lexer.line}:${lexer.col}")
        check(Type.EQUAL)
        check(Type.L_BLANKET_L)
    }

    @Throws(ScriptException::class)
    protected fun parseInstructions(): InstructionSequence {
        val seq = ArrayDeque<VM.Instruction>(16)
        var key: String

        while (lexer.hasNext()) {
            tk = lexer.next()
            if (tk.type == Type.L_BLANKET_R)
                break
            if (tk.type != Type.KEY)
                throw ScriptException("Illegal script format: expect a KEY at ${lexer.line}:${lexer.col}")
            key = tk.content

            check(Type.EQUAL)
            check(Type.L_BLANKET_L)

            seq.addLast(processInstruction(key))
        }

        val result = seq.toTypedArray()
        seq.clear()
        return result
    }

    @Throws(ScriptException::class)
    private fun processInstruction(key: String): VM.Instruction {
        var inst: VM.Instruction?
        root.children().forEach {
            inst = processInstructionInfo(key, it)
            if (inst != null)
                return inst!!
        }
        return parseVMInstructions(key, scopeName)
    }

    @Throws(ScriptException::class)
    private fun processInstructionInfo(name: String, inst: ParseInfo.InstructionInfo): VM.Instruction? {
        if (name == inst.name) {
            val parameters = parseInstructionParameters(inst)
            val instruction: VM.Instruction? = inst.allocate(parameters)
            if (instruction != null) {
                parameters.clear()
                return instruction
            }
        } else
            return null
        return null
    }

    @Throws(ScriptException::class)
    private fun parseInstructionParameters(inst: ParseInfo.InstructionInfo): HashMap<String, Any> {
        val complexes = ArrayDeque<String>(4)
        val parameters = ArrayDeque<String>(4)
        val optionals = ArrayDeque<ParseInfo.OptionalParameterInfo>(4)
        inst.children().forEach {
            when (it.type) {
                ParseType.PARAMETER -> parameters.addLast(it.name)
                ParseType.COMPLEX_PARAMETER -> complexes.addLast(it.name)
                else -> optionals.addLast(it as ParseInfo.OptionalParameterInfo)
            }
        }

        val instParameters = HashMap<String, Any>()
        while (lexer.hasNext()) {
            tk = lexer.next()
            if (tk.type == Type.L_BLANKET_R)
                break
            val key = tk.content
            check(Type.EQUAL)

            instParameters[key] = if (complexes.contains(key))
                parseComplexParameter(key)
            else if (parameters.contains(key))
                checkMixedParameterValue()
            else {
                var optional: ParseInfo.OptionalParameterInfo? = null
                optionals.forEach {
                    if (it.name == key)
                        optional = it
                }

                if (optional != null)
                    checkMixedParameterValue()
                else
                    throw ScriptException("Illegal script name: Can not parse parameter name $key at ${lexer.line}:${lexer.col}")
            }
        }

        optionals.forEach {
            val value = instParameters[it.name]
            if (value == null)
                instParameters[it.name] = it.defaultValue
        }

        complexes.clear()
        parameters.clear()
        optionals.clear()
        return instParameters
    }

    @Throws(ScriptException::class)
    override fun parse(): InstructionSequence {
        // build ParseInfo
        buildParseInfo()
        return parseActual()
    }
}

/**
 * Check if the char is legal for variables.
 *
 * @return if the char is legal for variables.
 */
internal fun Char.isVarChar(): Boolean {
    return isLetter() || this == '_' || this == ':'
}

fun String.processEscapes(): String {
    if (isEmpty())
        return ""
    val chars: CharArray = toCharArray()
    val length = chars.size
    var from = 0
    var to = 0
    while (from < length) {
        var ch = chars[from++]
        if (ch == '\\') {
            ch = if (from < length) chars[from++] else '\u0000'
            when (ch) {
                'b' -> ch = '\b'
                'f' -> ch = 12.toChar()
                'n' -> ch = '\n'
                'r' -> ch = '\r'
                's' -> ch = ' '
                't' -> ch = '\t'
                '\'', '\"', '\\' -> {}
                '0', '1', '2', '3', '4', '5', '6', '7' -> {
                    val limit = Integer.min(from + if (ch <= '3') 2 else 1, length)
                    var code = ch.code - '0'.code
                    while (from < limit) {
                        ch = chars[from]
                        if (ch < '0' || '7' < ch) {
                            break
                        }
                        from++
                        code = code shl 3 or ch.code - '0'.code
                    }
                    ch = code.toChar()
                }

                '\n' -> continue
                '\r' -> {
                    if (from < length && chars[from] == '\n')
                        from++
                    continue
                }

                else -> {
                    val msg = String.format(
                        "Invalid escape sequence: \\%c \\\\u%04X",
                        ch, ch.code
                    )
                    throw IllegalArgumentException(msg)
                }
            }
        }
        chars[to++] = ch
    }
    return String(chars, 0, to)
}

internal open class ResourcesScriptLexer(rest: CharIterator) : Lexer(rest) {
    private var lookup = '\u0000'

    @Throws(NoSuchElementException::class)
    override fun next(): Token {
        return if (rest.hasNext())
            next0()
        else
            throw NoSuchElementException()
    }

    private fun next0(): Token {
        lookup = nextChar()
        val tk = when (lookup) {
            '=' -> EQUAL
            '{' -> L_BLANKET_L
            '}' -> L_BLANKET_R
            '"' -> string()
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> number()
            '$' -> variable()
            else -> {
                if (lookup.isKeyChar())
                    key()
                else
                    return next0()
            }
        }
        return tk
    }

    private fun string(): Token {
        lookup = nextChar(true)
        val sb = StringBuilder()
        while (lookup != '"') {
            sb.append(lookup)
            lookup = nextChar(true)
        }
        return Token(sb.toString().processEscapes(), Type.STRING)
    }

    private fun key(): Token {
        val sb = StringBuilder()
        while (lookup.isKeyChar()) {
            sb.append(lookup)
            lookup = nextChar()
        }
        return Token(sb.toString(), Type.KEY)
    }

    private fun number(): Token {
        val sb = StringBuilder()
        while (lookup.isDigit() || lookup == '.') {
            sb.append(lookup)
            lookup = nextChar()
        }
        return Token(sb.toString(), Type.NUMBER)
    }

    private fun variable(): Token {
        val sb = StringBuilder()
        var blanketFlag = false
        lookup = nextChar()
        if (lookup == '(')
            blanketFlag = true
        while (lookup.isVarChar()) {
            if (lookup == ')' && blanketFlag)
                break
            sb.append(lookup)
            lookup = nextChar()
        }
        return Token(sb.toString(), Type.VAR)
    }

    @Throws(NoSuchElementException::class)
    private fun nextChar(lexString: Boolean = true): Char {
        col++
        var c = rest.nextChar()

        if (c == '\n') {
            col = 0
            line++
        }
        if (lexString)
            return c

        if (c == '\n')
            c = rest.nextChar()
        while (c.isWhitespace()) {
            col++
            c = rest.nextChar()
        }
        return c
    }

    private fun Char.isKeyChar(): Boolean {
        return !(isWhitespace() || this == '\r' || this == '\n')
    }

    override fun hasNext(): Boolean {
        return rest.hasNext()
    }
}

internal object ScriptConstants {
    const val SCOPE_GFX_LOADER = "gfx_loader"
    const val SCOPE_AUDIO_LOADER = "audio_loader"

    @JvmStatic
    private val storedInstructionInfo = HashMap<KClass<out VM.Instruction>, ParseInfo.InstructionInfo>()

    @JvmStatic
    fun registerInstructionInfo(kClass: KClass<out VM.Instruction>, info: ParseInfo.InstructionInfo) {
        storedInstructionInfo[kClass] = info
    }

    @JvmStatic
    fun getInstructionInfo(kClass: KClass<out VM.Instruction>) = storedInstructionInfo[kClass]
}

internal object ResourcesInstructions {
    internal operator fun InstructionSequence.invoke(begin: Int, end: Int, varName: String?, vars: LocalVariables) =
        if (varName != null)
            (begin..end).forEach {
                vars.setVar<Number>(varName, it)
                forEach { inst -> if (inst.needVars()) inst(vars) else inst() }
            }
        else
            (begin..end).forEach { _ -> forEach { inst -> inst(vars) } }

    internal class Loop(
        private val begin: Int,
        private val end: Int,
        private val name: String?,
        private val instructions: InstructionSequence,
    ) : VM.Instruction(0x20) {
        override fun needVars(): Boolean = true

        override fun invoke(vars: LocalVariables) {
            instructions(begin, end, name, vars)
        }
    }

    internal class LoopInfo : ParseInfo.InstructionInfo("loop") {
        init {
            add(ParseInfo.OptionalParameterInfo("begin", 0))
            add(ParseInfo.OptionalParameterInfo("end", 0))
            add(ParseInfo.CommonParameterInfo("var_name"))
            add(ParseInfo.ComplexParameterInfo("load"))
        }

        @Suppress("UNCHECKED_CAST")
        override fun allocate(parameters: Map<String, Any>): VM.Instruction {
            return Loop(
                parameters["begin"]!!.toString().toInt(),
                parameters["end"]!!.toString().toInt(),
                parameters["var_name"]!!.toString(),
                (parameters["load"]!! as InstructionSequence)
            )
        }
    }
}