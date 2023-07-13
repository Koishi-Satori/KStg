package top.kkoishi.stg.audio

import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import java.nio.file.Path
import kotlin.io.path.exists

object Sounds {
    @JvmStatic
    private val NOT_FOUND: Audio

    private val audios: MutableMap<String, Audio> = HashMap(1024)

    init {
        loadAudio("NOT_FOUND", "./resources/SOUND_NOT_FOUND.wav")
        NOT_FOUND = audios["NOT_FOUND"] ?: throw FailedLoadingResourceException("Lack of engine resources")
    }

    fun getAudio(key: String): Audio {
        return audios[key] ?: NOT_FOUND
    }

    @Throws(FailedLoadingResourceException::class)
    fun loadAudio(key: String, path: String) {
        try {
            val p = Path.of(path)
            if (!p.exists())
                throw FailedLoadingResourceException("Can not find texture: $path")
            audios[key] = Audio(p)
        } catch (e: Exception) {
            throw FailedLoadingResourceException(e)
        }
    }
}