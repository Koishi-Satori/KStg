package top.kkoishi.stg.audio

import top.kkoishi.stg.logic.Threads
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

class AudioPlayer private constructor() : Runnable {
    private val lock = Any()
    private val rest: ArrayDeque<Audio> = ArrayDeque(64)
    private val clips: ArrayDeque<Clip> = ArrayDeque(64)
    private val backgroud: Clip = AudioSystem.getClip()

    override fun run() {
        synchronized(lock) {
            //println("begin audio stage")
            var audio: Audio
            while (rest.isNotEmpty()) {
                audio = rest.removeFirst()
                val clip = AudioSystem.getClip()
                clip.open(audio.stream())
                clip.start()
                clips.addLast(clip)
            }

            var cur = 0
            val copy = clips.toTypedArray()
            for (clip in copy) {
                if (!clip.isRunning) {
                    clip.close()
                    clips.removeAt(cur--)
                }
                ++cur
            }
            //println("end audio stage")
        }
    }

    private fun clear() {
        synchronized(lock) {
            rest.clear()
            clips.forEach(Clip::close)
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