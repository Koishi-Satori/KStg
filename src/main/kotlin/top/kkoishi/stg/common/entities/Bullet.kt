package top.kkoishi.stg.common.entities

import java.awt.Point
import java.awt.Shape
import java.util.*
import java.util.concurrent.atomic.AtomicReference

abstract class Bullet(initialX: Int, initialY: Int) : Object {
    private val x = AtomicReference<Double>(initialX.toDouble())
    private val y = AtomicReference<Double>(initialY.toDouble())
    private val bulletUUID = UUID.randomUUID()

    override val uuid: UUID
        get() = bulletUUID

    fun x(): Int = x.get().toInt()
    fun y(): Int = y.get().toInt()
    fun xD(): Double = x.get()
    fun yD(): Double = y.get()
    fun pos(): Point = Point(x(), y())

    fun setX(nX: Int) {
        x.set(nX.toDouble())
    }

    fun setY(nY: Int) {
        y.set(nY.toDouble())
    }

    fun setX(nX: Double) {
        x.set(nX)
    }

    fun setY(nY: Double) {
        y.set(nY)
    }

    abstract fun shape(): Shape

    final override fun toString(): String = "Bullet@$uuid"
}