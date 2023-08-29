package top.kkoishi.stg.common.ui

import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.GenericSystem
import java.awt.Font
import java.awt.Graphics2D

abstract class Menu(private val actionState: Int): UIObject(0, 0) {
    protected lateinit var rootItem: MenuItem
    lateinit var curLevel: MenuItem

    fun setRoot(root: MenuItem) {
        rootItem = root
        curLevel = rootItem
    }

    abstract fun paintBackground(r: Graphics2D)

    final override fun shouldAction(): Boolean = GenericSystem.gameState.get() == actionState

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