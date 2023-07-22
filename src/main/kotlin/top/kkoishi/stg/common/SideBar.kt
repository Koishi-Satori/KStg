package top.kkoishi.stg.common

import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.gfx.Graphics
import java.awt.Font

abstract class SideBar: Object {
    abstract fun background(): String

    @Throws(FailedLoadingResourceException::class)
    protected open fun font(): Font = Graphics.font("sidebar")
}