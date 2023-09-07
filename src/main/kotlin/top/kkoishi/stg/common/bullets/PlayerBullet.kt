package top.kkoishi.stg.common.bullets

import top.kkoishi.stg.common.entities.*
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.logic.ObjectPool

abstract class PlayerBullet(initialX: Int, initialY: Int) : AbstractBullet(initialX, initialY) {
    final override fun collideTest(): Boolean {
        for (e in ObjectPool.objects()) {
            if (e is Enemy)
                if (collide(e))
                    return true
        }
        return false
    }

    override fun collide(o: Object): Boolean {
        if (o is Entity) {
            if (CollideSystem.collide(o, this)) {
                this.erase()
                o.beingHit(this)
                return true
            }
        }
        return false
    }

    override fun from(): Player = ObjectPool.player()
}

