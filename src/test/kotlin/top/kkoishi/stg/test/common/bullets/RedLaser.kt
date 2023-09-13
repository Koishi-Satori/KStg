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

package top.kkoishi.stg.test.common.bullets

import top.kkoishi.stg.common.bullets.Laser2unType
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Texture
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Point2D
import java.lang.StrictMath.PI

class RedLaser(initialX: Int, initialY: Int, val speed: Double) : Laser2unType(initialX, initialY) {
    init {
        addBullet(Head())
        addBullet(Body())
        addBullet(Body())
        addBullet(Body())
        addBullet(Body())
        addBullet(Body())
        addBullet(Tail())
    }

    abstract inner class LaserSub : SubBullet() {
        abstract val texture: Texture

        override fun move() {
            val oldY = yD()
            setY(oldY + speed)
        }

        override fun paint(g: Graphics2D) {
            val x = xD()
            val y = yD()
            val rd = texture.renderPoint(x, y, -PI / 2)
            texture.paint(g, texture.rotate(PI / 2), rd.x, rd.y)
        }

        override fun calculatePos(laserHeadX: Double, laserHeadY: Double): Point2D {
            val dy = this@RedLaser.count() * 20
            return Point2D.Double(laserHeadX, dy + laserHeadY)
        }
    }

    inner class Head : LaserSub() {
        override val texture = GFX["laser_red_0"]

        override fun shape(): Shape = CollideSystem.createRectangle(xD(), yD(), 21.0, 10.0)
    }

    inner class Tail : LaserSub() {
        override val texture = GFX["laser_red_0"]

        override fun shape(): Shape = CollideSystem.createRectangle(xD(), yD(), 21.0, 10.0)

        override fun paint(g: Graphics2D) {
            val x = xD()
            val y = yD()
            val rd = texture.renderPoint(x, y, PI / 2)
            texture.paint(g, texture.rotate(-PI / 2), rd.x, rd.y)
        }
    }

    inner class Body : LaserSub() {
        override val texture = GFX["laser_red_1"]

        override fun shape(): Shape = CollideSystem.createRectangle(xD(), yD(), 21.0, 10.0)
    }
}