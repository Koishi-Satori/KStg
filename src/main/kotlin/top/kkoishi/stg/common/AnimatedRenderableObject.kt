package top.kkoishi.stg.common

import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.Timer
import java.awt.Graphics2D
import java.awt.image.AffineTransformOp

abstract class AnimatedRenderableObject(initialX: Int, initialY: Int, frames: Long, initialTexture: String) :
    RenderableObject(initialX, initialY) {
    private val timer = Timer.Default(frames)
    private val lock = Any()
    protected var curTexture: Texture = GFX[initialTexture]
    protected var op: AffineTransformOp = Texture.NORMAL_MATRIX

    abstract fun texture(curFrame: Long): String
    abstract fun transform(curFrame: Long)

    override fun update(): Boolean {
        if (timer.end())
            return true
        synchronized(lock) {
            val cur = timer.cur()
            transform(cur)
            curTexture = GFX[texture(cur)]
        }
        return false
    }

    override fun paint(g: Graphics2D) = synchronized(lock) {
        curTexture.paint(g, op, x().toInt(), y().toInt())
    }
}