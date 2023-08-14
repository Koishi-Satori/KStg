package top.kkoishi.stg.common.entities

import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.logic.ObjectPool
import java.awt.Graphics2D
import java.awt.Shape
import java.util.concurrent.atomic.AtomicReference
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

    override fun from(): Player = ObjectPool.player
}

abstract class BaseItem(
    initialX: Int,
    initialY: Int,
    val speed: Double,
) :
    Item(initialX, initialY) {
    override fun move() {
        val oldY = y.get()
        y.set(oldY + speed)
    }

    override fun moveToPlayer(player: Player) {}

    final override fun update(): Boolean = super.update()

    final override fun collide(o: Object): Boolean = super.collide(o)

    final override fun paint(g: Graphics2D) = super.paint(g)
}

abstract class BaseEnemy(health: Int, initialX: Int, initialY: Int): Enemy(health) {
    private val x: AtomicReference<Double> = AtomicReference(initialX.toDouble())
    private val y: AtomicReference<Double> = AtomicReference(initialY.toDouble())

    fun x(): Double = x.get()
    fun y(): Double = y.get()
    fun x(x: Double) = this.x.set(x)
    fun y(y: Double) = this.y.set(y)


    abstract fun texture(): String
    abstract fun paintOthers(r: Graphics2D)
    abstract fun bullet()

    override fun collide(o: Object): Boolean {
        if (o is PlayerBullet && CollideSystem.collide(this, o)) {
            beingHit(o)
            return true
        }
        return false
    }

    override fun isDead(): Boolean = health <= 0

    override fun beingHit(o: Object) {
        this.health -= ObjectPool.player.bulletDamage()
    }

    override fun paint(g: Graphics2D) {
        val t = GFX.getTexture(texture())
        val rd = t.renderPoint(x.get(), y.get())
        t.paint(g, t.normalMatrix(), rd.x, rd.y)
        paintOthers(g)
    }

    override fun update(): Boolean {
        val dead = super.update()
        if (!dead)
            bullet()
        return dead
    }
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
                    val x = x()
                    val y = y()
                    val dx = ObjectPool.player.y.get() - y
                    val dy = ObjectPool.player.x.get() - x
                    val scale = sqrt(dx * dx + dy * dy)
                    sin = dy / scale
                    cos = dx / scale
                    vx = speed * sin
                    vy = speed * cos
                }

                override fun move() {
                    val xBefore = x()
                    val yBefore = y()
                    setX(xBefore + vx.toInt())
                    setY(yBefore + vy.toInt())
                }

                override fun collide(o: Object): Boolean {
                    TODO("Not yet implemented")
                }

                override fun shape(): Shape {
                    TODO("Not yet implemented")
                }

                override fun paint(g: Graphics2D) {
                    val x = xD()
                    val y = yD()
                    val p = texture.renderPoint(x, y)
                    texture.paint(g, texture.rotate(-sin, cos), p.x, p.y)
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
                val x = x()
                val y = y()
                val dx = (ObjectPool.player.y.get() - y).toDouble()
                val dy = (ObjectPool.player.x.get() - x).toDouble()
                val scale = sqrt(dx * dx + dy * dy)
                sin = dy / scale
                cos = dx / scale
                vx = speed * sin
                vy = speed * cos
            }

            override fun move() {
                val xBefore = x()
                val yBefore = y()
                setX(xBefore + vx.toInt())
                setY(yBefore + vy.toInt())
            }

            override fun collide(o: Object): Boolean {
                TODO("Not yet implemented")
            }

            override fun shape(): Shape {
                TODO("Not yet implemented")
            }

            override fun paint(g: Graphics2D) {
                val x = xD()
                val y = yD()
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