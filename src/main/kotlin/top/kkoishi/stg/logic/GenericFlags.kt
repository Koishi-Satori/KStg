package top.kkoishi.stg.logic

import java.util.concurrent.atomic.AtomicInteger

object GenericFlags {
    const val STATE_MENU = 1
    const val STATE_PAUSE = 2
    const val STATE_PLAYING = 3

    @JvmStatic
    val gameState = AtomicInteger(STATE_MENU)

    @JvmStatic
    var logToFile: Boolean = false
}