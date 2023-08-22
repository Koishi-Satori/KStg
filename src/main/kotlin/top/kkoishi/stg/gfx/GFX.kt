package top.kkoishi.stg.gfx

import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.Threads
import java.awt.Color
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.Throws
import kotlin.io.path.exists
import kotlin.io.path.inputStream

/**
 * Used to load and get the textures.
 * @author KKoishi_
 */
object GFX {
    private val logger = GFX::class.logger()

    const val KEY_NOT_FOUND = "NOT_FOUND"

    @JvmStatic
    private var NOT_FOUND: Texture

    private val textures: MutableMap<String, Texture> = HashMap(1024)

    init {
        loadTexture(KEY_NOT_FOUND, "${Threads.workdir()}/resources/TEXTURE_NOT_FOUND.png")
        NOT_FOUND = textures[KEY_NOT_FOUND] ?: tryInitNotFound()
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
        return Texture(buf)
    }

    /**
     * According to the given rectangular range, cut out another texture from one texture, and the texture to be cut is
     * given by its key; the key of the new texture is the parameter nKey.
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
            textures[nKey] = texture.cut(x, y, w, h)
        } catch (e: Exception) {
            logger.log(System.Logger.Level.WARNING, "Failed to load the texture $nKey")
            throw FailedLoadingResourceException(e)
        }
    }

    fun getTexture(key: String): Texture {
        return textures[key] ?: NOT_FOUND
    }

    @Throws(FailedLoadingResourceException::class)
    fun loadTexture(key: String, path: String) {
        logger.log(System.Logger.Level.INFO, "Try to load the texture: $path")
        try {
            val p = Path.of(path)
            val ins = ImageIO.createImageInputStream(seekTexture(p))
            val img = ImageIO.read(ins)
            textures[key] = Texture(img)
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
}