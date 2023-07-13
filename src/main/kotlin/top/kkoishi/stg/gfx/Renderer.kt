package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.*
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp

class Renderer : Runnable {
    fun paint() {
        //println("begin render stage")
        val gameState = GenericFlags.gameState.get()
        if (gameState == GenericFlags.STATE_PLAYING) {
            ObjectPool.lock()

            val r = Graphics.render()
            val bRender = Graphics.buffer().createGraphics()
            val size = Graphics.getScreenSize()
            val insets = Graphics.getFrameInsets()

            // clear the screen
            bRender.color = Color.WHITE
            bRender.fillRect(0, 0, size.width.toInt(), size.height.toInt())

            ObjectPool.player.paint(bRender)
            PlayerManager.cur.paint(bRender)
            for (o in ObjectPool.objects())
                o.paint(bRender)
            for (b in ObjectPool.bullets())
                b.paint(bRender)

            r.drawImage(
                Graphics.buffer(),
                AffineTransformOp(AffineTransform(), AffineTransformOp.TYPE_NEAREST_NEIGHBOR),
                insets.left,
                insets.top
            )
            ObjectPool.release()
        } else if (gameState == GenericFlags.STATE_PAUSE) {
            // render menu
        }
        //println("end render stage")
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
    }
}