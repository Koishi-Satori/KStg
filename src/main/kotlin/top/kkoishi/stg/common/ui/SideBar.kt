package top.kkoishi.stg.common.ui

import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.GenericSystem
import java.awt.Font
import java.awt.Graphics2D

abstract class SideBar(x: Int, y: Int) : UIObject(x, y) {
    abstract fun background(): String

    abstract fun paintInfo(r: Graphics2D)

    override fun shouldAction(): Boolean = GenericSystem.gameState.get() != GenericSystem.STATE_MENU

    override fun font(): Font = Graphics.font("sidebar")

    override fun update(): Boolean {
        if (shouldAction()) {
            updateInfo()
        }
        return false
    }

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