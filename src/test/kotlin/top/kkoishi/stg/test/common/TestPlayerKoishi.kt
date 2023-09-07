package top.kkoishi.stg.test.common

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.common.bullets.PlayerBullet
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.test.Test
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import kotlin.math.PI

class TestPlayerKoishi(initialX: Int, initialY: Int, bulletTexture: String) : Player(initialX, initialY, Test.invincible) {
    private var textureIndex = 0
    private val bullet = GFX.getTexture(bulletTexture)
    private val slowBullet = GFX.getTexture("bullet_koishi_slow_0")


    override fun bulletDamage(): Int = 5

    override fun bullet(dx: Int, dy: Int): PlayerBullet =
        if (slower)
            object : PlayerBullet(xInt() + dx, yInt() + dy) {
                override fun move() {
                    setY(y() - 6)
                }

                override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 5)

                override fun paint(g: Graphics2D) {
                    // 16 * 32
                    val x = xD()
                    val y = yD()
                    val rd = slowBullet.renderPoint(x, y, PI / 2)
                    slowBullet.paint(g, slowBullet.rotate(-PI / 2), rd.x, rd.y)
                }
            }
        else
            object : PlayerBullet(xInt() + dx, yInt() + dy) {
                override fun move() {
                    setY(y() - 4)
                }

                override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 5)

                override fun paint(g: Graphics2D) {
                    // 16 * 36
                    val x = xD()
                    val y = yD()
                    val rd = bullet.renderPoint(x, y, PI / 2)
                    bullet.paint(g, bullet.rotate(-PI / 2), rd.x, rd.y)
                }
            }

    override fun shot() {
        AudioPlayer.addTask("test_player_shot")
        AudioPlayer.addTask("test_player_shot")
        synchronized(lock) {
            if (slower) {
                if (power >= 0.0f && power < 2.0f)
                    PlayerManager.addBullet(bullet(0, -32))
                else if (power >= 2.0f && power < 3.0f) {
                    PlayerManager.addBullet(bullet(8, -32))
                    PlayerManager.addBullet(bullet(-8, -32))
                } else if (power >= 3.0f && power < 4.0f) {
                    PlayerManager.addBullet(bullet(8, -32))
                    PlayerManager.addBullet(bullet(0, -32))
                    PlayerManager.addBullet(bullet(-8, -32))
                } else {
                    PlayerManager.addBullet(bullet(16, -32))
                    PlayerManager.addBullet(bullet(8, -32))
                    PlayerManager.addBullet(bullet(-8, -32))
                    PlayerManager.addBullet(bullet(-16, -32))
                }
            } else {
                if (power >= 0.0f && power < 2.0f)
                    PlayerManager.addBullet(bullet(0, -32))
                else if (power >= 2.0f && power < 3.0f) {
                    PlayerManager.addBullet(bullet(16, -32))
                    PlayerManager.addBullet(bullet(-16, -32))
                } else if (power >= 3.0f && power < 4.0f) {
                    PlayerManager.addBullet(bullet(16, -32))
                    PlayerManager.addBullet(bullet(0, -32))
                    PlayerManager.addBullet(bullet(-16, -32))
                } else {
                    PlayerManager.addBullet(bullet(32, -32))
                    PlayerManager.addBullet(bullet(16, -32))
                    PlayerManager.addBullet(bullet(-16, -32))
                    PlayerManager.addBullet(bullet(-32, -32))
                }
            }
        }
    }

    override fun texture(): String {
        if (textureIndex >= 64)
            textureIndex = 0
        return "plane_koishi_${moveState}_${textureIndex++ / 8}"
    }

    override fun bomb() {
    }

    override fun dead() {
    }

    override fun shape(): Shape = CollideSystem.Circle(Point(xInt(), yInt()), 5)

    override fun paint(g: Graphics2D) {
        if (slower) {
            val key = texture()
            val t = GFX.getTexture(key)
            val xI = x()
            val yI = y()
            val effect = GFX.getTexture("slow_effect_final")
            var rd = effect.renderPoint(xI, yI)
            effect.paint(g, effect.normalMatrix(), rd.x, rd.y)
            rd = t.renderPoint(xI, yI)
            t.paint(g, t.normalMatrix(), rd.x, rd.y)

            val center = GFX.getTexture("center")
            rd = center.renderPoint(xI, yI)
            center.paint(g, center.normalMatrix(), rd.x, rd.y)
        } else {
            val key = texture()
            val t = GFX.getTexture(key)
            val xI = x()
            val yI = y()
            val point = t.renderPoint(xI, yI)
            t.paint(g, t.normalMatrix(), point.x, point.y)
        }

        // render player bullets
        PlayerManager.paintBullets(g)
    }

    override fun actionsImpl() {
    }
}