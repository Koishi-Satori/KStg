package top.kkoishi.stg.logic

import top.kkoishi.stg.common.entities.Bullet
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.common.entities.Player
import java.util.concurrent.atomic.AtomicReference

object ObjectPool {
    private val player: AtomicReference<Player> = AtomicReference()
    private val bullets = ArrayDeque<Bullet>(1024)
    private val objects = ArrayDeque<Object>(128)
    private val UIObjects = ArrayDeque<Object>(16)
    private val lock = Any()

    fun player(): Player = player.get()

    fun player(p: Player) = player.set(p)


    fun uiObjects(): Iterator<Object> {
        synchronized(lock) {
            return UIObjects.toTypedArray().iterator()
        }
    }

    fun addUIObject(o: Object) {
        synchronized(lock) {
            UIObjects.addLast(o)
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
