package top.kkoishi.stg.test.common

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.common.entities.PlayerBullet
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Renderer
import top.kkoishi.stg.logic.GenericFlags
import top.kkoishi.stg.logic.GenericFlags.gameState
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.logic.PlayerManager
import top.kkoishi.stg.logic.Threads
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.outputStream
import kotlin.math.PI

class TestPlayerKoishi(initialX: Int, initialY: Int, bulletTexture: String) : Player(initialX, initialY) {
    private var textureIndex = 0
    private val bullet = GFX.getTexture(bulletTexture)
    private val slowBullet = GFX.getTexture("bullet_koishi_slow_0")


    override fun bulletDamage(): Int = 5

    override fun bullet(dx: Int, dy: Int): PlayerBullet =
        if (slower)
            object : PlayerBullet(this.xInt() + dx, this.yInt() + dy) {
                override fun move() {
                    setY(y() - 6)
                }

                override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 4)

                override fun paint(g: Graphics2D) {
                    // 16 * 35
                    val x = xD()
                    val y = yD()
                    val rd = slowBullet.renderPoint(x, y, PI / 2)
                    slowBullet.paint(g, slowBullet.rotate(-PI / 2), rd.x, rd.y)
                }
            }
        else
            object : PlayerBullet(this.xInt() + dx, this.yInt() + dy) {
                override fun move() {
                    setY(y() - 4)
                }

                override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 5)

                override fun paint(g: Graphics2D) {
                    // 16 * 35
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
        if (PlayerManager.binds[KeyEvent.VK_F11]) {
            this::class.logger().log(System.Logger.Level.INFO, "Screenshot pressed.")
            PlayerManager.binds[KeyEvent.VK_F11] = false
            // save screenshot
            val out =
                Path.of("${Threads.workdir()}/screenshots/screenshot_${System.currentTimeMillis()}_${Renderer.frame()}.png")
                    .outputStream()
            val imageOut = ImageIO.createImageOutputStream(out)
            val img = BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB)
            val bRender = img.createGraphics()
            when (gameState.get()) {
                GenericFlags.STATE_PLAYING -> {
                    try {
                        PlayerManager.cur.paint(bRender)
                        ObjectPool.player().paint(bRender)
                        ObjectPool.objects().forEach { it.paint(bRender) }
                        ObjectPool.bullets().forEach { it.paint(bRender) }
                        ObjectPool.uiObjects().forEach { it.paint(bRender) }
                        bRender.dispose()
                    } catch (e: Throwable) {
                        logger.log(System.Logger.Level.ERROR, e)
                    }
                }

                GenericFlags.STATE_PAUSE -> {
                    PlayerManager.cur.paint(bRender)
                    ObjectPool.player().paint(bRender)
                    ObjectPool.objects().forEach { it.paint(bRender) }
                    ObjectPool.bullets().forEach { it.paint(bRender) }
                    ObjectPool.uiObjects().forEach { it.paint(bRender) }
                }

                GenericFlags.STATE_MENU -> {
                    try {
                        // main menu
                        ObjectPool.uiObjects().forEach { it.paint(bRender) }
                    } catch (e: Throwable) {
                        logger.log(System.Logger.Level.ERROR, e)
                    }
                }
            }
            bRender.dispose()
            ImageIO.write(img, "png", imageOut)
            imageOut.close()
            out.close()
        }
    }
}