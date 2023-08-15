package top.kkoishi.stg.gfx.replay

import top.kkoishi.stg.common.entities.Player
import java.io.RandomAccessFile

class ReplayRecorder(val randomSeed: Long, val player: Player) {
    private val temp = RandomAccessFile("./temp_replay.replay", "rw")
    fun start() {

    }

    fun end() {

    }
}