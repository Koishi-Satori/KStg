package top.kkoishi.stg.common.entities

import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.CollideSystem.Circle
import java.awt.Polygon
import java.awt.Shape
import java.awt.geom.*

/**
 * All the entities' base class.
 *
 * @param health the health of the entity.
 * @author KKoishi_
 */
abstract class Entity(protected var health: Int) : Object {
    /**
     * If the entity is dead.
     */
    abstract fun isDead(): Boolean

    /**
     * Actions invoked after the entity is dead.
     */
    abstract fun dead()

    /**
     * What to do after hit.
     */
    abstract fun beingHit(o: Object)

    /**
     * How the entity move.
     */
    abstract fun move()

    /**
     * The shape of the entity, used for collide test.
     *
     * ## ⚠ The shape should be one of [Rectangle2D], [Polygon], [Circle]
     * ### And if you want this engine supports more shapes, please implement your collide checks of them using
     * [CollideSystem.registeredSSCollideCheck], [CollideSystem.registeredCSCollideCheck]
     *
     * @return Shape.
     */
    abstract fun shape(): Shape

    override fun update(): Boolean {
        if (isDead()) {
            dead()
            return true
        }
        return false
    }
}