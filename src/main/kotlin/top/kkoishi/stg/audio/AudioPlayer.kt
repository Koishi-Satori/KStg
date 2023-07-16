package top.kkoishi.stg.audio

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.Threads
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

class AudioPlayer private constructor() : Runnable {
    private val lock = Any()
    private val rest: ArrayDeque<String> = ArrayDeque(64)
    private val clips: HashMap<String, Clip> = HashMap(64)
    private val backgroud: Clip = AudioSystem.getClip()

    override fun run() {
        val logger = AudioPlayer::class.logger()
        synchronized(lock) {
            //println("begin audio stage")
            var key: String
            while (rest.isNotEmpty()) {
                key = rest.removeFirst()
                val audio = Sounds.getAudio(key)
                var clip = clips[key]
                if (clip == null) {
                    clip = AudioSystem.getClip()
                    clip.open(audio.stream())
                    logger.log(System.Logger.Level.INFO, "create new clip: $audio -> $clip")
                    clips[key] = clip
                }
                clip!!.loop(1)
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

    private fun add(key: String) {
        synchronized(lock) {
            rest.add(key)
        }
    }

    companion object {
        private val instance = AudioPlayer()

        fun start(threads: Threads) {
            threads.schedule(instance, Threads.period(), 2L)
        }

        fun setBackground(background: Audio) {
            val logger = Companion::class.logger()
            instance.backgroud.stop()
            logger.log(System.Logger.Level.INFO, "Stop current background.")

            logger.log(System.Logger.Level.INFO, "Opening new background audio...")
            try {
                instance.backgroud.open(background.stream())
                instance.backgroud.loop(Clip.LOOP_CONTINUOUSLY)
            } catch (e: Exception) {
                logger.log(System.Logger.Level.ERROR, e)
            }
            logger.log(System.Logger.Level.INFO, "Set the background to $background")
        }

        fun addTask(audio: String) {
            instance.add(audio)
        }

        fun clearTask() {
            instance.clear()
        }
    }
}