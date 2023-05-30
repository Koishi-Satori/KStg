package top.kkoishi.stg.common

import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.CollideSystem
import top.kkoishi.stg.logic.PlayerManager
import java.awt.Graphics2D

abstract class Player(initialX: Int, initialY: Int) : Entity(0) {
    val x: ThreadLocal<Int> = ThreadLocal()
    val y: ThreadLocal<Int> = ThreadLocal()

    init {
        x.set(initialX)
        y.set(initialY)
    }

    override fun isDead(): Boolean = PlayerManager.life() == 0

    abstract fun bulletDamage(): Int

    abstract fun bullet(): PlayerBullet

    abstract fun texture(): String

    abstract fun bomb()

    open fun shot() = PlayerManager.addBullet(bullet())

    open fun action(state: Int) {

    }

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
        return false
    }
}