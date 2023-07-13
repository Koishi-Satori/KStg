package top.kkoishi.stg.common

import java.awt.Point
import java.awt.Shape
import java.util.concurrent.atomic.AtomicInteger

abstract class Bullet(initialX: Int, initialY: Int) : Object {
    protected val x = AtomicInteger(initialX)
    protected val y = AtomicInteger(initialY)

    fun x(): Int = x.get()
    fun y(): Int = y.get()
    fun pos(): Point = Point(x(), y())

    abstract fun shape(): Shape

    final override fun toString(): String = "Bullet@${hashCode()}-${pos()}"
}