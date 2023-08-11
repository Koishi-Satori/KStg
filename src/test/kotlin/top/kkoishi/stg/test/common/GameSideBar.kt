package top.kkoishi.stg.test.common

import top.kkoishi.stg.common.ui.SideBar
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Graphics
import java.awt.Color
import java.awt.Graphics2D
import java.util.concurrent.atomic.AtomicLong

class GameSideBar: SideBar(0, 0) {
    private val score: AtomicLong = AtomicLong(0)

    fun reset() {
        score.set(0)
    }

    fun add(s: Long) {
        score.addAndGet(s)
    }

    override fun background(): String = "ui_sidebar"

    override fun paintInfo(r: Graphics2D) {
        var texture = GFX.getTexture("ui_diff_easy")
        var infoX: Int
        var infoY = 5
        val insets = Graphics.getUIInsets()
        val size = Graphics.getScreenSize()
        infoX = size.width.toInt() - (insets.left + insets.right / 2)
        infoY += insets.top
        texture.paint(r, texture.normalMatrix(), infoX, infoY)
        infoX = size.width.toInt() - (insets.left + insets.right) + 50
        infoY += 40
        texture = GFX.getTexture("ui_score")
        texture.paint(r, texture.normalMatrix(), infoX, infoY)
        infoX += texture.width + 10
        infoY += 18
        r.color = Color.WHITE
        r.font = Graphics.font("sidebar")
        r.drawString(getInfo(score.get()), infoX, infoY)
    }

    private fun getInfo(num: Long): String {
        val sb = StringBuilder()
        sb.append(num)
        val len = sb.length
        if (len < 8) {
            sb.clear()
            (0 until (10 - len)).forEach { _ -> sb.append('0') }
            sb.append(num)
        }
        return sb.toString()
    }

    override fun updateInfo() {
        score.getAndAdd(10)
    }
}