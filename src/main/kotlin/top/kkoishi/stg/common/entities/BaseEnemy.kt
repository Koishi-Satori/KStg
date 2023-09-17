package top.kkoishi.stg.common.entities

import top.kkoishi.stg.common.bullets.PlayerBullet
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.ObjectPool
import java.awt.Graphics2D
import java.util.concurrent.atomic.AtomicReference

abstract class BaseEnemy(health: Int, initialX: Int, initialY: Int) : Enemy(health) {
    private val x: AtomicReference<Double> = AtomicReference(initialX.toDouble())
    private val y: AtomicReference<Double> = AtomicReference(initialY.toDouble())

    /**
     * The X coordinate of the enemy in Double.
     */
    fun x(): Double = x.get()

    /**
     * The X coordinate of the enemy in Double.
     */
    fun y(): Double = y.get()
    fun xInt() = x().toInt()
    fun yInt() = y().toInt()
    fun x(x: Double) = this.x.set(x)
    fun y(y: Double) = this.y.set(y)

    /**
     * The key of enemy texture.
     */
    abstract fun texture(): String

    /**
     * Render other textures of the enemy.
     */
    abstract fun paintOthers(r: Graphics2D)

    /**
     * How the enemy fire bullets.
     */
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
        this.health -= ObjectPool.player().bulletDamage()
    }

    override fun paint(g: Graphics2D) {
        val t = GFX.getTexture(texture())
        val rd = t.renderPoint(x.get(), y.get())
        t.paint(g, t.normalMatrix(), rd.x, rd.y)
        paintOthers(g)
    }

    override fun update(): Boolean {
        val dead = super.update()
        if (!dead) {
            move()
            bullet()
        }
        return dead
    }
}