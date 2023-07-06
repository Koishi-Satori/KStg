package top.kkoishi.stg.script

import top.kkoishi.stg.logic.Lexer
import top.kkoishi.stg.logic.Parser
import top.kkoishi.stg.logic.ReaderIterator
import top.kkoishi.stg.logic.Token
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.isRegularFile

class GFXLoader(private val root: Path) {
    private var size = 0
    fun loadDefinitions() {
        for (path in root) {
            if (path.isRegularFile())
                size += loadDefinitionFile(path)
        }
    }

    private fun loadDefinitionFile(path: Path): Int {
        val rd = path.bufferedReader()
        val lexer = GFXScriptLexer(ReaderIterator(rd))
        TODO()
    }

    fun getTextures(): Array<Pair<String, String>> {
        TODO()
    }

    private class GFXScriptLexer(rest: CharIterator) : Lexer(rest) {
        private var end: Boolean = false
        private var peek: Token? = null
        private var lookup = '\u0000'
        private var line = 0
        private var col = 0

        override fun next(): Token {
            val res = peek ?: next0()
            peek = null
            return res
        }

        private fun next0(): Token {
            lookup = nextChar()
            when (lookup) {

            }
            TODO()
        }

        private fun nextChar(): Char {
            TODO()
        }

        override fun hasNext(): Boolean {
            if (end)
                return false
            return peek != null || rest.hasNext()
        }
    }

    private class GFXScriptParser(lexer: GFXScriptLexer) : Parser(lexer) {

    }
}