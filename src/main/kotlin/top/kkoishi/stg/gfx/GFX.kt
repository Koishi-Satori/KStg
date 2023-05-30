package top.kkoishi.stg.gfx

import top.kkoishi.stg.exceptions.FailedLoadingTextureException
import java.awt.Point
import java.awt.geom.Dimension2D
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.jvm.Throws

object GFX {
    @JvmStatic
    private var NOT_FOUND: Texture

    private val textures: MutableMap<String, Texture> = HashMap(1024)

    init {
        loadTexture("NOT_FOUND", "./resources")
        NOT_FOUND = textures["NOT_FOUND"] ?: throw FailedLoadingTextureException("Lack of engine resources")
    }

    fun getTexture(key: String): Texture {
        return textures[key] ?: NOT_FOUND
    }

    @Throws(FailedLoadingTextureException::class)
    fun loadTexture(key: String, path: String) {
        try {
            val p = Path.of(path)
            val ins = ImageIO.createImageInputStream(seekTexture(p))
            val img = ImageIO.read(ins)
            textures[key] = Texture(img)
        } catch (e: IOException) {
            throw FailedLoadingTextureException(e)
        }
    }

    @Throws(FailedLoadingTextureException::class)
    private fun seekTexture(path: Path): InputStream {
        if (!path.exists())
            throw FailedLoadingTextureException("Can not find texture: $path")
        return path.inputStream()
    }
}