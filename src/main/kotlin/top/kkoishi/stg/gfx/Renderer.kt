package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.util.concurrent.atomic.AtomicLong

class Renderer private constructor() : Runnable {
    private val frame = AtomicLong(0)
    private var fullScreen = false
    private var scale: Pair<Double, Double> = 1.0 to 1.0
    private var dx = 0
    private var dy = 0

    private fun scale(targetWidth: Int, targetHeight: Int) {
        val screenSize = Graphics.getScreenSize()
        val transform =
            GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform

        val targetRate = targetWidth.toDouble() / targetHeight
        val rate = screenSize.width / screenSize.height
        var oScale: Double
        if (targetRate > rate) {
            oScale = targetHeight.toDouble() / screenSize.height
            dx = ((targetWidth - oScale * screenSize.width) / 2).toInt()
            oScale /= transform.scaleX
        } else {
            oScale = targetWidth.toDouble() / screenSize.width
            dy = ((targetHeight - oScale * screenSize.height) / 2).toInt()
            oScale /= transform.scaleY
        }

        scale = oScale to oScale
    }

    private fun fullScreen() {
        val monitorMode = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode
        scale(monitorMode.width, monitorMode.height)
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
        bRender.color = Color.WHITE
        bRender.setPaintMode()
        val insets = Graphics.getFrameInsets()

        // clear the screen
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