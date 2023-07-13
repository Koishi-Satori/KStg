package top.kkoishi.stg.audio

import java.nio.file.Path
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

data class Audio(private val path: Path) {
    fun stream(): AudioInputStream {
        return AudioSystem.getAudioInputStream(path.toFile())
    }
}