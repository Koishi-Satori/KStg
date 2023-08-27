package top.kkoishi.stg.common.entities

import java.awt.Graphics2D
import java.util.*

/**
 * The basic class of all the render-able and calculated object.
 *
 * @author KKoishi_
 */
interface Object {

    /**
     * The universally unique identifier(UUID) of this object
     *
     * @see UUID
     */
    val uuid: UUID

    /**
     * Check the state of the object, and if this need to be removed, true should be returned.
     *
     * @return if this need to be removed
     */
    fun update(): Boolean

    /**
     * Test if the object collide with another object o.
     *
     * @param o the object to be tested.
     */
    fun collide(o: Object): Boolean

    /**
     * Render the object.
     */
    fun paint(g: Graphics2D)
}
