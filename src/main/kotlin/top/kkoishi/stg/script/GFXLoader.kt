package top.kkoishi.stg.script

import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.Lexer
import top.kkoishi.stg.logic.Parser
import top.kkoishi.stg.logic.ReaderIterator
import java.io.File
import java.nio.file.Path

class GFXLoader(private val root: Path) {
    private val logger = GFXLoader::class.logger()
    fun loadDefinitions() {
        logger.log(System.Logger.Level.INFO, "Load textures from scripts.")
        for (path in root.toFile().listFiles()) {
            if (path.isFile)
                try {
                    logger.log(System.Logger.Level.INFO, "Try to load textures from $path")
                    loadDefinitionFile(path)
                } catch (e: Exception) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
        }
    }

    private fun loadDefinitionFile(path: File) {
        val rd = path.bufferedReader()
        val lexer = GFXScriptLexer(ReaderIterator(rd))
        val parser = GFXScriptParser(lexer)
        val items = parser.parse()
        while (items.isNotEmpty()) {
            val item = items.removeFirst()
            item.load()
        }
    }

    private class GFXScriptLexer(rest: CharIterator) : Lexer(rest) {
        private var peek = ArrayDeque<Token>()
        private var lookup = '\u0000'

        @Throws(NoSuchElementException::class)
        override fun next(): Token {
            if (peek.isNotEmpty())
                return peek.removeFirst()
            if (rest.hasNext())
                return next0()
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
            while (lookup.isDigit()) {
                sb.append(lookup)
                lookup = nextChar()
            }
            return Token(sb.toString(), Type.NUMBER)
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
        private lateinit var tk: Token

        @Throws(ScriptException::class)
        fun parse(): ArrayDeque<GFXItem> {
            val arr = ArrayDeque<GFXItem>()

            // check header
            check(Type.KEY)
            check(Type.EQUAL)
            check(Type.L_BLANKET_L)

            // check entries
            while (lexer.hasNext()) {
                tk = lexer.next()
                if (tk.type == Type.L_BLANKET_R)
                    break
                if (tk.type != Type.KEY)
                    throw ScriptException("Illegal script format: expect KEY at ${lexer.line}:${lexer.col}")
                val key = tk.content
                check(Type.EQUAL)
                check(Type.L_BLANKET_L)
                when (key) {
                    "gfx" -> arr.addLast(GFXItemImpl(checkProperty("name"), checkProperty("path")))
                    "shear" -> arr.addLast(
                        GFXshearItem(
                            checkProperty("name"),
                            checkProperty("key"),
                            checkProperty("x").toInt(),
                            checkProperty("y").toInt(),
                            checkProperty("w").toInt(),
                            checkProperty("h").toInt()
                        )
                    )

                    else -> throw ScriptException("Incorrect key: $key at ${lexer.line}:${lexer.col}")
                }
                check(Type.L_BLANKET_R)
            }

            if (tk.type == Type.L_BLANKET_R)
                return arr
            throw ScriptException("Uncompleted script: at ${lexer.line}:${lexer.col}")
        }

        private fun check(
            type: Type,
            msg: String = "Illegal script format: expect $type at ${lexer.line}:${lexer.col}",
        ) {
            if (!lexer.hasNext())
                throw ScriptException("$msg, but got nothing")
            tk = lexer.next()
            if (tk.type != type)
                throw ScriptException("$msg, but got ${tk.type}")
        }

        private fun checkProperty(key: String): String {
            check(Type.KEY)
            if (tk.content != key)
                throw ScriptException("Illegal property key: expect $key at ${lexer.line}:${lexer.col}")
            check(Type.EQUAL)
            if (!lexer.hasNext())
                throw ScriptException("Unfinished property: at ${lexer.line}:${lexer.col}")
            tk = lexer.next()
            if (tk.type != Type.STRING && tk.type != Type.NUMBER)
                throw ScriptException("The right side of property must be STRING or NUMBER: at ${lexer.line}:${lexer.col}")
            return tk.content
        }
    }

    private abstract inner class GFXItem(val name: String) {
        abstract fun load()
    }

    private inner class GFXItemImpl(name: String, val path: String) : GFXItem(name) {
        override fun load() = GFX.loadTexture(name, "$root/$path")
        override fun toString(): String {
            return "GFXItemImpl(name=$name ,path='$path')"
        }
    }

    private inner class GFXshearItem(
        name: String,
        private val key: String,
        private val x: Int,
        private val y: Int,
        private val w: Int,
        private val h: Int,
    ) : GFXItem(name) {
        override fun load() = GFX.shearTexture(key, name, x, y, w, h)
    }
}