package top.kkoishi.stg.gfx

import java.awt.Graphics2D
import java.awt.image.BufferedImage

class TexturedFont(
    texture: BufferedImage,
    private val basicWidth: Int,
    private val basicHeight: Int,
    private val find: (Char) -> Pair<Int, Int>,
) : Texture(texture) {
    private val remembered = HashMap<Char, BufferedImage>()
    private fun render(r: Graphics2D, c: Char, x: Int, y: Int) {
        var t = remembered[c]
        if (t == null) {
            val pos = find(c)
            t = texture.getSubimage(pos.first * basicWidth, pos.second * basicHeight, basicWidth, basicHeight)
            r.drawImage(t, NORMAL_MATRIX, x, y)
            remembered[c] = t
        } else
            r.drawImage(t, NORMAL_MATRIX, x, y)
    }

    fun render(r: Graphics2D, str: String, x: Int, y: Int) {
        str.forEachIndexed { i, c ->
            render(r, c, x + basicWidth * i, y)
        }
    }

    fun render(r: Graphics2D, number: Number, x: Int, y: Int) {
        render(r, number.toString(), x, y)
    }
}