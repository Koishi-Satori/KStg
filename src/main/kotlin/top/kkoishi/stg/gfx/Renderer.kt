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
            val buffer = Graphics.buffer().createGraphics()

            // clear the screen
            val size = Graphics.getScreenSize()
            val oldColor = buffer.color
            buffer.color = Color.WHITE
            buffer.fillRect(0, 0, size.width.toInt(), size.height.toInt())
            buffer.color = oldColor

            ObjectPool.player.paint(buffer)
            PlayerManager.cur.paint(buffer)
            for (o in ObjectPool.objects())
                o.paint(buffer)
            for (b in ObjectPool.bullets())
                b.paint(buffer)

            val r = Graphics.render()
            val insets = Graphics.getFrameInsets()
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