package top.kkoishi.stg.common.ui

import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.gfx.Texture
import java.awt.Font

abstract class MenuItem(x: Int, y: Int, val menu: Menu) : UIObject(x, y) {
    var parent: MenuItem? = null
    var select: Int = 0
    val children: ArrayDeque<MenuItem?> = ArrayDeque(4)
    val items = ArrayDeque<Texture>(4)

    final override fun shouldAction(): Boolean = false
    override fun shouldRender(): Boolean = true

    override fun font(): Font = Graphics.font("menu_item_default")

    final override fun update(): Boolean {
        updateInfo()
        return false
    }
}