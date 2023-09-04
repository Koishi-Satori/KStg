package top.kkoishi.stg.logic

import top.kkoishi.stg.common.entities.Bullet
import top.kkoishi.stg.common.Stage
import top.kkoishi.stg.logic.ObjectPool.objectMap
import top.kkoishi.stg.logic.ObjectPool.playerBullets
import java.awt.Graphics2D
import java.util.UUID

object PlayerManager {
    private var life = 2
    private val lock = Any()
    lateinit var curStage: Stage

    fun life(): Int {
        synchronized(lock) {
            return life
        }
    }

    fun setLife(nLife: Int) {
        synchronized(lock) {
            life = nLife
        }
    }

    fun addBullet(b: Bullet) {
        synchronized(lock) {
            playerBullets.addLast(b)
            objectMap[b.uuid] = b
        }
    }

    fun countBullets(): Int {
        synchronized(lock) {
            return playerBullets.size
        }
    }

    fun paintBullets(g: Graphics2D) {
        synchronized(lock) {
            for (b in playerBullets)
                b.paint(g)
        }
    }

    fun updateBullets() {
        synchronized(lock) {
            var index = 0
            var uuid: UUID
            for (b in playerBullets.toTypedArray().iterator()) {
                if (b.update()) {
                    uuid = playerBullets.removeAt(index--).uuid
                    objectMap.remove(uuid)
                }
                ++index
            }
        }
    }
}