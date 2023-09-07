package top.kkoishi.stg.logic

import top.kkoishi.stg.common.bullets.Bullet
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.common.entities.Player
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * An object pool used to store the game objects like UI, player, enemies, bullets and so on.
 *
 * @author KKoishi_
 */
object ObjectPool {
    private val player: AtomicReference<Player> = AtomicReference()
    private val bullets = ArrayDeque<Bullet>(1024)
    private val objects = ArrayDeque<Object>(128)
    private val UIObjects = ArrayDeque<Object>(16)
    internal val playerBullets: ArrayDeque<Bullet> = ArrayDeque(256)
    internal val objectMap = HashMap<UUID, Object>(2048)
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

    fun containsUIObject(o: Object): Boolean {
        val copy = synchronized(lock) {
            UIObjects.toTypedArray()
        }
        return copy.contains(o)
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
            objectMap[o.uuid] = o
        }
    }

    fun removeObject(index: Int) {
        synchronized(lock) {
            val uuid = objects.removeAt(index).uuid
            objectMap.remove(uuid)
        }
    }

    fun addBullet(b: Bullet) {
        synchronized(lock) {
            bullets.addLast(b)
            objectMap[b.uuid] = b
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

    fun clearAll() {
        synchronized(lock) {
            bullets.clear()
            objects.clear()
            playerBullets.clear()
            objectMap.clear()
            uiObjects().forEach { objectMap[it.uuid] = it }
        }
    }
}
