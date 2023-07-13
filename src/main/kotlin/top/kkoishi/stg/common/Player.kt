package top.kkoishi.stg.common

import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.CollideSystem
import top.kkoishi.stg.logic.GenericFlags
import top.kkoishi.stg.logic.Graphics
import top.kkoishi.stg.logic.PlayerManager
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

abstract class Player(initialX: Int, initialY: Int) : Entity(0) {
    val x = AtomicInteger(initialX)
    val y = AtomicInteger(initialY)
    protected var speed = 10
    protected var higherSpeed = 10
    protected var lowerSpeed = 5

    override fun isDead(): Boolean = PlayerManager.life() == 0

    abstract fun bulletDamage(): Int

    abstract fun bullet(): PlayerBullet

    abstract fun texture(): String

    abstract fun bomb()

    override fun beingHit(o: Object) {
        val oldLife = PlayerManager.life()
        PlayerManager.setLife(oldLife - 1)
    }

    open fun shot() = PlayerManager.addBullet(bullet())

    private fun actions() {
        for (keyCode in keyEvents.keys) {
            action(keyCode)
        }
    }

    private fun action(keyCode: Int) {
        if (keyCode == VK_SHIFT) {
            // press shift
            speed = if (PlayerManager.binds[keyCode])
                higherSpeed
            else
                lowerSpeed
        } else
            if (PlayerManager.binds[keyCode]) {
                println("press key: $keyCode")
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
        t.paint(g, t.normalMatrix(), xI, yI)

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
        private val keyEvents: MutableMap<Int, (Player) -> Unit> = HashMap(
            mapOf(VK_LEFT to {
                val oldX = it.x.get()
                var newX = oldX - it.speed
                if (newX < 0)
                    newX = 0
                it.x.set(newX)
            }, VK_RIGHT to {
                val oldX = it.x.get()
                var newX = oldX + it.speed
                if (newX > Graphics.getScreenSize().width)
                    newX = Graphics.getScreenSize().width.toInt()
                it.x.set(newX)
            }, VK_UP to {
                val oldY = it.y.get()
                var newY = oldY - it.speed
                if (newY < 0)
                    newY = 0
                it.y.set(newY)
            }, VK_DOWN to {
                val oldY = it.y.get()
                var newY = oldY + it.speed
                if (newY > Graphics.getScreenSize().height)
                    newY = Graphics.getScreenSize().height.toInt()
                it.y.set(newY)
            }, VK_Z to {
                it.shot()
            }, VK_X to {
                it.bomb()
                // avoid bomb continually
                PlayerManager.binds[VK_X] = false
            }, VK_C to {
                // unbind
            }, VK_ESCAPE to {
                val gameState = GenericFlags.gameState.get()
                if (gameState == GenericFlags.STATE_PLAYING)
                    GenericFlags.gameState.set(GenericFlags.STATE_PAUSE)
                else
                    GenericFlags.gameState.set(GenericFlags.STATE_PLAYING)
                PlayerManager.binds[VK_ESCAPE] = false
            }, VK_SHIFT to {
                // empty
            })
        )

        class EmptyPlayer : Player(25, 25) {
            override fun dead() {}

            override fun beingHit(o: Object) {}

            override fun shape(): Shape = CollideSystem.Circle(Point(x.get(), y.get()), 5)

            override fun bulletDamage(): Int = 0

            override fun bullet(): PlayerBullet = EmptyBullet(x.get(), y.get())

            override fun texture(): String = "NOT_FOUND"

            override fun bomb() {}
        }

        class EmptyBullet(initialX: Int, initialY: Int) : PlayerBullet(initialX, initialY) {
            override fun shape(): Shape = CollideSystem.Circle(Point(x.get(), y.get()), 10)

            override fun move() {
                y.set(y.get() + 1)
            }

            override fun paint(g: Graphics2D) {}
        }
    }
}