package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.Graphics
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.logic.PlayerManager
import top.kkoishi.stg.logic.Threads

class Renderer : Runnable {
    fun paint() {
        ObjectPool.lock()
        val r = Graphics.render()

        ObjectPool.player.paint(r)
        PlayerManager.cur.paint(r)
        for (o in ObjectPool.objects())
            o.paint(r)
        for (b in ObjectPool.bullets())
            b.paint(r)

        ObjectPool.release()
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