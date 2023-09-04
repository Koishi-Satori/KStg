package top.kkoishi.stg.common.ui

import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.gfx.Texture
import java.awt.Font

/**
 * The menu item controls the content of the menu, and it is structured as a dynamic tree, which nodes count is
 * dynamic and nodes can be null to specified special logic.
 *
 * The content of the menu should be the [Menu.curLevel].
 *
 * @author KKoishi_
 */
abstract class MenuItem(x: Int, y: Int, val menu: Menu) : UIObject(x, y) {
    var parent: MenuItem? = null
    var select: Int = 0

    /**
     * The children menu item.
     */
    val children: ArrayDeque<MenuItem?> = ArrayDeque(4)

    /**
     * The textures of the children items.
     */
    val items = ArrayDeque<Texture>(4)

    /**
     * Determines what to happen when the selected is confirmed.
     */
    abstract operator fun invoke()

    final override fun shouldAction(): Boolean = false
    override fun shouldRender(): Boolean = true

    override fun font(): Font = Graphics.font("menu_item_default")

    final override fun update(): Boolean {
        updateInfo()
        return false
    }
}