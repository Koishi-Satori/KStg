@file:Suppress("KotlinConstantConditions")

package top.kkoishi.stg.gfx

import top.kkoishi.stg.common.bullets.Bullet
import top.kkoishi.stg.common.entities.Entity
import top.kkoishi.stg.util.Mth.sqrt
import java.awt.Point
import java.awt.Polygon
import java.awt.Rectangle
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.absoluteValue

@Suppress("RedundantConstructorKeyword")
object CollideSystem {
    private val lock = Any()
    private var registeredCSCollideCheck: ((Circle, Shape) -> Boolean)? = null
    private var registeredSSCollideCheck: ((Shape, Shape) -> Boolean)? = null

    /**
     * The methods used for intersection test between polygons.
     */
    var polygonIntersectMethod: PolygonIntersectMethod = PolygonIntersectMethod.SAT
    fun registerCircleCollideMethod(check: (Circle, Shape) -> Boolean) {
        synchronized(lock) {
            registeredCSCollideCheck = check
        }
    }

    fun registerCollideMethod(check: (Shape, Shape) -> Boolean) {
        synchronized(lock) {
            registeredSSCollideCheck = check
        }
    }

    fun collide(o: Entity, b: Bullet): Boolean {
        val s1 = o.shape()
        val s2 = b.shape()
        if (preIntersectTest(s1, s2)) {
            return intersectTest(s1, s2)
        }
        return false
    }

    fun collide(o1: Entity, o2: Entity): Boolean {
        val s1 = o1.shape()
        val s2 = o2.shape()
        if (preIntersectTest(s1, s2)) {
            return intersectTest(s1, s2)
        }
        return false
    }

    /**
     * Check if the bullet is out of the screen.
     *
     * @return true if the bullet is out of the screen.
     */
    fun checkPos(b: Bullet): Boolean {
        val x = b.x()
        val y = b.y()
        val screenSize = Graphics.getScreenSize()
        val insets = Graphics.getUIInsets()
        return x < insets.left || x > (screenSize.width - insets.right) || y < insets.top || y > (screenSize.height - insets.bottom)
    }

    fun checkPos(x: Int, y: Int): Boolean {
        val screenSize = Graphics.getScreenSize()
        val insets = Graphics.getUIInsets()
        return x < insets.left || x > (screenSize.width - insets.right) || y < insets.top || y > (screenSize.height - insets.bottom)
    }

    /**
     * Used for rough collision detection.
     *
     * @return true if the bounds of two shapes intersected.
     */
    private fun preIntersectTest(s1: Shape, s2: Shape): Boolean = s1.bounds.intersects(s2.bounds)

    private fun intersectTest(s1: Shape, s2: Shape): Boolean {
        if (s1 is Circle || s2 is Circle) {
            val other: Shape
            val c: Circle
            if (s1 is Circle) {
                c = s1
                other = s2
            } else if (s2 is Circle) {
                c = s2
                other = s1
            } else {
                // this should not happen
                throw IllegalArgumentException()
            }
            when (other) {
                is Circle -> {
                    if (c.contains(other.center) || other.contains(c.center))
                        return true
                    return circleIntersectCircle(c.center, other.center, c.r, other.r)
                }

                is Rectangle2D -> return circleIntersectRect(c, other)
                is Polygon -> return convexIntersectCircle(other, c.center, c.r)
                else -> {
                    val check = registeredCSCollideCheck
                    if (check != null)
                        return check(c, other)
                }
            }
        } else if (s1 is Rectangle2D && s2 is Rectangle2D)
            return rectIntersectRect(s1, s2)
        else if (s1 is Polygon && s2 is Polygon)
            return polygonIntersectPolygon(s1, s2)

        val check = registeredSSCollideCheck
        if (check != null)
            return check(s1, s2)
        throw UnsupportedOperationException("Unsupported intersect check: $s1 collide $s2")
    }

    private fun circleIntersectCircle(p1: Point, p2: Point, r1: Int, r2: Int): Boolean {
        val distance = p1.distance(p2)
        val r = (r1 + r2).toDouble()
        return distance >= r
    }

    enum class PolygonIntersectMethod {
        SAT, GJK, ONLY_PRETEST
    }

    internal data class VPoint constructor(var x: Double, var y: Double) {
        constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

        fun euclidDistance(p: VPoint): Double {
            val dx = x - p.x
            val dy = y - p.y
            if ((dx >= 0 && dy.absoluteValue <= 0.2) || (dy >= 0 && dx.absoluteValue <= 0.2))
                return sqrt(dx * dx + dy * dy)
            return -1 * sqrt(dx * dx + dy * dy)
        }

