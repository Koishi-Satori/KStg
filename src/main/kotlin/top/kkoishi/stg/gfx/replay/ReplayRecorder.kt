package top.kkoishi.stg.gfx.replay

import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.logic.GenericFlags
import top.kkoishi.stg.logic.InfoSystem
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.PlayerManager
import top.kkoishi.stg.logic.Threads
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.file.Path
import kotlin.io.path.*

@Suppress("MemberVisibilityCanBePrivate")
open class ReplayRecorder @Throws(ExceptionInInitializerError::class) constructor(
    randomSeed: Long,
    val player: Player,
    val recordedKeyCodes: IntArray,
    serializePlayer: (Player) -> Int,
) : Thread() {
    protected val tempPath: Path = Path.of("./temp_replay.bin")

    init {
        synchronized(lock) {
            if (onlyInstance)
                throw ExceptionInInitializerError("ReplayRecorder can only have one instance!")
            onlyInstance = true
        }
        tempPath.createFile()
    }

    protected val temp = RandomAccessFile(tempPath.toFile(), "rw")
    private val fileLock: FileLock = temp.channel.tryLock()
    protected val playerID = serializePlayer(player)

    init {
        ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "Start record replay.")
        temp.write(FILE_HEAD)
        temp.writeLong(randomSeed)
        temp.writeInt(playerID)
        temp.writeInt(recordedKeyCodes.size)
        recordedKeyCodes.forEach(temp::writeInt)
    }

    final override fun run() {
        while (GenericFlags.gameState.get() == GenericFlags.STATE_PLAYING ||
            GenericFlags.gameState.get() == GenericFlags.STATE_PAUSE
        ) {
            recordFrame()
            sleep(Threads.period())
        }

        ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "End to record replay.")
    }

    protected open fun recordFrame() {
        temp.writeInt(InfoSystem.fps())
        val binds = PlayerManager.binds
        recordedKeyCodes.forEach { temp.writeBoolean(binds[it]) }
        temp.writeDouble(player.x.get())
        temp.writeDouble(player.y.get())
    }

    /**
     * Save the replay.
     *
     * @param dir the dir which replay file will be stored.
     * @param name the file name (without file extension)
     * @param id the only id of the replay, used to find the right one when different replays have the same name.
     *
     * @return if the replay is saved successfully.
     */
    fun save(dir: Path, name: String, id: Long): Boolean {
        if (!dir.exists())
            dir.createDirectories()
        val replayFile = Path.of("${dir.absolutePathString()}/${name}_$id.rep")
        if (replayFile.exists())
            return false
        replayFile.createFile()
        try {
            temp.writeLong(System.currentTimeMillis())
            val bytes = name.toByteArray()
            temp.writeInt(bytes.size)
            temp.write(bytes)
            temp.writeLong(id)
            saveImpl(replayFile)
            ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "Save the replay as $replayFile")
        } catch (ioe: IOException) {
            ReplayRecorder::class.logger().log(System.Logger.Level.ERROR, ioe)
            return false
        } finally {
            if (temp.channel.isOpen && fileLock.isValid)
                fileLock.release()
            temp.close()
            tempPath.deleteIfExists()
            ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "Delete the temp file.")
        }
        return true
    }

    @Throws(IOException::class)
    protected open fun saveImpl(replay: Path) {
        synchronized(temp) {
            val oldPointer = temp.filePointer
            temp.seek(0)
            val buf = ByteArray(1024)
            var len: Int
            val out = replay.outputStream()

            while (true) {
                len = temp.read(buf)
                if (len == -1)
                    break
                out.write(buf, 0, len)
            }

            temp.seek(oldPointer)
            out.flush()
            out.close()
        }
    }

    companion object {
        @JvmStatic
        val FILE_HEAD: ByteArray = "Koishi_Replay".toByteArray()

        @JvmStatic
        private var onlyInstance = false

        @JvmStatic
        private val lock = Any()
    }
}