package top.kkoishi.stg.common.entities

import java.awt.Graphics2D

/**
 * It is more recommend to use this replaces of [Item], because you can implement certain function easily.
 *
 * @author KKoishi_
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class BaseItem(
    initialX: Int,
    initialY: Int,
    protected val speed: Double,
) :
    Item(initialX, initialY) {
    override fun move() {
        val oldY = y.get()
        y.set(oldY + speed)
    }

    /**
     * Determines that how the item moves to the player after it is absorbed.
     * And after the item is fully absorbed, you should invoke super.moveToPlayer or use ```shouldRemove = true```
     * for removing this item.
     */
    override fun moveToPlayer(player: Player) {
        shouldRemove = true
    }

    final override fun update(): Boolean = super.update()

    final override fun collide(o: Object): Boolean = super.collide(o)

    final override fun paint(g: Graphics2D) = super.paint(g)
}

