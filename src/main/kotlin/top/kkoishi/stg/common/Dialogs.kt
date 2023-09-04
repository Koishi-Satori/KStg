package top.kkoishi.stg.common

import top.kkoishi.stg.gfx.Texture
import java.awt.Graphics2D

class Dialogs(
    val dialogs: ArrayDeque<Dialog>,
    private val messageX: Int,
    private val messageY: Int,
    initialX: Int,
    initialY: Int,
) : RenderableObject(initialX, initialY) {
    class Dialog(private val faces: ArrayDeque<Face>, private val message: String) {
        fun paintMessage(g: Graphics2D, x: Int, y: Int) {

        }
    }

    data class Face(private val face: Texture, private val x: Int, private val y: Int, private val state: FaceState)

    enum class FaceState {
        DISPLAY, SHADOW, HIDE
    }

    override fun update(): Boolean {
        TODO("Not yet implemented")
    }

    override fun paint(g: Graphics2D) {
        TODO("Not yet implemented")
    }
}