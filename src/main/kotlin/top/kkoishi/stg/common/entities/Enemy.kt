package top.kkoishi.stg.common.entities

/**
 * The basic class for enemies.
 * It is not necessary to implement the [Object.collide] method, for all the collide between is checked in
 * [Player.update] method.
 *
 * @author KKoishi_
 */
abstract class Enemy(health: Int) : Entity(health) {
    override fun isDead(): Boolean = health <= 0
}