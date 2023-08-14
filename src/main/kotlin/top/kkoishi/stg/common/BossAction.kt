package top.kkoishi.stg.common

import top.kkoishi.stg.common.entities.Boss

abstract class BossAction(val health: Int, var frames: Long) {
    abstract fun action(boss: Boss)
}