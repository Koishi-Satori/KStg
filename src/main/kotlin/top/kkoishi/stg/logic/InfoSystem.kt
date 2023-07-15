package top.kkoishi.stg.logic

import top.kkoishi.stg.gfx.Renderer
import java.lang.System.Logger.Level
import java.text.DateFormat
import java.text.DateFormat.DEFAULT
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class InfoSystem private constructor() : Runnable {
    private var count = 0
    private var before = System.currentTimeMillis()
    private var logicBefore = GameLoop.logicFrame()
    private var frameBefore = Renderer.frame()
    override fun run() {
        val logger = InfoSystem::class.logger()
        if (count++ % 900 == 0) {
            logger.log(Level.INFO, "Try to GC.")
            System.gc()
        }
        if (count % 300 == 0) {
            val cur = System.currentTimeMillis()
            val d = (cur - before).toDouble() / 1000.0


            val logicCur = GameLoop.logicFrame()
            val frameCur = Renderer.frame()

            val lfps = (logicCur - logicBefore) / d
            val fps = (frameCur - frameBefore) / d
            logger.log(Level.INFO, "LogicFrame pre second: $lfps, FPS: $fps")

            before = cur
            logicBefore = logicCur
            frameBefore = frameCur
        }
    }

    companion object {
        private val instance: InfoSystem = InfoSystem()

        private val loggers: MutableMap<KClass<out Any>, Logger> = ConcurrentHashMap()

        fun start(threads: Threads) {
            threads.schedule(instance, 64L)
        }

        private val format = DateFormat.getTimeInstance(DEFAULT, Locale.ENGLISH)
        fun <T : Any> KClass<T>.logger(): Logger {
            var logger = loggers[this@logger]
            if (logger == null)
                logger = Logger(Thread.currentThread().name, this)
            return logger
        }

        class Logger(val name: String, private val kClass: KClass<out Any>) {
            private fun prefix(level: Level) =
                "[${format.format(Date.from(Instant.now()))}] [$name<-${kClass.simpleName}/${level.name}]"

            @JvmOverloads
            fun log(level: Level, info: String = "") {
                println("${prefix(level)}: $info")
            }

            fun log(level: Level, e: Exception) {
                println("${prefix(level)}: ${e.message}")
                e.printStackTrace()
            }

            fun log(level: Level, a: Any) {
                if (a is Exception)
                    log(level, a)
                log(level, a.toString())
            }
        }
    }
}