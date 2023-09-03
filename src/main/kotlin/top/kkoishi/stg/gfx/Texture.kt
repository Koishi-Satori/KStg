@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package top.kkoishi.stg.gfx

import top.kkoishi.stg.gfx.Graphics.makeTranslucent
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.util.Mth.cos
import top.kkoishi.stg.util.Mth.setScale
import top.kkoishi.stg.util.Mth.sin
import top.kkoishi.stg.util.Options
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.*
import java.awt.image.AffineTransformOp.TYPE_NEAREST_NEIGHBOR
import java.lang.StrictMath.PI
import java.util.*
import kotlin.math.absoluteValue

/**
 * The Texture class stores the textures used for rendering, which are loaded from local images,and provides
 * some methods for rotating this texture, and the ability of using caches to optimize the rendering performance.
 * It uses [TextureOp] for rendering, you can implement your TextureOp, or just use the provided method
 * to allocate the instances represented rotation.
 *
 * Also, this class uses some method in [top.kkoishi.stg.util.Mth] to calculate, which will cache the results
 * to optimize the performance of math calculation.
 *
 * You can use [Texture.renderPoint] methods to calculate the right coordinates for rendering.
 *
 * There is a subclass using VRAM for rendering, [Texture.Volatile], and it has the better hardware acceleration
 * possibilities.
 *
 * @author KKoishi_
 */
open class Texture internal constructor(protected val texture: BufferedImage, val name: String = "") {
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
    protected val caches = HashMap<TextureOp, BufferedImage>()

    /**
     * The cached RotateOps.
     */
    protected val cachedRotateOps = TreeMap<Double, RotateOp>()

    init {
        if (Options.State.debug)
            Texture::class.logger()
                .log(
                    System.Logger.Level.DEBUG,
                    "Initializing the cached texture for NORMAL_MATRIX and ${name.ifEmpty { "unnamed_texture" }}"
                )
        createCache(NORMAL_MATRIX)
    }

