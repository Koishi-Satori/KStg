package top.kkoishi.stg.common.bullets

import top.kkoishi.stg.common.entities.Object
import java.awt.Point
import java.awt.Shape
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * This class represents the bullets in stg and provides shape and coordinate support for bullets
 * for collision detection.
 *
 * @param initialX the initial X coordinates of the bullet.
 * @param initialY the initial Y coordinates of the bullet.
 * @author KKoishi_
 */
abstract class Bullet(initialX: Int, initialY: Int) : Object {
    private val x = AtomicReference(initialX.toDouble())
    private val y = AtomicReference(initialY.toDouble())
    private val bulletUUID = UUID.randomUUID()

    override val uuid: UUID
        get() = bulletUUID

    /**
     * Get the X coordinates of the bullet in int.
     */
    fun x(): Int = x.get().toInt()

    /**
     * Get the Y coordinates of the bullet in int.
     */
    fun y(): Int = y.get().toInt()

    /**
     * Get the X coordinates of the bullet in double.
     */
    fun xD(): Double = x.get()

    /**
     * Get the Y coordinates of the bullet in double.
     */
    fun yD(): Double = y.get()

    /**
     * Get the coordinates of the bullet.
     */
    fun pos(): Point = Point(x(), y())

    fun setX(nX: Int) {
        x.set(nX.toDouble())
    }

    fun setY(nY: Int) {
        y.set(nY.toDouble())
    }

    fun setX(nX: Double) {
        x.set(nX)
    }

    fun setY(nY: Double) {
        y.set(nY)
    }

    /**
     * The shape for collide test.
     *
     * Please check [top.kkoishi.stg.gfx.CollideSystem] for more instructions.
     *
     * @return a shape for collide test.
     */
    abstract fun shape(): Shape

    final override fun toString(): String = "Bullet@$uuid"
}