package top.kkoishi.stg.common.ui

import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.GenericSystem
import java.awt.Font
import java.awt.Graphics2D

/**
 * The game menu ui.
 *
 * @author Kkoishi_
 */
abstract class Menu(private val actionState: Int) : UIObject(0, 0) {
    /**
     * The root item of the menu.
     */
    private lateinit var rootItem: MenuItem

    /**
     * Current item of the menu.
     */
    lateinit var curLevel: MenuItem

    /**
     * Set the root item, and set the current item to the new root.
     */
    fun setRoot(root: MenuItem) {
        rootItem = root
        curLevel = rootItem
    }

    /**
     * Paint the background of the menu.
     *
     * @param r the Graphics
     */
    abstract fun paintBackground(r: Graphics2D)

    /**
     * Should the menu actions, used to control when the menu should be updated.
     *
     * @return should the menu be updated.
     */
    final override fun shouldAction(): Boolean = GenericSystem.gameState.get() == actionState

    /**
     * Returns should the menu be rendered.
     *
     * @return should the menu be rendered.
     */
    override fun shouldRender(): Boolean = shouldAction()

    override fun font(): Font = Graphics.font("menu")

    override fun updateInfo() {
        curLevel.update()
    }

    override fun update(): Boolean {
        if (shouldAction())
            updateInfo()
        return false
    }

    override fun paint(g: Graphics2D) {
        if (shouldAction()) {
            paintBackground(g)
            curLevel.paint(g)
        }
    }
}