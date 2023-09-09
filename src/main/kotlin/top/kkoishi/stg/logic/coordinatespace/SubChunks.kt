package top.kkoishi.stg.logic.coordinatespace

import top.kkoishi.stg.common.bullets.Bullet
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.util.Mth.setScale
import top.kkoishi.stg.util.Options
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.TreeSet
import java.util.UUID

object SubChunks {
    var chunkAmountX = 8
    var chunkAmountY = 10
    private var subChunkWidth = 0.0
    private var subChunkHeight = 0.0
    private var spaceSize: Pair<Double, Double> = realSpace()
    private var subChunks: Array<Array<TreeSet<UUID>>> = divideSpace()
    private var playerChunks = ArrayDeque<Pair<Int, Int>>()
    private var playerChunkObjectCount = 0

    private fun realCoordinates(x: Int, y: Int): Point2D = realCoordinates(x.toDouble(), y.toDouble())

    private fun realCoordinates(x: Double, y: Double): Point2D {
        val insets = Graphics.getUIInsets()
        return Point2D.Double(x - insets.left, y - insets.top)
    }

    private fun realSpace(): Pair<Double, Double> {
        val screenSize = Graphics.getScreenSize()
        val insets = Graphics.getUIInsets()
        return (screenSize.width - insets.left - insets.right) to (screenSize.height - insets.top - insets.bottom)
    }

    private fun divideSpace(): Array<Array<TreeSet<UUID>>> {
        subChunkWidth = (spaceSize.first / chunkAmountX).setScale()
        subChunkHeight = (spaceSize.second / chunkAmountY).setScale()
        return Array(chunkAmountX) { Array(chunkAmountY) { TreeSet<UUID>() } }
    }

    private fun pointPos(x: Double, y: Double): Pair<Int, Int> =
        (x / subChunkWidth).toInt() to (y / subChunkHeight).toInt()

    private fun shapePos(shape: Shape): ArrayDeque<Pair<Int, Int>> {
        val pos = ArrayDeque<Pair<Int, Int>>(5)
        if (shape is Rectangle2D) {
            val upperLeft = realCoordinates(shape.x, shape.y)
            val bottomRight = realCoordinates(shape.x + shape.width, shape.y + shape.height)
            val nUpperLeft = pointPos(upperLeft.x, upperLeft.y)
            val nBottomRight = pointPos(bottomRight.x, bottomRight.y)
            (nUpperLeft.first..nBottomRight.first).forEach { xIndex ->
                (nUpperLeft.second..nBottomRight.second).forEach { yIndex ->
                    val lineIndex: Int = if (xIndex >= chunkAmountX)
                        chunkAmountX - 1
                    else xIndex
                    val colIndex: Int = if (yIndex >= chunkAmountY)
                        chunkAmountY - 1
                    else yIndex
                    pos.addLast(lineIndex to colIndex)
                }
            }
        } else
            return shapePos(shape.bounds2D)
        return pos
    }

    private fun putBullet(b: Bullet) {
        if (CollideSystem.checkPos(b))
            return
        val uuid = b.uuid
        val pos = shapePos(b.shape())
        pos.forEach { (xIndex, yIndex) ->
            val set: MutableSet<UUID> = subChunks[xIndex][yIndex]
            set.add(uuid)
        }
    }

    private fun calculatePlayer() {
        val p = ObjectPool.player()
        val pos = shapePos(p.shape())
        if (playerChunks.isEmpty()) {
            playerChunks.addAll(pos)
        } else {
            var newCount = 0

            pos.forEach { (x, y) ->
                newCount += subChunks[x][y].size
            }

            if (Options.State.debug && newCount != playerChunkObjectCount)
                SubChunks::class.logger().log(System.Logger.Level.DEBUG, "Bullets in player chunks: $newCount")

            playerChunks.clear()
            playerChunks.addAll(pos)
        }
    }

    fun updateSpace() {
        subChunks.forEach { col ->
            col.forEach {
                it.clear()
            }
        }
        ObjectPool.bullets().forEach { putBullet(it) }
        calculatePlayer()
    }

    fun isInPlayerSubChunks(uuid: UUID): Boolean {
        val chunks = ArrayDeque<Pair<Int, Int>>()
        subChunks.forEachIndexed { xIndex, line ->
            line.forEachIndexed { yIndex, it ->
                if (it.contains(uuid))
                    chunks.addLast(xIndex to yIndex)
            }
        }

        playerChunks.forEach { playerChunk ->
            chunks.forEach {
                if (it.first == playerChunk.first && it.second == playerChunk.second)
                    return true
            }
        }
        return false
    }

    fun refresh() {
        spaceSize = realSpace()
    }

    fun show(r: Graphics2D) {
        r.color = Color.WHITE
        val insets = Graphics.getUIInsets()

        (0 until chunkAmountX).forEach {
            val x = (it * subChunkWidth).toInt() + insets.left
            r.drawLine(x, insets.top, x, spaceSize.second.toInt() + insets.top)
            r.drawString(it.toString(), x, 10)
        }

        (0 until chunkAmountY).forEach {
            val y = (it * subChunkHeight).toInt() + insets.top
            r.drawLine(insets.left, y, spaceSize.first.toInt() + insets.left, y)
            r.drawString(it.toString(), 0, y)
        }
    }

    override fun toString(): String = subChunks.contentDeepToString()
}