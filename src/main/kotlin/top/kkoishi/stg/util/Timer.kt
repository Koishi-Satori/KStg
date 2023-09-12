package top.kkoishi.stg.util

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import kotlin.math.abs

/**
 * A simple timer for timing, and the time unit of it is logic frames.
 *
 * @author KKoishi_
 */
sealed class Timer(delay: Long, protected var modifier: Long = 0) {
    protected val time: Long

    init {
        if (delay < 0) {
            Timer::class.logger().log(System.Logger.Level.WARNING, "The delay should be positive!")
            this.time = abs(delay)
        } else
            this.time = delay
    }

    /**
     * If the timer is ended.
     *
     * @return if the timer is ended.
     */
    open fun end(): Boolean = modifier++ >= time

    fun time() = time
    fun cur() = modifier

    class Default(time: Long) : Timer(time)
    class Delayed(time: Long, delay: Long) : Timer(time, -delay)

    class Loop(interval: Long) : Timer(interval, 0) {
        override fun end(): Boolean {
            if (modifier++ >= time) {
                reset()
                return true
            }
            return false
        }

        fun reset() {
            modifier = 0
        }
    }
}