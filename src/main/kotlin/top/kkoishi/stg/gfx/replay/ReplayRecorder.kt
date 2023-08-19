package top.kkoishi.stg.gfx.replay

import top.kkoishi.stg.common.entities.Player
import java.io.RandomAccessFile
import java.nio.channels.FileLock

class ReplayRecorder(val randomSeed: Long, val player: Player, serializePlayer: (Player) -> Int) {
    private val temp = RandomAccessFile("./temp_replay.out", "rw")
    private val fileLock: FileLock = temp.channel.tryLock()
    private val playerID = serializePlayer(player)

    init {
        temp.write(FILE_HEAD)
        temp.writeLong(randomSeed)
        temp.write(playerID)
    }

    fun start() {

    }

    fun end() {
        fileLock.release()
    }

    companion object {
        @JvmStatic
        val FILE_HEAD: ByteArray = "Koishi".toByteArray()
    }
}