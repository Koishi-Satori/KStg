package top.kkoishi.stg.common

abstract class Enemy(health: Int) : Entity(health) {
    override fun isDead(): Boolean = health == 0
}