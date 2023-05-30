package top.kkoishi.stg.common

import java.awt.Graphics2D

interface Object {
    /**
     * Check the state of the object, and if this need to be removed, true should be returned.
     */
    fun update(): Boolean
    fun collide(o: Object): Boolean
    fun paint(g: Graphics2D)
}
