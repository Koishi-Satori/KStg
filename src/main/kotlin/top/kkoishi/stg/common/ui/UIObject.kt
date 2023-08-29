package top.kkoishi.stg.common.ui

import top.kkoishi.stg.common.entities.Object
import java.awt.Font
import java.util.*

/**
 * The basic class of all the UI elements like Menu, Sidebar.
 *
 * @param x the X coordinate of the UIObject.
 * @param y the Y coordinate of the UIObject.
 * @author KKoishi_
 */
abstract class UIObject(val x: Int, val y: Int) : Object {
    private val uiUUID = UUID.randomUUID()
    override val uuid: UUID
        get() = uiUUID

    /**
     * Should the UI logic be calculated.
     */
    abstract fun shouldAction(): Boolean

    /**
     * The font used for paint information.
     *
     * ## Maybe you can use [top.kkoishi.stg.gfx.TexturedFont]?
     */
    abstract fun font(): Font

    /**
     * How the GameLoop update the information of the UIObject.
     */
    abstract fun updateInfo()

    /**
     * Should the UI logic be rendered.
     */
    abstract fun shouldRender(): Boolean

    override fun update(): Boolean {
        if (shouldAction()) {
            updateInfo()
        }
        return false
    }

    final override fun collide(o: Object): Boolean = false
}