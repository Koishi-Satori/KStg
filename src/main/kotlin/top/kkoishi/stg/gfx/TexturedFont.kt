package top.kkoishi.stg.gfx

import java.awt.Graphics2D
import java.awt.image.BufferedImage

class TexturedFont(
    texture: BufferedImage,
    private val basicWidth: Int,
    private val basicHeight: Int,
    private val find: (Char) -> Pair<Int, Int>,
) : Texture(texture) {
    private fun render(r: Graphics2D, c: Char) {

    }

    fun render(r: Graphics2D, str: String) {
        str.forEach { render(r, it) }
    }

    fun render(r: Graphics2D, number: Number) {
        render(r, number.toString())
    }
}