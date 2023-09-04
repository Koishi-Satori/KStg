package top.kkoishi.stg.logic

import top.kkoishi.stg.common.ui.*
import java.util.concurrent.atomic.AtomicInteger

object GenericSystem {
    /**
     * Identify that the game engine should display menus.
     *
     * ## Instructions of this constant
     * In this state, only [UIObject] in [ObjectPool.UIObjects] will be rendered and calculated.
     *
     * @see top.kkoishi.stg.test.Test.menu()
     */
    const val STATE_MENU = 1

    /**
     * Identify that the game has been paused.
     *
     * ## Instructions of this constant
     * Before switch the game state to this, please make sure you have initialized or reinitialized the stage and
     * player instance, and you should add a pause menu into UIObjects at the mean time.
     * ### They are located in [PlayerManager.curStage], [ObjectPool.player] and [ObjectPool.UIObjects].
     * Also, some [UIObject] which are added to UIObjects and can action during playing will
     * be rendered and calculated.
     *
     */
    const val STATE_PAUSE = 2

    /**
     * Identify that the game is has begun.
     *
     * ## Instructions of this constant
     * Before switch the game state to this, please make sure you have initialized or reinitialized the stage
     * and the player correctly.
     * ### They are located in [PlayerManager.curStage] and [ObjectPool.player].
     * Also, some [UIObject] which are added to [ObjectPool.UIObjects] and can action during playing will
     * be rendered and calculated.
     *
     * @see top.kkoishi.stg.test.Test.start
     */
    const val STATE_PLAYING = 3

    /**
     * Used to identify the game state.
     *
     * ### This static variable should be set to [STATE_PLAYING], [STATE_PAUSE] or [STATE_MENU].
     */
    @JvmStatic
    val gameState = AtomicInteger(STATE_MENU)

    @JvmStatic
    var logToFile: Boolean = false
}