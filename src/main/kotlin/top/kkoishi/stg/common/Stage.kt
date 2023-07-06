package top.kkoishi.stg.common

import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.Graphics
import java.awt.Graphics2D

abstract class Stage {
    open fun paint(g: Graphics2D) {
        val back = background()
        back.paint(g, back.normalMatrix(), Graphics.getCenterX(), Graphics.getCenterY())
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