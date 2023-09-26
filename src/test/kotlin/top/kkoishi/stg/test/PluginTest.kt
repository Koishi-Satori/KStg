package top.kkoishi.stg.test

import top.kkoishi.stg.boot.jvm.JvmPlugin
import top.kkoishi.stg.logic.InfoSystem.Companion.logger

class PluginTest: JvmPlugin {
    override fun main(args: Array<String>) {
        PluginTest::class.logger().log(System.Logger.Level.INFO, "Success call plugin")
        Test.main(args)
    }

    override fun info(): Array<String> = arrayOf("test")
}