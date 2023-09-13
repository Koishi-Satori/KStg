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

package top.kkoishi.stg.test

import top.kkoishi.stg.localization.ClassLocalization
import top.kkoishi.stg.logic.Threads
import java.util.Locale

@Suppress("ClassName")
object TestDialog_zh_CN : ClassLocalization<TestDialog_zh_CN>(
    Locale.CHINESE,
    TestDialog_zh_CN::class.java,
    "${Threads.workdir()}/test/localization/test_zh_CN.yml"
) {
    override fun constantFieldsName(): Array<String>? = null

    override fun reference(): TestDialog_zh_CN = this

    @JvmStatic
    lateinit var TEST_0: String

    @JvmStatic
    lateinit var TEST_1: String

    @JvmStatic
    lateinit var TEST_2: String

    @JvmStatic
    lateinit var TEST_3: String

    @JvmStatic
    lateinit var TEST_4: String
}