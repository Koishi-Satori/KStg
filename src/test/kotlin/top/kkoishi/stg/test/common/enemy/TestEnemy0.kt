/*
 *
 * This file is only used for testing, and there is no need for you to add this
 * module "KStg.test" when build this engine.
 *
 *
 * Some resources for art and sound in this test module are from a touhou STG game
 * which called "东方夏夜祭", for the author is in lack of synthesizing music and
 * game painting. :(
 *
 *
 *
 */

package top.kkoishi.stg.test.common.enemy

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.common.bullets.AbstractBullet
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.common.entities.BaseEnemy
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.test.common.GameSystem
import top.kkoishi.stg.test.common.items.PowerItem
import top.kkoishi.stg.util.Mth.cos
import top.kkoishi.stg.util.Mth.sin
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.awt.geom.Point2D
import kotlin.math.PI
import kotlin.math.absoluteValue

class TestEnemy0(
    initialX: Int,
    initialY: Int,
    health: Int,
    private val texture: String,
    val moveFunc: (Double, Double) -> Point2D,
) : BaseEnemy(health, initialX, initialY) {
    override fun texture(): String = texture

    override fun paintOthers(r: Graphics2D) {}

    private var bulletCount = 0
    private var ang = 0.0

    override fun dead() {
        AudioPlayer.addTask("enemy_dead")
        ObjectPool.addObject(PowerItem(xInt() - 10, yInt() - 20))
        ObjectPool.addObject(PowerItem(xInt(), yInt()))
        GameSystem.sideBar.add(10000L)
    }

    override fun beingHit(o: Object) {
        super.beingHit(o)
        AudioPlayer.addTask("th15_enemy_damage_0${(GameSystem.rand.nextInt() % 2).absoluteValue + 1}")
    }

    override fun move() {
        val newPos = moveFunc(x(), y())
        x(newPos.x)
        y(newPos.y)
    }

    override fun shape(): Shape = CollideSystem.Circle(Point(xInt(), yInt()), 16)

    override fun bullet() {
        if (bulletCount % 30 == 0)
            AudioPlayer.addTask("enemy_shoot")
        if (bulletCount % 360 == 0)
            bulletCount = 0
        val fx = -PI / 360 * bulletCount + PI / 2
        ang += fx
        ObjectPool.addBullet(
            SBullet(
                this.xInt(),
                this.yInt(),
                ang % (2 * PI)
            )
        )
        bulletCount++
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
}