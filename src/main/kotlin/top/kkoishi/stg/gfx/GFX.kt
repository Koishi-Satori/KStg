package top.kkoishi.stg.gfx

import top.kkoishi.stg.Resources
import top.kkoishi.stg.Resources.Companion.KEY_NOT_FOUND
import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.awt.Color
import java.awt.Graphics2D
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.exists
import kotlin.io.path.inputStream

/**
 * Used to load and get the textures.
 *
 * @author KKoishi_
 */
@Suppress("unused")
object GFX : Resources<Texture, String> {
    private val logger = GFX::class.logger()

    @JvmStatic
    private var NOT_FOUND: Texture

    private val textures: MutableMap<String, Texture> = HashMap(1024)

    init {
        try {
            val ins = Resources.getEngineResources()
            val imageInput = ImageIO.createImageInputStream(ins)
            textures[KEY_NOT_FOUND] = Texture(ImageIO.read(imageInput))
            ins!!.close()
        } catch (e: Exception) {
            logger.log(System.Logger.Level.TRACE, e)
        }
        NOT_FOUND = textures[KEY_NOT_FOUND] ?: tryInitNotFound()
        logger.log(System.Logger.Level.INFO, "Init TEXTURE_NOT_FOUND")
    }

    private fun tryInitNotFound(): Texture {
        val buf = Graphics.createBuffer(40, 40)
        val g = buf.createGraphics()
        g.color = Color.BLACK
        g.fillRect(0, 0, 20, 20)
        g.fillRect(20, 20, 20, 20)
        g.color = Color(163, 73, 164)
        g.fillRect(0, 20, 20, 20)
        g.fillRect(20, 0, 20, 20)
        g.dispose()
        val texture = Texture(buf)
        textures[KEY_NOT_FOUND] = texture
        return texture
    }

    /**
     * According to the given rectangular range, it will cut out another texture from one texture,
     * and the texture to be cut is given by its key; the key of the new texture is the parameter nKey.
     *
     * @param key the key of the texture which will be shea red
     * @param nKey the new key of the new texture
     * @param x the x coordinate of the shear beginning point
     * @param y the y coordinate of the shear beginning point
     * @param w the width of the shear area
     * @param h the height of the shear area
     */
    @Throws(FailedLoadingResourceException::class)
    fun shearTexture(key: String, nKey: String, x: Int, y: Int, w: Int, h: Int) {
        logger.log(System.Logger.Level.INFO, "Try to cut the texture: $key -> $nKey")
        val texture = getTexture(key)
        try {
            logger.log(System.Logger.Level.INFO, "Begin: ($x, $y), w=$w, h=$h")
            textures[nKey] = texture.cut(x, y, w, h, nKey)
        } catch (e: Exception) {
            logger.log(System.Logger.Level.WARNING, "Failed to load the texture $nKey")
            throw FailedLoadingResourceException(e)
        }
    }

    fun getTexture(key: String): Texture {
        return textures[key] ?: NOT_FOUND
    }

    /**
     * Get the texture returned when the key is not found.
     *
     * @return Not Found Texture.
     */
    fun notFound() = NOT_FOUND

    @JvmOverloads
    @Throws(FailedLoadingResourceException::class)
    fun loadTexture(key: String, path: String, useVRAM: Boolean = false) {
        logger.log(System.Logger.Level.INFO, "Try to load the texture: $path")
        try {
            val p = Path.of(path)
            val ins = ImageIO.createImageInputStream(seekTexture(p))
            val img = ImageIO.read(ins)
            if (useVRAM)
                textures[key] = Texture.Volatile(img, key)
            else
                textures[key] = Texture(img, key)
        } catch (e: IOException) {
            logger.log(System.Logger.Level.WARNING, "Failed to load the texture $key")
            throw FailedLoadingResourceException(e)
        }
    }

    @Throws(FailedLoadingResourceException::class)
    private fun seekTexture(path: Path): InputStream {
        if (!path.exists())
            throw FailedLoadingResourceException("Can not find texture: $path")
        return path.inputStream()
    }

    override fun get(key: String): Texture = getTexture(key)

    override fun set(key: String, value: String) = loadTexture(key, value)

    operator fun set(key: String, useVRAM: Boolean, path: String) = loadTexture(key, path, useVRAM)

    @JvmName("  register  ")
    internal fun register(key: String, value: Texture) {
        textures[key] = value
    }

    @JvmStatic
    fun Graphics2D.renderString(s: String, x: Int, y: Int, texturedFont: String) {
        val texture = getTexture(texturedFont)
        if (texture == NOT_FOUND || texture !is TexturedFont)
            drawString(s, x, y)
        else
            texture.render(this, s, x, y)
    }

    @JvmStatic
    fun Graphics2D.renderNumber(n: Number, x: Int, y: Int, texturedFont: String) {
        val texture = getTexture(texturedFont)
        if (texture == NOT_FOUND || texture !is TexturedFont)
            drawString(n.toString(), x, y)
        else
            texture.render(this, n, x, y)
    }

    @JvmStatic
    @JvmOverloads
    fun Graphics2D.renderTypedNumber(n: Long, x: Int, y: Int, texturedFont: String, maxLength: Int = 10) =
        renderString(getTypeString(n, maxLength), x, y, texturedFont)

    @JvmStatic
    private fun longToChars(l: Long, maxLength: Int = 10): CharArray {
        if (l < 0)
            return longToChars(-l, maxLength)
        if (l == 0L)
            return CharArray(maxLength) { '0' }

        val res = CharArray(maxLength) { '0' }
        var copy = l
        var count = 0
        while (count < maxLength) {
            res[maxLength - 1 - count++] = Char((copy % 10 + 48).toInt())
            copy /= 10
        }
        if (copy >= 1)
            return CharArray(maxLength) { '9' }
        return res
    }

    @JvmStatic
    private fun getTypeString(num: Long, maxLength: Int): String {
        val sb = StringBuilder()
        val cs = longToChars(num, maxLength)
        for ((count, it) in ((maxLength - 1) downTo 0).withIndex()) {
            if (count % 3 == 0 && count != 0)
                sb.append(',')
            sb.append(cs[it])
        }
        return sb.reverse().toString()
    }
}