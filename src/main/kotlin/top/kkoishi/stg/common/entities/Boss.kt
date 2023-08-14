package top.kkoishi.stg.common.entities

import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import java.awt.Graphics2D
import java.util.concurrent.atomic.AtomicReference

abstract class Boss(health: Int, initialX: Int, initialY: Int) : Enemy(health) {
    private val x: AtomicReference<Double> = AtomicReference(initialX.toDouble())
    private val y: AtomicReference<Double> = AtomicReference(initialY.toDouble())

    fun x(): Double = x.get()
    fun y(): Double = y.get()
    fun x(x: Double) = this.x.set(x)
    fun y(y: Double) = this.y.set(y)

    abstract fun texture(): String
    abstract fun paintOthers(r: Graphics2D)
    abstract fun paintBossBar(r: Graphics2D)
    abstract fun end(): Boolean
    abstract fun action()

    override fun collide(o: Object): Boolean {
        if (o is PlayerBullet && CollideSystem.collide(this, o)) {
            beingHit(o)
            return true
        }
        return false
    }

    override fun isDead(): Boolean = health <= 0

    override fun update(): Boolean {
        val dead = super.update()
        if (!dead)
            action()
        return end() && dead
    }

    override fun paint(g: Graphics2D) {
        val texture = GFX.getTexture(texture())
        val rd = texture.renderPoint(x().toInt(), y().toInt())
        texture.paint(g, texture.normalMatrix(), rd.x, rd.y)
        paintOthers(g)
        paintBossBar(g)
    }
}