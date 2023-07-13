package top.kkoishi.stg.gfx

import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.Throws
import kotlin.io.path.exists
import kotlin.io.path.inputStream

object GFX {
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
        val texture = getTexture(key)
        try {
            textures[nKey] = texture.cut(x, y, w, h)
        } catch (e: Exception) {
            throw FailedLoadingResourceException(e)
        }
    }

    fun getTexture(key: String): Texture {
        return textures[key] ?: NOT_FOUND
    }

    @Throws(FailedLoadingResourceException::class)
    fun loadTexture(key: String, path: String) {
        try {
            val p = Path.of(path)
            val ins = ImageIO.createImageInputStream(seekTexture(p))
            val img = ImageIO.read(ins)
            textures[key] = Texture(img)
        } catch (e: IOException) {
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