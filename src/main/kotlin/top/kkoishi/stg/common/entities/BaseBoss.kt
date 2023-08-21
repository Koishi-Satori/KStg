package top.kkoishi.stg.common.entities

import top.kkoishi.stg.common.BossAction

abstract class BaseBoss(initialX: Int, initialY: Int, firstAction: BossAction) :
    Boss(firstAction.health, initialX, initialY) {
    protected val actions: ArrayDeque<BossAction> = ArrayDeque(8)
    private var cur: BossAction = firstAction
    val lock = Any()

    fun addAction(action: BossAction) {
        synchronized(lock) {
            actions.addLast(action)
        }
    }

    override fun end(): Boolean = synchronized(lock) {
        return actions.isEmpty() && cur.frames-- <= 0
    }

    override fun action() {
        synchronized(lock) {
            if (cur.frames >= 0)
                cur.action(this)
            if (cur.frames <= 0 && actions.isNotEmpty()) {
                cur = actions.removeFirst()
                health = cur.health
            }
        }
    }

    override fun afterDead() {
        synchronized(lock) {
            if (actions.isNotEmpty()) {
                cur = actions.removeFirst()
                health = cur.health
            } else
                cur.frames = -114514
        }
    }

    override fun isDead(): Boolean = synchronized(lock) {
        return super.isDead()
    }
}