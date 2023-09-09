package top.kkoishi.stg.common.bullets

import top.kkoishi.stg.common.entities.Entity
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.logic.coordinatespace.SubChunks

/**
 * More abstracted class of the bullets, and it is easy to extend it.
 * This is more recommended than [Bullet].
 *
 * @author KKoishi_
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractBullet(initialX: Int, initialY: Int) : Bullet(initialX, initialY) {
    /**
     * Determines that whether the bullet should be erased.
     */
    protected var erased = false
    protected var collideStrategy: Int = PLAYER
    override fun update(): Boolean {
        if (!collideTest())
            move()
        return erased || CollideSystem.checkPos(this)
    }

    /**
     * Determines how the bullet moves.
     */
    abstract fun move()

    override fun collide(o: Object): Boolean {
        if (o is Entity) {
            if (o is Player && o.invincible)
                return false
            val from = from()
            if (from == null || from != o)
                if (CollideSystem.collide(o, this)) {
                    this.erase()
                    o.beingHit(this)
                    return true
                }
        }
        return false
    }

    internal fun erase() {
        erased = true
    }

    /**
     * The entity which fired this, and you can just return null.
     */
    open fun from(): Entity? = null

    /**
     * Collide test, and it is unnecessary to override this method, otherwise you know how this works.
     */
    open fun collideTest(): Boolean {
        var strategy = collideStrategy
        if (strategy >= PLAYER) {
            strategy -= PLAYER
            if (ObjectPool.usingSubChunk())
                if (SubChunks.isInPlayerSubChunks(uuid) && collide(ObjectPool.player()))
                    return true
                else {
                    if (collide(ObjectPool.player()))
                        return true
                }
        }

        if (strategy >= ENTITIES) {
            strategy -= ENTITIES
            for (e in ObjectPool.objects()) {
                if (collide(e))
                    return true
            }
        }

        if (strategy >= BULLETS) {
            for (b in ObjectPool.bullets()) {
                if (collide(b))
                    return true
            }
        }

        return false
    }

    companion object {
        const val PLAYER = 1
        const val ENTITIES = 2
        const val BULLETS = 4
        const val PLAYER_AND_BULLETS = PLAYER + BULLETS
        const val PLAYER_AND_ENTITIES = PLAYER + ENTITIES
        const val BULLET_AND_ENTITIES = BULLETS + ENTITIES
        const val ALL = PLAYER + ENTITIES + BULLETS
    }
}