/*
 *
 * This file is only used for testing, and there is no need for you to add this
 * module "KStg.test" when build this engine.
 *
 *
 * Some resources for art and sound in this test module are from a touhou STG game
 * which called "东方夏夜祭", for the author is in lack of synthesizing music and
 * game painting. :(
 *
 *
 *
 */

@file:Suppress("unused")

package top.kkoishi.stg.test

object Fonts {
    /**
     * ## This method is only invoked by the script.
     */
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