package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.awt.image.VolatileImage
import java.util.concurrent.atomic.AtomicLong

class Renderer private constructor() : Runnable {
    private val frame = AtomicLong(0)
    private var fullScreen = false
    private var scaled = false
    private var useVRAM = false
    private var op: AffineTransformOp = AffineTransformOp(AffineTransform(), AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
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
        op =
            AffineTransformOp(AffineTransform.getScaleInstance(oScale, oScale), AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
        scaled = true
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

    private fun paintImpl(bufferRender: Graphics2D) {
        val logger = Renderer::class.logger()
        bufferRender.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        bufferRender.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        when (GenericFlags.gameState.get()) {
            GenericFlags.STATE_PLAYING -> {
                try {
                    PlayerManager.cur.paint(bufferRender)
                    ObjectPool.player().paint(bufferRender)
                    ObjectPool.objects().forEach { it.paint(bufferRender) }
                    ObjectPool.bullets().forEach { it.paint(bufferRender) }
                    ObjectPool.uiObjects().forEach { it.paint(bufferRender) }
                    renderFPS(bufferRender)
                    bufferRender.dispose()
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            GenericFlags.STATE_PAUSE -> {
                PlayerManager.cur.paint(bufferRender)
                ObjectPool.player().paint(bufferRender)
                ObjectPool.objects().forEach { it.paint(bufferRender) }
                ObjectPool.bullets().forEach { it.paint(bufferRender) }
                ObjectPool.uiObjects().forEach { it.paint(bufferRender) }
                renderFPS(bufferRender)
            }

            GenericFlags.STATE_MENU -> {
                try {
                    // main menu
                    ObjectPool.uiObjects().forEach { it.paint(bufferRender) }
                    renderFPS(bufferRender)
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }
        }
        bufferRender.dispose()
    }

    private fun paintScreen(r: Graphics2D, buffer: BufferedImage) {
        if (!fullScreen) {
            val insets = Graphics.getFrameInsets()
            if (scaled)
                r.drawImage(buffer, op, insets.left + dx, insets.top + dy)
            else
                r.drawImage(buffer, Texture.NORMAL_MATRIX, insets.left, insets.top)
        } else
            r.drawImage(buffer, op, dx, dy)
    }

    private fun paintScreen(r: Graphics2D, buffer: VolatileImage) {
        if (!fullScreen) {
            val insets = Graphics.getFrameInsets()
            if (scaled)
                r.drawImage(
                    buffer.getScaledInstance(
                        (buffer.width * scale.first).toInt(),
                        (buffer.height * scale.second).toInt(),
                        Image.SCALE_DEFAULT
                    ), insets.left + dx, insets.top + dy, null
                ) else
                r.drawImage(buffer, insets.left, insets.top, null)
        } else
            r.drawImage(
                buffer.getScaledInstance(
                    (buffer.width * scale.first).toInt(),
                    (buffer.height * scale.second).toInt(),
                    Image.SCALE_DEFAULT
                ),
                dx, dy, null
            )
    }

    fun paint() {
        val r = Graphics.render()
        val buffer = Graphics.buffer()
        val bRender = buffer.createGraphics()
        bRender.color = Color.WHITE
        bRender.setPaintMode()

        paintImpl(bRender)
        paintScreen(r, buffer)

        frame.incrementAndGet()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun paintWithVRAM() {
        val vImg: VolatileImage = Graphics.vramBuffer()
        val r = Graphics.render()
        var repaintCount = 0

        while (repaintCount++ <= VRAM_MAX_REPAINT_COUNT) {
            val vRender = vImg.createGraphics()
            vRender.color = Color.WHITE
            //vRender.setPaintMode()
            paintImpl(vRender)
            if (!vImg.contentsLost())
                break
        }
        paintScreen(r, vImg)

        frame.incrementAndGet()
    }

    override fun run() {
        if (useVRAM)
            paintWithVRAM()
        else
            paint()
    }

    companion object {
        private var VRAM_MAX_REPAINT_COUNT = 32

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

        fun scale(targetWidth: Int, targetHeight: Int) = instance.scale(targetWidth, targetHeight)

        @JvmOverloads
        fun useVRAM(maxRepaintCount: Int = 32) {
            instance.useVRAM = true
            VRAM_MAX_REPAINT_COUNT = maxRepaintCount
        }

        fun monitorSize(): Dimension {
            val monitorMode = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode
            return Dimension(monitorMode.width, monitorMode.height)
        }

        fun actualScaledSize(targetWidth: Int, targetHeight: Int): Dimension {
            val transform =
                GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform
            return Dimension(
                (targetWidth / transform.scaleX).toInt() + 14,
                (targetHeight / transform.scaleY).toInt() + 37
            )
        }
    }
}