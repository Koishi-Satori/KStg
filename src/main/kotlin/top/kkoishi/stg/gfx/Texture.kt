@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package top.kkoishi.stg.gfx

import top.kkoishi.stg.exceptions.InternalError
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
     * Return an instance of TextureOp which implements a convolution from the source texture to the destination
     * texture provided a Gaussian blur effect for textures, and its convolve kernel conforms to the Gaussian
     * distribution with sum of 1.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @param factor the factor of the transparency and brightness.
     * @return an instance of TextureOp which implements a convolution from the source texture to the destination.
     */
    fun gaussianBlurConvolve(factor: Float): TextureOp {
        return createConvolve33Op(gaussianFunction33(factor))
    }

    /**
     * Return an instance of TextureOp which implements a convolution from the source texture to the destination
     * texture with the ability adjusting the transparency and brightness of this texture.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @param factor the factor of the transparency and brightness.
     * @return an instance of TextureOp which implements a convolution from the source texture to the destination.
     */
    @Strictfp
    fun alphaConvolve(factor: Float): TextureOp {
        return createConvolve33Op(FloatArray(9) { factor / 9f })
    }

    /**
     * Return an instance of TextureOp which implements a convolution from the source texture to the destination texture.
     * Convolution using a convolution kernel is a spatial operation that computes the output pixel from an input pixel
     * by multiplying the kernel with the surround of the input pixel. This allows the output pixel to be affected by
     * the immediate neighborhood in a way that can be mathematically specified with a kernel, and the kernel size
     * is 3 * 3 by passing in a float array with the length of 9.
     *
     * The destination texture always has a alpha channel, and color components will be pre-multiplied with
     * the alpha component. The convolution operation provides a free transformation effect for the input
     * texture, and two predefined convolution operations are provided in the Texture class, which are
     * transparency and Gaussian blur([alphaConvolve] and [gaussianBlurConvolve]). Also, you can use the method
     * [convolve] to get an instance of this class. All convolution operations and processed textures will be
     * cached by the Texture class to improve rendering performance.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @param data the kernel of the convolution.
     * @return an instance of TextureOp which implements a convolution from the source texture to the destination texture.
     */
    fun convolve(data: FloatArray): TextureOp {
        assert(data.size == 9) {
            throw InternalError("The matrix of the convolve kernel must be 3 * 3, you should pass an float array with length of 9")
        }
        return createConvolve33Op(data)
    }

    private fun createConvolve33Op(data: FloatArray): Convolve33Op {
        var convolve = cachedConvolve33Op[data]
        if (convolve != null)
            return convolve
        Texture::class.logger()
            .log(System.Logger.Level.INFO, "Constructing a convolve kernel with ${data.contentToString()}")
        convolve = Convolve33Op(data)
        cachedConvolve33Op[data] = convolve
        return convolve
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
     * This class implements a convolution from the source texture to the destination texture. Convolution using
     * a convolution kernel is a spatial operation that computes the output pixel from an input pixel by multiplying
     * the kernel with the surround of the input pixel. This allows the output pixel to be affected by the immediate
     * neighborhood in a way that can be mathematically specified with a kernel, and the kernel size is 3 * 3 by
     * passing in a float array with the length of 9.
     *
     * The destination texture always has a alpha channel, and color components will be pre-multiplied with
     * the alpha component. The convolution operation provides a free transformation effect for the input
     * texture, and two predefined convolution operations are provided in the Texture class, which are
     * transparency and Gaussian blur([alphaConvolve] and [gaussianBlurConvolve]). Also, you can use the method
     * [convolve] to get an instance of this class. All convolution operations and processed textures will be
     * cached by the Texture class to improve rendering performance.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @author KKoishi_
     */
    protected class Convolve33Op(kernelMatrix: FloatArray) : TextureOp(AffineTransform()) {
        private val convolveOp = ConvolveOp(Kernel(3, 3, kernelMatrix))

        override fun createDestImage(texture: BufferedImage): BufferedImage =
            BufferedImage(texture.width, texture.height, BufferedImage.TYPE_INT_ARGB)

        override fun apply(g: Graphics2D, texture: BufferedImage) {
            val temp = convolveOp.filter(texture, null)
            Graphics.applyRenderingHints(g)
            g.drawImage(temp, 0, 0, null)
            g.dispose()
        }
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

    private object IdentifyOp : TextureOp(AffineTransform()) {
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
        private val kernel33Coordinates = intArrayOf(
            -1, 1, 0, 1, 1, 1,
            -1, 0, 0, 0, 1, 0,
            -1, -1, 0, -1, 1, -1
        )

        @JvmStatic
        internal val NORMAL_MATRIX: TextureOp = IdentifyOp

        @JvmStatic
        private val cachedConvolve33Op = TreeMap<FloatArray, Convolve33Op>(Arrays::compare)

        @Strictfp
        @JvmStatic
        private fun gaussianFunction(factor: Float, x: Int, y: Int): Float {
            val temp = 2 * factor * factor
            val index = -(x * x + y * y) / temp
            return (StrictMath.pow(StrictMath.E, index.toDouble()) / (PI * temp)).toFloat()
        }

        private val cachedGaussianFunction33 = TreeMap<Float, FloatArray>()

        @Strictfp
        @JvmStatic
        private fun gaussianFunction33(factor: Float): FloatArray {
            var res = cachedGaussianFunction33[factor]
            if (res != null)
                return res

            var x: Int
            var y: Int
            var total = 0f
            res = FloatArray(9)
            (0..8).forEach {
                x = kernel33Coordinates[it * 2]
                y = kernel33Coordinates[it * 2 + 1]
                val value = gaussianFunction(factor, x, y)
                res[it] = value
                total += value
            }

            // make the total is 1.
            res.indices.forEach {
                res[it] /= total
            }

            cachedGaussianFunction33[factor] = res
            return res
        }
    }
}