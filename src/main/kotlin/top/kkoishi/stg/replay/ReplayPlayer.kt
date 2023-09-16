package top.kkoishi.stg.replay

import top.kkoishi.stg.common.entities.Player.Companion.VK_C
import top.kkoishi.stg.common.entities.Player.Companion.VK_DOWN
import top.kkoishi.stg.common.entities.Player.Companion.VK_ESCAPE
import top.kkoishi.stg.common.entities.Player.Companion.VK_LEFT
import top.kkoishi.stg.common.entities.Player.Companion.VK_RIGHT
import top.kkoishi.stg.common.entities.Player.Companion.VK_SHIFT
import top.kkoishi.stg.common.entities.Player.Companion.VK_UP
import top.kkoishi.stg.common.entities.Player.Companion.VK_X
import top.kkoishi.stg.common.entities.Player.Companion.VK_Z
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.keys.KeyBinds
import java.awt.geom.Point2D
import java.nio.file.Path
import java.util.TreeMap
import java.util.TreeSet

abstract class ReplayPlayer(private var repPath: Path) {
    val logger = ReplayRecorder::class.logger()
    protected val keys = TreeMap<Int, Boolean>()
    protected val ignoredKeys = TreeSet(listOf(VK_ESCAPE))
    fun setReplay(repPath: Path) {
        this.repPath = repPath
    }

    abstract fun decompressData(repPath: Path): ByteArray

    abstract fun readData(data: ByteArray): ArrayDeque<ReplayFrame>

    abstract fun syncFrameData(f: ReplayFrame)

    open fun putBarrier() {
        KeyBinds.inputBarrier(
            VK_C,
            VK_X,
            VK_Z,
            VK_UP,
            VK_SHIFT,
            VK_RIGHT,
            VK_LEFT,
            VK_DOWN
        )
    }

    open fun playFrame(f: ReplayFrame) {
        with(f) {
            keys.keys.forEach { keys[it] = false }
            pressedKeys.forEach {
                if (!ignoredKeys.contains(it))
                    keys[it] = true
            }
            keys.keys.forEach {
                if (keys[it] == true)
                    KeyBinds.forcePress(it)
                else
                    KeyBinds.forceRelease(it)
            }

            syncFrameData(f)
        }
        TODO("finish this.")
    }

    fun start() {
        logger.log(System.Logger.Level.INFO, "Ready to read replay $repPath")
        val frames = readData(decompressData(repPath))
        logger.log(System.Logger.Level.INFO, "Decompress and read data success, frame size: ${frames.size}")
        putBarrier()
        frames.forEach(this::playFrame)
        logger.log(System.Logger.Level.INFO, "End play replay.")
    }

    data class ReplayFrame @JvmOverloads constructor(
        val pressedKeys: Array<Int>,
        val fps: Int,
        val playerPos: Point2D,
        val other: Any? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ReplayFrame) return false

            if (!pressedKeys.contentEquals(other.pressedKeys)) return false
            if (fps != other.fps) return false
            if (playerPos != other.playerPos) return false
            return other == other.other
        }

        override fun hashCode(): Int {
            var result = pressedKeys.contentHashCode()
            result = 31 * result + fps
            result = 31 * result + playerPos.hashCode()
            result = 31 * result + (other?.hashCode() ?: 0)
            return result
        }
    }
}