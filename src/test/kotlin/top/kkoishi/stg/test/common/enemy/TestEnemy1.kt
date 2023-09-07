package top.kkoishi.stg.test.common.enemy

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.common.bullets.AbstractBullet
import top.kkoishi.stg.common.entities.Enemy
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.common.bullets.PlayerBullet
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.test.common.GameSystem
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.absoluteValue

class TestEnemy1(
    initialX: Int,
    initialY: Int,
) : Enemy(200) {
    private var x = AtomicInteger(initialX)
    private var y = AtomicInteger(initialY)
    private var which: Int = 0
    private var speed: Int = 3
    private var count: Long = 0L

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
        if (count++ % 45 == 0L) {
            ObjectPool.addBullet(TestBullet(x.get(), y.get()))
            ObjectPool.addBullet(TestBullet(x.get(), y.get() - 4))
            ObjectPool.addBullet(TestBullet(x.get(), y.get() - 8))
            ObjectPool.addBullet(TestBullet(x.get(), y.get() - 12))
            ObjectPool.addBullet(TestBullet(x.get(), y.get() - 16))
        }
    }

    class TestBullet(
        iX: Int,
        iY: Int,
        private val speed: Double = 3.1,
    ) : AbstractBullet(iX, iY) {
        override fun move() {
            val oldY = yD()
            setY(oldY + speed)
        }

        override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 1)

        override fun paint(g: Graphics2D) {
            val t = GFX.getTexture("test_bullet")
            val x = xD()
            val y = yD()
            val p = t.renderPoint(x, y, PI)
            t.paint(g, t.rotate(PI), p.x, p.y)
        }
    }
}