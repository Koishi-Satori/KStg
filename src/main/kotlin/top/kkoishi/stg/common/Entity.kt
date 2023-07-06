package top.kkoishi.stg.common

import java.awt.Shape

abstract class Entity(protected var health: Int): Object {
    abstract fun isDead(): Boolean
    abstract fun dead()
    abstract fun beingHit(o: Object)
    abstract fun move()
    abstract fun shape(): Shape

    override fun update(): Boolean {
        if (isDead()) {
            dead()
            return true
        }
        return false
    }
}