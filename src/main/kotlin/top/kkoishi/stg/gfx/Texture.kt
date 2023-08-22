@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.awt.Graphics2D
import java.awt.GraphicsConfiguration
import java.awt.Point
import java.awt.Transparency
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.AffineTransformOp.TYPE_NEAREST_NEIGHBOR
import java.awt.image.BufferedImage
import java.awt.image.RasterFormatException
import java.awt.image.VolatileImage
import kotlin.math.PI
import kotlin.math.cos

open class Texture internal constructor(protected val texture: BufferedImage) {
    val width = texture.width
    val height = texture.height
    fun renderPoint(x: Int, y: Int): Point {
        val dx = width / 2
        val dy = height / 2
        return Point(x - dx, y - dy)
    }

    fun renderPoint(x: Int, y: Int, rad: Double): Point {
        val theta = rad % (PI / 2)
        return renderPoint(x, y, kotlin.math.sin(theta), cos(theta))
    }

    fun renderPoint(x: Int, y: Int, sin: Double, cos: Double): Point {
        val dx = (height * sin + width * cos) / 2
        val dy = (width * sin + height * cos) / 2
        val nX = x - dx
        val nY = y - dy
        return Point(nX.toInt(), nY.toInt())
    }

    fun renderPoint(x: Double, y: Double): Point {
        val dx = width / 2
        val dy = height / 2
        val nX = x - dx
        val nY = y - dy
        return Point(nX.toInt(), nY.toInt())
    }

    fun renderPoint(x: Double, y: Double, rad: Double): Point {
        val theta = rad % (PI / 2)
        return renderPoint(x, y, kotlin.math.sin(theta), cos(theta))
    }

    fun renderPoint(x: Double, y: Double, sin: Double, cos: Double): Point {
        val dx = (height * sin + width * cos) / 2
        val dy = (width * sin + height * cos) / 2
        val nX = x - dx
        val nY = y - dy
        return Point(nX.toInt(), nY.toInt())
    }

    @Throws(RasterFormatException::class)
    open fun cut(x: Int, y: Int, w: Int, h: Int): Texture {
        return Texture(texture.getSubimage(x, y, w, h))
    }

    fun rotate(radian: Double): AffineTransformOp {
        val transform = AffineTransform.getRotateInstance(radian, width.toDouble() / 2, height.toDouble() / 2)
        return AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC)
    }

    fun rotate(sin: Double, cos: Double): AffineTransformOp {
        val hw = width / 2.0
        val hh = height / 2.0
        val m01 = -1 * sin
        val m02 = hw - hw * cos + hh * sin
        val m12 = hh - hw * sin - hh * cos
        val transform = AffineTransform(cos, sin, m01, cos, m02, m12)
        return AffineTransformOp(transform, TYPE_NEAREST_NEIGHBOR)
    }

    /**
     * Returns a AffineTransform operation without any change.
     * The matrix is:
     *
     * 1    0    0
     *
     * 0    1    0
     *
     * 0    0    1
     *
     *
     * @return a AffineTransform operation.
     */
    fun normalMatrix(): AffineTransformOp {
        return AffineTransformOp(AffineTransform(), TYPE_NEAREST_NEIGHBOR)
    }

    operator fun invoke() = texture

    open fun paint(r: Graphics2D, op: AffineTransformOp, x: Int, y: Int) {
        r.drawImage(texture, op, x, y)
    }

    /**
     * A texture class using VolatileImage.
     *
     * ## Please make sure you have initialized [Graphics.GC] before using this.
     * ### GFXLoader can load textures using this by providing useVram = true in the constructor.
     *
     * @author KKoishi_
     * @see top.kkoishi.stg.script.GFXLoader
     */
    class Volatile(texture: BufferedImage, private val graphicsConfiguration: GraphicsConfiguration) :
        Texture(texture) {
        private var vImg: VolatileImage = graphicsConfiguration.createCompatibleVolatileImage(width, height)

        init {
            Volatile::class.logger().log(System.Logger.Level.INFO, "Use VolatileImage.")
            copyContent()
            TODO("debug")
        }

        private fun copyContent() {
            val r = vImg.createGraphics()
            r.setPaintMode()
            r.drawImage(texture, 0, 0, null)
            r.dispose()
        }

        private fun ensureCompatible() {
            when (vImg.validate(graphicsConfiguration)) {
                VolatileImage.IMAGE_INCOMPATIBLE -> {
                    vImg = graphicsConfiguration.createCompatibleVolatileImage(width, height, Transparency.OPAQUE)
                    copyContent()
                }

                VolatileImage.IMAGE_RESTORED -> copyContent()
            }
        }

        private fun ensureContent() {
            if (vImg.contentsLost())
                copyContent()

            var repaintCount = 0
            while (repaintCount++ <= VRAM_MAX_REPAINT_COUNT) {
                if (!vImg.contentsLost())
                    break

                copyContent()
            }
        }

        override fun paint(r: Graphics2D, op: AffineTransformOp, x: Int, y: Int) {
            ensureCompatible()
            ensureContent()

            r.drawImage(vImg, op.transform, null)
        }

        override fun cut(x: Int, y: Int, w: Int, h: Int): Texture =
            Volatile(texture.getSubimage(x, y, w, h), graphicsConfiguration)

        companion object {
            const val VRAM_MAX_REPAINT_COUNT = 64
        }
    }
}