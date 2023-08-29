@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package top.kkoishi.stg.gfx

import top.kkoishi.stg.gfx.Graphics.makeTranslucent
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.Mth.setScale
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.AffineTransformOp.TYPE_NEAREST_NEIGHBOR
import java.awt.image.BufferedImage
import java.awt.image.RasterFormatException
import java.awt.image.VolatileImage
import kotlin.math.PI
import kotlin.math.cos

/**
 * The Texture class stores the textures used for rendering, which are loaded from local images.
 * And this class provides some methods for rotating this texture, and the ability of using caches to optimize
 * the rendering performance.
 *
 * You can use [Texture.renderPoint] methods to calculate the right coordinates for rendering.
 *
 * There is a subclass using VRAM for rendering, [Texture.Volatile].
 *
 * @author KKoishi_
 */
open class Texture internal constructor(protected val texture: BufferedImage) {
    /**
     * The width of the Texture.
     */
    val width = texture.width

    /**
     * The height of the Texture.
     */
    val height = texture.height

    /**
     * Cached textures.
     */
    protected val cached = HashMap<AffineTransformOp, BufferedImage>()

    /**
     * Calculate and return the correct rendering coordinate, which can make the texture's center coincides with
     * the given point specified by its coordinates.
     *
     * @param x the X coordinate of the point.
     * @param y the Y coordinate of the point.
     * @return the rendering point.
     */
    fun renderPoint(x: Int, y: Int): Point {
        val dx = width / 2
        val dy = height / 2
        return Point(x - dx, y - dy)
    }

    /**
     * Calculate and return the correct rendering coordinate, which can make center of the rotated texture, the rotate degree is
     * specified in parameters, coincides with the given point specified by its coordinates.
     *
     * @param x the X coordinate of the point.
     * @param y the Y coordinate of the point.
     * @param rad the rotate degree.
     * @return the rendering point.
     */
    fun renderPoint(x: Int, y: Int, rad: Double): Point {
        val theta = rad % (PI / 2)
        return renderPoint(x, y, kotlin.math.sin(theta), cos(theta))
    }

    /**
     * Calculate and return the correct rendering coordinate, which can make center of the rotated texture, the rotate degree is
     * specified in parameters, coincides with the given point specified by its coordinates.
     *
     * @param x the X coordinate of the point.
     * @param y the Y coordinate of the point.
     * @param sin the sin value of the rotate degree
     * @param cos the cos value of the rotate degree
     * @return the rendering point.
     */
    fun renderPoint(x: Int, y: Int, sin: Double, cos: Double): Point {
        val dx = (height * sin + width * cos) / 2
        val dy = (width * sin + height * cos) / 2
        val nX = x - dx
        val nY = y - dy
        return Point(nX.toInt(), nY.toInt())
    }

    /**
     * Calculate and return the correct rendering coordinate, which can make the texture's center coincides with
     * the given point specified by its coordinates.
     *
     * @param x the X coordinate of the point.
     * @param y the Y coordinate of the point.
     * @return the rendering point.
     */
    fun renderPoint(x: Double, y: Double): Point {
        val dx = width / 2
        val dy = height / 2
        val nX = x - dx
        val nY = y - dy
        return Point(nX.toInt(), nY.toInt())
    }

    /**
     * Calculate and return the correct rendering coordinate, which can make center of the rotated texture, the rotate degree is
     * specified in parameters, coincides with the given point specified by its coordinates.
     *
     * @param x the X coordinate of the point.
     * @param y the Y coordinate of the point.
     * @param rad the rotate degree.
     * @return the rendering point.
     */
    fun renderPoint(x: Double, y: Double, rad: Double): Point {
        val theta = rad % (PI / 2)
        return renderPoint(x, y, kotlin.math.sin(theta), cos(theta))
    }

    /**
     * Calculate and return the correct rendering coordinate, which can make center of the rotated texture, the rotate degree is
     * specified in parameters, coincides with the given point specified by its coordinates.
     *
     * @param x the X coordinate of the point.
     * @param y the Y coordinate of the point.
     * @param sin the sin value of the rotate degree
     * @param cos the cos value of the rotate degree
     * @return the rendering point.
     */
    fun renderPoint(x: Double, y: Double, sin: Double, cos: Double): Point {
        val dx = (height * sin + width * cos) / 2
        val dy = (width * sin + height * cos) / 2
        val nX = x - dx
        val nY = y - dy
        return Point(nX.toInt(), nY.toInt())
    }

    /**
     * Returns a sub-texture defined by a specified rectangular region. The returned Texture shares the same data
     * array as the original.
     *
     * @param x the X coordinate of the upper-left corner of the specified rectangular region
     * @param y the Y coordinate of the upper-left corner of the specified rectangular region
     * @param w the width of the specified rectangular region
     * @param h the height of the specified rectangular region
     * @return sub-texture
     * @throws RasterFormatException if the specified area is not contained within this Texture.
     */
    @Throws(RasterFormatException::class)
    open fun cut(x: Int, y: Int, w: Int, h: Int): Texture {
        return Texture(texture.getSubimage(x, y, w, h))
    }

    /**
     * Returns a transform that can be used to rotate the texture.
     *
     * This operation is equivalent to the following sequence of calls:
     * ```
     *       AffineTransform Tx = AffineTransform()
     *       Tx.translate(width, height)    // S3: final translation
     *       Tx.rotate(theta)                  // S2: rotate around anchor
     *       Tx.translate(-width, -height)  // S1: translate anchor to origin
     *       AffineTransformOp(Tx);
     * ```
     *
     * The matrix representing the returned transform operation is:
     * ```
     *            [   cos(radian)    -sin(radian)     width-width*cos+height*sin  ]
     *            [   sin(radian)     cos(radian)    height-width*sin-height*cos  ]
     *            [       0               0                      1                ]
     * ```
     *
     * @param radian the angle of rotation measured in radians.
     * @return a transform that can be used to rotate the texture.
     */
    fun rotate(radian: Double): AffineTransformOp {
        val scaled = radian.setScale()
        var op = cachedRotateOps1[scaled]
        if (op == null)
            op = createRotate(scaled)
        return op
    }

