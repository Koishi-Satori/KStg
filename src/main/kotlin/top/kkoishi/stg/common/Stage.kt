package top.kkoishi.stg.common

import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.gfx.Texture
import java.awt.Graphics2D

abstract class Stage {
    open fun paint(g: Graphics2D) {
        val back = background()
        val uiInsets = Graphics.getUIInsets()
        var pX = uiInsets.left - 2
        var pY = uiInsets.top - 2
        if (pX < 0)
            pX = 0
        if (pY < 0)
            pY = 0
        back.paint(g, back.normalMatrix(), pX, pY)
    }

    abstract fun background(): Texture

    abstract fun action()

    abstract fun toNextStage(): Boolean

    abstract fun nextStage(): Stage

    companion object {
        class EmptyStage : Stage() {
            override fun background(): Texture = throw IllegalStateException()
            override fun action() {}
            override fun toNextStage(): Boolean = false
            override fun nextStage(): Stage = throw IllegalStateException()
            override fun paint(g: Graphics2D) {}
        }
    }
}