package top.kkoishi.stg.logic

import java.io.IOException
import java.io.Reader
import kotlin.properties.Delegates

enum class Type {
    KEY,
    EQUAL, BG, SM, BG_EQ, SM_EQ,
    CALL, FUNC_CALL, VAR, STRING,
    L_BLANKET_L, L_BLANKET_R, NUMBER
}

data class Token(val content: String, val type: Type)

val L_BLANKET_L = Token("", Type.L_BLANKET_L)
val L_BLANKET_R = Token("", Type.L_BLANKET_R)
val EQUAL = Token("", Type.EQUAL)
val BG = Token("", Type.BG)
val BG_EQ = Token("", Type.BG_EQ)
val SM = Token("", Type.SM)
val SM_EQ = Token("", Type.SM_EQ)

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
}

internal abstract class Lexer(protected val rest: CharIterator) {
    var line = 0
    var col = 0

    abstract fun next(): Token
    abstract fun hasNext(): Boolean
}

internal abstract class Parser(protected val lexer: Lexer)

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