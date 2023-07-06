package top.kkoishi.stg.logic

class GameLoop private constructor() : Runnable {
    fun update() {
        val gameState = GenericFlags.gameState.get()
        if (gameState == GenericFlags.STATE_PLAYING) {
            ObjectPool.player.update()

            var cur = PlayerManager.cur
            if (cur.toNextStage()) {
                cur = cur.nextStage()
                PlayerManager.cur = cur
            }
            cur.action()

            var index = 0
            for (o in ObjectPool.objects()) {
                if (o.update())
                    ObjectPool.removeObject(index--)
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
    }
}