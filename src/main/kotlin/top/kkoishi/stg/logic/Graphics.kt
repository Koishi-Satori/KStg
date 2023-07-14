package top.kkoishi.stg.logic

import java.awt.Graphics2D
import java.awt.GraphicsConfiguration
import java.awt.Insets
import java.awt.Point
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

    private val CENTER = Point()

    fun refresh(f: JFrame) {
        GC = f.graphicsConfiguration
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
}