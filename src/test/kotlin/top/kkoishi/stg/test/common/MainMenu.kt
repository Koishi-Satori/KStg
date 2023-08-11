package top.kkoishi.stg.test.common

import top.kkoishi.stg.common.ui.Menu
import top.kkoishi.stg.common.ui.MenuItem
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.GenericFlags
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.PlayerManager
import top.kkoishi.stg.test.Test
import java.awt.Graphics2D
import java.awt.event.KeyEvent
import kotlin.system.exitProcess

class MainMenu : Menu(GenericFlags.STATE_MENU) {
    private val bg = GFX.getTexture("menu_bg")

    override fun paintBackground(r: Graphics2D) {
        bg.paint(r, bg.normalMatrix(), x, y)
    }

    open class MainMenuItem(x: Int, y: Int, menu: Menu, protected val selectedTextures: ArrayDeque<Texture>) :
        MenuItem(x, y, menu) {

        // 灵梦白丝
        private val baisi = GFX.getTexture("reimu_baisi")
        override fun paint(g: Graphics2D) {
            baisi.paint(g, baisi.normalMatrix(), 0, 0)
            var pY = y
            items.forEachIndexed { index, item ->
                if (index == select) {
                    val selectedTexture = selectedTextures[index]
                    selectedTexture.paint(g, selectedTexture.normalMatrix(), 10, pY)
                }
                item.paint(g, item.normalMatrix(), x, pY)
                pY += item.height
            }
        }

        override fun updateInfo() {
            keyEvents.keys.forEach(this::action)
        }

        private fun action(keyCode: Int) {
            if (PlayerManager.binds[keyCode]) {
                this::class.logger().log(System.Logger.Level.INFO, "Press key: $keyCode")
                keyEvents[keyCode]?.invoke(this)
                PlayerManager.binds[keyCode] = false
            }
        }

        protected open fun nullAction() {
            when (select) {
                3 -> exitProcess(0)
                0, 1, 2 -> Test.start(0)
                else -> { /* do nothing */
                }
            }
        }

        companion object {
            val keyEvents: MutableMap<Int, (MainMenuItem) -> Unit> = mutableMapOf(
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
                        it.nullAction()
                },
                KeyEvent.VK_X to {
                    val root = it.parent
                    if (root != null)
                        it.menu.curLevel = root
                }
            )
        }
    }

    class PlayerSelectMenuItem(x: Int, y: Int, menu: Menu, selectedTextures: ArrayDeque<Texture>) : MainMenuItem(
        x, y, menu,
        selectedTextures
    ) {
        override fun paint(g: Graphics2D) {
            items.forEachIndexed { index, item ->
                if (index == select) {
                    val me = selectedTextures[index]
                    me.paint(g, me.normalMatrix(), 0, y)
                    item.paint(g, item.normalMatrix(), x, y)
                }
            }
        }

        override fun nullAction() {
            when (select) {
                0 -> Test.start(0)
                else -> { /* do nothing */
                }
            }
        }
    }
}