package top.kkoishi.stg.gfx

import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import java.awt.*
import java.awt.RenderingHints.*
import java.awt.geom.Dimension2D
import java.awt.image.BufferedImage
import java.awt.image.VolatileImage
import javax.swing.JFrame

object Graphics {
    private lateinit var VRAM_BUFFER: VolatileImage
    private lateinit var BUFFER: BufferedImage

    private lateinit var GC: GraphicsConfiguration

    private lateinit var render: Graphics2D

    private lateinit var SCREEN: Dimension2D

    private lateinit var insets: Insets

    private var fpsPosition: Point = Point(0, 20)

    private val fonts: MutableMap<String, Font> = HashMap(64)

    private val CENTER = Point()

    private val DEFAULT_FONT = Font("Times New Roman", Font.BOLD, 20)

    private val INSETS: Insets = Insets(0, 0, 0, 0)

    init {
        setFont("fps_render", DEFAULT_FONT)
    }

    fun refresh(f: JFrame) {
        GC = f.graphicsConfiguration
        if (f.graphics != null)
            setRender(f.graphics as Graphics2D)
        val insets = f.insets
        setFrameInsets(insets)
        val size = f.size
        size.height -= (insets.top + insets.bottom)
        size.width -= (insets.left + insets.right)
        setScreenSize(size)
        setBufferSize(size.width, size.height)
    }

    fun setGC(graphicsConfiguration: GraphicsConfiguration) {
        GC = graphicsConfiguration
    }

    fun setRender(r: Graphics2D) {
        render = r
        render.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
        render.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
    }

    fun render() = render

    fun setScreenSize(d: Dimension2D) {
        SCREEN = d
        val xc = d.width / 2
        val yc = d.height / 2
        CENTER.setLocation(xc, yc)
    }

    fun getScreenSize() = SCREEN

    fun getCenter() = CENTER

    fun getCenterX() = CENTER.x

    fun getCenterY() = CENTER.y

    fun setFrameInsets(nInsets: Insets) {
        insets = nInsets
    }

    fun getFrameInsets() = insets

    fun setBufferSize(width: Int, height: Int) {
        VRAM_BUFFER = GC.createCompatibleVolatileImage(width, height)
        BUFFER = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    }

    fun buffer() = BUFFER

    fun vramBuffer() = VRAM_BUFFER

    fun setFont(key: String, font: Font) {
        fonts[key] = font
    }

    @Throws(FailedLoadingResourceException::class)
    fun font(key: String): Font {
        return fonts[key] ?: DEFAULT_FONT
    }

    fun renderPointFPS() = fpsPosition

    fun setRenderPointFPS(x: Double, y: Double) {
        fpsPosition.setLocation(x, y)
    }

    fun setUIInsets(top: Int, left: Int, bottom: Int, right: Int) {
        INSETS.set(top, left, bottom, right)
        CENTER.setLocation((SCREEN.width.toInt() + left - right) / 2, (top + SCREEN.height.toInt() - bottom) / 2)
    }

    fun getUIInsets() = INSETS
}