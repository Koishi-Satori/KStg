@file:Suppress("MemberVisibilityCanBePrivate")

package top.kkoishi.stg.common

import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.GenericSystem
import top.kkoishi.stg.logic.keys.KeyBinds
import java.awt.Color
import java.awt.Graphics2D

/**
 * Dialogs of the game, used to explain the useless game background.
 *
 * @author KKoishi_
 */
abstract class Dialogs(
    dialogs: ArrayDeque<Dialog>,
    private val messageX: Int,
    private val messageY: Int,
    initialX: Int,
    initialY: Int,
) : RenderableObject(initialX, initialY) {
    private val lock = Any()

    protected val dialogQueue: ArrayDeque<Dialog> = dialogs

    protected var cur: Dialog? = null

    init {
        if (dialogQueue.isNotEmpty())
            cur = dialogQueue.removeFirst()
    }

    open class Dialog(val faces: ArrayDeque<Face>, val message: String) {
        val font = Graphics.font("dialog")
        open fun paintFaces(g: Graphics2D) {
            faces.forEach {
                if (it.state != FaceState.HIDE) {
                    val texture = it.face
                    val op = when (it.state) {
                        FaceState.DISPLAY -> texture.normalMatrix()
                        FaceState.SHADOW -> texture.alphaConvolve(0.5f)
                        else -> texture.normalMatrix()
                    }
                    it.face.paint(g, op, it.x, it.y)
                }
            }
        }

        open fun paintMessage(g: Graphics2D, x: Int, y: Int) {
            val oldFont = g.font
            val oldColor = g.color
            g.color = Color.WHITE
            g.font = font
            g.drawString(message, x, y)
            g.font = oldFont
            g.color = oldColor
        }

        override fun toString(): String {
            return "Dialog(faces=$faces, message='$message')"
        }
    }

    data class Face(val face: Texture, val x: Int, val y: Int, val state: FaceState) {
        constructor(face: String, x: Int, y: Int, state: FaceState) : this(GFX[face], x, y, state)
    }

    enum class FaceState {
        DISPLAY, SHADOW, HIDE
    }

    abstract fun paintBackground(g: Graphics2D)

    open fun action() {
        if (KeyBinds.isPressed(Player.VK_Z)) {
            KeyBinds.release(Player.VK_Z)
            cur = if (dialogQueue.isEmpty())
                null
            else
                dialogQueue.removeFirst()
        }
    }

    fun isEnd() = dialogQueue.isEmpty() && cur == null

    override fun update(): Boolean {
        if (dialogQueue.isEmpty() && cur == null) {
            GenericSystem.inDialog = false
            return true
        }
        GenericSystem.inDialog = true
        synchronized(lock) {
            action()
        }
        return false
    }

    override fun paint(g: Graphics2D) {
        var cur: Dialog?
        synchronized(lock) {
            cur = this.cur
            if (dialogQueue.isEmpty() && cur == null) {
                paintBackground(g)
                return
            }
        }
        if (cur == null) {
            paintBackground(g)
            return
        }
        cur!!.paintFaces(g)
        paintBackground(g)
        cur!!.paintMessage(g, messageX, messageY)
    }

    companion object {
        @JvmStatic
        private val storedDialogs = HashMap<String, Dialogs>()

        fun addDialog(key: String, dialogs: Dialogs) {
            storedDialogs[key] = dialogs
        }

        fun getDialog(key: String) = storedDialogs[key]
    }
}