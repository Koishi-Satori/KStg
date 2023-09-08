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

    override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 20)

    override fun paint(g: Graphics2D) {
        val t = GFX.getTexture("bullet_bg_ball_red")
        val x = xD()
        val y = yD()
        val p = t.renderPoint(x, y, PI)
        t.paint(g, t.rotate(PI), p.x, p.y)
    }
}