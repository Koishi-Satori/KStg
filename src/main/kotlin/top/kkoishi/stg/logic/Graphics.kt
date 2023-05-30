package top.kkoishi.stg.logic

import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.Dimension2D
import javax.swing.JFrame
import kotlin.properties.Delegates

object Graphics {
    private lateinit var render: Graphics2D

    private lateinit var SCREEN: Dimension2D

    private var baseY by Delegates.notNull<Double>()

    private val CENTER = Point()

    fun refresh(f: JFrame) {
        TODO()
    }

    fun setRender(r: Graphics2D) {
        render = r
    }

    fun render() = render

    fun setScreenSize(d: Dimension2D) {
        SCREEN = d
        val xc = d.width / 2
        val yc = d.height / 2
        CENTER.setLocation(xc, yc)
    }

    fun getScreenSize() = SCREEN

    fun getCenter() = CENTER

    fun getCenterX() = CENTER.x

    fun getCenterY() = CENTER.y

    fun setFrameDy(dy: Double) {
        baseY = dy
    }

    fun getFrameDy() = baseY
}