package top.kkoishi.stg.logic

import java.util.concurrent.atomic.AtomicLong

class GameLoop private constructor() : Runnable {
    private val logicFrame = AtomicLong(0L)

    fun update() {
        //println("begin logic stage")
        val gameState = GenericFlags.gameState.get()
        //println("GAME STATE: $gameState")
        if (gameState == GenericFlags.STATE_PLAYING) {
            //println("updating player state")
            ObjectPool.player.update()

            var cur = PlayerManager.cur
            if (cur.toNextStage()) {
                cur = cur.nextStage()
                PlayerManager.cur = cur
            }
            cur.action()

            var index = 0
            for (o in ObjectPool.objects()) {
                if (o.update()) {
                    ObjectPool.removeObject(index--)
                    println("remove $o")
                }
                ++index
            }

            index = 0
            for (b in ObjectPool.bullets()) {
                if (b.update())
                    ObjectPool.removeBullet(index--)
                ++index
            }
        } else if (gameState == GenericFlags.STATE_PAUSE) {
            // menu logic
        }
        //println("end logic stage")
        logicFrame.incrementAndGet()
    }

    override fun run() {
        update()
    }

    companion object {
        private val instance = GameLoop()

        @JvmStatic
        fun start(threads: Threads) {
            threads.schedule(instance, Threads.period(), 1L)
        }

        fun logicFrame() = instance.logicFrame.get()
    }
}