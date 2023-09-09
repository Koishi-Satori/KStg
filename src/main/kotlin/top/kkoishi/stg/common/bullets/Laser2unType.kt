package top.kkoishi.stg.common.bullets

import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.exceptions.InternalError
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Point2D

@Suppress("DeprecatedCallableAddReplaceWith")
abstract class Laser2unType : Bullet {
    constructor(initialX: Int, initialY: Int, vararg initialBullets: SubBullet) : super(initialX, initialY) {
        initialBullets.forEach {
            val p = it.calculatePos(initialX.toDouble(), initialY.toDouble())
            it.setX(p.x)
            it.setY(p.y)
            bullets.addLast(it)
        }
    }

    constructor(initialX: Int, initialY: Int, initialBullets: Collection<SubBullet>) : super(initialX, initialY) {
        initialBullets.forEach {
            val p = it.calculatePos(initialX.toDouble(), initialY.toDouble())
            it.setX(p.x)
            it.setY(p.y)
            bullets.addLast(it)
        }
    }

    protected val bullets = ArrayDeque<SubBullet>()
    protected val lock = Any()

    protected open fun removeBullet(index: Int) {
        bullets.removeAt(index)
    }

    protected open fun addBullet(subBullet: SubBullet) = synchronized(lock) {
        val p = subBullet.calculatePos(xD(), yD())
        subBullet.setX(p.x)
        subBullet.setY(p.y)
        bullets.addLast(subBullet)
    }

    fun count() = synchronized(lock) { bullets.size }

    override fun update(): Boolean {
        var count: Int
        val rest: Array<SubBullet>
        synchronized(lock) {
            count = bullets.size
            rest = bullets.toTypedArray()
        }

        if (count > 0) {
            val removeIndexes = ArrayDeque<Int>()
            rest.forEachIndexed { index, bullet ->
                if (update(bullet)) {
                    removeIndexes.addLast(index)
                    return@forEachIndexed
                }
            }

            synchronized(lock) {
                while (removeIndexes.isNotEmpty()) {
                    removeBullet(removeIndexes.removeFirst())
                    --count
                }
            }
        }

        return count <= 0
    }

    override fun paint(g: Graphics2D) {
        val rest = synchronized(lock) { bullets.toTypedArray() }
        println("$uuid ${rest.size}")
        if (rest.isNotEmpty())
            rest.forEach { it.paint(g) }
    }

    private fun update(subBullet: SubBullet): Boolean = subBullet.update()

    /**
     * ## Do not invoke this method.
     */
    @Deprecated("Collide test is finished in update method.", level = DeprecationLevel.ERROR)
    final override fun collide(o: Object): Boolean {
        val rest: Iterator<IndexedValue<SubBullet>> =
            synchronized(lock) { bullets.toTypedArray().withIndex().iterator() }

        var index = -1
        while (rest.hasNext()) {
            val bullet = rest.next()
            val b = bullet.value
            if (b.collide(o))
                index = bullet.index
        }

        if (index != -1) {
            synchronized(lock) {
                removeBullet(index)
                return true
            }
        }
        return false
    }

    /**
     * ## Do not invoke this method.
     */
    @Deprecated("This method is useless.", level = DeprecationLevel.ERROR)
    final override fun shape(): Shape = throw InternalError("You should not invoke this method.")



    abstract class SubBullet : AbstractBullet(0, 0) {
        abstract fun calculatePos(laserHeadX: Double, laserHeadY: Double): Point2D
    }
}