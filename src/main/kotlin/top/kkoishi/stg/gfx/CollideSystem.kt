package top.kkoishi.stg.gfx

import top.kkoishi.stg.common.Bullet
import top.kkoishi.stg.common.Entity
import java.awt.Point
import java.awt.Polygon
import java.awt.Rectangle
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.absoluteValue
import kotlin.math.sqrt

object CollideSystem {
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
        return x < 0 || x > screenSize.width || y < 0 || y > screenSize.height
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
                is Circle -> return circleIntersectCircle(c.center, other.center, c.r, other.r)
                is Rectangle2D -> return circleIntersectRect(c, other)
                is Polygon -> return convexIntersectCircle(other, c.center, c.r)
            }
        } else if (s1 is Rectangle2D && s2 is Rectangle2D)
            return rectIntersectRect(s1, s2)
        else if (s1 is Polygon && s2 is Polygon)
            return convexIntersectConvex(s1, s2)
        throw UnsupportedOperationException("Unsupported intersect check: $s1 collide $s2")
    }

    private fun circleIntersectCircle(p1: Point, p2: Point, r1: Int, r2: Int): Boolean {
        val distance = p1.distance(p2)
        val r = (r1 + r2).toDouble()
        return distance >= r
    }

    private data class VPoint constructor(val x: Double, val y: Double) {
        constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

        fun directDistance(p: VPoint): Double {
            val dx = x - p.x
            val dy = y - p.y
            if (dx >= 0)
                return sqrt(dx * dx + dy * dy)
            return -1 * sqrt(dx * dx + dy * dy)
        }

        companion object {
            @JvmStatic
            val ORIGIN = VPoint(0, 0)
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
            TODO("Not yet implemented")
        }

        override fun contains(r: Rectangle2D?): Boolean {
            TODO("Not yet implemented")
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

        override fun getPathIterator(at: AffineTransform): PathIterator {
            TODO("Not yet implemented")
        }

        override fun getPathIterator(at: AffineTransform, flatness: Double): PathIterator {
            TODO("Not yet implemented")
        }
    }

    private fun projection(v: VPoint, vBegin: VPoint, p: VPoint): VPoint {
        val u = VPoint(vBegin.x - p.x, vBegin.y - p.y)
        val t: Double = (v.x * u.x + v.y * u.y) / (v.x * v.x + v.y * v.y)
        return VPoint(vBegin.x + t * v.x, vBegin.y + t * v.y)
    }

    private fun circleIntersectRect(c: Circle, r: Rectangle2D): Boolean = c.intersects(r)

    private fun rectIntersectRect(r1: Rectangle2D, r2: Rectangle2D): Boolean = r1.intersects(r2)

    private fun convexIntersectConvex(p1: Polygon, p2: Polygon): Boolean {
        // using SAT:
        // Select an edge(called e) of p1, and get a line(called l) which is perpendicular to e.
        // Get the collection of every projection point on l for all points on the edges of p1 and p2,
        // mark them as c1 and c2.
        // Check if the two line segments, that consists of c1 and c2, can intersect.
        // If they can intersect, return false, or continue this process.
        // If all the edges are traversed, return true.
        var xPoints = p1.xpoints
        var yPoints = p1.ypoints
        val size = xPoints.size
        val points1 = Array(size) { VPoint(xPoints[it], yPoints[it]) }
        xPoints = p2.xpoints
        yPoints = p2.ypoints
        val points2 = Array(size) { VPoint(xPoints[it], yPoints[it]) }
        xPoints = null
        yPoints = null

        var index = 0
        var before: VPoint = points1[index++]
        while (true) {
            if (index >= size)
                return true
            val cur = points1[index++]
            val normalVector = VPoint(before.x - cur.x, cur.x - before.x)

            val p1Min = 0
            var p1Max = Double.MIN_VALUE
            for (p in points1) {
                val projectionPoint = projection(normalVector, before, p)
                val dis = cur.directDistance(projectionPoint)
                if (dis > p1Max)
                    p1Max = dis
            }

            var p2Min = Double.MAX_VALUE
            var p2Max = Double.MIN_VALUE
            for (p in points2) {
                val projectionPoint = projection(normalVector, before, p)
                val dis = cur.directDistance(projectionPoint)
                if (dis < p2Min)
                    p2Min = dis
                if (dis > p2Max)
                    p2Max = dis
            }

            if (p1Max < p2Min || p2Max < p1Min)
                return false

            before = cur
        }
    }

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
            val dis = center.directDistance(vertex)
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
            val dis = center.directDistance(projectionPoint)
            if (dis < lengthMin)
                lengthMin = dis
            if (dis > lengthMax)
                lengthMax = dis
        }

        return !(lengthMax < 0 || lengthMin > r)
    }
}