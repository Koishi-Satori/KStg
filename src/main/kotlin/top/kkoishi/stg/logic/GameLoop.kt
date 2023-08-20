package top.kkoishi.stg.logic

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.util.concurrent.atomic.AtomicLong

/**
 * Control the logic of the game.
 *
 * @author KKoishi_
 */
class GameLoop private constructor() : Runnable {
    private val logicFrame = AtomicLong(0L)

    /**
     * upgrade the game states.
     */
    fun update() {
        val logger = GameLoop::class.logger()
        val gameState = GenericFlags.gameState.get()
        when (gameState) {
            GenericFlags.STATE_PLAYING -> {
                try {
                    // update player logic first.
                    ObjectPool.player.update()

                    var cur = PlayerManager.cur
                    if (cur.toNextStage()) {
                        cur = cur.nextStage()
                        PlayerManager.cur = cur
                    }
                    cur.action()

                    ObjectPool.uiObjects().forEach { it.update() }

                    var index = 0
                    for (o in ObjectPool.objects()) {
                        if (o.update())
                            ObjectPool.removeObject(index--)
                        ++index
                    }

                    index = 0
                    for (b in ObjectPool.bullets()) {
                        if (b.update())
                            ObjectPool.removeBullet(index--)
                        ++index
                    }
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }
            GenericFlags.STATE_PAUSE -> {
                // menu logic
            }
            GenericFlags.STATE_MENU -> {
                // main menu
                try {
                    ObjectPool.uiObjects().forEach { it.update() }
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }
        }
        logicFrame.incrementAndGet()
    }

    override fun run() {
        update()
    }

    companion object {
        private val instance = GameLoop()

        @JvmStatic
        fun start(threads: Threads) {
            threads.schedule(instance, Threads.period(), 1L)
        }

        fun logicFrame() = instance.logicFrame.get()
    }
}