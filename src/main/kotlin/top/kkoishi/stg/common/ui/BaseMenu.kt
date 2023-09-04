package top.kkoishi.stg.common.ui

import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.keys.KeyBinds
import top.kkoishi.stg.util.Keys
import java.awt.event.KeyEvent
import java.util.TreeMap

abstract class BaseMenu(actionState: Int) : Menu(actionState) {
    @Suppress("MemberVisibilityCanBePrivate")
    protected val keyEvents = TreeMap<Int, (MenuItem) -> Unit>()

    open fun initKeyEvents() {
        keyEvents.putAll(DEFAULT_EVENTS)
    }

    fun restore() {
        rootItem.select = 0
        curLevel = rootItem
    }

    abstract class AbstractMenuItem(
        x: Int,
        y: Int,
        menu: BaseMenu,
        protected val selectedTextures: ArrayDeque<Texture>,
    ) :
        MenuItem(x, y, menu) {

        @Suppress("MemberVisibilityCanBePrivate")
        protected val menuAsBaseMenu: BaseMenu
            get() = menu as BaseMenu

        override fun updateInfo() {
            menuAsBaseMenu.keyEvents.forEach {
                val keyCode = it.key
                if (KeyBinds.isPressed(keyCode)) {
                    this::class.logger().log(System.Logger.Level.INFO, "Press key: ${Keys[keyCode]}")
                    it.value(this)
                    KeyBinds.release(keyCode)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        val DEFAULT_EVENTS = TreeMap<Int, (MenuItem) -> Unit>(
            mapOf(
                KeyEvent.VK_UP to {
                    if (it.select == 0)
                        it.select = it.items.size - 1
                    else
                        it.select--
                },
                KeyEvent.VK_DOWN to {
                    if (it.select == it.items.size - 1)
                        it.select = 0
                    else
                        it.select++
                },
                KeyEvent.VK_Z to {
                    val item = it.children[it.select]
                    if (item != null)
                        it.menu.curLevel = item
                    else
                        it()
                },
                KeyEvent.VK_X to {
                    val root = it.parent
                    if (root != null)
                        it.menu.curLevel = root
                })
        )
    }
}