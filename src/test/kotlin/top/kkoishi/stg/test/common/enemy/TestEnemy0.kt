package top.kkoishi.stg.test.common.enemy

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.common.entities.AbstractBullet
import top.kkoishi.stg.common.entities.Enemy
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.common.entities.PlayerBullet
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.test.common.GameSystem
import top.kkoishi.stg.util.Mth.cos
import top.kkoishi.stg.util.Mth.sin
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.absoluteValue

class TestEnemy0(
    initialX: Int,
    initialY: Int,
    health: Int,
    private val texture: String,
    val moveFunc: (AtomicInteger, AtomicInteger) -> Unit,
) : Enemy(health) {
    private var x = AtomicInteger(initialX)
    private var y = AtomicInteger(initialY)
    private var bulletCount = 0
    private var ang = 0.0

    override fun dead() {
        AudioPlayer.addTask("enemy_dead")
        ObjectPool.player().setPlayerPower(2.3f)
        GameSystem.sideBar.add(10000L)
    }

    override fun beingHit(o: Object) {
        this.health -= ObjectPool.player().bulletDamage()
        AudioPlayer.addTask("th15_enemy_damage_0${(GameSystem.rand.nextInt() % 2).absoluteValue + 1}")
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

    private inner class SBullet(
        iX: Int,
        iY: Int,
        val degree: Double,
        private val speed: Double = 2.46,
    ) :
        AbstractBullet(iX, iY) {

        private val sin = sin(degree)
        private val cos = cos(degree)

        private var lifeTime = 0
        override fun move() {
            val oldX = xD()
            val oldY = yD()
            val speed = this.speed + lifeTime++ / 786
            setX(oldX + (speed * sin))
            setY(oldY - (speed * cos))
        }

        override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 1)

        override fun paint(g: Graphics2D) {
            val t = GFX.getTexture("test_bullet")
            val x = xD()
            val y = yD()
            val p = t.renderPoint(x, y, degree)
            t.paint(g, t.rotate(degree), p.x, p.y)
        }
    }

    private fun bullet() {
        if (bulletCount % 30 == 0)
            AudioPlayer.addTask("enemy_shoot")
        if (bulletCount % 360 == 0)
            bulletCount = 0
        val fx = -PI / 360 * bulletCount + PI / 2
        ang += fx
        ObjectPool.addBullet(
            SBullet(
                this.x.get(),
                this.y.get(),
                ang % (2 * PI)
            )
        )
        bulletCount++
    }
}