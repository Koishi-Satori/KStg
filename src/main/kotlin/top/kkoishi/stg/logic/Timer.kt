package top.kkoishi.stg.logic

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import kotlin.math.abs

/**
 * A simple timer for timing, the time unit is a logical frame, and the current logical frame can be obtained
 * by using the [GameLoop.logicFrame] method.
 *
 * @author KKoishi_
 */
sealed class Timer(
    protected var delay: Long,
    protected val begin: Long = GameLoop.logicFrame(),
) {

    init {
        if (delay < 0) {
            delay = abs(delay)
            Timer::class.logger().log(System.Logger.Level.WARNING, "The delay should be positive!")
        }
    }

    open fun end(): Boolean = GameLoop.logicFrame() - begin >= delay

    class Default(delay: Long) : Timer(delay)
    class Delayed(delay: Long, begin: Long) : Timer(delay, begin) {
        override fun end(): Boolean {
            val cur = GameLoop.logicFrame()
            if (cur < begin)
                return false
            return cur - begin >= delay
        }
    }
}