@file:Suppress("unused")

package top.kkoishi.stg.test

object Fonts {
    @JvmStatic
    fun getScoreNumIndexes(c: Char): Pair<Int, Int> {
        /*
        * ','(44) -> 0
        * '.'(46) -> 1
        * '0'-'9'(48-57) -> 2-11
        */
        return when (c) {
            ',' -> 0
            '.' -> 1
            else -> (c.code - 46)
        } to 0
    }
}