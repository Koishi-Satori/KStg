package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage

class Texture(private val texture: BufferedImage) {
    val width = texture.width / 2
    val height = texture.height / 2
    fun renderPoint(x: Int, y: Int): Point {
        val hw = width / 2
        val hh = height / 2
        return Point(x - hw, y - hh)
    }

    fun rotate(radian: Double, x: Double, y: Double): AffineTransformOp {
        val transform = AffineTransform.getRotateInstance(radian, x, y)
        return AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
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

    fun paint(r: Graphics2D ,op: AffineTransformOp, x: Int, y: Int) {
        val insets = Graphics.getFrameInsets()
        r.drawImage(texture, op, x + insets.left, y + insets.top)
    }
}