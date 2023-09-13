package top.kkoishi.stg.util

import java.util.*

/**
 * The Mth class provides some optimized mathematical calculation methods, and it is recommended to call these
 * methods instead of those provided in the math library in kotlin/java.
 *
 * This class uses a large amount of cache and trades space for time to optimize computing efficiency.
 *
 * @author KKoishi_
 */
@Suppress("FunctionName")
object Mth {
    /**
     * Generated caches.
     */
    @JvmStatic
    private val CACHED_SIN = TreeMap<Double, Double>()

    /**
     * Generated caches.
     */
    @JvmStatic
    private val CACHED_COS = TreeMap<Double, Double>()

    /**
     * Generated caches.
     */
    @JvmStatic
    private val CACHED_TAN = TreeMap<Double, Double>()

    /**
     * Generated caches.
     */
    @JvmStatic
    private val CACHED_ASIN = TreeMap<Double, Double>()

    /**
     * Generated caches.
     */
    @JvmStatic
    private val CACHED_ACOS = TreeMap<Double, Double>()

    /**
     * Generated caches.
     */
    @JvmStatic
    private val CACHED_ATAN = TreeMap<Double, Double>()

    /**
     * Generated caches.
     */
    @JvmStatic
    private val CACHED_HYPOT = TreeMap<DoubleArray, Double>(Arrays::compare)

    /**
     * Generated caches.
     */
    @JvmStatic
    private val CACHED_SQRT = TreeMap<Double, Double>()

    /**
     * Generated caches.
     */
    @JvmStatic
    private val CACHED_GCD = TreeMap<IntArray, Int>(Arrays::compare)

    /**
     * Retains the previous certain number of digits in a double-precision floating-point number.
     *
     * The default is five digits.
     *
     * @param newScale The number of reserved digits, counting from the first digit after the decimal point.
     * @return Double with a certain number of digits after the decimal point.
     */
    @Strictfp
    @JvmStatic
    fun Double.setScale(newScale: Int = 5): Double = MathInner.setScale(this, newScale)

    /**
     * Retains the previous certain number of digits in a single-precision floating-point number.
     *
     * The default is five digits.
     *
     * @param newScale The number of reserved digits, counting from the first digit after the decimal point.
     * @return Double with a certain number of digits after the decimal point.
     */
    @Strictfp
    @JvmStatic
    fun Float.setScale(newScale: Int = 5): Float = MathInner.setScale(this, newScale)

    /**
     * Generated optimized method for trigonometric functions.
     *
     * @param radian the degree in radian.
     * @return the value of trigonometric functions.
     */
    @Strictfp
    @JvmStatic
    fun sin(radian: Double): Double {
        val scaled = radian.setScale()
        var cached = CACHED_SIN[scaled]
        if (cached == null) {
            cached = StrictMath.sin(scaled)
            CACHED_SIN[scaled] = cached
        }
        return cached
    }

    /**
     * Generated optimized method for trigonometric functions.
     *
     * @param radian the degree in radian.
     * @return the value of trigonometric functions.
     */
    @Strictfp
    @JvmStatic
    fun cos(radian: Double): Double {
        val scaled = radian.setScale()
        var cached = CACHED_COS[scaled]
        if (cached == null) {
            cached = StrictMath.cos(scaled)
            CACHED_COS[scaled] = cached
        }
        return cached
    }

    /**
     * Generated optimized method for trigonometric functions.
     *
     * @param radian the degree in radian.
     * @return the value of trigonometric functions.
     */
    @Strictfp
    @JvmStatic
    fun tan(radian: Double): Double {
        val scaled = radian.setScale()
        var cached = CACHED_TAN[scaled]
        if (cached == null) {
            cached = StrictMath.tan(scaled)
            CACHED_TAN[scaled] = cached
        }
        return cached
    }

    /**
     * Generated optimized method for anti-trigonometric functions.
     *
     * @param sin the degree in radian.
     * @return the value of anti-trigonometric functions.
     */
    @Strictfp
    @JvmStatic
    fun asin(sin: Double): Double {
        val scaled = sin.setScale()
        var cached = CACHED_ASIN[scaled]
        if (cached == null) {
            cached = StrictMath.asin(scaled)
            CACHED_ASIN[scaled] = cached
        }
        return cached
    }

    /**
     * Generated optimized method for anti-trigonometric functions.
     *
     * @param cos the degree in radian.
     * @return the value of anti-trigonometric functions.
     */
    @Strictfp
    @JvmStatic
    fun acos(cos: Double): Double {
        val scaled = cos.setScale()
        var cached = CACHED_ACOS[scaled]
        if (cached == null) {
            cached = StrictMath.acos(scaled)
            CACHED_ACOS[scaled] = cached
        }
        return cached
    }

