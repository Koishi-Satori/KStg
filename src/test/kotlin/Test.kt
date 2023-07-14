import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.common.*
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Renderer
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.*
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JFrame
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.system.exitProcess

object Test {
    private val rand = Random(System.currentTimeMillis())

    @JvmStatic
    fun main(args: Array<String>) {
        GenericFlags.gameState.set(GenericFlags.STATE_PLAYING)
        GFX.loadTexture("test_img", "./test/gfx/icons/test_img.png")
        GFX.loadTexture("mirror", "./test/gfx/icons/mirror.png")
        GFX.loadTexture("center", "./test/gfx/icons/center.png")
        GFX.loadTexture("planes_koishi", "./test/gfx/icons/planes_koishi.png")
        GFX.loadTexture("bg_1_0", "./test/gfx/icons/bg_1_0.png")
        GFX.loadTexture("slow_effect", "./test/gfx/icons/slow_effect.png")
        GFX.loadTexture("bullets_0", "./test/gfx/icons/bullets_0.png")
        GFX.cutTexture("planes_koishi", "plane_koishi_0_0", 0, 0, 32, 48)
        GFX.cutTexture("planes_koishi", "plane_koishi_0_1", 32, 0, 32, 48)
        GFX.cutTexture("planes_koishi", "plane_koishi_0_2", 64, 0, 32, 48)
        GFX.cutTexture("planes_koishi", "plane_koishi_0_3", 96, 0, 32, 48)
        GFX.cutTexture("planes_koishi", "plane_koishi_0_4", 128, 0, 32, 48)
        GFX.cutTexture("planes_koishi", "plane_koishi_0_5", 160, 0, 32, 48)
        GFX.cutTexture("planes_koishi", "plane_koishi_0_6", 192, 0, 32, 48)
        GFX.cutTexture("planes_koishi", "plane_koishi_0_7", 224, 0, 32, 48)
        GFX.cutTexture("planes_koishi", "bullet_koishi", 220, 144, 35, 16)
        GFX.cutTexture("slow_effect", "slow_effect_final", 0, 0, 64, 64)
        GFX.cutTexture("bullets_0", "test_bullet", 128, 0, 8, 8)

        Sounds.loadAudio("bk_1", "./test/audio/sounds/bk_1.wav")
        Sounds.loadAudio("test_player_shot", "./test/audio/sounds/th15_player_shot_0.wav")
        Sounds.loadAudio("th15_enemy_damage_01", "./test/audio/sounds/th15_enemy_damage_01.wav")
        Sounds.loadAudio("th15_enemy_damage_02", "./test/audio/sounds/th15_enemy_damage_02.wav")
        Sounds.loadAudio("enemy_dead", "./test/audio/sounds/enemy_dead_0.wav")
        Sounds.loadAudio("enemy_shoot", "./test/audio/sounds/enemy_shoot.wav")

        //println(GFX.getTexture("test_img"))
        val f = JFrame("test")
        f.setSize(512 + 14, 512 + 37)
        f.isVisible = true
        f.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exitProcess(514)
            }
        })

        Graphics.refresh(f)
        PlayerManager.cur = object : Stage() {
            override fun background(): Texture = GFX.getTexture("bg_1_0")

            override fun action() {}

            override fun toNextStage(): Boolean = false

            override fun nextStage(): Stage {
                TODO("Not yet implemented")
            }

            override fun paint(g: Graphics2D) {
                super.paint(g)
            }
        }

        val bullet = GFX.getTexture("bullet_koishi")
        ObjectPool.player = object : Player(250, 200) {
            private var texture_index = 0

            override fun bulletDamage(): Int = 10

            override fun bullet(dx: Int, dy: Int): PlayerBullet =
                object : PlayerBullet(this.x.get() + dx, this.y.get() + dy) {
                    override fun move() {
                        setY(y() - 5)
                    }

                    override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 5)

                    override fun paint(g: Graphics2D) {
                        // 16 * 35
                        val rd = bullet.renderPoint(x(), y())
                        bullet.paint(g, bullet.rotate(PI / 2), rd.x, rd.y)
                    }
                }

            override fun shot() {
                AudioPlayer.addTask(Sounds.getAudio("test_player_shot"))
                if (power >= 0.0f)
                    PlayerManager.addBullet(bullet(0, -32))
                else if (power >= 2.0f) {
                    PlayerManager.addBullet(bullet(8, -32))
                    PlayerManager.addBullet(bullet(-8, -32))
                }
            }

            override fun texture(): String {
                if (texture_index >= 64)
                    texture_index = 0
                return "plane_koishi_0_${texture_index++ / 8}"
            }

            override fun bomb() {
                println("bomb")
            }

            override fun dead() {
            }

            override fun shape(): Shape = CollideSystem.Circle(Point(x.get(), y.get()), 5)

            override fun paint(g: Graphics2D) {
                if (showCenter) {
                    val key = texture()
                    val t = GFX.getTexture(key)
                    val xI = x.get()
                    val yI = y.get()
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
                    val xI = x.get()
                    val yI = y.get()
                    val point = t.renderPoint(xI, yI)
                    t.paint(g, t.normalMatrix(), point.x, point.y)
                }

                // render player bullets
                PlayerManager.paintBullets(g)
            }
        }

        val inst = Threads.getInstance()
        Renderer.start(inst)
        GameLoop.start(inst)
        AudioPlayer.start(inst)

        AudioPlayer.setBackground(Sounds.getAudio("bk_1"))
        PlayerManager.keyBinds(
            f,
            Player.VK_C,
            Player.VK_DOWN,
            Player.VK_ESCAPE,
            Player.VK_LEFT,
            Player.VK_RIGHT,
            Player.VK_SHIFT,
            Player.VK_UP,
            Player.VK_X,
            Player.VK_Z
        )

        ObjectPool.addObject(enemy(100, 100, 100, "mirror") { _, _ -> })
    }

    fun enemy(
        initialX: Int,
        initialY: Int,
        health: Int,
        texture: String,
        moveFunc: (AtomicInteger, AtomicInteger) -> Unit,
    ): Enemy {
        return object : Enemy(health) {
            private var x = AtomicInteger(initialX)
            private var y = AtomicInteger(initialY)
            private var bulletCount = 0
            private var ang = 0.0

            override fun dead() {
                AudioPlayer.addTask(Sounds.getAudio("enemy_dead"))
            }

            override fun beingHit(o: Object) {
                this.health -= ObjectPool.player.bulletDamage()
                AudioPlayer.addTask(Sounds.getAudio("th15_enemy_damage_0${(rand.nextInt() % 2).absoluteValue + 1}"))
            }

            override fun move() {
                moveFunc(x, y)
            }

            override fun shape(): Shape = CollideSystem.Circle(Point(x.get(), y.get()), 16)

            override fun collide(o: Object): Boolean {
                if (o is PlayerBullet && CollideSystem.collide(this, o)) {
                    beingHit(o)
                    return true
                }
                return false
            }

            override fun paint(g: Graphics2D) {
                val t = GFX.getTexture(texture)
                val xI = x.get()
                val yI = y.get()
                t.paint(g, t.normalMatrix(), xI, yI)
            }

            override fun update(): Boolean {
                val dead = super.update()
                if (!dead) {
                    bullet()
                }
                return dead
            }

            private inner class SBullet(
                iX: Int,
                iY: Int,
                private val degree: Double,
                private val speed: Double = 2.46,
            ) :
                AbstractBullet(iX, iY) {

                override fun move() {
                    val oldX = xD()
                    val oldY = yD()
                    val speed = this.speed + bulletCount / 786
                    setX(oldX + (speed * cos(degree)))
                    setY(oldY + (speed * sin(degree)))
                }

                override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 1)

                override fun paint(g: Graphics2D) {
                    val t = GFX.getTexture("test_bullet")
                    val x = x()
                    val y = y()
                    val p = t.renderPoint(x, y)
                    t.paint(g, t.normalMatrix(), p.x, p.y)
                }
            }

            private fun bullet() {
                if (bulletCount % 30 == 0)
                    AudioPlayer.addTask(Sounds.getAudio("enemy_shoot"))
                val fx = -PI / 360 * bulletCount + PI / 2
                ang += fx
                ObjectPool.addBullet(
                    SBullet(
                        256,
                        256,
                        ang
                    )
                )
                bulletCount++
            }
        }
    }
}