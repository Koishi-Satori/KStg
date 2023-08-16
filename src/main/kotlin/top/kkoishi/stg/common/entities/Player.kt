package top.kkoishi.stg.common.entities

import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.logic.GenericFlags
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.PlayerManager
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.HashMap

/**
 * The basic class of player.
 *
 * @param initialX the initial X coordinate of the player.
 * @param initialY the initial Y coordinate of the player.
 * @author KKoishi_
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class Player(initialX: Int, initialY: Int) : Entity(0) {
    /**
     * The X coordinate of the player.
     */
    val x = AtomicReference(initialX.toDouble())

    /**
     * The Y coordinate of the player.
     */
    val y = AtomicReference(initialY.toDouble())

    /**
     * The speed of the player.
     */
    protected var speed = 7.0
    protected var higherSpeed = 7.0
    protected var lowerSpeed = 3.0

    /**
     * The shot coll down of the player.
     */
    protected var shotCoolDown = 3
    protected var shotCoolCount = 0

    /**
     * If player is in slow state.
     */
    protected var slower: Boolean = false
    protected var moveState: Int = STATE_STILL
    protected var power: Float = 1.0f
    protected val logger = Player::class.logger()

    fun x() = x.get().toInt()
    fun y() = y.get().toInt()

    fun setPlayerPower(p: Float) {
        synchronized(logger) {
            power = p
        }
    }

    fun power(): Float {
        synchronized(logger) {
            return power
        }
    }

    override fun isDead(): Boolean = PlayerManager.life() == 0

    abstract fun bulletDamage(): Int

    abstract fun bullet(dx: Int = 0, dy: Int = 0): PlayerBullet

    abstract fun texture(): String

    abstract fun bomb()

    override fun beingHit(o: Object) {
        val oldLife = PlayerManager.life()
        PlayerManager.setLife(oldLife - 1)
        logger.log(System.Logger.Level.INFO, "$this is hit by $o")
    }

    open fun shot() = PlayerManager.addBullet(bullet())

    fun actions() {
        for (keyCode in keyEvents.keys) {
            action(keyCode)
        }
        actionsImpl()
    }

    abstract fun actionsImpl()

    private fun action(keyCode: Int) {
        when (keyCode) {
            VK_SHIFT -> {
                // press shift
                speed = if (PlayerManager.binds[keyCode]) {
                    slower = true
                    lowerSpeed
                } else {
                    slower = false
                    higherSpeed
                }
            }

            VK_LEFT, VK_RIGHT -> {
                if (!PlayerManager.binds[keyCode]) {
                    moveState = STATE_STILL
                }
            }
        }
        if (PlayerManager.binds[keyCode]) {
            val func = keyEvents[keyCode]
            if (func != null)
                func(this)
        }
    }

    final override fun move() {}

    override fun collide(o: Object): Boolean {
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
        val xI = x.get()
        val yI = y.get()
        val point = t.renderPoint(xI, yI)
        t.paint(g, t.normalMatrix(), point.x, point.y)

        if (slower) with(GFX.getTexture("center")) {
            val rd = renderPoint(xI, yI)
            this.paint(g, normalMatrix(), rd.x, rd.y)
        }

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
        val keyEvents: MutableMap<Int, (Player) -> Unit> = HashMap(
            mapOf(VK_LEFT to {
                it.moveState = STATE_LEFT
                val oldX = it.x.get()
                var newX = oldX - it.speed
                val limitX = Graphics.getUIInsets().left.toDouble()
                if (newX < limitX)
                    newX = limitX
                it.x.set(newX)
            }, VK_RIGHT to {
                it.moveState = STATE_RIGHT
                val oldX = it.x.get()
                var newX = oldX + it.speed
                val limitX = Graphics.getScreenSize().width - Graphics.getUIInsets().right
                if (newX > limitX)
                    newX = limitX
                it.x.set(newX)
            }, VK_UP to {
                val oldY = it.y.get()
                var newY = oldY - it.speed
                val limitY = Graphics.getUIInsets().top.toDouble()
                if (newY < limitY)
                    newY = limitY
                it.y.set(newY)
            }, VK_DOWN to {
                val oldY = it.y.get()
                var newY = oldY + it.speed
                val limitY = Graphics.getScreenSize().height - Graphics.getUIInsets().bottom
                if (newY > limitY)
                    newY = limitY
                it.y.set(newY)
            }, VK_Z to {
                if (it.shotCoolCount++ == it.shotCoolDown) {
                    it.shotCoolCount = 0
                    it.shot()
                }
            }, VK_X to {
                it.bomb()
                // avoid bomb continually
                PlayerManager.binds[VK_X] = false
            }, VK_C to {
                // unbind
            }, VK_ESCAPE to {
                val gameState = GenericFlags.gameState.get()
                if (gameState == GenericFlags.STATE_PLAYING) {
                    GenericFlags.gameState.set(GenericFlags.STATE_PAUSE)
                    it.logger.log(System.Logger.Level.INFO, "Pause the game.")
                } else if (gameState == GenericFlags.STATE_PAUSE) {
                    GenericFlags.gameState.set(GenericFlags.STATE_PLAYING)
                    it.logger.log(System.Logger.Level.INFO, "Continue the game.")
                }
                PlayerManager.binds[VK_ESCAPE] = false
            }, VK_SHIFT to {
                // empty
            })
        )

        class EmptyPlayer : Player(25, 25) {
            override fun dead() {}

            override fun beingHit(o: Object) {}
            override fun actionsImpl() {}

            override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 5)

            override fun bulletDamage(): Int = 0

            override fun bullet(dx: Int, dy: Int): PlayerBullet = EmptyBullet(x() + dx, y() + dy)

            override fun texture(): String = "NOT_FOUND"

            override fun bomb() {}
        }

        class EmptyBullet(initialX: Int, initialY: Int) : PlayerBullet(initialX, initialY) {
            override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 10)

            override fun move() {
                setY(y() + 1)
            }

            override fun paint(g: Graphics2D) {}
        }
    }
}