    /**
     * Generated optimized method for anti-trigonometric functions.
     *
     * @param cos the degree in radian.
     * @return the value of anti-trigonometric functions.
     */
    @Strictfp
    @JvmStatic
    fun atan(cos: Double): Double {
        val scaled = cos.setScale()
        var cached = CACHED_ATAN[scaled]
        if (cached == null) {
            cached = StrictMath.atan(scaled)
            CACHED_ATAN[scaled] = cached
        }
        return cached
    }

    @JvmStatic
    fun sqrt(a: Double): Double {
        val scaled = a.setScale()
        var cached = CACHED_SQRT[scaled]
        if (cached == null) {
            cached = StrictMath.sqrt(scaled)
            CACHED_SQRT[scaled] = cached
        }
        return cached
    }

    @JvmStatic
    fun coprime(a: Int, b: Int) = gcd(a, b) == 1

    @JvmStatic
    fun gcd(a: Int, b: Int): Int {
        val key = intArrayOf(a, b)
        var gcd = CACHED_GCD[key]
        if (gcd != null)
            return gcd
        gcd = stein_gcd(a, b)
        CACHED_GCD[key] = gcd
        return gcd
    }

    @JvmStatic
    private fun stein_gcd(a: Int, b: Int): Int {
        if (a == 0)
            return b
        if (b == 0)
            return a

        // if a/b is even number
        val aFlag = a / 2 * 2 == a
        val bFlag = b / 2 * 2 == b

        return if (aFlag && bFlag)
            2 * stein_gcd(a shr 1, b shr 1)
        else if (aFlag)
            stein_gcd(a shr 1, b)
        else if (bFlag)
            stein_gcd(a, b shr 1)
        else {
            if (a >= b)
                stein_gcd(b, a - b)
            else
                stein_gcd(a, b - a)
        }
    }

    /**
     * hypot(x,y) = sqrt(x * x + y * y)
     *
     * Method :
     *
     *      If (assume round-to-nearest) z = x*x + y*y
     *      has error less than sqrt(2)/2 ulp, than
     *      sqrt(z) has error less than 1 ulp (exercise).
     *
     *      So, compute sqrt(x*x + y*y) with some care as
     *      follows to get the error below 1 ulp:
     *
     *      Assume x > y > 0;
     *      (if possible, set rounding to round-to-nearest)
     *      1. if x > 2y  use
     *              x1*x1 + (y*y + (x2*(x + x1))) for x*x + y*y
     *      where x1 = x with lower 32 bits cleared, x2 = x - x1; else
     *      2. if x <= 2y use
     *              t1*y1 + ((x-y) * (x-y) + (t1*y2 + t2*y))
     *      where t1 = 2x with lower 32 bits cleared, t2 = 2x - t1,
     *      y1= y with lower 32 bits chopped, y2 = y - y1.
     *
     *      NOTE: scaling may be necessary if some argument is too
     *            large or too tiny
     *
     *      You might need to use setScale method before invoking this.
     *
     * Special cases:
     *
     *      hypot(x,y) is INF if x or y is +INF or -INF; else
     *      hypot(x,y) is NAN if x or y is NAN.
     *
     * Accuracy:
     *
     *      hypot(x,y) returns sqrt(x^2 + y^2) with error less
     *      than 1 ulp (unit in the last place)
     */
    @JvmStatic
    fun hypot(x: Double, y: Double): Double {
        val key = doubleArrayOf(x, y)
        var result = CACHED_HYPOT[key]
        if (result != null)
            return result
        result = StrictMath.hypot(x, y)
        CACHED_HYPOT[key] = result
        return result
    }

    private object MathInner {
        /**
         * Generated caches.
         */
        @JvmStatic
        private val SCALE_INTEGERS = TreeMap<Int, Int>()

        /**
         * Generated caches.
         */
        @JvmStatic
        private val SCALE_DOUBLES = TreeMap<Int, Double>()

        /**
         * Generated caches.
         */
        @JvmStatic
        private val SCALE_FLOATS = TreeMap<Int, Float>()

        init {
            init()
        }

        @Strictfp
        @JvmStatic
        private fun init() {
            var i = 10
            var d = 10.0
            var f = 10f
            for (scale in 1..24) {
                SCALE_INTEGERS[scale] = i
                SCALE_DOUBLES[scale] = d
                SCALE_FLOATS[scale] = f
                i *= 10
                d *= 10.0
                f *= 10f
            }
        }

        @Strictfp
        @JvmStatic
        fun setScale(d: Double, newScale: Int): Double {
            return (d * SCALE_INTEGERS[newScale]!!).toInt() / SCALE_DOUBLES[newScale]!!
        }

        @Strictfp
        @JvmStatic
        fun setScale(f: Float, newScale: Int): Float {
            return (f * SCALE_INTEGERS[newScale]!!).toInt() / SCALE_FLOATS[newScale]!!
        }
    }
}