package top.kkoishi.stg.logic

import top.kkoishi.stg.common.entities.Bullet
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.common.Stage
import top.kkoishi.stg.logic.ObjectPool.objectMap
import top.kkoishi.stg.logic.ObjectPool.playerBullets
import java.awt.Graphics2D
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.UUID
import javax.swing.JFrame

object PlayerManager {
    private var life = 2
    private val lock = Any()
    val binds = BooleanArray(526)
        get(): BooleanArray {
            synchronized(lock) {
                return field
            }
        }
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

    fun keyBind(f: JFrame, keyCode: Int) {
        binds[keyCode] = false
        f.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
            }

            override fun keyPressed(e: KeyEvent) {
                binds[e.keyCode] = true
            }

            override fun keyReleased(e: KeyEvent) {
                binds[e.keyCode] = false
            }
        })
    }

    fun keyBinds(f: JFrame, vararg keyCodes: Int) {
        for (keyCode in keyCodes)
            keyBind(f, keyCode)
    }
}