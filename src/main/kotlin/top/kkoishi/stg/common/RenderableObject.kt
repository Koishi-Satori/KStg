package top.kkoishi.stg.common

import top.kkoishi.stg.common.entities.Object
import java.util.*
import java.util.concurrent.atomic.AtomicReference

abstract class RenderableObject(initialX: Int, initialY: Int) : Object {
    private val x: AtomicReference<Double> = AtomicReference(initialX.toDouble())
    private val y: AtomicReference<Double> = AtomicReference(initialY.toDouble())
    override val uuid: UUID
        get() = RENDERABLE_OBJECT_UUID

    fun x(): Double = x.get()
    fun y(): Double = y.get()
    fun x(x: Double) = this.x.set(x)
    fun y(y: Double) = this.y.set(y)

    final override fun collide(o: Object): Boolean = false

    companion object {
        internal val RENDERABLE_OBJECT_UUID = UUID.randomUUID()
    }
}