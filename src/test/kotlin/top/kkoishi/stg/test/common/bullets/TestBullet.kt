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

import top.kkoishi.stg.common.bullets.AbstractBullet
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.GFX
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import kotlin.math.PI

class TestBullet(
    iX: Int,
    iY: Int,
    private val speed: Double = 3.5,
) : AbstractBullet(iX, iY) {
    override fun move() {
        val oldY = yD()
        setY(oldY + speed)
    }

    override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 16)

    override fun paint(g: Graphics2D) {
        val t = GFX.getTexture("bullet_bg_ball_red")
        val x = xD()
        val y = yD()
        val p = t.renderPoint(x, y)
        t.paint(g, t.averageConvolve33(0.9f), p.x, p.y)
    }
}