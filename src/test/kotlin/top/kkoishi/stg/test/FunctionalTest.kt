package top.kkoishi.stg.test

import top.kkoishi.stg.localization.ClassLocalization
import java.util.*

fun main() {
    println(LocalizationTest_zh_CN.TEST_0)
    println(LocalizationTest_zh_CN.TEST_1)
}

@Suppress("ClassName")
object LocalizationTest_zh_CN : ClassLocalization<LocalizationTest_zh_CN>(
    Locale.CHINESE,
    LocalizationTest_zh_CN::class.java,
    "./test/localization/test_zh_CN.yml"
) {
    override fun constantFieldsName(): Array<String>? = null

    override fun reference(): LocalizationTest_zh_CN = this

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