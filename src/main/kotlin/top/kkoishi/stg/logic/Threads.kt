package top.kkoishi.stg.logic

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class Threads private constructor(size: Int = 4) {
    private val threadPool: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(size, DefaultThreadFactory())

    @JvmOverloads
    fun schedule(r: Runnable, period: Long, delay: Long = 0L) {
        threadPool.scheduleAtFixedRate(r, delay, period, TimeUnit.MILLISECONDS)
    }

    companion object {
        fun getInstance() = Threads()

        @JvmStatic
        private val poolIndex: AtomicInteger = AtomicInteger(1)

        private class DefaultThreadFactory : ThreadFactory {
            private val group = Thread.currentThread().threadGroup
            private val threadIndex: AtomicInteger = AtomicInteger(0)
            private val prefix = "Pool-${poolIndex.getAndIncrement()}-thread"
            override fun newThread(r: Runnable): Thread = Thread(group, r, prefix + threadIndex.getAndIncrement())
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