package top.kkoishi.stg.logic

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.nio.file.Path
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.random.Random

class Threads private constructor(size: Int = 4) {
    private val logger = Threads::class.logger()
    private val threadPool: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(size, DefaultThreadFactory())

    @JvmOverloads
    fun schedule(r: Runnable, period: Long, delay: Long = 0L) {
        threadPool.scheduleAtFixedRate(r, delay, period, TimeUnit.MILLISECONDS)
        logger.log(System.Logger.Level.INFO, "Add a new thread $r")
    }

    companion object {
        private val randomSeed: AtomicLong = AtomicLong(System.currentTimeMillis())
        private var random = Random(randomSeed())
        private var workdir = Path.of(".").absolutePathString()

        fun workdir() = workdir

        /**
         * Try to set the engine's internal work dir.
         *
         * @param nWorkdir new work dir.
         * @return if the new work dir exists
         */
        fun workdir(nWorkdir: String): Boolean {
            val nPath = Path.of(nWorkdir)
            if (nPath.exists()) {
                workdir = nPath.absolutePathString()
                Threads::class.logger().log(System.Logger.Level.INFO, "Set workdir to $workdir")
                return true
            }
            return false
        }

        fun randomSeed() = randomSeed.get()

        fun refreshRandomSeed() {
            randomSeed.set(System.currentTimeMillis())
            random = Random(randomSeed())
        }

        fun random() = random

        fun getInstance() = Threads()

        @JvmStatic
        private val poolIndex: AtomicInteger = AtomicInteger(1)

        private class DefaultThreadFactory : ThreadFactory {
            private val group = Thread.currentThread().threadGroup
            private val threadIndex: AtomicInteger = AtomicInteger(0)
            private val prefix = "Pool-${poolIndex.getAndIncrement()}["
            override fun newThread(r: Runnable): Thread {
                return Thread(group, r, "$prefix${threadIndex.getAndIncrement()}] ${r.javaClass.simpleName}")
            }
        }

        // 1s = 1000ms
        const val UNIT = 1000L

        // 15ms, 67fps
        @JvmStatic
        private val period: AtomicLong = AtomicLong(16L)
        fun period(): Long {
            return period.get()
        }

        fun recalPeriod(targetFPS: Long) {
            TODO()
        }
    }
}