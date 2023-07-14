package top.kkoishi.stg.audio

import top.kkoishi.stg.logic.Threads
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

class AudioPlayer private constructor() : Runnable {
    private val lock = Any()
    private val rest: ArrayDeque<Audio> = ArrayDeque(64)
    private val clips: HashMap<Audio, Clip> = HashMap(64)
    private val backgroud: Clip = AudioSystem.getClip()

    override fun run() {
        synchronized(lock) {
            //println("begin audio stage")
            var audio: Audio
            while (rest.isNotEmpty()) {
                audio = rest.removeFirst()
                var clip = clips[audio]
                if (clip == null) {
                    clip = AudioSystem.getClip()
                    clip.open(audio.stream())
                }
                if (!clip!!.isRunning)
                    clip.start()
            }

            //println("end audio stage")
        }
    }

    private fun clear() {
        synchronized(lock) {
            rest.clear()
            clips.forEach { (_, c) -> c.close() }
            clips.clear()
        }
    }

    private fun add(audio: Audio) {
        synchronized(lock) {
            rest.add(audio)
        }
    }

    companion object {
        private val instance = AudioPlayer()

        fun start(threads: Threads) {
            threads.schedule(instance, Threads.period(), 2L)
        }

        fun setBackground(background: Audio) {
            println("Stop current background.")
            instance.backgroud.stop()

            println("Opening new background audio...")
            instance.backgroud.open(background.stream())
            instance.backgroud.loop(Clip.LOOP_CONTINUOUSLY)
        }

        fun addTask(audio: Audio) {
            instance.add(audio)
        }

        fun clearTask() {
            instance.clear()
        }
    }
}