    /**
     * Returns a transform that can be used to rotate the texture.
     *
     * The matrix representing the returned transform operation is:
     * ```
     *            [   cos    -sin     width-width*cos+height*sin  ]
     *            [   sin     cos    height-width*sin-height*cos  ]
     *            [    0       0                 1                ]
     * ```
     *
     * @param sin sin value of the angle of rotation measured in radians.
     * @param cos cos value of the angle of rotation measured in radians.
     * @return a transform that can be used to rotate the texture.
     */
    fun rotate(sin: Double, cos: Double): AffineTransformOp {
        var op = cachedRotateOps2[sin to cos]
        if (op == null)
            op = createRotate(sin, cos)
        return op
    }

    private fun createRotate(radian: Double): AffineTransformOp {
        val transform = AffineTransform.getRotateInstance(radian, width.toDouble() / 2, height.toDouble() / 2)
        val temp = AffineTransformOp(transform, TYPE_NEAREST_NEIGHBOR)
        //Texture::class.logger().log(System.Logger.Level.DEBUG, "Create Cache for $radian")
        cachedRotateOps1[radian] = temp
        return temp
    }

    private fun createRotate(sin: Double, cos: Double): AffineTransformOp {
        val hw = width / 2.0
        val hh = height / 2.0
        val m01 = -1 * sin
        val m02 = hw - hw * cos + hh * sin
        val m12 = hh - hw * sin - hh * cos
        val transform = AffineTransform(cos, sin, m01, cos, m02, m12)
        val temp = AffineTransformOp(transform, TYPE_NEAREST_NEIGHBOR)
        cachedRotateOps2[sin to cos] = temp
        return temp
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
    fun normalMatrix(): AffineTransformOp = NORMAL_MATRIX

    operator fun invoke() = texture

    protected fun createCache(op: AffineTransformOp): BufferedImage {
        val temp: BufferedImage = op.filter(texture, null)
        cached[op] = temp
        //Texture::class.logger().log(System.Logger.Level.DEBUG, "Create Cached Image for $op")
        return temp
    }

    /**
     * Renders a Texture that is filtered with a BufferedImageOp. The rendering attributes applied to include
     * the Clip, Transform and Composite attributes.
     *
     * This method uses local cache to accelerate the rendering.
     *
     * @param r the Graphics.
     * @param op the filter to be applied to the texture before rendering.
     * @param x the x coordinate of the location in user space where the upper left corner of the texture is rendered
     * @param y the y coordinate of the location in user space where the upper left corner of the texture is rendered
     */
    open fun paint(r: Graphics2D, op: AffineTransformOp, x: Int, y: Int) {
        var img = cached[op]
        if (img == null)
            img = createCache(op)
        r.drawImage(img, x, y, null)
    }

    /**
     * A texture class using VolatileImage.
     *
     * @author KKoishi_
     * @see top.kkoishi.stg.script.GFXLoader
     */
    class Volatile(texture: BufferedImage) :
        Texture(texture) {
        private var vImg: VolatileImage
        private val gc: GraphicsConfiguration

        init {
            Volatile::class.logger().log(System.Logger.Level.INFO, "Use VolatileImage.")
            val g = texture.createGraphics()
            gc = g.deviceConfiguration
            vImg = gc.createCompatibleVolatileImage(width, height, Transparency.BITMASK)
            g.dispose()

            makeTranslucent(vImg)
            copyContent()
        }

        private fun copyContent() {
            val r = vImg.createGraphics()
            r.drawImage(texture, AffineTransformOp(AffineTransform(), TYPE_NEAREST_NEIGHBOR), 0, 0)
            r.dispose()
        }

        override fun paint(r: Graphics2D, op: AffineTransformOp, x: Int, y: Int) {
            if (vImg.contentsLost()) {
                makeTranslucent(vImg)
                copyContent()

                var repaintCount = 0
                while (repaintCount++ <= VRAM_MAX_REPAINT_COUNT) {
                    if (!vImg.contentsLost())
                        break

                    makeTranslucent(vImg)
                    copyContent()
                }
            }

            when (vImg.validate(gc)) {
                VolatileImage.IMAGE_INCOMPATIBLE -> {
                    vImg = gc.createCompatibleVolatileImage(width, height, Transparency.BITMASK)
                    makeTranslucent(vImg)
                    copyContent()
                }

                VolatileImage.IMAGE_RESTORED -> {
                    makeTranslucent(vImg)
                    copyContent()
                }
            }

            r.drawImage(vImg.snapshot, op, x, y)
        }

        override fun cut(x: Int, y: Int, w: Int, h: Int): Texture =
            Volatile(texture.getSubimage(x, y, w, h))
    }

    companion object {
        private const val VRAM_MAX_REPAINT_COUNT = 64

        @JvmStatic
        internal val NORMAL_MATRIX = AffineTransformOp(AffineTransform(), TYPE_NEAREST_NEIGHBOR)

        @JvmStatic
        internal val cachedRotateOps1 = HashMap<Double, AffineTransformOp>()

        @JvmStatic
        internal val cachedRotateOps2 = HashMap<Pair<Double, Double>, AffineTransformOp>()
    }
}