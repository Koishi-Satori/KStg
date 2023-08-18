package top.kkoishi.stg.script

import top.kkoishi.stg.Loader
import top.kkoishi.stg.Loader.Companion.register
import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.Lexer
import top.kkoishi.stg.logic.Parser
import top.kkoishi.stg.logic.ReaderIterator
import top.kkoishi.stg.script.VM.parseVMInstructions
import top.kkoishi.stg.script.VM.Instruction
import top.kkoishi.stg.script.VM.processVars
import java.io.File
import java.nio.file.Path

class GFXLoader(private val root: Path) : LocalVariables("gfx_loader"), Loader {

    init {
        LocalVariables[scopeName] = this
    }

    private val logger = GFXLoader::class.logger()
    override fun loadDefinitions() {
        logger.log(System.Logger.Level.INFO, "Load textures from scripts.")
        for (path in root.toFile().listFiles()!!) {
            if (path.isFile)
                try {
                    logger.log(System.Logger.Level.INFO, "Try to load textures from $path")
                    register(path.canonicalPath.toString())
                    loadDefinitionFile(path)
                    logger.log(System.Logger.Level.INFO, "Success to load all the textures from $path")
                } catch (e: Exception) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
        }
    }

    private fun loadDefinitionFile(path: File) {
        val rd = path.bufferedReader()
        val lexer = GFXScriptLexer(ReaderIterator(rd))
        val parser = GFXScriptParser(lexer)
        val instructions = parser.parse()
        for (it in instructions) {
            try {
                if (it.needVars()) it.invoke(this) else it.invoke()
            } catch (e: Exception) {
                logger.log(System.Logger.Level.ERROR, e)
            }
        }
//        val items = parser.parseItems()
//        while (items.isNotEmpty()) {
//            val item = items.removeFirst()
//            item.load()
//        }
    }

    private class GFXScriptLexer(rest: CharIterator) : Lexer(rest) {
        private var peek = ArrayDeque<Token>()
        private var lookup = '\u0000'

        @Throws(NoSuchElementException::class)
        override fun next(): Token {
            return if (peek.isNotEmpty())
                peek.removeFirst()
            else if (rest.hasNext())
                next0()
            else
                throw NoSuchElementException()
        }

        @Throws(NoSuchElementException::class)
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
            return peek.isEmpty() && rest.hasNext()
        }
    }

    private inner class GFXScriptParser(lexer: GFXScriptLexer) : Parser(lexer) {

        @Throws(ScriptException::class)
        fun parse(): InstructionSequence {

            // check head
            check(Type.KEY)
            check(Type.EQUAL)
            check(Type.L_BLANKET_L)

            // check instructions
            val seq = parseInstructions()
            if (tk.type == Type.L_BLANKET_R)
                return seq
            throw ScriptException("Uncompleted script: at ${lexer.line}:${lexer.col}")
        }

        private fun parseInstructions(): InstructionSequence {
            val seq = ArrayDeque<Instruction>(16)
            while (lexer.hasNext()) {
                tk = lexer.next()
                if (tk.type == Type.L_BLANKET_R)
                    break
                if (tk.type != Type.KEY)
                    throw ScriptException("Illegal script format: expect a KEY at ${lexer.line}:${lexer.col}")
                val key = tk.content

                check(Type.EQUAL)
                check(Type.L_BLANKET_L)

                val inst: Instruction = when (key) {
                    "gfx" -> parseGfx()
                    "shear" -> parseShear()
                    "loop" -> parseLoop()
                    else -> parseVMInstructions(key, "gfx_loader")
                }
                seq.addLast(inst)
            }
            return seq.toTypedArray()
        }

        private operator fun InstructionSequence.invoke(begin: Int, end: Int, varName: String?, vars: LocalVariables) =
            if (varName != null)
                (begin..end).forEach {
                    vars.setVar<Number>(varName, it)
                    forEach { inst -> if (inst.needVars()) inst(vars) else inst() }
                }
            else
                (begin..end).forEach { _ -> forEach { inst -> inst(vars) } }

        private fun parseLoop(): loop {
            val properties = HashMap<String, String>()
            var instructions: InstructionSequence? = null
            while (lexer.hasNext()) {
                tk = lexer.next()
                if (tk.type == Type.L_BLANKET_R)
                    break

                val key = tk.content
                check(Type.EQUAL)
                if (key == "load") {
                    check(Type.L_BLANKET_L)
                    instructions = parseInstructions()
                    check(Type.L_BLANKET_R)
                } else
                    properties[key] = check(Type.STRING, Type.NUMBER)
            }

            if (instructions == null)
                instructions = arrayOf()
            val begin: Int = properties["begin"]?.toInt() ?: 0
            val end: Int = properties["end"]?.toInt() ?: 0
            val name = properties["var_name"]

            val loop = loop(begin, end, name, instructions)
            properties.clear()
            return loop
        }

        private fun parseGfx(): gfx {
            val (key, value1: String) = checkProperty()
            val (_, value2: String) = checkProperty()
            check(Type.L_BLANKET_R)
            return if (key == "name")
                gfx(value1, value2)
            else
                gfx(value2, value1)
        }

        private fun parseShear(): shear {
            val properties = HashMap<String, String>()
            (0..5).forEach { _ -> with(checkProperty()) { properties[first] = second } }
            val shear = shear(
                properties["name"]!!,
                properties["key"]!!,
                properties["x"]!!,
                properties["y"]!!,
                properties["w"]!!,
                properties["h"]!!
            )
            check(Type.L_BLANKET_R)
            properties.clear()
            return shear
        }

        private inner class loop(
            private val begin: Int,
            private val end: Int,
            private val name: String?,
            private val instructions: InstructionSequence,
        ) : Instruction(0x20) {
            override fun needVars(): Boolean = true

            override fun invoke(vars: LocalVariables) {
                instructions(begin, end, name, vars)
            }
        }

        private inner class gfx(private val name: String, private val path: String) : Instruction(0x21) {
            override fun needVars(): Boolean = false
            override fun invoke() = GFX.loadTexture(name, "$root/$path")
        }

        private inner class shear(
            private val name: String,
            private val key: String,
            private val x: String,
            private val y: String,
            private val w: String,
            private val h: String,
        ) : Instruction(0x22) {
            override fun needVars(): Boolean = false

            @Throws(FailedLoadingResourceException::class)
            override fun invoke() {
                GFX.shearTexture(
                    processVars<String>(key, "gfx_loader"),
                    processVars<String>(name, "gfx_loader"),
                    processVars<Int>(x, "gfx_loader"),
                    processVars<Int>(y, "gfx_loader"),
                    processVars<Int>(w, "gfx_loader"),
                    processVars<Int>(h, "gfx_loader")
                )
            }
        }
    }
}