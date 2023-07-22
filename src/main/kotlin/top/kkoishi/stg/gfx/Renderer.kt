package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.util.concurrent.atomic.AtomicLong

class Renderer private constructor() : Runnable {
    private val frame = AtomicLong(0)

    private fun renderFPS(r: Graphics2D) {
        val fps = InfoSystem.fps().toString()
        val rd = Graphics.renderPointFPS()
        r.font = Graphics.font("fps_render")
        r.drawString(fps, rd.x, rd.y)
    }

    fun paint() {
        val logger = Renderer::class.logger()
        val gameState = GenericFlags.gameState.get()
        if (gameState == GenericFlags.STATE_PLAYING) {
            try {
                ObjectPool.lock()

                val r = Graphics.render()
                val bRender = Graphics.buffer().createGraphics()
                val size = Graphics.getScreenSize()
                val insets = Graphics.getFrameInsets()
                r.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                r.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

                // clear the screen
                bRender.color = Color.WHITE
                bRender.fillRect(0, 0, size.width.toInt(), size.height.toInt())

                PlayerManager.cur.paint(bRender)
                ObjectPool.player.paint(bRender)
                renderFPS(bRender)
                for (o in ObjectPool.objects())
                    o.paint(bRender)
                for (b in ObjectPool.bullets())
                    b.paint(bRender)
                bRender.dispose()

                r.drawImage(
                    Graphics.buffer(),
                    AffineTransformOp(AffineTransform(), AffineTransformOp.TYPE_NEAREST_NEIGHBOR),
                    insets.left,
                    insets.top
                )
                ObjectPool.release()
            } catch (e: Exception) {
                logger.log(System.Logger.Level.ERROR, e)
            }
        } else if (gameState == GenericFlags.STATE_PAUSE) {
            // render menu
        }
        frame.incrementAndGet()
    }

    override fun run() {
        paint()
    }

    companion object {
        private val instance = Renderer()

        @JvmStatic
        fun start(threads: Threads) {
            threads.schedule(instance, Threads.period())
        }

        fun frame(): Long = instance.frame.get()
    }
}