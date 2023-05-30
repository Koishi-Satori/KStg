package top.kkoishi.stg.common

import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.CollideSystem
import top.kkoishi.stg.logic.ObjectPool
import java.awt.Graphics2D
import kotlin.math.sqrt

abstract class AbstractBullet(initialX: Int, initialY: Int) : Bullet(initialX, initialY) {
    protected var erased = false
    override fun update(): Boolean {
        if (!collideTest())
            move()
        return erased || CollideSystem.checkPos(this)
    }

    abstract fun move()

    override fun collide(o: Object): Boolean {
        if (o is Entity) {
            val from = from()
            if (from == null || from != o)
                if (CollideSystem.collide(o, this)) {
                    this.erase()
                    o.beingHit(this)
                    return true
                }
        }
        return false
    }

    internal fun erase() {
        erased = true
    }

    open fun from(): Entity? = null

    open fun collideTest(): Boolean = collide(ObjectPool.player)
}

abstract class PlayerBullet(initialX: Int, initialY: Int) : AbstractBullet(initialX, initialY) {
    override fun collideTest(): Boolean {
        for (e in ObjectPool.objects()) {
            if (e is Enemy)
                if (collide(e))
                    return true
        }
        return false
    }

    override fun collide(o: Object): Boolean {
        if (o is Entity) {
            if (CollideSystem.collide(o, this)) {
                this.erase()
                o.beingHit(this)
                return true
            }
        }
        return false
    }

    override fun from(): Entity? = ObjectPool.player
}

object Bullets {
    fun createSniper(initialX: Int, initialY: Int, speed: Float, gfx: String, rotated: Boolean = false): Bullet {
        if (rotated) {
            return object : AbstractBullet(initialX, initialY) {
                val texture = GFX.getTexture(gfx)
                val sin: Double
                val cos: Double
                val vx: Double
                val vy: Double

                init {
                    val x = x.get()
                    val y = y.get()
                    val dx = (ObjectPool.player.y.get() - y).toDouble()
                    val dy = (ObjectPool.player.x.get() - x).toDouble()
                    val scale = sqrt(dx * dx + dy * dy)
                    sin = dy / scale
                    cos = dx / scale
                    vx = speed * sin
                    vy = speed * cos
                }

                override fun move() {
                    val xBefore = x.get()
                    val yBefore = y.get()
                    x.set(xBefore + vx.toInt())
                    y.set(yBefore + vy.toInt())
                }

                override fun collide(o: Object): Boolean {
                    TODO("Not yet implemented")
                }

                override fun paint(g: Graphics2D) {
                    val x = x.get()
                    val y = y.get()
                    val p = texture.renderPoint(x, y)
                    texture.paint(g, texture.rotate(sin, cos, x.toDouble(), y.toDouble()), p.x, p.y)
                }
            }
        }

        return object : AbstractBullet(initialX, initialY) {
            val texture = GFX.getTexture(gfx)
            val sin: Double
            val cos: Double
            val vx: Double
            val vy: Double

            init {
                val x = x.get()
                val y = y.get()
                val dx = (ObjectPool.player.y.get() - y).toDouble()
                val dy = (ObjectPool.player.x.get() - x).toDouble()
                val scale = sqrt(dx * dx + dy * dy)
                sin = dy / scale
                cos = dx / scale
                vx = speed * sin
                vy = speed * cos
            }

            override fun move() {
                val xBefore = x.get()
                val yBefore = y.get()
                x.set(xBefore + vx.toInt())
                y.set(yBefore + vy.toInt())
            }

            override fun collide(o: Object): Boolean {
                TODO("Not yet implemented")
            }

            override fun paint(g: Graphics2D) {
                val x = x.get()
                val y = y.get()
                val p = texture.renderPoint(x, y)
                texture.paint(g, texture.normalMatrix(), p.x, p.y)
            }
        }
    }

    fun createLine(
        initialX: Int,
        initialY: Int,
        endX: Int,
        endY: Int,
        speed: Float,
        gfx: String,
        rotated: Boolean = false,
    ) {

    }
}