    /**
     * Calculate and return the correct rendering coordinate, which can make the texture's center coincides with
     * the given point specified by its coordinates.
     *
     * @param x the X coordinate of the point.
     * @param y the Y coordinate of the point.
     * @return the rendering point.
     */
    @Strictfp
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
    @Strictfp
    fun renderPoint(x: Int, y: Int, rad: Double): Point {
        val theta = rad % (PI / 2)
        return renderPoint(x, y, sin(theta), cos(theta))
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
    @Strictfp
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
    @Strictfp
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
    @Strictfp
    fun renderPoint(x: Double, y: Double, rad: Double): Point {
        val theta = rad % (PI / 2)
        return renderPoint(x, y, sin(theta), cos(theta))
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
    @Strictfp
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
    open fun cut(x: Int, y: Int, w: Int, h: Int, name: String = ""): Texture {
        return Texture(texture.getSubimage(x, y, w, h), name)
    }

    /**
     * Returns a TextureOp that can be used to rotate the texture.
     *
     * The matrix representing the returned texture operation is:
     * ```
     *            [   cos(radian)    -sin(radian)     width-width*cos+height*sin  ]
     *            [   sin(radian)     cos(radian)    height-width*sin-height*cos  ]
     *            [       0               0                      1                ]
     * ```
     *
     * @param radian the angle of rotation measured in radians.
     * @return a transform that can be used to rotate the texture.
     */
    fun rotate(radian: Double): TextureOp {
        val scaled = radian.setScale()
        var op = cachedRotateOps[scaled]
        if (op == null) op = createRotate(scaled)
        return op
    }

    private fun createRotate(radian: Double): RotateOp {
        val transform = AffineTransform()
        transform.translate(width / 2.0, height / 2.0)
        transform.rotate(radian)
        transform.translate(-width / 2.0, -height / 2.0)
        val temp = RotateOp(radian, transform)
        if (Options.State.debug)
            Texture::class.logger().log(System.Logger.Level.DEBUG, "Create Cache for $radian")
        cachedRotateOps[radian] = temp
        return temp
    }

    /**
     * Returns a Texture operation without any change.
     * The matrix is:
     *
     * 1    0    0
     *
     * 0    1    0
     *
     * 0    0    1
     *
     *
     * @return a Texture operation.
     */
    fun normalMatrix(): TextureOp = NORMAL_MATRIX

    operator fun invoke() = texture

    protected fun createCache(op: TextureOp): BufferedImage {
        val dst: BufferedImage = op.createDestImage(texture)
        op.apply(dst.createGraphics(), texture)
        // val temp: BufferedImage = op.filter(texture, null)
        caches[op] = dst
        if (Options.State.debug)
            Texture::class.logger()
                .log(System.Logger.Level.DEBUG, "Create Cached Texture for ${op.transform} and $this.")
        return dst
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
    open fun paint(r: Graphics2D, op: TextureOp, x: Int, y: Int) {
        var img = caches[op]
        if (img == null) img = createCache(op)
        r.drawImage(img, x, y, null)
    }

    override fun toString(): String =
        "Texture(name='${name.ifEmpty { "unnamed_texture" }}', width=$width, height=$height)"

    /**
     * A texture class using VolatileImage.
     *
     * @author KKoishi_
     * @see top.kkoishi.stg.script.GFXLoader
     */
    class Volatile(texture: BufferedImage, name: String = "") : Texture(texture, name) {
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

        override fun paint(r: Graphics2D, op: TextureOp, x: Int, y: Int) {
            if (vImg.contentsLost()) {
                makeTranslucent(vImg)
                copyContent()

                var repaintCount = 0
                while (repaintCount++ <= VRAM_MAX_REPAINT_COUNT) {
                    if (!vImg.contentsLost()) break

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

        override fun cut(x: Int, y: Int, w: Int, h: Int, name: String): Texture =
            Volatile(texture.getSubimage(x, y, w, h), name)
    }

    /**
     * The inner rotate TextureOp has optimized performance.
     *
     * @param rad the rotate radian, should be the same as the one [xform] keeps.
     * @param xform the AffineTransform
     * @author KKoishi_
     */
    protected inner class RotateOp(rad: Double, xform: AffineTransform) : TextureOp(xform) {
        /**
         * The bounding box of the transformed [Texture.texture], this cache can avoid that invoke [getBounds2D]
         * every time while the methods [createDestImage] and [apply] is called.
         */
        private val bounds = getBounds2D(texture).bounds

        /**
         * The sin value of the rad, uses this cache for acceleration.
         */
        private val sin = sin(rad)

        /**
         * The cos value of the rad, uses this cache for acceleration.
         */
        private val cos = cos(rad)

        override fun createDestImage(texture: BufferedImage): BufferedImage {
            val bounds = this.bounds
            val w = bounds.width + bounds.x
            val h = bounds.height + bounds.y
            val cm = texture.colorModel

            return if (interpolationType != TYPE_NEAREST_NEIGHBOR &&
                (cm is IndexColorModel || cm.transparency == Transparency.OPAQUE)
            )
                BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            else
                BufferedImage(
                    cm,
                    texture.raster.createCompatibleWritableRaster(w, h),
                    cm.isAlphaPremultiplied,
                    null
                )
        }

        override fun apply(g: Graphics2D, texture: BufferedImage) {
            val bounds = this.bounds
            val w = bounds.width
            val h = bounds.height
            val x = bounds.x.absoluteValue
            val y = bounds.y.absoluteValue
            val temp = allocateTemp()

            val dx = (x * sin).toInt().absoluteValue
            val dy = (y * cos).toInt().absoluteValue
            Graphics.applyRenderingHints(g)
            g.drawImage(temp, dx, dy, w + dx, h + dy, dx, dy, temp.width, temp.height, null)
            g.dispose()
        }

        private fun allocateTemp(): BufferedImage {
            val bounds = this.bounds
            val w = bounds.width
            val h = bounds.height
            val x = bounds.x.absoluteValue
            val y = bounds.y.absoluteValue
            val dx = (x * sin).toInt().absoluteValue
            val dy = (y * cos).toInt().absoluteValue
            val temp = BufferedImage(
                w + dx, h + dy,
                BufferedImage.TYPE_INT_ARGB
            )

            val g = temp.createGraphics()
            Graphics.applyRenderingHints(g)
            // g.color = Color.WHITE
            // g.drawRect(x, 0, temp.width, temp.height)
            g.drawImage(texture, this@RotateOp, 0, y)
            g.dispose()
            return temp
        }
    }

    private class IdentifyOp : TextureOp(AffineTransform()) {
        override fun createDestImage(texture: BufferedImage): BufferedImage =
            BufferedImage(texture.width, texture.height, BufferedImage.TYPE_INT_ARGB)

        override fun apply(g: Graphics2D, texture: BufferedImage) {
            Graphics.applyRenderingHints(g)
            g.drawImage(texture, 0, 0, null)
            g.dispose()
        }
    }

    abstract class TextureOp : AffineTransformOp {
        @JvmOverloads
        constructor(xform: AffineTransform, interpolationType: Int = TYPE_BILINEAR) : super(xform, interpolationType)
        constructor(xform: AffineTransform, hints: RenderingHints) : super(xform, hints)

        /**
         * Creates a zeroed destination image with the correct size and number of bands.
         *
         * @param texture input image.
         * @return an image.
         */
        abstract fun createDestImage(texture: BufferedImage): BufferedImage

        /**
         * Apply this operation to the specified [Graphics2D] [g] and render the texture using [g].
         *
         * @param g the Graphics2D used for rendering.
         * @param texture input image.
         */
        abstract fun apply(g: Graphics2D, texture: BufferedImage)
    }

    companion object {
        private const val VRAM_MAX_REPAINT_COUNT = 32

        @JvmStatic
        internal val NORMAL_MATRIX: TextureOp = IdentifyOp()
    }
}