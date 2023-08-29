package top.kkoishi.stg.logic

import java.math.BigDecimal
import java.math.RoundingMode

internal object Mth {
    fun Double.setScale(count: Int = 5): Double {
        val b = BigDecimal(this)
        return b.setScale(count, RoundingMode.DOWN).toDouble()
    }
}