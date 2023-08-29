package top.kkoishi.stg.logic

import top.kkoishi.stg.gfx.Renderer
import java.io.PrintStream
import java.lang.System.Logger.Level
import java.nio.file.Path
import java.text.DateFormat
import java.text.DateFormat.DEFAULT
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.outputStream
import kotlin.reflect.KClass

@Suppress("MemberVisibilityCanBePrivate")
class InfoSystem private constructor() : Runnable {
    private var count = 0
    private var before = System.currentTimeMillis()
    private var logicBefore = GameLoop.logicFrame()
    private var frameBefore = Renderer.frame()
    private var fps = AtomicInteger(60)
    override fun run() {
        val logger = InfoSystem::class.logger()
        if (count % 900 == 0) {
            logger log "Try to GC." with Level.INFO
            System.gc()
        }
        if (++count % 100 == 0) {
            val cur = System.currentTimeMillis()
            val d = (cur - before).toDouble() / 1000.0


            val logicCur = GameLoop.logicFrame()
            val frameCur = Renderer.frame()

            val logicFps = (logicCur - logicBefore) / d
            val fps = (frameCur - frameBefore) / d
            this.fps.set(fps.toInt())
            logger.log(
                Level.INFO,
                "LogicFrame pre second: $logicFps, FPS: $fps;Bullets count: ${ObjectPool.countBullets() + PlayerManager.countBullets()}"
            )

            before = cur
            logicBefore = logicCur
            frameBefore = frameCur
        }
    }

    companion object {
        private val instance: InfoSystem = InfoSystem()

        private val logToFile
            get() = GenericSystem.logToFile
        private val output =
            PrintStream(Path.of("${Threads.workdir()}/output.log").outputStream(), false, Charsets.UTF_8)

        private val loggers: MutableMap<KClass<out Any>, Logger> = ConcurrentHashMap()

        fun start(threads: Threads) {
            threads.schedule(instance, 64L)
        }

        fun fps() = instance.fps.get()

        fun syncFrame() {
            instance.frameBefore = instance.logicBefore
        }

        private val format = DateFormat.getTimeInstance(DEFAULT, Locale.ENGLISH)
        fun <T : Any> KClass<T>.logger(): Logger {
            var logger = loggers[this@logger]
            if (logger == null)
                logger = Logger(Thread.currentThread().name, this)
            return logger
        }

        infix fun Logger.log(any: Any) = this to any
        infix fun Pair<Logger, Any>.with(level: Level) = first.log(level, second)

        class Logger(val name: String, private val kClass: KClass<out Any>) {

            private fun prefix(level: Level) =
                "[${format.format(Date.from(Instant.now()))}] [$name<-${kClass.simpleName}/${level.name}]"

            @JvmOverloads
            fun log(level: Level, info: String = "") {
                val msg = "${prefix(level)}: $info"
                println(msg)
                if (logToFile) {
                    output.println(msg)
                    output.flush()
                }
            }

            fun log(level: Level, e: Throwable) {
                if (level == Level.WARNING)
                    log(level, "${e.javaClass.name}->${e.message}")
                else
                    log(level, "${e.message}\n${e.stackTraceToString()}")
            }

            fun log(level: Level, a: Any) {
                if (a is Throwable)
                    log(level, a)
                else
                    log(level, a.toString())
            }
        }
    }
}