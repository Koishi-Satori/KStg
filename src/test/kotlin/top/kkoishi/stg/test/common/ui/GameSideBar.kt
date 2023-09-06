package top.kkoishi.stg.test.common.ui

import top.kkoishi.stg.common.ui.SideBar
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.GFX.renderTypedNumber
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.GenericSystem
import java.awt.Color
import java.awt.Graphics2D
import java.util.concurrent.atomic.AtomicLong

class GameSideBar : SideBar(0, 0) {
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
        infoX = size.width.toInt() - (insets.left + insets.right) + 45
        infoY += 40
        texture = GFX.getTexture("ui_score")
        texture.paint(r, texture.normalMatrix(), infoX, infoY)
        infoX += texture.width
        infoY += 3
        r.color = Color.WHITE
        r.renderTypedNumber(score.get(), infoX, infoY, "score_font", 9)
    }

    override fun updateInfo() {
        if (!GenericSystem.inDialog)
            score.getAndAdd(10)
    }
}