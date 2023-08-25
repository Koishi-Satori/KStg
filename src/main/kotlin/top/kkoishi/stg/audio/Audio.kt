package top.kkoishi.stg.audio

import java.io.InputStream
import java.nio.file.Path
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

sealed class Audio(private val path: Path) {
    open fun stream(): AudioInputStream {
        return AudioSystem.getAudioInputStream(path.toFile())
    }

    class Common(path: Path): Audio(path)
    private class Special(val ins: InputStream): Audio(Path.of("SOUND_NOT_FOUND.wav")) {
        override fun stream(): AudioInputStream {
            return AudioSystem.getAudioInputStream(ins)
        }
    }

    internal companion object `  Audio  ` {
        @JvmStatic
        @JvmName("  audio  ")
        internal fun audio(ins: InputStream): Audio = Special(ins)
    }
}