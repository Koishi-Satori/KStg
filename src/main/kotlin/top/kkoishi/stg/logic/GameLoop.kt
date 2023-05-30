package top.kkoishi.stg.logic

class GameLoop private constructor() : Runnable {
    fun update() {
        ObjectPool.player.update()
        PlayerManager.cur.action()
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