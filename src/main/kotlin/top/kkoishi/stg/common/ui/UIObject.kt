package top.kkoishi.stg.common.ui

import top.kkoishi.stg.common.entities.Object
import java.awt.Font

abstract class UIObject(val x: Int, val y: Int): Object {
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