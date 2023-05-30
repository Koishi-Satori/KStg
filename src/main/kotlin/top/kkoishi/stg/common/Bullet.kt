package top.kkoishi.stg.common

import java.awt.Point
import java.awt.Shape

abstract class Bullet(initialX: Int, initialY: Int) : Object {
    protected val x: ThreadLocal<Int> = ThreadLocal()
    protected val y: ThreadLocal<Int> = ThreadLocal()

    init {
        x.set(initialX)
        y.set(initialY)
    }

    fun x(): Int = x.get()
    fun y(): Int = y.get()
    fun pos(): Point = Point(x(), y())

    abstract fun shape(): Shape

    final override fun toString(): String = "Bullet@${hashCode()}-${pos()}"
}