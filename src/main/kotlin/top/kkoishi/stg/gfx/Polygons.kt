package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.awt.Polygon
import kotlin.math.absoluteValue

internal object Polygons {
    internal fun polygonIntersectPolygon(
        p1: Polygon,
        p2: Polygon,
        method: CollideSystem.PolygonIntersectMethod,
    ): Boolean {
        val points1 = makePolygon(p1)
        val points2 = makePolygon(p2)
        when (method) {
            CollideSystem.PolygonIntersectMethod.SAT -> {
                if (isConcave(points1)) {
                    val polygons1 = divideConcave(points1)
                    if (isConcave(points2)) {
                        val polygons2 = divideConcave(points2)
                        polygons1.forEach { p ->
                            polygons2.forEach {
                                if (SAT.convexIntersectConvexImpl(p, it))
                                    return true
                            }
                        }
                    } else
                        polygons1.forEach {
                            if (SAT.convexIntersectConvexImpl(it, points2))
                                return true
                        }
                } else if (isConcave(points2)) {
                    val polygons2 = divideConcave(points2)
                    polygons2.forEach {
                        if (SAT.convexIntersectConvexImpl(it, points1))
                            return true
                    }
                } else
                    return SAT.convexIntersectConvexImpl(points1, points2)
            }

            CollideSystem.PolygonIntersectMethod.GJK -> {
                if (isConcave(points1)) {
                    val polygons1 = divideConcave(points1)
                    if (isConcave(points2)) {
                        val polygons2 = divideConcave(points2)
                        polygons1.forEach { p ->
                            polygons2.forEach {
                                if (GJK.convexIntersectConvexImpl(p, it, p[0] - it[0]))
                                    return true
                            }
                        }
                    } else
                        polygons1.forEach {
                            if (GJK.convexIntersectConvexImpl(it, points2, it[0] - points2[0]))
                                return true
                        }
                } else if (isConcave(points2)) {
                    val polygons2 = divideConcave(points2)
                    polygons2.forEach {
                        if (GJK.convexIntersectConvexImpl(it, points1, it[0] - points1[0]))
                            return true
                    }
                } else
                    return GJK.convexIntersectConvexImpl(points1, points2, points1[0] - points2[0])
            }

            CollideSystem.PolygonIntersectMethod.ONLY_PRETEST -> {
                Polygons::class.logger().log(System.Logger.Level.WARNING, "Detailed collide test is skipped.")
                return true
            }
        }
        return false
    }

    private fun isConcave(p: Array<CollideSystem.VPoint>): Boolean {
        if (p.size <= 4)
            return false

        var index = 0
        var a = p[index++]
        var b = p[index++]
        var c = p[index]

        while (index++ < p.size) {
            val d = p[index]
            val w = (b - c)
            val cross0 = (a - b) x w
            val cross1 = w x (c - d)
            if (cross0 * cross1 <= 0)
                return true

            a = b
            b = d
            c = d
        }
        return false
    }

    private fun divideConcave(p: Array<CollideSystem.VPoint>): Array<Array<CollideSystem.VPoint>> {
        val result = ArrayDeque<Array<CollideSystem.VPoint>>()
        (p.indices).forEach OutLoop@{ i ->
            val index0 = (i + 1) % p.size
            val index1 = (i + 2) % p.size
            val p0 = p[i]
            val p1 = p[index0]
            val p2 = p[index1]
            val edge = (p0 - p1)
            val cross = edge x (p1 - p2)

            if (cross > 0) {
                (p.indices).forEach {
                    val current = p[it]
                    val index = (it + 1) % p.size
                    val next = p[index]
                    if (!(i == it || i == index)) {
                        val cross0 = edge x (p0 - current)
                        val cross1 = edge x (p1 - next)

                        if (cross0 * cross1 < 0) {
                            // TODO:spilt concave.
                            return@OutLoop
                        }
                    }
                }

            }
        }
        val res = result.toTypedArray()
        result.clear()
        return res
    }

    private fun makePolygon(p: Polygon): Array<CollideSystem.VPoint> {
        val xPoints = p.xpoints
        val yPoints = p.ypoints
        val size = xPoints.size
        return Array(size) { CollideSystem.VPoint(xPoints[it], yPoints[it]) }
    }

