package top.kkoishi.stg.test

import top.kkoishi.stg.gfx.Texture
import java.awt.image.BufferedImage

fun main() {
    val testTexture = Texture(BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB), "test")
    println(testTexture.gaussianBlurConvolve(1.5f, 3))
    println(testTexture.gaussianBlurConvolve(1.5f, 7))
    println(testTexture.alphaConvolve(0.5f, 5))
}