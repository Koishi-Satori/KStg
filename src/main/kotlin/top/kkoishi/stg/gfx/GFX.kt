package top.kkoishi.stg.gfx

import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.Throws
import kotlin.io.path.exists
import kotlin.io.path.inputStream

object GFX {
    private val logger = GFX::class.logger()
    @JvmStatic
    private var NOT_FOUND: Texture

    private val textures: MutableMap<String, Texture> = HashMap(1024)

    init {
        loadTexture("NOT_FOUND", "./resources/TEXTURE_NOT_FOUND.png")
        NOT_FOUND = textures["NOT_FOUND"] ?: throw FailedLoadingResourceException("Lack of engine resources")
    }

    @JvmOverloads
    @Throws(FailedLoadingResourceException::class)
    fun cutTexture(key: String, nKey: String, x: Int, y: Int, w: Int, h: Int, rotate: Double = 0.0) {
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