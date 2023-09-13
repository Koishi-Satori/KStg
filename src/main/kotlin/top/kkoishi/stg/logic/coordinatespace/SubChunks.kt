package top.kkoishi.stg.logic.coordinatespace

import top.kkoishi.stg.common.bullets.Bullet
import top.kkoishi.stg.exceptions.InternalError
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.util.Mth.gcd
import top.kkoishi.stg.util.Mth.setScale
import top.kkoishi.stg.util.Options
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.TreeSet
import java.util.UUID
import kotlin.math.roundToInt

/**
 * This class uses the mesh method to divide the space to optimize collision detection by reducing the number of
 * calculations, but the worst-case time complexity of the algorithm when all objects are in dense space is
 * still O(n^2 - n), which is same as the traversal algorithm.
 *
 * A mesh in the mesh method is called Chunk/SubChunk here.
 *
 * The implementation builds a two-dimensional array to store all the entities there (actually the UUID of the entity)
 * and represent the mesh that divides the space, and within each logical frame, update the mesh in which the
 * entities are located. Each time when an entity is processing collide testing, only those that are in the same meshes
 * where it is located, might be more than one mesh, as the entity are detected, reducing the number of collision
 * checks.
 *
 * The default meshing number is 12*14, you can use the provided functions to automatically mesh, or set the number
 * of divisions on the x-axis and y-axis yourself.
 *
 * @author KKoishi_
 */
object SubChunks {
    /**
     * The base length for a chunk, default value: 64.
     *
     * Adjust strategy:
     *
     *      1. When the space to be divided is too small (less than this value), only one chunk will remain.
     *
     *      2. When the given space is coprime between width and height, the space is divided according to this value.
     *
     *      3. When there is no coprime between the width and height of a given space, and its greatest common divisor factor a is greater than this value, [baseSubChunkLength] will be used for division; If a is less than this value, a will be used for division.
     *
     *      4. When the final undivided area is too small, it will also be divided separately.
     */
    @JvmStatic
    private var maxSubChunkLength = 64

    /**
     * The base length for a chunk, default value: 16.
     *
     * Adjust strategy:
     *
     *      1. When the space to be divided is too small (less than this value), only one chunk will remain.
     *
     *      2. When the given space is coprime between width and height, the space is divided according to this value.
     *
     *      3. When there is no coprime between the width and height of a given space, and its greatest common divisor factor a is smaller than this value, [baseSubChunkLength] will be used for division; If a is less than this value, a will be used for division.
     *
     *      4. When the final undivided area is too small, it will also be divided separately.
     */
    @JvmStatic
    private var minSubChunkLength = 16

    @JvmStatic
    private var baseSubChunkLength = 32

    @JvmStatic
    private var chunkAmountX = 12

    @JvmStatic
    private var chunkAmountY = 14

    @JvmStatic
    private var subChunkWidth = 0.0

    @JvmStatic
    private var subChunkHeight = 0.0

    /**
     * The space size.
     */
    @JvmStatic
    private var spaceSize: Pair<Double, Double> = realSpace()

    /**
     * A two-dimensional array used to simulate the SubChunks, each array element stores the UUID of all entities in
     * that SubChunk.
     */
    @JvmStatic
    private var subChunks: Array<Array<TreeSet<UUID>>> = divideSpace()

    /**
     * The chunks where the player instance is located in.
     */
    @JvmStatic
    private var playerChunks = ArrayDeque<Pair<Int, Int>>()

    @JvmStatic
    private var playerChunkObjectCount = 0

    @JvmStatic
    private fun realCoordinates(x: Int, y: Int): Point2D = realCoordinates(x.toDouble(), y.toDouble())

    @JvmStatic
    private fun realCoordinates(x: Double, y: Double): Point2D {
        val insets = Graphics.getUIInsets()
        return Point2D.Double(x - insets.left, y - insets.top)
    }

    @JvmStatic
    private fun realSpace(): Pair<Double, Double> {
        val screenSize = Graphics.getScreenSize()
        val insets = Graphics.getUIInsets()
        return (screenSize.width - insets.left - insets.right) to (screenSize.height - insets.top - insets.bottom)
    }

    @JvmStatic
    private fun divideSpace(): Array<Array<TreeSet<UUID>>> {
        subChunkWidth = (spaceSize.first / chunkAmountX).setScale()
        subChunkHeight = (spaceSize.second / chunkAmountY).setScale()
        return Array(chunkAmountX) { Array(chunkAmountY) { TreeSet<UUID>() } }
    }

