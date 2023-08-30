package top.kkoishi.stg.util

import java.util.*

object Mth {
    @JvmStatic
    private val cachedSin = TreeMap<Double, Double>()

    @JvmStatic
    private val cachedCos = TreeMap<Double, Double>()

    @JvmStatic
    private val cachedAsin = TreeMap<Double, Double>()

    @JvmStatic
    private val cachedAcos = TreeMap<Double, Double>()

    @JvmStatic
    private val cachedSqrt = TreeMap<Double, Double>()

    @Strictfp
    @JvmStatic
    fun Double.setScale(newScale: Int = 5): Double = MathInner.setScale(this, newScale)

    @Strictfp
    @JvmStatic
    fun sin(radian: Double): Double {
        val scaled = radian.setScale()
        var cached = cachedSin[scaled]
        if (cached == null) {
            cached = StrictMath.sin(scaled)
            cachedSin[scaled] = cached
        }
        return cached
    }

    @Strictfp
    @JvmStatic
    fun cos(radian: Double): Double {
        val scaled = radian.setScale()
        var cached = cachedCos[scaled]
        if (cached == null) {
            cached = StrictMath.cos(scaled)
            cachedCos[scaled] = cached
        }
        return cached
    }

    @Strictfp
    @JvmStatic
    fun asin(sin: Double): Double {
        val scaled = sin.setScale()
        var cached = cachedAsin[scaled]
        if (cached == null) {
            cached = StrictMath.asin(scaled)
            cachedAsin[scaled] = cached
        }
        return cached
    }

    @Strictfp
    @JvmStatic
    fun acos(cos: Double): Double {
        val scaled = cos.setScale()
        var cached = cachedAcos[scaled]
        if (cached == null) {
            cached = StrictMath.acos(scaled)
            cachedAcos[scaled] = cached
        }
        return cached
    }

    fun sqrt(a: Double): Double {
        val scaled = a.setScale()
        var cached = cachedSqrt[scaled]
        if (cached == null) {
            cached = StrictMath.sqrt(scaled)
            cachedSqrt[scaled] = cached
        }
        return cached
    }

    private object MathInner {
        @JvmStatic
        private val SCALE_INTEGERS = TreeMap<Int, Int>()

        @JvmStatic
        private val SCALE_DOUBLES = TreeMap<Int, Double>()

        init {
            init()
        }

        @Strictfp
        @JvmStatic
        private fun init() {
            var i = 10
            var d = 10.0
            for (scale in 1..24) {
                SCALE_INTEGERS[scale] = i
                SCALE_DOUBLES[scale] = d
                i *= 10
                d *= 10.0
            }
        }

        @Strictfp
        @JvmStatic
        fun setScale(d: Double, newScale: Int): Double {
            return (d * SCALE_INTEGERS[newScale]!!).toInt() / SCALE_DOUBLES[newScale]!!
        }
    }
}