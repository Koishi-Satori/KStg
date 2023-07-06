package top.kkoishi.stg.logic

object GenericFlags {
    const val STATE_MENU = 1
    const val STATE_PAUSE = 2
    const val STATE_PLAYING = 3

    @JvmStatic
    val gameState: ThreadLocal<Int> = ThreadLocal()

    init {
        gameState.set(STATE_MENU)
    }
}