@file:Suppress("unused")
@file:JvmName("  ScriptSyntax")

package top.kkoishi.stg.script.execution

import top.kkoishi.stg.exceptions.InternalError
import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.script.LocalVariables
import top.kkoishi.stg.script.execution.VM.parseVMInstructions
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset
import java.nio.file.Path
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.io.path.inputStream
import kotlin.reflect.KClass

/**
 * The Token type.
 *
 * @author KKoishi_
 */
internal enum class Type {
    /*----------------------- Non-Constant Token Type ------------------------*/
    /**
     * A token type represented the key of key-value entry.
     *
     * e.g. the name is a key: name = "xxx" or name = {}
     */
    KEY,
    CALL, FUNC_CALL,

    /**
     * The variables.
     *
     * if you want to use a variable, you should use $variable_name or $(variable_name).
     *
     * The variable has its [LocalVariables], and its name can be local_variables_name::variable_name.
     */
    VAR,

    /**
     * The strings.
     *
     * You can use variables in strings using "xxx$variable_name" or "xxx$(variable_name)xxx", and the value of
     * the variable will be inserted to the string.
     */
    STRING,

    /**
     * The numbers.
     *
     * This engine use Double, [BigDecimal] for floating numbers, and Int, Long, [BigInteger] for interval numbers.
     */
    NUMBER,


    /*----------------------- Constant Token Type ------------------------*/

    /**
     * A token type represented '='
     */
    EQUAL,

    /**
     * A token type represented '>'
     */
    BG,

    /**
     * A token type represented '<'
     */
    SM,

    /**
     * A token type represented '>='
     */
    BG_EQ,

    /**
     * A token type represented '<='
     */
    SM_EQ,

    /**
     * A token type represented '{'
     */
    L_BLANKET_L,

    /**
     * A token type represented '}'
     */
    L_BLANKET_R
}

/**
 * The token used for lexing.
 *
 * @param content the contents of the token.
 * @param type the type of the token.
 * @author KKoishi_
 */
internal data class Token(val content: String, val type: Type)

/**
 * A constant token represents '{'.
 *
 * ### When you are using a token ```Token("", Type.L_BLANKET_L)```, you should use this constant instead for optimizing the performance.
 */
internal val L_BLANKET_L = Token("", Type.L_BLANKET_L)


/**
 * A constant token represents '}'.
 *
 * ### When you are using a token ```Token("", Type.L_BLANKET_R)```, you should use this constant instead for optimizing the performance.
 */
internal val L_BLANKET_R = Token("", Type.L_BLANKET_R)


/**
 * A constant token represents '='.
 *
 * ### When you are using a token ```Token("", Type.EQUAL)```, you should use this constant instead for optimizing the performance.
 */
internal val EQUAL = Token("", Type.EQUAL)


/**
 * A constant token represents '>'.
 *
 * ### When you are using a token ```Token("", Type.BG)```, you should use this constant instead for optimizing the performance.
 */
internal val BG = Token("", Type.BG)

/**
 * A constant token represents '>='.
 *
 * ### When you are using a token ```Token("", Type.BG_EQ)```, you should use this constant instead for optimizing the performance.
 */
internal val BG_EQ = Token("", Type.BG_EQ)

/**
 * A constant token represents '<'.
 *
 * ### When you are using a token ```Token("", Type.SM)```, you should use this constant instead for optimizing the performance.
 */
internal val SM = Token("", Type.SM)

/**
 * A constant token represents '<='.
 *
 * ### When you are using a token ```Token("", Type.SM_EQ)```, you should use this constant instead for optimizing the performance.
 */
internal val SM_EQ = Token("", Type.SM_EQ)

/**
 * Construct an iterator from the given Reader.
 *
 * @param rd the reader instance.
 * @author KKoishi_
 */
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

/**
 * The super class for all the lexer, which used for tokenizing the input char sequence to tokens.
 *
 * If you do not know how to implement this, please read Compile Principle and follow its instructions.
 *
 * @param rest the rest char iterator
 * @author KKoishi_
 */
internal abstract class Lexer(protected val rest: CharIterator) : Iterator<Token> {
    /**
     * The line number.
     */
    var line = 0

    /**
     * The column number
     */
    var col = 0

    abstract override fun next(): Token

    abstract override fun hasNext(): Boolean
}

/**
 * A parser use to convert the input tokens to the AST(Abstract Syntax Tree) or instructions.
 *
 * If you do not know how to implement this, please read Compile Principle and follow its instructions.
 *
 * @param lexer the lexer used for getting the input tokens.
 * @author KKoishi_
 */