    @JvmStatic
    private fun pointPos(x: Double, y: Double): Pair<Int, Int> =
        (x / subChunkWidth).toInt() to (y / subChunkHeight).toInt()

    /**
     * Get the indexes of the SubChunks where the given shape located.
     *
     * @param shape the Shape.
     * @return the indexes of the SubChunks where the given shape located.
     */
    @JvmStatic
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

    /**
     * Put a bullet into the SubChunks.
     */
    @JvmStatic
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

    /**
     * Calculate where the player is located in.
     */
    @JvmStatic
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

    @JvmStatic
    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkPositive(digit: Int) {
        if (digit <= 0)
            throw InternalError("The amount must be positive, but got $digit instead.")
    }

    @JvmStatic
    private fun modifySubChunkImpl(widthAmount: Int, heightAmount: Int) {
        chunkAmountX = widthAmount
        chunkAmountY = heightAmount
    }

    /**
     * Adjust the SubChunks according to the specified [width] and [height].
     *
     * The space specified by [width] and [height] should be the space where the player can move(player space), and it can be:
     * ```kotlin
     *         val screen = Graphics.getScreenSize()
     *         val uiInsets = Graphics.getUIInsets()
     *         val screenWidth = screen.width
     *         val screenHeight = screen.height
     *         val width = screenWidth - uiInsets.left - uiInsets.right
     *         val height = screenHeight - uiInsets.top - uiInsets.bottom
     * ```
     *
     * Adjust strategy:
     *
     *      1. When the space to be divided is too small (less than this value), only one chunk will remain.
     *
     *      2. When the given space is coprime between width and height, the space is divided according to this value.
     *
     *      3. When there is no coprime between the width and height of a given space, and its greatest common divisor factor α is greater than [maxSubChunkLength] or smaller than [minSubChunkLength], this value "baseSubChunkLength" will be used for division; If α is less than this value, α will be used for division.
     *
     *      4. When the final undivided area is too small, it will also be divided separately.
     *
     * @param width the width of the player space.
     * @param height the height of the player space.
     */
    @JvmStatic
    fun autoAdjustSubChunkAmount(width: Int, height: Int) {
        // make sure width and height are both positive.
        checkPositive(width)
        checkPositive(height)

        val gcd = gcd(width, height)
        val subChunkLength: Int = if (gcd == 1 || gcd < minSubChunkLength || gcd > maxSubChunkLength)
            baseSubChunkLength
        else
            gcd
        var dividedXAmount = width / subChunkLength
        var dividedYAmount = height / subChunkLength

        // if those SubChunks can not fully cover the space:
        // add more chunks for covering.
        if (width > dividedXAmount * subChunkLength)
            ++dividedXAmount
        if (height > dividedYAmount * subChunkLength)
            ++dividedYAmount

        // does not need check -> already ensure positive.
        modifySubChunkImpl(dividedXAmount, dividedYAmount)
    }

    @JvmStatic
    fun modifySubChunk(widthAmount: Int, heightAmount: Int) {
        checkPositive(widthAmount)
        checkPositive(heightAmount)

        modifySubChunkImpl(widthAmount, heightAmount)
    }

    /**
     * Set the [baseSubChunkLength] to the given value.
     *
     * ## Deprecated for it is not recommended to use.
     * ## Yet there is no risk to invoke this method.
     */
    @JvmStatic
    @Deprecated("This is not recommended to use.", level = DeprecationLevel.WARNING)
    fun setBaseSubChunk(newValue: Int) {
        baseSubChunkLength = newValue
        maxSubChunkLength = 2 * baseSubChunkLength
        minSubChunkLength = (baseSubChunkLength / 2f).roundToInt()
    }

    @JvmStatic
    fun getCurrentBaseSubChunkLength() = baseSubChunkLength

    /**
     * Reindex the space.
     */
    fun updateSpace() {
        subChunks.forEach { col ->
            col.forEach {
                it.clear()
            }
        }
        ObjectPool.bullets().forEach { putBullet(it) }
        calculatePlayer()
    }

    /**
     * Returns if an entity which has the given UUID is in the SubChunks where the player located in.
     *
     * @param uuid the UUID of the entity.
     * @return if an entity which has the given UUID is in the SubChunks where the player located in.
     */
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

    /**
     * ## Debug use only.
     *
     * When debug mode on, will render the meshes.
     */
    fun renderDebug(r: Graphics2D) {
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