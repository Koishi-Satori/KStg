package top.kkoishi.stg.common

import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.Graphics
import java.awt.Graphics2D

abstract class Stage {
    open fun paint(g: Graphics2D) {
        val items = background()
        for (item in items)
            item.paint(g, item.normalMatrix(), Graphics.getCenterX(), Graphics.getCenterY())
    }

    abstract fun background(): Iterable<Texture>

    abstract fun action()
}