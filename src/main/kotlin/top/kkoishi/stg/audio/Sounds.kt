package top.kkoishi.stg.audio

import top.kkoishi.stg.Resources
import top.kkoishi.stg.Resources.Companion.KEY_NOT_FOUND
import top.kkoishi.stg.Resources.Companion.getEngineResources
import top.kkoishi.stg.exceptions.CrashReportGenerator
import top.kkoishi.stg.exceptions.CrashReporter
import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess

object Sounds: Resources<Audio, String> {
    @JvmStatic
    val NOT_FOUND: Audio

    private val audios: MutableMap<String, Audio> = HashMap(1024)

    init {
        loadAudio(KEY_NOT_FOUND, getEngineResources<InputStream>()!!)
        NOT_FOUND = audios[KEY_NOT_FOUND] ?: crash()
    }

    private fun crash(): Audio {
        val generator = CrashReportGenerator()
        generator.description("Lack of engine resources")
        CrashReporter().report(generator.generate(FailedLoadingResourceException("Lack of engine resources")))
        exitProcess(1)
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
            audios[key] = Audio.Common(p)
        } catch (e: Exception) {
            throw FailedLoadingResourceException(e)
        }
    }

    @Suppress("SameParameterValue")
    private fun loadAudio(key: String, ins: InputStream) {
        try {
            audios[key] = Audio.audio(ins)
        } catch (e: Exception) {
            throw FailedLoadingResourceException(e)
        }
    }

    override fun get(key: String): Audio = getAudio(key)
    override fun keys(): Array<String> = audios.keys.toTypedArray()

    override fun set(key: String, value: String) = loadAudio(key, value)
}