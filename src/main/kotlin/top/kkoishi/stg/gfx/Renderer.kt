package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.*

class Renderer : Runnable {
    fun paint() {
        val gameState = GenericFlags.gameState.get()
        if (gameState == GenericFlags.STATE_PLAYING) {
            ObjectPool.lock()
            val r = Graphics.render()

            ObjectPool.player.paint(r)
            PlayerManager.cur.paint(r)
            for (o in ObjectPool.objects())
                o.paint(r)
            for (b in ObjectPool.bullets())
                b.paint(r)

            ObjectPool.release()
        } else if (gameState == GenericFlags.STATE_PAUSE) {
            // render menu
        }
    }

    override fun run() {
        paint()
    }

    companion object {
        private val instance = Renderer()

        @JvmStatic
        fun start(threads: Threads) {
            threads.schedule(instance, Threads.period())
        }
    }
}