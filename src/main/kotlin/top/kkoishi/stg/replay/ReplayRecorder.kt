package top.kkoishi.stg.replay

import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.keys.KeyBinds
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.file.Path
import java.util.TreeSet
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

@Suppress("MemberVisibilityCanBePrivate")
class ReplayRecorder @Throws(ExceptionInInitializerError::class) constructor(
    randomSeed: Long,
    val player: Player,
    recordedKeyCodes: IntArray,
    serializePlayer: (Player) -> Int,
) : Thread() {
    private val tempPath: Path = Path.of("${Threads.workdir()}/temp_replay.bin")

    init {
        if (tempPath.exists())
            tempPath.deleteExisting()
        tempPath.createFile()
    }

    private val temp = RandomAccessFile(tempPath.toFile(), "rw")
    private val fileLock: FileLock = temp.channel.tryLock()
    private val playerID = serializePlayer(player)
    private val frames = AtomicLong(0L)
    private val keyCodeSets: MutableSet<Int> = TreeSet(recordedKeyCodes.toList())
    private var disposed = false

    init {
        hasRunningRecorder = true
        ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "Initialize record replay.")
        temp.write(FILE_HEAD)
        temp.writeLong(randomSeed)
        temp.writeInt(playerID)
        temp.writeInt(keyCodeSets.size)
        keyCodeSets.forEach(temp::writeInt)

        runningInstance = this
    }

    override fun run() {
        ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "Start record replay.")

        while (GenericSystem.gameState.get() == GenericSystem.STATE_PLAYING ||
            GenericSystem.gameState.get() == GenericSystem.STATE_PAUSE
        ) {
            if (disposed) {
                ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "dispose.")
                return
            }
            recordFrame()
            frames.incrementAndGet()
            sleep(Threads.period())
        }

        ReplayRecorder::class.logger()
            .log(System.Logger.Level.INFO, "End to record replay, Frames: 0x${frames.get().toString(16)}")
    }

    private fun recordFrame() {
        temp.writeInt(InfoSystem.fps())
        val binds = KeyBinds.keys
        keyCodeSets.forEach { temp.writeBoolean(binds[it]) }
        temp.writeDouble(player.x())
        temp.writeDouble(player.y())
    }

    fun dispose() {
        hasRunningRecorder = false
        disposed = true
        if (temp.channel.isOpen && fileLock.isValid)
            fileLock.release()
        temp.close()
        tempPath.deleteIfExists()
        ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "Delete the temp file.")
    }

    fun save(dir: String, name: String, id: Long) = save(Path.of(dir), name, id)

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
    private fun saveImpl(replay: Path) {
        val cache = Path.of("./latest_rep.bin")
        cache.deleteIfExists()
        cache.createFile()
        val cacheOut = cache.outputStream()

        synchronized(temp) {
            temp.seek(0)
            temp.skipBytes(FILE_HEAD.size)

            with(cacheOut) {
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

        }
        cacheOut.flush()
        cacheOut.close()

        compress(cache.inputStream(), replay.outputStream(), replay.fileName.toString())
        cache.deleteIfExists()
    }

    private fun compress(bin: InputStream, dst: OutputStream, entryName: String) {
        val zip = ZipOutputStream(dst)
        val buf = ByteArray(1024)
        var len: Int

        zip.putNextEntry(ZipEntry(entryName))
        while (true) {
            len = bin.read(buf)
            if (len == -1)
                break
            zip.write(buf, 0, len)
        }

        zip.closeEntry()
        zip.flush()
        zip.close()
        bin.close()
        dst.close()
    }

    companion object {
        @JvmStatic
        private lateinit var runningInstance: ReplayRecorder

        @JvmStatic
        private var hasRunningRecorder: Boolean = false

        @JvmStatic
        fun hasRunningRecorder() = hasRunningRecorder

        @JvmStatic
        fun tryDisposeRecorder() {
            if (this::runningInstance.isInitialized) {
                if (!runningInstance.disposed) {
                    ReplayRecorder::class.logger().log(System.Logger.Level.INFO, "$runningInstance is disposed.")
                    runningInstance.dispose()
                }
            }
        }

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