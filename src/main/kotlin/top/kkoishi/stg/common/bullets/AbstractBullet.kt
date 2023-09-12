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
 * If provides the default implementations of [update], [collide].
 *
 * @author KKoishi_
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractBullet(initialX: Int, initialY: Int) : Bullet(initialX, initialY) {
    /**
     * Determines that whether the bullet should be erased.
     */
    protected var erased = false
    protected var collideStrategy: Int = COLLIDE_STRATEGY_PLAYER
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
        if (strategy >= COLLIDE_STRATEGY_PLAYER) {
            strategy -= COLLIDE_STRATEGY_PLAYER
            if (ObjectPool.usingSubChunk())
                if (SubChunks.isInPlayerSubChunks(uuid) && collide(ObjectPool.player()))
                    return true
                else {
                    if (collide(ObjectPool.player()))
                        return true
                }
        }

        if (strategy >= COLLIDE_STRATEGY_ENTITIES) {
            strategy -= COLLIDE_STRATEGY_ENTITIES
            for (e in ObjectPool.objects()) {
                if (collide(e))
                    return true
            }
        }

        if (strategy >= COLLIDE_STRATEGY_BULLETS) {
            for (b in ObjectPool.bullets()) {
                if (collide(b))
                    return true
            }
        }

        return false
    }

    @Suppress("unused")
    companion object {
        /**
         * Only perform collision detection with player instances.
         */
        const val COLLIDE_STRATEGY_PLAYER = 1

        /**
         * Only perform collision detection with entity instances in [ObjectPool.objects].
         */
        const val COLLIDE_STRATEGY_ENTITIES = 2

        /**
         * Only perform collision detection with bullet instances.
         */
        const val COLLIDE_STRATEGY_BULLETS = 4

        /**
         * Only perform collision detection with player instances and bullet instances.
         */
        const val COLLIDE_STRATEGY_PLAYER_AND_BULLETS = COLLIDE_STRATEGY_PLAYER + COLLIDE_STRATEGY_BULLETS

        /**
         * Only perform collision detection with player instances and entity instances in [ObjectPool.objects].
         */
        const val COLLIDE_STRATEGY_PLAYER_AND_ENTITIES = COLLIDE_STRATEGY_PLAYER + COLLIDE_STRATEGY_ENTITIES

        /**
         * Only perform collision detection with bullet instances and entity instances in [ObjectPool.objects]
         */
        const val COLLIDE_STRATEGY_BULLET_AND_ENTITIES = COLLIDE_STRATEGY_BULLETS + COLLIDE_STRATEGY_ENTITIES

        /**
         * Perform collision detection with all.
         */
        const val COLLIDE_STRATEGY_ALL = COLLIDE_STRATEGY_PLAYER + COLLIDE_STRATEGY_ENTITIES + COLLIDE_STRATEGY_BULLETS
    }
}