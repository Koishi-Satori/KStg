package top.kkoishi.stg.common.bullets

import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.util.Mth
import java.awt.Graphics2D
import java.awt.Shape

object Bullets {
    fun createSniper(initialX: Int, initialY: Int, speed: Float, gfx: String, rotated: Boolean = false): Bullet {
        if (rotated) {
            return object : AbstractBullet(initialX, initialY) {
                val texture = GFX.getTexture(gfx)
                val vx: Double
                val vy: Double
                val delta: Double

                init {
                    val x = x()
                    val y = y()
                    val dx = ObjectPool.player().y() - y
                    val dy = ObjectPool.player().x() - x
                    val scale = Mth.sqrt(dx * dx + dy * dy)
                    val sin = dy / scale
                    val cos = dx / scale
                    delta = Mth.asin(sin)
                    vx = speed * sin
                    vy = speed * cos
                }

                override fun move() {
                    val xBefore = x()
                    val yBefore = y()
                    setX(xBefore + vx.toInt())
                    setY(yBefore + vy.toInt())
                }

                override fun collide(o: Object): Boolean {
                    TODO("Not yet implemented")
                }

                override fun shape(): Shape {
                    TODO("Not yet implemented")
                }

                override fun paint(g: Graphics2D) {
                    val x = xD()
                    val y = yD()
                    val p = texture.renderPoint(x, y, delta)
                    texture.paint(g, texture.rotate(delta), p.x, p.y)
                }
            }
        }

        return object : AbstractBullet(initialX, initialY) {
            val texture = GFX.getTexture(gfx)
            val sin: Double
            val cos: Double
            val vx: Double
            val vy: Double

            init {
                val x = x()
                val y = y()
                val dx = (ObjectPool.player().y() - y)
                val dy = (ObjectPool.player().x() - x)
                val scale = Mth.sqrt(dx * dx + dy * dy)
                sin = dy / scale
                cos = dx / scale
                vx = speed * sin
                vy = speed * cos
            }

            override fun move() {
                val xBefore = x()
                val yBefore = y()
                setX(xBefore + vx.toInt())
                setY(yBefore + vy.toInt())
            }

            override fun collide(o: Object): Boolean {
                TODO("Not yet implemented")
            }

            override fun shape(): Shape {
                TODO("Not yet implemented")
            }

            override fun paint(g: Graphics2D) {
                val x = xD()
                val y = yD()
                val p = texture.renderPoint(x, y)
                texture.paint(g, texture.normalMatrix(), p.x, p.y)
            }
        }
    }

    fun createLine(
        initialX: Int,
        initialY: Int,
        endX: Int,
        endY: Int,
        speed: Float,
        gfx: String,
        rotated: Boolean = false,
    ) {

    }
}