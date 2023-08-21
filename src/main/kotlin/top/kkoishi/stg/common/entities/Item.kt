package top.kkoishi.stg.common.entities

import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.ObjectPool
import java.awt.Graphics2D
import java.util.concurrent.atomic.AtomicReference

abstract class Item(initialX: Int, initialY: Int) : Entity(-1) {
    val x: AtomicReference<Double> = AtomicReference(initialX.toDouble())
    val y: AtomicReference<Double> = AtomicReference(initialY.toDouble())
    private var erased: Boolean = false
    protected var shouldRemove: Boolean = false

    abstract fun texture(): String

    abstract fun getItem(player: Player)

    abstract fun moveToPlayer(player: Player)

    fun x() = x.get().toInt()
    fun y() = y.get().toInt()

    override fun update(): Boolean {
        if (erased && shouldRemove)
            return true
        if (erased)
            moveToPlayer(ObjectPool.player())
        else {
            erased = collide(ObjectPool.player())
            if (!erased)
                move()
        }

        return false
    }

    final override fun isDead(): Boolean = false

    final override fun dead() {}

    final override fun beingHit(o: Object) {}

    override fun collide(o: Object): Boolean {
        if (o is Player) {
            if (CollideSystem.collide(o, this)) {
                getItem(o)
                return true
            }
        }
        return CollideSystem.checkPos(x(), y())
    }

    override fun paint(g: Graphics2D) {
        with(GFX.getTexture(texture())) {
            this@with.paint(g, normalMatrix(), x(), y())
        }
    }
}