    object SAT {
        internal fun convexIntersectConvexImpl(
            points1: Array<CollideSystem.VPoint>,
            points2: Array<CollideSystem.VPoint>,
        ): Boolean {
            /*
            using SAT:
            1. Selects an edge(called e) of p1, and get a line(called l) which is perpendicular to e.
            2. Gets the collections of projection points on l for all points on the edges of p1 and p2, and labels them as c1 and c2.
            3. Checks if the two line segments, that respectively consists of c1 and c2 can intersect.
            4. If they can intersect, return false, or continue this process.
            5. If all the edges are traversed, return true.
             */
            val size = points1.size
            var index = 0
            var before: CollideSystem.VPoint = points1[index++]
            while (true) {
                if (index >= size)
                    return true
                val cur = points1[index++]
                // line between before and cur.
                val normalVector = CollideSystem.VPoint(before.x - cur.x, cur.x - before.x)

                var p1Min = Double.MAX_VALUE
                var p1Max = Double.MIN_VALUE
                points1.forEach {
                    // gets the projection point of p on the line.
                    val projectionPoint = CollideSystem.projection(normalVector, before, it)
                    val dis = cur.euclidDistance(projectionPoint)
                    if (dis > p1Max)
                        p1Max = dis
                    if (dis < p1Min)
                        p1Min = dis
                }

                var p2Min = Double.MAX_VALUE
                var p2Max = Double.MIN_VALUE
                points2.forEach {
                    val projectionPoint = CollideSystem.projection(normalVector, before, it)
                    val dis = cur.euclidDistance(projectionPoint)
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
    }

    object GJK {
        /**
         * Find the further point of the polygon on the given direction.
         *
         * @param p the polygon
         * @param direction the direction
         * @return the further point
         */
        private fun support(p: Array<CollideSystem.VPoint>, direction: CollideSystem.VPoint): CollideSystem.VPoint {
            // Iterate through each point and find the maximum value of the vector point multiplied between that
            // point vector and the direction vector
            var max: Double = Double.MIN_VALUE
            val result = CollideSystem.VPoint(0, 0)
            p.forEach {
                val dot = it * direction
                if (dot > max) {
                    max = dot
                    result.setLocation(it.x, it.y)
                }
            }
            return result
        }

        private fun newDirection(
            begin: CollideSystem.VPoint,
            end: CollideSystem.VPoint,
            direction: CollideSystem.VPoint,
        ) {
            val dx = begin.x - end.x
            val dy = end.y - begin.y
            val factor = dx / dy
            val cross = (begin - end) x (begin - CollideSystem.VPoint.ORIGIN)
            when {
                cross.absoluteValue <= 0.2 -> {
                    if (begin.y > 0)
                        direction.setLocation(-1.0, -factor)
                    else
                        direction.setLocation(1.0, factor)
                }

                cross > 0 -> direction.setLocation(-1.0, -factor)
                else -> direction.setLocation(1.0, factor)
            }
        }

        private fun Array<CollideSystem.VPoint>.nearestSimplex(direction: CollideSystem.VPoint):
                Boolean {
            if (this[2] == CollideSystem.VPoint.ILLEGAL)
                newDirection(this[0], this[1], direction)
            else {
                val area = ((this[0] - this[1]) x (this[1] - this[2])).absoluteValue
                val arr = arrayOf(0 to 1, 1 to 2, 2 to 0)
                var oArea = 0.0
                arr.forEach { oArea += this[it.first] x this[it.second] }
                if ((area - oArea.absoluteValue).absoluteValue <= 0.02)
                    return true

                var index = 0
                var min = Double.MAX_VALUE
                arr.forEachIndexed { i, (f, s) ->
                    val value = this[f] x this[s]
                    if (min < value) {
                        index = i
                        min = value
                    }
                }
                with(arr[index]) {
                    newDirection(this@nearestSimplex[first], this@nearestSimplex[second], direction)
                }
            }
            return false
        }

        internal fun convexIntersectConvexImpl(
            p1: Array<CollideSystem.VPoint>,
            p2: Array<CollideSystem.VPoint>,
            initDirection: CollideSystem.VPoint,
        ): Boolean {
            var supportPoint = support(p1, initDirection) - support(p2, initDirection)
            val simplex = Array(3) { CollideSystem.VPoint.ILLEGAL }
            var index = 0
            simplex[index++] = supportPoint
            val direction = -supportPoint

            while (true) {
                supportPoint = support(p1, direction) - support(p2, direction)
                if (supportPoint * direction < 0)
                    return false
                simplex[index++] = supportPoint
                if (index >= 3)
                    index = 0

                // use cross product of vectors to judge direction.
                if (simplex.nearestSimplex(direction))
                    return true
            }
        }
    }
}