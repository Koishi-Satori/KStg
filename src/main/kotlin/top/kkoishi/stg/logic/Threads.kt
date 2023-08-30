package top.kkoishi.stg.logic

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.gfx.Renderer
import java.nio.file.Path
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.random.Random

/**
 * This class is a thread pool used to run the key threads of the stg engine, like [GameLoop] and [Renderer],
 * and it contains a simple [ThreadFactory] for naming the created tasks.
 *
 * @param corePoolSize the number of threads to keep in the pool, even if they are idle, unless allowCoreThreadTimeOut is set
 * @author KKoishi_
 */
class Threads private constructor(corePoolSize: Int = bestCorePoolSize()) {
    private val logger = Threads::class.logger()
    private val threadPool: ScheduledThreadPoolExecutor =
        ScheduledThreadPoolExecutor(corePoolSize, DefaultThreadFactory())

    /**
     * Submits a periodic action to the thread pool that becomes enabled first after the given initial delay,
     * and subsequently with the given period.
     *
     * If the current task is not completed and the next task should be executed at this time, the next task will be
     * executed immediately after the current task is executed to ensure that the number of executions is correct as
     * much as possible.
     *
     * @param r the action.
     * @param period the period between each execution.
     * @param delay the initial delay.
     */
    @JvmOverloads
    fun schedule(r: Runnable, period: Long, delay: Long = 0L) {
        threadPool.scheduleAtFixedRate(r, delay, period, TimeUnit.MILLISECONDS)
        logger.log(System.Logger.Level.INFO, "Add a new thread $r")
    }

    companion object {
        @JvmStatic
        private val randomSeed: AtomicLong = AtomicLong(System.currentTimeMillis())

        @JvmStatic
        private var random = Random(randomSeed())

        @JvmStatic
        private var workdir = Path.of(".").absolutePathString()

        @JvmStatic
        fun workdir() = workdir

        /**
         * Try to set the engine's internal work dir.
         *
         * @param nWorkdir new work dir.
         * @return if the new work dir exists
         */
        @JvmStatic
        fun workdir(nWorkdir: String): Boolean {
            val nPath = Path.of(nWorkdir)
            if (nPath.exists()) {
                workdir = nPath.absolutePathString()
                Threads::class.logger().log(System.Logger.Level.INFO, "Set workdir to $workdir")
                return true
            }
            return false
        }

        @JvmStatic
        fun randomSeed() = randomSeed.get()

        @JvmStatic
        fun refreshRandomSeed() {
            randomSeed.set(System.currentTimeMillis())
            random = Random(randomSeed())
        }

        @JvmStatic
        fun random() = random

        @JvmStatic
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

        // 15ms, 67fps
        @JvmStatic
        private val period: AtomicLong = AtomicLong(16L)

        @JvmStatic
        fun period(): Long {
            return period.get()
        }

        /**
         * Get the best corePoolSize for the constructor of [Threads]. and it is equal to your cpu's logic core amount.
         *
         * The logic and render calculation both are computationally intensive tasks, so  for better multicore and
         * multi-thread performance, the maximum number of threads should be consistent with the number of your
         * CPU logical cores.
         *
         * @return the best corePoolSize
         */
        @JvmStatic
        fun bestCorePoolSize() = Runtime.getRuntime().availableProcessors()
    }
}