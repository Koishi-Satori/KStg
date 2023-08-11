package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.util.concurrent.atomic.AtomicLong

class Renderer private constructor() : Runnable {
    private val frame = AtomicLong(0)
    var fullScreen = false
    var scale: Pair<Double, Double> = 1.0 to 1.0
    var dx = 0
    var dy = 0

    private fun fullScreen() {
        val monitorMode = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode
        val oldScreenSize = Dimension(800, 600)
        val monitorRate = monitorMode.width.toDouble() / monitorMode.height
        val rate = oldScreenSize.width / oldScreenSize.height
        val oScale: Double
        if (monitorRate > rate) {
            oScale = monitorMode.height.toDouble() / oldScreenSize.height
            dx = ((monitorMode.width - oScale * oldScreenSize.width) / 2).toInt()
        }
        else {
            oScale = monitorMode.width.toDouble() / oldScreenSize.width
            dy = ((monitorMode.height - oScale * oldScreenSize.height) / 2).toInt()
        }

        scale = oScale to oScale
        fullScreen = true
        Graphics.setFrameInsets(Insets(0, 0, 0, 0))
        this::class.logger().log(System.Logger.Level.INFO, "Switch to full screen mode.")
    }

    private fun renderFPS(r: Graphics2D) {
        val fps = InfoSystem.fps().toString()
        val rd = Graphics.renderPointFPS()
        r.font = Graphics.font("fps_render")
        r.drawString(fps, rd.x, rd.y)
    }

    fun paint() {
        val logger = Renderer::class.logger()
        val gameState = GenericFlags.gameState.get()
        val r = Graphics.render()
        val bRender = Graphics.buffer().createGraphics()
        val size = Graphics.getScreenSize()
        val insets = Graphics.getFrameInsets()
        val uiInsets = Graphics.getUIInsets()

        // clear the screen
        bRender.color = Color.WHITE
        bRender.fillRect(0, 0, size.width.toInt() + uiInsets.left, size.height.toInt() + uiInsets.top)
        when (gameState) {
            GenericFlags.STATE_PLAYING -> {
                try {
                    PlayerManager.cur.paint(bRender)
                    ObjectPool.player.paint(bRender)
                    ObjectPool.objects().forEach { it.paint(bRender) }
                    ObjectPool.bullets().forEach { it.paint(bRender) }
                    ObjectPool.uiObjects().forEach { it.paint(bRender) }
                    renderFPS(bRender)
                    bRender.dispose()
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            GenericFlags.STATE_PAUSE -> {
                PlayerManager.cur.paint(bRender)
                ObjectPool.player.paint(bRender)
                ObjectPool.objects().forEach { it.paint(bRender) }
                ObjectPool.bullets().forEach { it.paint(bRender) }
                ObjectPool.uiObjects().forEach { it.paint(bRender) }
                renderFPS(bRender)
            }

            GenericFlags.STATE_MENU -> {
                try {
                    // main menu
                    ObjectPool.uiObjects().forEach { it.paint(bRender) }
                    renderFPS(bRender)
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }
        }
        bRender.dispose()
        if (!fullScreen) {
            r.drawImage(
                Graphics.buffer(),
                AffineTransformOp(AffineTransform(), AffineTransformOp.TYPE_NEAREST_NEIGHBOR),
                insets.left,
                insets.top
            )
        } else {
            r.drawImage(
                Graphics.buffer(),
                AffineTransformOp(
                    AffineTransform.getScaleInstance(scale.first, scale.second),
                    AffineTransformOp.TYPE_NEAREST_NEIGHBOR
                ),
                dx,
                dy
            )
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

        fun syncFrame() {
            instance.frame.set(GameLoop.logicFrame())
        }

        fun fullScreen() = instance.fullScreen()

        fun monitorSize(): Dimension {
            val monitorMode = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode
            return Dimension(monitorMode.width, monitorMode.height)
        }
    }
}