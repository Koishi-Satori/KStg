@file:Suppress("MemberVisibilityCanBePrivate")

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
import java.awt.image.AffineTransformOp.TYPE_BILINEAR
import java.lang.StrictMath.PI
import java.util.*
import kotlin.math.absoluteValue
import java.awt.image.ConvolveOp as Java2DConvolveOp

/**
 * The Texture class stores the textures used for rendering, which are loaded from local images,and provides
 * some methods for rotating this texture, and the ability of using caches to optimize the rendering performance.
 * It uses [TextureOp] for rendering, you can implement your TextureOp, or just use the provided method
 * to allocate the instances.
 *
 * ### Provided TextureOp Methods
 *
 * | name | return type | instructions |
 * | :--: | :---------: | :----------- |
 * | [rotate] | [RotateOp] | Provides TextureOp can be used for rotating the texture around its center point |
 * | [averageConvolve] | [ConvolveOp] | Provides convolution filters that can remove the image noise of this texture. |
 * | [averageConvolve33] | [Convolve33Op] | Provides convolution filters that can remove the image noise of this texture, but with a 3 * 3 convolution kernel. |
 * | [gaussianBlurConvolve] | [ConvolveOp] | Provides a convolution filter that can perform Gaussian blur processing on textures and with its convolution kernel follows a two-dimensional Gaussian distribution. |
 * | [gaussianBlurConvolve33] | [Convolve33Op] | Provides a convolution filter that can perform Gaussian blur processing on textures and with its convolution kernel which is in the size of 3 * 3, follows a two-dimensional Gaussian distribution. |
 * | [sharpenConvolve] | [ConvolveOp] | Provides a convolution filter that can sharpen the texture. |
 * | [strokeConvolve] | [ConvolveOp] | Provides a convolution filter that can stroke the texture. |
 *
 * Also, this class uses some method in [top.kkoishi.stg.util.Mth] to calculate, which will cache the results
 * to optimize the performance of math calculation. And you can use [Texture.renderPoint] methods to calculate
 * the right coordinates for rendering which make sure that the center of the texture will be rendered at the
 * given coordinates.
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
        if (op == null)
            op = createRotate(scaled)
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
     * distribution with sum of 1, and the kernel is a 3 * 3 matrix.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @param factor the factor of the transparency and brightness.
     * @return an instance of TextureOp which implements a convolution from the source texture to the destination.
     */
    fun gaussianBlurConvolve33(factor: Float): TextureOp {
        return createConvolve33Op(gaussianFunction33(factor))
    }

    /**
     * Return an instance of TextureOp which implements a convolution from the source texture to the destination
     * texture with the ability removing the image noise of this texture., and the kernel is
     * a 3 * 3 matrix.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @param factor the factor of the transparency and brightness.
     * @return an instance of TextureOp which implements a convolution from the source texture to the destination.
     */
    @Strictfp
    fun averageConvolve33(factor: Float): TextureOp {
        return createConvolve33Op(FloatArray(9) { factor / 9f })
    }

    /**
     * Return an instance of TextureOp which implements a convolution from the source texture to the destination
     * texture provided a Gaussian blur effect for textures, and its convolve kernel conforms to the Gaussian
     * distribution with sum of 1, and the kernel is a 3 * 3 matrix, and the [length] should be odd.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @param factor the factor of the transparency and brightness.
     * @param length the length of the kernel.
     * @return an instance of TextureOp which implements a convolution from the source texture to the destination.
     */
    fun gaussianBlurConvolve(factor: Float, length: Int): TextureOp {
        if (length <= 0)
            throw IllegalArgumentException("length should be positive.")
        return createConvolveOp(gaussianFunction(factor, length), length)
    }

    /**
     * Return an instance of TextureOp which implements a convolution from the source texture to the destination
     * texture with the ability removing the image noise of this texture.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @param factor the factor of the transparency and brightness.
     * @param length the length of the kernel.
     * @return an instance of TextureOp which implements a convolution from the source texture to the destination.
     */
    @Strictfp
    fun averageConvolve(factor: Float, length: Int): TextureOp {
        if (length <= 0)
            throw IllegalArgumentException("length should be positive.")
        val amount = length * length
        return createConvolveOp(FloatArray(amount) { factor / amount.toFloat() }, length)
    }

    fun sharpenConvolve(factor: Float, length: Int): TextureOp {
        if (length <= 0)
            throw IllegalArgumentException("length should be positive.")
        val amount = length * length
        val data = FloatArray(amount) { -1f }
        if (length / 2 * 2 != length)
            data[length / 2] = factor
        else
            throw IllegalArgumentException("length should be odd, but got $length")
        return createConvolveOp(data, length)
    }

    fun strokeConvolve(length: Int) = sharpenConvolve(length * length - 1f, length)

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
     * transparency and Gaussian blur([averageConvolve33] and [gaussianBlurConvolve33]). Also, you can use the method
     * [convolve] to get an instance of this class. All convolution operations and processed textures will be
     * cached by the Texture class to improve rendering performance.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @param data the kernel of the convolution.
     * @return an instance of TextureOp which implements a convolution from the source texture to the destination texture.
     */
    fun convolve(data: FloatArray, length: Int): TextureOp {
        assert(data.size == 9) {
            throw InternalError("The matrix of the convolve kernel must be 3 * 3, you should pass an float array with length of 9")
        }
        return createConvolveOp(data, length)
    }

    /**
     * Create the cache of a [ConvolveOp].
     *
     * @param data the kernel data array.
     * @param length the length of the kernel matrix.
     */
    private fun createConvolveOp(data: FloatArray, length: Int): ConvolveOp {
        var convolve = cachedConvolveOp[data]
        if (convolve != null)
            return convolve
        Texture::class.logger()
            .log(System.Logger.Level.INFO, "Constructing a convolve kernel with ${data.contentToString()}")
        convolve = ConvolveOp(data, length)
        cachedConvolveOp[data] = convolve
        return convolve
    }

    /**
     * Create the cache of a [Convolve33Op].
     *
     * @param data the kernel data array.
     */
    private fun createConvolve33Op(data: FloatArray): ConvolveOp {
        var convolve = cachedConvolveOp[data]
        if (convolve != null)
            return convolve
        Texture::class.logger()
            .log(System.Logger.Level.INFO, "Constructing a convolve kernel with ${data.contentToString()}")
        convolve = Convolve33Op(data)
        cachedConvolveOp[data] = convolve
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

    /**
     * Get the texture.
     */
    operator fun invoke() = texture

    /**
     * Create a cache of the texture which applied with [op].
     *
     * @param op [TextureOp] instance.
     * @return created cache.
     */
    protected fun createCache(op: TextureOp): BufferedImage {
        val dst: BufferedImage = op.createDestImage(texture)
        op.apply(dst.createGraphics(), texture)
        // val temp: BufferedImage = op.filter(texture, null)
        caches[op] = dst
        if (Options.State.debug)
            Texture::class.logger()
                .log(System.Logger.Level.DEBUG, "Create Cached Texture for op and $this.")
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
            r.drawImage(texture, AffineTransformOp(AffineTransform(), TYPE_BILINEAR), 0, 0)
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

            paintImpl(r, op, x, y)
        }

        private fun paintImpl(r: Graphics2D, op: TextureOp, x: Int, y: Int) {
            var snapshot = super.caches[op]
            if (snapshot == null) {
                snapshot = op.createDestImage(texture)
                op.apply(snapshot.createGraphics(), vImg.snapshot)
                super.caches[op] = snapshot
            }
            r.drawImage(snapshot, x, y, null)
        }

        override fun cut(x: Int, y: Int, w: Int, h: Int, name: String): Texture =
            Volatile(texture.getSubimage(x, y, w, h), name)
    }

    /**
     * This class implements a convolution from the source texture to the destination texture. Convolution using
     * a convolution kernel is a spatial operation that computes the output pixel from an input pixel by multiplying
     * the kernel with the surround of the input pixel. This allows the output pixel to be affected by the immediate
     * neighborhood in a way that can be mathematically specified with a kernel, and the kernel size is length * length by
     * passing in a float array with the length of length ^ 2.
     *
     * The destination texture always has a alpha channel, and color components will be pre-multiplied with
     * the alpha component. The convolution operation provides a free transformation effect for the input
     * texture, and two predefined convolution operations are provided in the Texture class, which are
     * transparency and Gaussian blur([averageConvolve33], [gaussianBlurConvolve33] and so on). Also, you can use the
     * method [convolve] to get an instance of this class. All convolution operations and processed textures will be
     * cached by the Texture class to improve rendering performance.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * The transform in its super class TextureOp is useless here for it is [NORMAL_MATRIX].
     *
     * @param kernelData kernel data in row major order.
     * @param length the length of the kernel (square) matrix.
     * @author KKoishi_
     */
    open class ConvolveOp(private val kernelData: FloatArray, length: Int) : TextureOp {
        init {
            verifyKernel(kernelData, length)
        }

        val convolveOp = Java2DConvolveOp(Kernel(length, length, kernelData))

        /**
         * Verify if the matrix constructed from the data float array is a square matrix.
         */
        private fun verifyKernel(kernelData: FloatArray, length: Int) {
            if (length <= 0)
                throw IllegalArgumentException("length should be positive.")
            val exceptedSize = length * length
            if (kernelData.size < exceptedSize)
                throw ExceptionInInitializerError("The data array of the convolve kernel is tool small(${kernelData.size} should be $exceptedSize)")
        }

        override fun createDestImage(texture: BufferedImage): BufferedImage =
            BufferedImage(texture.width, texture.height, BufferedImage.TYPE_INT_ARGB)

        override fun apply(g: Graphics2D, texture: BufferedImage) {
            val temp = convolveOp.filter(texture, null)
            Graphics.applyRenderingHints(g)
            g.drawImage(temp, 0, 0, null)
            g.dispose()
        }

        override fun toString(): String {
            return "ConvolveOp(${kernelData.contentToString()})"
        }
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
     * transparency and Gaussian blur([averageConvolve33], [gaussianBlurConvolve33] and so on). Also, you can use the
     * method [convolve] to get an instance of this class. All convolution operations and processed textures will be
     * cached by the Texture class to improve rendering performance.
     *
     * For the edge pixels of the image, the default value of missing pixels within the convolution radius is
     * set to 0 when performing convolution operations.
     *
     * @author KKoishi_
     */
    protected class Convolve33Op(kernelMatrix: FloatArray) : ConvolveOp(kernelMatrix, 3)

    /**
     * The inner rotate TextureOp has optimized performance.
     *
     * This class is only used for rotation, although it actually works for other type of affine-transform matrix,
     * for example Translation.
     *
     * @param xform the AffineTransform
     * @param rad the rotate radian, should be the same as the one xform keeps.
     * @author KKoishi_
     */
    protected inner class RotateOp(rad: Double, xform: AffineTransform) : TextureOp,
        AffineTextureOp(xform, TYPE_BILINEAR) {
        /**
         * The bounding box of the transformed [Texture.texture], this cache can avoid that invoke getBounds2D
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
            val temp = allocateTemp(texture)

            val dx = (x * sin).toInt().absoluteValue
            val dy = (y * cos).toInt().absoluteValue
            Graphics.applyRenderingHints(g)
            g.drawImage(temp, dx, dy, w + dx, h + dy, dx, dy, temp.width, temp.height, null)
            g.dispose()
        }

        private fun allocateTemp(texture: BufferedImage): BufferedImage {
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

    private object IdentifyOp : TextureOp {
        override fun createDestImage(texture: BufferedImage): BufferedImage =
            BufferedImage(texture.width, texture.height, BufferedImage.TYPE_INT_ARGB)

        override fun apply(g: Graphics2D, texture: BufferedImage) {
            Graphics.applyRenderingHints(g)
            g.drawImage(texture, 0, 0, null)
            g.dispose()
        }

        override fun toString(): String {
            return "IdentifyOp"
        }
    }

    abstract class AffineTextureOp(xform: AffineTransform, interpolationType: Int) : TextureOp,
        AffineTransformOp(xform, interpolationType) {
        override fun toString(): String {
            return "AffineTextureOp($transform)"
        }
    }

    /**
     * An filiter/transform used for image processing.
     *
     * @author KKoishi_
     */
    @Suppress("unused")
    interface TextureOp {
        /**
         * Creates a zeroed destination image with the correct size and number of bands.
         *
         * @param texture input image.
         * @return an image.
         */
        fun createDestImage(texture: BufferedImage): BufferedImage

        /**
         * Apply this operation to the specified [Graphics2D] [g] and render the texture using [g].
         *
         * @param g the Graphics2D used for rendering.
         * @param texture input image.
         */
        fun apply(g: Graphics2D, texture: BufferedImage)
    }

    companion object {
        private const val VRAM_MAX_REPAINT_COUNT = 32

        /**
         * The coordinates used for calculating the convolution kernel and optimize its performance.
         */
        @JvmStatic
        private val kernel33Coordinates = intArrayOf(
            -1, 1, 0, 1, 1, 1,
            -1, 0, 0, 0, 1, 0,
            -1, -1, 0, -1, 1, -1
        )

        /**
         * The cached coordinates used for calculating the convolution kernel and optimize its performance.
         */
        @JvmStatic
        private val generatedCoordinates = TreeMap<Int, IntArray>()

        @JvmStatic
        internal val NORMAL_MATRIX: TextureOp = IdentifyOp

        @JvmStatic
        internal val NORMAL_OP = AffineTransformOp(AffineTransform(), TYPE_BILINEAR)

        /**
         * The cached convolution used for optimizing the performance.
         */
        @JvmStatic
        private val cachedConvolveOp = TreeMap<FloatArray, ConvolveOp>(Arrays::compare)

        /**
         * Calculate the function value of the two-dimensional Gaussian distribution function at a certain point.
         *
         * @param factor sigma in Gaussian distribution function.
         * @param x x coordinate of the point.
         * @param y y coordinate of the point.
         * @return the function value of the two-dimensional Gaussian distribution function at a certain point.
         */
        @Strictfp
        @JvmStatic
        private fun gaussianFunction(factor: Float, x: Int, y: Int): Float {
            val temp = 2 * factor * factor
            val index = -(x * x + y * y) / temp
            return (StrictMath.pow(StrictMath.E, index.toDouble()) / (PI * temp)).toFloat()
        }

        @JvmStatic
        private val cachedGaussianFunction33 = TreeMap<Float, FloatArray>()

        @JvmStatic
        private val cachedGaussianFunction = TreeMap<Pair<Float, Int>, FloatArray> { p1, p2 ->
            val len1 = p1.second
            val len2 = p2.second
            if (len1 != len2)
                return@TreeMap len1 - len2
            val f1 = p1.first
            val f2 = p2.first
            if (f1 == f2)
                return@TreeMap 0
            else if (f1 > f2)
                return@TreeMap 1
            return@TreeMap -1
        }

        @JvmStatic
        private fun coordinates(length: Int): IntArray {
            var coordinates = generatedCoordinates[length]
            if (coordinates != null)
                return coordinates
            coordinates = IntArray(length * length * 2)
            generateCoordinates(length, coordinates)
            return coordinates
        }

        @JvmStatic
        private fun generateCoordinates(length: Int, data: IntArray) {
            data class Point(val x: Int, val y: Int)

            val origin = Point(0, 0)
            var value = length / 2
            var cater = 0
            val all = Array(length) { Array(length) { origin } }

            //  (-len / 2, -len / 2) -> (len / 2, len / 2)
            // (-cater_value, cater_value    ), (-cater_value + 1, cater_value), ... , (-cater_value + n, cater_value)
            // (-cater_value, cater_value - 1)
            // ...
            // (-cater_value, cater_value - n)
            while (cater < length) {
                ((length - 1 - cater) downTo 0).forEachIndexed { index, pos ->
                    val other = length - pos - 1
                    all[cater][other] = Point(-value + index, value)
                    all[other][cater] = Point(-value, value - index)
                }
                ++cater
                --value
            }

            // fill in data
            (0 until length).forEach { x ->
                (0 until length).forEach { y ->
                    val p = all[x][y]
                    data[2 * (x * length + y)] = p.x
                    data[2 * (x * length + y) + 1] = p.y
                }
            }
        }

        @Strictfp
        @JvmStatic
        private fun gaussianFunction(factor: Float, length: Int): FloatArray {
            val key = factor to length
            var res = cachedGaussianFunction[key]
            if (res != null)
                return res

            var x: Int
            var y: Int
            var total = 0f
            val len = length * length
            val coordinates = coordinates(length)

            res = FloatArray(len)
            (0 until len).forEach {
                x = coordinates[it * 2]
                y = coordinates[it * 2 + 1]
                val value = gaussianFunction(factor, x, y)
                res[it] = value
                total += value
            }

            // make the total is 1.
            res.indices.forEach {
                res[it] /= total
            }

            cachedGaussianFunction[key] = res
            return res
        }

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