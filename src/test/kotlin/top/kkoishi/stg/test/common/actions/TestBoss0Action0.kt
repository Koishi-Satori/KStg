package top.kkoishi.stg.test.common.actions

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.common.BossAction
import top.kkoishi.stg.common.entities.AbstractBullet
import top.kkoishi.stg.common.entities.Boss
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.ObjectPool
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class TestBoss0Action0(health: Int, frames: Long) : BossAction(health, frames) {
    private var bulletCount = 0
    private var ang = 0.0

    override fun action(boss: Boss) {
        if (bulletCount % 30 == 0)
            AudioPlayer.addTask("enemy_shoot")
        if (bulletCount % 360 == 0)
            bulletCount = 0
        if (bulletCount % 2 == 0) {
            val fx = -PI / 720 * bulletCount / 2 + PI / 2
            ang += fx
            ObjectPool.addBullet(
                BossBullet(
                    boss.x().toInt(),
                    boss.y().toInt() + 20,
                    ang % PI
                )
            )
            ObjectPool.addBullet(
                BossBullet(
                    boss.x().toInt(),
                    boss.y().toInt() - 20,
                    ang % PI + PI
                )
            )
        }

        bulletCount++
    }

    class BossBullet(
        iX: Int,
        iY: Int,
        val degree: Double,
        private val speed: Double = 1.76,
    ) : AbstractBullet(iX, iY) {
        private val sin = sin(degree)
        private val cos = cos(degree)
        private var lifeTime = 0
        override fun move() {
            val oldX = xD()
            val oldY = yD()
            val speed = this.speed + lifeTime / 678
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