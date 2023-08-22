package top.kkoishi.stg.audio

import top.kkoishi.stg.exceptions.CrashReportGenerator
import top.kkoishi.stg.exceptions.CrashReporter
import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.logic.Threads
import java.nio.file.Path
import kotlin.io.path.exists

object Sounds {
    const val KEY_NOT_FOUND = "NOT_FOUND"

    @JvmStatic
    val NOT_FOUND: Audio

    private val audios: MutableMap<String, Audio> = HashMap(1024)

    init {
        loadAudio(KEY_NOT_FOUND, "${Threads.workdir()}/resources/SOUND_NOT_FOUND.wav")
        NOT_FOUND = audios[KEY_NOT_FOUND] ?: crash()
    }

    private fun crash(): Audio {
        val generator = CrashReportGenerator()
        generator.description("Lack of engine resources")
        CrashReporter().report(generator.generate(FailedLoadingResourceException("Lack of engine resources")))
        throw FailedLoadingResourceException("Lack of engine resources")
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