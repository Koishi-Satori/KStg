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
        override fun next(): Token {
            TODO()
        }

        override fun hasNext(): Boolean {
            TODO()
        }
    }

    private class GFXScriptParser(lexer: GFXScriptLexer) : Parser(lexer) {

    }
}