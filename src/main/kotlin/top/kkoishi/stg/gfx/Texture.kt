package top.kkoishi.stg.gfx

import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.awt.image.RasterFormatException

class Texture(private val texture: BufferedImage) {
    val width = texture.width
    val height = texture.height
    fun renderPoint(x: Int, y: Int): Point {
        val dx = width / 2
        val dy = height / 2
        var nX = x - dx
        var nY = y - dy
        if (nX < 0)
            nX = 0
        if (nY < 0)
            nY = 0
        return Point(nX, nY)
    }

    fun renderPoint(x: Int, y: Int, rad: Double) {

    }

    @Throws(RasterFormatException::class)
    fun cut(x: Int, y: Int, w: Int, h: Int): Texture {
        return Texture(texture.getSubimage(x, y, w, h))
    }

    fun rotate(radian: Double): AffineTransformOp {
        val transform = AffineTransform.getRotateInstance(radian, width.toDouble() / 2, height.toDouble() / 2)
        return AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC)
    }

    fun rotate(sin: Double, cos: Double, x: Double, y: Double): AffineTransformOp {
        val m01 = -1 * sin
        val m02 = x - x * cos + y * sin
        val m12 = y - x * sin - y * cos
        val transform = AffineTransform(cos, sin, m01, cos, m02, m12)
        return AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
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
        return AffineTransformOp(AffineTransform(), AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
    }

    operator fun invoke() = texture

    fun paint(r: Graphics2D, op: AffineTransformOp, x: Int, y: Int) {
        r.drawImage(texture, op, x, y)
    }
}