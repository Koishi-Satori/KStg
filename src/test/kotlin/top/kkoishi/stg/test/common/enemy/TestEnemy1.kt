package top.kkoishi.stg.test.common.enemy

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.common.entities.Enemy
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.common.bullets.PlayerBullet
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.test.common.GameSystem
import top.kkoishi.stg.test.common.bullets.RedLaser
import top.kkoishi.stg.test.common.bullets.TestBullet
import top.kkoishi.stg.util.Timer
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue

class TestEnemy1(
    initialX: Int,
    initialY: Int,
) : Enemy(200) {
    private var x = AtomicInteger(initialX)
    private var y = AtomicInteger(initialY)
    private var which: Int = 0
    private var speed: Int = 3
    private val shotTimer = Timer.Loop(45)

    private fun texture(): String {
        if (which > 70)
            which = 0
        return "ghost_fire_${(which++) / 10}"
    }

    override fun dead() {
        AudioPlayer.addTask("enemy_dead")
        ObjectPool.player().setPlayerPower(4.0f)
        GameSystem.sideBar.add(10000L)
    }

    override fun beingHit(o: Object) {
        this.health -= ObjectPool.player().bulletDamage()
        AudioPlayer.addTask("th15_enemy_damage_0${(GameSystem.rand.nextInt() % 2).absoluteValue + 1}")
    }

    override fun move() {
        val newX = x.addAndGet(speed)
        if (newX > 400 || newX < 80) {
            speed *= -1
        }
    }

    override fun shape(): Shape = CollideSystem.Circle(Point(x.get(), y.get()), 8)

    override fun collide(o: Object): Boolean {
        if (o is PlayerBullet && CollideSystem.collide(this, o)) {
            beingHit(o)
            return true
        }
        return false
    }

    override fun paint(g: Graphics2D) {
        val t = GFX.getTexture(texture())
        val rd = t.renderPoint(x.get(), y.get())
        t.paint(g, t.normalMatrix(), rd.x, rd.y)
        //g.draw(shape().bounds)
    }

    override fun update(): Boolean {
        val dead = super.update()
        if (!dead) {
            move()
            bullet()
        }
        return dead
    }

    private fun bullet() {
        if (shotTimer.end()) {
            //ObjectPool.addBullet(TestBullet(x.get(), y.get()))
            ObjectPool.addBullet(RedLaser(x.get() + 20, y.get(), 2.0))
        }
    }
}