internal abstract class Parser(val lexer: Lexer) {
    /**
     * The token which is traversal latest.
     */
    lateinit var tk: Token

    /**
     * Parse the tokens to a [InstructionSequence] instance.
     */
    abstract fun parse(): InstructionSequence

    /**
     * Checks if the type of next token is one of which contains in the given types.
     *
     * @param types check list.
     * @return if matches
     */
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

/**
 * The parse type if [ParseInfo].
 *
 * @param isNode is the type can be node.
 */
internal enum class ParseType(val isNode: Boolean = false) {
    // Node Types
    /**
     * The instructions.
     */
    INSTRUCTION(true),

    /**
     * The root type.
     */
    ROOT(true),

    // Parameter Types
    /**
     * Common parameter which can only use String, vaiables and Numbers.
     */
    PARAMETER,

    /**
     * Optional parameters.
     */
    OPTIONAL_PARAMETER,

    /**
     * Complex parameter which can contain instructions.
     */
    COMPLEX_PARAMETER(true)
}

/**
 * The ParseInfo is used in [InfoParser] and its subclasses, and describes how to parse the tokens.
 *
 * @author KKoishi_
 */
internal sealed interface ParseInfo {
    /**
     * The name of this Info.
     *
     * In parser, it is used for verifying the "key" token.
     */
    val name: String

    /**
     * The type of this Info.
     */
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

/**
 * A parser which can be easily extended and implemented its methods, can parse the tokens to instructions and verify
 * the key of the root node.
 *
 * @param lexer the lexer used for producing the token stream.
 * @param rootName the name of the root node.
 * @param scopeName the scope name used for [LocalVariables]
 * @author KKoishi_
 */
@Suppress("MemberVisibilityCanBePrivate")
internal abstract class InfoParser(lexer: Lexer, rootName: String, val scopeName: String) : Parser(lexer) {
    /**
     * The root node.
     */
    protected val root: ParseInfo.RootInfo = ParseInfo.RootInfo(rootName)

    /**
     * Determines how to build the parse info.
     */
    abstract fun buildParseInfo()

    /**
     * The way of parse the complex parameters to InstructionSequence.
     */
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

/**
 * Returns a string whose value is this string, with escape sequences translated as if in a string literal,
 * and this method is used for translate the escape chars in the string of scripts.
 *
 * Escape sequences are translated as follows:
 *
 * |       Escape       |      Name        |       Translation       |
 * |  :--------------:  | :--------------  | ----------------------- |
 * | \b                 |   backspace      |  U+0008                 |
 * | \t                 |  horizontal tab  |  U+0009                 |
 * | \n                 |   line feed      |  U+000A                 |
 * | \f                 |   form feed      |  U+000C                 |
 * | \r                 |  carriage return |  U+000D                 |
 * | \s                 |     space        |  U+0020                |
 * | \"                 |  double quote    |  U+0022                 |
 * | \'                 |  single quote    |  U+0027                 |
 * | \\                 |   backslash      |  U+005C                 |
 * | \0 - \377          |  octal escape    |  code point equivalents |
 * | \<line-terminator> |  continuation    |  discard                |
 *
 * ## This method does not translate Unicode escapes such as "\u2022".
 * ## Unicode escapes are translated by the Java compiler when reading input characters and are not part of the string literal specification.
 *
 * @return String with escape sequences translated.
 * @throws IllegalArgumentException when an escape sequence is malformed.
 *
 */
@Throws(IllegalArgumentException::class)
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

/**
 * Construct an iterator from the Reader.
 */
fun Reader.iterator(): CharIterator = ReaderIterator(this)

/**
 * Construct an iterator from a buffered reader created on this input stream using UTF-8 or the specified charset.
 *
 * @param charset the Charset used for the iterator.
 */
@JvmOverloads
fun InputStream.iterator(charset: Charset = Charsets.UTF_8): CharIterator = bufferedReader(charset).iterator()

/**
 * Constructs an iterator from a buffered reader created on this input stream of this file and returns it as a result,
 * using UTF-8 or the specified charset.
 *
 * @param charset the Charset used for the iterator.
 */
@JvmOverloads
fun Path.contentIterator(charset: Charset = Charsets.UTF_8): CharIterator = inputStream().iterator(charset)

/**
 * Constructs an iterator from a buffered reader created on this input stream of this file and returns it as a result,
 * using UTF-8 or the specified charset.
 *
 * @param charset the Charset used for the iterator.
 */
@JvmOverloads
fun File.contentIterator(charset: Charset = Charsets.UTF_8): CharIterator = inputStream().iterator(charset)

/**
 * The Script Lexer is used for the Resources of the game.
 */
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