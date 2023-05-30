package top.kkoishi.stg.logic

import top.kkoishi.stg.common.Bullet
import top.kkoishi.stg.common.Player
import top.kkoishi.stg.common.Stage
import java.awt.Graphics2D

object PlayerManager {
    private var life = 2
    private val lock = Any()
    private val bullets: ArrayDeque<Bullet> = ArrayDeque(256)
    lateinit var cur: Stage

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

    fun getDamage(p: Player): Int {
        synchronized(lock) {
            return p.bulletDamage()
        }
    }

    fun addBullet(b: Bullet) {
        synchronized(lock) {
            bullets.addLast(b)
        }
    }

    fun paintBullets(g: Graphics2D) {
        synchronized(lock) {
            for (b in bullets)
                b.paint(g)
        }
    }

    fun updateBullets() {
        synchronized(lock) {
            var index = 0
            for (b in bullets.toTypedArray().iterator()) {
                if (b.update())
                    bullets.removeAt(index--)
                ++index
            }
        }
    }
}