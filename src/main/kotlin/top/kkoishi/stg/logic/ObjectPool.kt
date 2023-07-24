package top.kkoishi.stg.logic

import top.kkoishi.stg.common.entities.Bullet
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.common.entities.Player

object ObjectPool {
    lateinit var player: Player
    private val bullets = ArrayDeque<Bullet>(1024)
    private val objects = ArrayDeque<Object>(128)
    private var locked = false
    private val lock = Any()

    fun lock() {
        synchronized(lock) {
            locked = true
        }
    }

    fun release() {
        synchronized(lock) {
            locked = false
        }
    }

    fun bullets(): Iterator<Bullet> {
        synchronized(lock) {
            return bullets.toTypedArray().iterator()
        }
    }

    fun objects(): Iterator<Object> {
        synchronized(lock) {
            return objects.toTypedArray().iterator()
        }
    }

    fun addObject(o: Object) {
        synchronized(lock) {
            objects.addLast(o)
        }
    }

    fun removeObject(index: Int) {
        synchronized(lock) {
            objects.removeAt(index)
        }
    }

    fun addBullet(b: Bullet) {
        synchronized(lock) {
            bullets.addLast(b)
        }
    }

    fun removeBullet(index: Int) {
        synchronized(lock) {
            bullets.removeAt(index)
        }
    }

    fun countBullets(): Int {
        synchronized(lock) {
            return bullets.size
        }
    }
}
