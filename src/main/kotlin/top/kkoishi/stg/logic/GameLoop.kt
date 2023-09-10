package top.kkoishi.stg.logic

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.coordinatespace.SubChunks
import top.kkoishi.stg.logic.keys.KeyBinds
import java.util.concurrent.atomic.AtomicLong

/**
 * Control the logic of the game.
 *
 * For the four state of the game, GameLoop class has different way of processing, and if the state is not
 * one of them, nothing will ve invoked.
 *
 * * [GenericSystem.STATE_PLAYING] - update the player logic, then the stage and ui, finally will be bullets
 * and enemies.
 * * [GenericSystem.STATE_PAUSE] - only ui will be updated.
 * * [GenericSystem.STATE_MENU] - same as pause state.
 * * [GenericSystem.STATE_LOADING] - only [ObjectPool.loadingContent].
 *
 * If you want to start this thread, please use the method [GameLoop.start].
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
                    // if current should be updated to next stage, then invoke Stage::nextStage and cur will be
                    // set to the next stage.
                    // finally, invoke Stage::action method of cur.
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

            GenericSystem.STATE_LOADING -> {
                // loading content
                try {
                    ObjectPool.loadingContents().forEach { it.update() }
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            else -> {
                logger.log(System.Logger.Level.ERROR, "Error game state!")
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