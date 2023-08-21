package top.kkoishi.stg.gfx.replay

import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.logic.GenericFlags
import top.kkoishi.stg.logic.InfoSystem
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.PlayerManager
import top.kkoishi.stg.logic.Threads
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.*

@Suppress("MemberVisibilityCanBePrivate")
open class ReplayRecorder @Throws(ExceptionInInitializerError::class) constructor(
    randomSeed: Long,
    val player: Player,
    recordedKeyCodes: IntArray,
    serializePlayer: (Player) -> Int,
) : Thread() {
    protected val tempPath: Path = Path.of("./temp_replay.bin")

    init {
        if (tempPath.exists())
            tempPath.deleteExisting()
        tempPath.createFile()
    }

    protected val temp = RandomAccessFile(tempPath.toFile(), "rw")
    private val fileLock: FileLock = temp.channel.tryLock()
    protected val playerID = serializePlayer(player)
    protected val frames = AtomicLong(0L)
    protected val keyCodeSets: MutableSet<Int> = HashSet(recordedKeyCodes.toList())

    init {
        ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "Initialize record replay.")
        temp.write(FILE_HEAD)
        temp.writeLong(randomSeed)
        temp.writeInt(playerID)
        temp.writeInt(keyCodeSets.size)
        keyCodeSets.forEach(temp::writeInt)
    }

    final override fun run() {
        ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "Start record replay.")

        while (GenericFlags.gameState.get() == GenericFlags.STATE_PLAYING ||
            GenericFlags.gameState.get() == GenericFlags.STATE_PAUSE
        ) {
            recordFrame()
            frames.incrementAndGet()
            sleep(Threads.period())
        }

        ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "End to record replay.")
    }

    protected open fun recordFrame() {
        temp.writeInt(InfoSystem.fps())
        val binds = PlayerManager.binds
        keyCodeSets.forEach { temp.writeBoolean(binds[it]) }
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
            temp.writeUTF(name)
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
            temp.seek(0)

            val out = replay.outputStream()

            temp.skipBytes(FILE_HEAD.size)
            with(out) {
                write(FILE_HEAD)
                writeLong(temp.readLong())
                writeInt(temp.readInt())
                val codes = ShortArray(temp.readInt())
                keyCodeSets.forEachIndexed { i, v -> codes[i] = v.toShort() }
                writeLong(frames.get())

                // write every frame
                val range = 0 until keyCodeSets.size
                val pressedKeys = ArrayDeque<Short>()
                for (l in (0 until frames.get())) {
                    writeInt(temp.readInt())
                    range.forEach { if (temp.readBoolean()) pressedKeys.addLast(it.toShort()) }
                    writeInt(pressedKeys.size)
                    pressedKeys.forEach { writeShort(it.toInt()) }
                    writeDouble(temp.readDouble())
                    writeDouble(temp.readDouble())

                    pressedKeys.clear()
                }

                writeLong(temp.readLong())
                val name = temp.readUTF()
                write(name.toByteArray().size)
                write(name.toByteArray())
                writeLong(temp.readLong())
            }

            out.flush()
            out.close()
        }
    }

    internal class FrameInfo(val fps: Int, val pressedKeys: ShortArray, val x: Double, val y: Double) {
        fun length() = 24 + pressedKeys.size * 2

        fun write(oos: OutputStream) {
            with(oos) {
                writeInt(fps)
                writeInt(pressedKeys.size)
                pressedKeys.forEach { writeShort(it.toInt()) }
                writeDouble(x)
                writeDouble(y)
            }
        }
    }

    companion object {
        @JvmStatic
        val FILE_HEAD: ByteArray = "Koishi_Replay".toByteArray()
        internal fun OutputStream.writeInt(v: Int) {
            write(v ushr 24 and 0xFF)
            write(v ushr 16 and 0xFF)
            write(v ushr 8 and 0xFF)
            write(v ushr 0 and 0xFF)
        }

        internal fun OutputStream.writeShort(v: Int) {
            write(v ushr 8 and 0xFF)
            write(v ushr 0 and 0xFF)
        }

        internal fun OutputStream.writeLong(v: Long) {
            write((v ushr 56).toInt() and 0xFF)
            write((v ushr 48).toInt() and 0xFF)
            write((v ushr 40).toInt() and 0xFF)
            write((v ushr 32).toInt() and 0xFF)
            write((v ushr 24).toInt() and 0xFF)
            write((v ushr 16).toInt() and 0xFF)
            write((v ushr 8).toInt() and 0xFF)
            write((v ushr 0).toInt() and 0xFF)
        }

        internal fun OutputStream.writeDouble(v: Double) {
            writeLong(java.lang.Double.doubleToLongBits(v))
        }

        internal fun OutputStream.write(constPool: ConstPool) {
            writeInt(constPool.size)
            constPool.forEach {
                writeInt(it.size)
                write(it)
            }
        }
    }
}

internal typealias ConstPool = Array<ByteArray>