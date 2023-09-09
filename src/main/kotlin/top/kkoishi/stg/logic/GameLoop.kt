package top.kkoishi.stg.logic

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.coordinatespace.SubChunks
import top.kkoishi.stg.logic.keys.KeyBinds
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
        when (GenericSystem.gameState.get()) {
            GenericSystem.STATE_PLAYING -> {
                try {
                    // update player logic first.
                    ObjectPool.player().update()

                    // update the stage logic.
                    var cur = PlayerManager.curStage
                    if (cur.toNextStage()) {
                        cur = cur.nextStage()
                        PlayerManager.curStage = cur
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

                    SubChunks.updateSpace()
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            GenericSystem.STATE_PAUSE -> {
                // menu logic
                try {
                    ObjectPool.uiObjects().forEach { it.update() }
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            GenericSystem.STATE_MENU -> {
                // main menu
                try {
                    ObjectPool.uiObjects().forEach { it.update() }
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }
        }
        KeyBinds.invokeGenericBinds()

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