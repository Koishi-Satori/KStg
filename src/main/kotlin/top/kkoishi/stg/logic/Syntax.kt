package top.kkoishi.stg.logic

import java.io.IOException
import java.io.Reader
import kotlin.properties.Delegates

enum class Type {
    KEY,
    EQUAL, BG, SM, BG_EQ, SM_EQ,
    CALL, FUNC_CALL, VAR, STRING,
    L_BLANKET_L, L_BLANKET_R
}

data class Token(val content: String, val type: Type)

private val L_BLANKET_L = Token("", Type.L_BLANKET_L)
private val L_BLANKET_R = Token("", Type.L_BLANKET_R)
private val EQUAL = Token("", Type.EQUAL)
private val BG = Token("", Type.BG)
private val BG_EQ = Token("", Type.BG_EQ)
private val SM = Token("", Type.SM)
private val SM_EQ = Token("", Type.SM_EQ)

internal class ReaderIterator(private val rd: Reader) : CharIterator() {
    private var peek by Delegates.notNull<Int>()

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

    override fun hasNext(): Boolean = peek == -1
}

internal abstract class Lexer(protected val rest: CharIterator) {
    abstract fun next(): Token
    abstract fun hasNext(): Boolean
}

internal abstract class Parser(protected val lexer: Lexer)