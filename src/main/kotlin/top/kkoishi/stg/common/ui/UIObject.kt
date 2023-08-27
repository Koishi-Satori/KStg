package top.kkoishi.stg.common.ui

import top.kkoishi.stg.common.entities.Object
import java.awt.Font
import java.util.*

abstract class UIObject(val x: Int, val y: Int): Object {
    private val uiUUID = UUID.randomUUID()
    override val uuid: UUID
        get() = uiUUID

    abstract fun shouldAction(): Boolean
    abstract fun font(): Font
    abstract fun updateInfo()

    override fun update(): Boolean {
        if (shouldAction()) {
            updateInfo()
        }
        return false
    }

    final override fun collide(o: Object): Boolean = false
}