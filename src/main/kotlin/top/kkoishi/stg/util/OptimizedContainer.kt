package top.kkoishi.stg.util

import java.awt.Color
import java.awt.Graphics
import java.awt.GraphicsConfiguration
import javax.swing.JFrame

class OptimizedContainer : JFrame {
    constructor(title: String, gc: GraphicsConfiguration) : super(title, gc) {
        contentPane.isVisible = false
        background = Color.BLACK
    }

    @JvmOverloads
    constructor(title: String = DEFAULT_TITLE) : super(title) {
        contentPane.isVisible = false
        background = Color.BLACK
    }

    constructor(gc: GraphicsConfiguration) : super(DEFAULT_TITLE, gc) {
        contentPane.isVisible = false
        background = Color.BLACK
    }

    override fun paintAll(g: Graphics?) {
    }

    override fun paintComponents(g: Graphics?) {
    }

    override fun paint(g: Graphics?) {
    }

    companion object {
        const val DEFAULT_TITLE = "KKoishi_ Stg Engine"
    }
}