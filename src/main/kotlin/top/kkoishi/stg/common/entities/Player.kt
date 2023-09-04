package top.kkoishi.stg.common.entities

import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.logic.GenericSystem
import top.kkoishi.stg.logic.GenericSystem.gameState
import top.kkoishi.stg.logic.GenericSystem.STATE_PLAYING
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.keys.KeyBindInvoker
import top.kkoishi.stg.logic.keys.KeyBinds
import top.kkoishi.stg.logic.PlayerManager
import top.kkoishi.stg.logic.PlayerManager.life
import top.kkoishi.stg.util.Options
import top.kkoishi.stg.util.Timer
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.HashMap

/**
 * The basic class of player. After reinitialize the [top.kkoishi.stg.logic.ObjectPool.player], you can switch
 * the GameStage([gameState]) to [STATE_PLAYING], then this class can be rendered and upgrade correctly.
 *
 * You can change the events in [keyEvents] to override the behavior of the player, or implement [actionsImpl] method.
 * Please make sure that you recreate the Player instance or set its position to the initial position.
 *
 * The rest life is held by [PlayerManager], so you can get it by invoking [life] method for upgrading the information.
 *
 * @param initialX the initial X coordinate of the player.
 * @param initialY the initial Y coordinate of the player.
 * @param invincible if the player is invincible.
 * @author KKoishi_
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class Player(var initialX: Int, var initialY: Int, val invincible: Boolean = false) : Entity(0) {
    /**
     * The X coordinate of the player.
     */
    private val x = AtomicReference(initialX.toDouble())

    /**
     * The Y coordinate of the player.
     */
    private val y = AtomicReference(initialY.toDouble())

    /**
     *  Get the X coordinate of the player in Double.
     *
     *  @see AtomicReference.getAcquire
     */
    fun x(): Double = x.acquire

    /**
     *  Get the Y coordinate of the player in Double.
     *
     *  @see AtomicReference.getAcquire
     */
    fun y(): Double = y.acquire

    /**
     *  Set the X coordinate of the player.
     *
     * @param newX the new X coordinate.
     *  @see AtomicReference.setRelease
     */
    fun x(newX: Double) = x.setRelease(newX)

    /**
     *  Set the Y coordinate of the player.
     *
     * @param newY the new Y coordinate.
     *  @see AtomicReference.setRelease
     */
    fun y(newY: Double) = y.setRelease(newY)

    /**
     * The current speed of the player.
     */
    protected var speed = 7.0

    /**
     * The higher speed of the player.
     */
    protected var higherSpeed = 7.0

    /**
     * The slower speed of the player.
     */
    protected var lowerSpeed = 3.0

    /**
     * The shot coll down timer of the player.
     */
    protected var shotCollDownTimer = Timer.Loop(3)

    /**
     * The protection time after the player is hit(in logic frame count).
     */
    protected var protectionTimer = Timer.Loop(5)

    /**
     * If player is in slow state.
     */
    protected var slower: Boolean = false

    /**
     * The move state of player, used to judge if the player is moving to left, right or forward/down.
     *
     * This state should be one of [STATE_STILL], [STATE_RIGHT], [STATE_LEFT]
     */
    protected var moveState: Int = STATE_STILL

    /**
     * The firepower of the player.
     */
    protected var power: Float = 1.0f
    protected val lock = Any()
    val logger = Player::class.logger()

    /**
     * the X coordinate of the player in Integer.
     */
    fun xInt() = x().toInt()

    /**
     * the Y coordinate of the player in Integer.
     */
    fun yInt() = y().toInt()

    /**
     * Reinitialize the player's state.
     */
    open fun reinitialize() {
        synchronized(lock) {
            x(initialX.toDouble())
            y(initialY.toDouble())
            power = 1.0f
            protectionTimer.reset()
            shotCollDownTimer.reset()
            slower = false
            speed = higherSpeed
            moveState = STATE_STILL
            keyEvents.keys.forEach(KeyBinds::release)
        }
    }

    /**
     * Set the player's firepower to the given float number.
     *
     * @param p the new firepower.
     */
    fun setPlayerPower(p: Float) {
        synchronized(lock) {
            power = p
        }
    }

    /**
     * Get the firepower of the player
     *
     * @return firepower in float.
     */
    fun power(): Float {
        synchronized(lock) {
            return power
        }
    }

    override fun isDead(): Boolean = life() <= 0

    abstract fun bulletDamage(): Int

    abstract fun bullet(dx: Int = 0, dy: Int = 0): PlayerBullet

    abstract fun texture(): String

    abstract fun bomb()

    override fun beingHit(o: Object) {
        if (invincible) {
            logger.log(System.Logger.Level.INFO, "$this is invincible.")
            return
        }
        if (protectionTimer.end()) {
            val oldLife = life()
            if (oldLife <= 1) {
                dead()
                if (Options.State.debug)
                    logger.log(System.Logger.Level.DEBUG, "$this has already dead.")
            } else {
                PlayerManager.setLife(oldLife - 1)
                if (Options.State.debug)
                    logger.log(System.Logger.Level.DEBUG, "$this is hit by $o")
            }
        }
    }

    open fun shot() = PlayerManager.addBullet(bullet())

    fun actions() {
        for (keyCode in keyEvents.keys) {
            preAction(keyCode)
            action(keyCode)
        }
        actionsImpl()
    }

    abstract fun actionsImpl()

    protected open fun preAction(keyCode: Int) {
        when (keyCode) {
            VK_SHIFT -> {
                // press shift
                speed = if (KeyBinds.isPressed(keyCode)) {
                    slower = true
                    lowerSpeed
                } else {
                    slower = false
                    higherSpeed
                }
            }

            VK_LEFT, VK_RIGHT -> {
                if (!KeyBinds.isPressed(keyCode)) {
                    moveState = STATE_STILL
                }
            }
        }
    }

    protected open fun action(keyCode: Int) {
        if (KeyBinds.isPressed(keyCode))
            KeyBinds(keyCode, this)
    }

    @Deprecated("Move function is implemented in key events")
    final override fun move() {
    }

    override fun collide(o: Object): Boolean {
        if (invincible)
            return false
        if (o is Entity) {
            if (CollideSystem.collide(this, o)) {
                o.beingHit(this)
                return true
            }
        }
        return false
    }

    override fun paint(g: Graphics2D) {
        val key = texture()
        val t = GFX.getTexture(key)
        val xI = x()
        val yI = y()
        val point = t.renderPoint(xI, yI)
        t.paint(g, t.normalMatrix(), point.x, point.y)

        // render player bullets
        PlayerManager.paintBullets(g)
    }

    override fun update(): Boolean {
        if (super.update())
            return true
        PlayerManager.updateBullets()
        actions()
        return false
    }

    override fun toString(): String {
        return "Player{$invincible, ($x, $y), uuid=$uuid}"
    }

    companion object {
        const val STATE_STILL = 0
        const val STATE_LEFT = 1
        const val STATE_RIGHT = 2

        /**
         * Constant for the non-numpad **left** arrow key.
         */
        const val VK_LEFT = 0x25

        /**
         * Constant for the non-numpad **up** arrow key.
         */
        const val VK_UP = 0x26

        /**
         * Constant for the non-numpad **right** arrow key.
         */
        const val VK_RIGHT = 0x27

        /**
         * Constant for the non-numpad **down** arrow key.
         */
        const val VK_DOWN = 0x28

        /** Constant for the "C" key.  */
        const val VK_C = 0x43

        /** Constant for the "X" key.  */
        const val VK_X = 0x58

        /** Constant for the "Z" key.  */
        const val VK_Z = 0x5A

        /** Constant for the SHIFT virtual key.  */
        const val VK_SHIFT = 0x10

        /** Constant for the ESCAPE virtual key.  */
        const val VK_ESCAPE = 0x1B

        @JvmStatic
        private val DEFAULT_KEY_EVENTS = HashMap<Int, (Player) -> Unit>(mapOf(VK_LEFT to {
            it.moveState = STATE_LEFT
            val oldX = it.x()
            var newX = oldX - it.speed
            val limitX = Graphics.getUIInsets().left.toDouble()
            if (newX < limitX)
                newX = limitX
            it.x(newX)
        }, VK_RIGHT to {
            it.moveState = STATE_RIGHT
            val oldX = it.x()
            var newX = oldX + it.speed
            val limitX = Graphics.getScreenSize().width - Graphics.getUIInsets().right
            if (newX > limitX)
                newX = limitX
            it.x(newX)
        }, VK_UP to {
            val oldY = it.y()
            var newY = oldY - it.speed
            val limitY = Graphics.getUIInsets().top.toDouble()
            if (newY < limitY)
                newY = limitY
            it.y(newY)
        }, VK_DOWN to {
            val oldY = it.y()
            var newY = oldY + it.speed
            val limitY = Graphics.getScreenSize().height - Graphics.getUIInsets().bottom
            if (newY > limitY)
                newY = limitY
            it.y(newY)
        }, VK_Z to {
            if (it.shotCollDownTimer.end())
                it.shot()
        }, VK_X to {
            it.bomb()
            // avoid bomb continually
            KeyBinds.release(VK_X)
        }, VK_C to {
            // unbind
        }, VK_ESCAPE to {
            val gameState = gameState.get()
            if (gameState == STATE_PLAYING) {
                GenericSystem.gameState.set(GenericSystem.STATE_PAUSE)
                it.logger.log(System.Logger.Level.INFO, "Pause the game.")
            } else if (gameState == GenericSystem.STATE_PAUSE) {
                GenericSystem.gameState.set(STATE_PLAYING)
                it.logger.log(System.Logger.Level.INFO, "Continue the game.")
            }
            KeyBinds.release(VK_ESCAPE)
        }, VK_SHIFT to {
            // empty
        }))

        /**
         * Key events which is used to store the functions of the game player instance when
         * player pressing the specified keys.
         */
        @JvmStatic
        val keyEvents: MutableMap<Int, (Player) -> Unit> = HashMap(DEFAULT_KEY_EVENTS)

        /**
         * Register the key events in [keyEvents] to [KeyBinds].
         */
        @JvmStatic
        fun registerKeyEvents() {
            keyEvents.map { it.key to KeyBindInvoker(it.value) }.forEach { KeyBinds.bind(it.first, it.second) }
        }

        class EmptyPlayer : Player(25, 25, true) {
            override fun dead() {}

            override fun actionsImpl() {}

            override fun shape(): Shape = CollideSystem.Circle(Point(xInt(), yInt()), 5)

            override fun bulletDamage(): Int = 0

            override fun bullet(dx: Int, dy: Int): PlayerBullet = EmptyBullet(xInt() + dx, yInt() + dy)

            override fun texture(): String = "NOT_FOUND"

            override fun bomb() {}
        }

        class EmptyBullet(initialX: Int, initialY: Int) : PlayerBullet(initialX, initialY) {
            override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 10)

            override fun move() {
                setY(y() + 1)
            }

            override fun paint(g: Graphics2D) {
            }
        }
    }
}