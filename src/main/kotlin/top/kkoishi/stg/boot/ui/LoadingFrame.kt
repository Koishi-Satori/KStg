package top.kkoishi.stg.boot.ui

import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.swing.JFrame

class LoadingFrame(val background: BufferedImage): JFrame() {
    init {
        size = Dimension(background.width, background.height)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isUndecorated = true
        isResizable = false
        isVisible = true
    }

    fun end() {
        dispose()
    }

    val graphics: Graphics2D = getGraphics() as Graphics2D

    override fun repaint() {
        graphics.drawImage(background, 0, 0, null)
    }

    override fun paint(g: Graphics?) {
        g?.drawImage(background, 0, 0, null)
    }
}