        fun euclidDistance(p: Point): Double {
            val dx = x - p.x
            val dy = y - p.y
            if (dx >= 0)
                return sqrt(dx * dx + dy * dy)
            return -1 * sqrt(dx * dx + dy * dy)
        }

        fun setLocation(x: Double, y: Double) {
            this.x = x
            this.y = y
        }

        operator fun unaryMinus(): VPoint = VPoint(-x, -y)
        operator fun minus(vector: VPoint): VPoint = VPoint(x - vector.x, y - vector.y)
        operator fun times(vector: VPoint): Double = x * vector.x + y * vector.y
        infix fun x(vector: VPoint): Double = x * vector.y - y * vector.x

        companion object {
            @JvmStatic
            val ORIGIN = VPoint(0, 0)

            @JvmStatic
            val ILLEGAL = VPoint(Double.NaN, Double.NaN)
        }
    }

    data class Circle(val center: Point, val r: Int) : Shape {
        override fun getBounds(): Rectangle = Rectangle(center.x - r, center.y - r, 2 * r, 2 * r)

        override fun getBounds2D(): Rectangle2D = bounds

        override fun contains(x: Double, y: Double): Boolean {
            val dx = x - center.x
            val dy = y - center.y
            val dis = sqrt(dx * dx + dy * dy)
            return dis < r
        }

        override fun contains(p: Point2D): Boolean = contains(p.x, p.y)

        override fun contains(x: Double, y: Double, w: Double, h: Double): Boolean {
            // upper-left: (x, y) lower-right: (x + w, y + h)
            val p = VPoint(x, y)
            if (p.euclidDistance(center) < r) {
                return true
            }
            p.setLocation(x + w, y + h)
            return p.euclidDistance(center) < r
        }

        override fun contains(r: Rectangle2D?): Boolean {
            if (r == null)
                return false
            return contains(r.x, r.y, r.width, r.height)
        }

        override fun intersects(x: Double, y: Double, w: Double, h: Double): Boolean {
            val normalizedX = center.x.absoluteValue
            val normalizedY = center.y.absoluteValue
            val dx = x - w - normalizedX
            val dy = y - normalizedY
            val dis = sqrt(dx * dx + dy * dy)
            return dis < r
        }

        override fun intersects(r: Rectangle2D): Boolean = intersects(r.x, r.y, r.width, r.height)

        private fun getEllipse() =
            Ellipse2D.Double(center.x.toDouble(), center.y.toDouble(), 2 * r.toDouble(), 2 * r.toDouble())

        override fun getPathIterator(at: AffineTransform): PathIterator = getEllipse().getPathIterator(at)

        override fun getPathIterator(at: AffineTransform, flatness: Double): PathIterator =
            getEllipse().getPathIterator(at, flatness)
    }

    internal fun projection(v: VPoint, vBegin: VPoint, p: VPoint): VPoint {
        val u = VPoint(vBegin.x - p.x, vBegin.y - p.y)
        val t: Double = (v.x * u.x + v.y * u.y) / (v.x * v.x + v.y * v.y)
        return VPoint(vBegin.x + t * v.x, vBegin.y + t * v.y)
    }

    private fun circleIntersectRect(c: Circle, r: Rectangle2D): Boolean = c.intersects(r)

    private fun rectIntersectRect(r1: Rectangle2D, r2: Rectangle2D): Boolean = r1.intersects(r2)

    private fun polygonIntersectPolygon(p1: Polygon, p2: Polygon): Boolean =
        Polygons.polygonIntersectPolygon(p1, p2, polygonIntersectMethod)

    private fun convexIntersectCircle(p: Polygon, c: Point, r: Int): Boolean {
        // Find the vertex of the convex polygon p that closest to the circle, mark as "a".
        // Same as the SAT, but the line l is the line ac.
        val xPoints = p.xpoints
        val yPoints = p.ypoints
        val size = p.npoints
        val points = Array(size) { VPoint(xPoints[it], yPoints[it]) }
        val center = VPoint(c.x, c.y)

        var minDis = Double.MAX_VALUE
        var minPoint = VPoint.ORIGIN
        for (vertex in points) {
            val dis = center.euclidDistance(vertex)
            if (dis < minDis) {
                minPoint = vertex
                minDis = dis
            }
        }

        val vector = VPoint(center.x - minPoint.x, center.y - minPoint.y)
        var lengthMin = Double.MAX_VALUE
        var lengthMax = Double.MIN_VALUE
        for (vertex in points) {
            val projectionPoint = projection(vector, center, vertex)
            val dis = center.euclidDistance(projectionPoint)
            if (dis < lengthMin)
                lengthMin = dis
            if (dis > lengthMax)
                lengthMax = dis
        }

        return !(lengthMax < 0 || lengthMin > r)
    }
}