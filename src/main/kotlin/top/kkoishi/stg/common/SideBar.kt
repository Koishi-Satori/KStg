package top.kkoishi.stg.common

import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.GenericFlags
import java.awt.Font
import java.awt.Graphics2D

abstract class SideBar(val x: Int, val y: Int) : Object {
    abstract fun background(): String

    abstract fun paintInfo(r: Graphics2D)

    abstract fun updateInfo()

    protected open fun shouldAction(): Boolean = GenericFlags.gameState.get() > 0

    protected open fun font(): Font = Graphics.font("sidebar")

    override fun update(): Boolean {
        if (shouldAction()) {
            updateInfo()
        }
        return false
    }

    override fun collide(o: Object): Boolean = false

    override fun paint(g: Graphics2D) {
        if (shouldAction()) {
            // render background
            val texture = GFX.getTexture(background())
            texture.paint(g, texture.normalMatrix(), x, y)
            // render scores
            paintInfo(g)
        }
    }
}