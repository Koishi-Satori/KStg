package top.kkoishi.stg.test.common.ui

import top.kkoishi.stg.common.ui.Menu
import top.kkoishi.stg.common.ui.MenuItem
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.GenericSystem
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.PlayerManager
import top.kkoishi.stg.test.Test
import top.kkoishi.stg.test.common.GameSystem
import java.awt.Graphics2D
import java.awt.event.KeyEvent


class PauseMenu : Menu(GenericSystem.STATE_PAUSE) {
    open class PauseMenuItem(x: Int, y: Int, menu: Menu, private val selectedTextures: ArrayDeque<Texture>) :
        MenuItem(x, y, menu) {
        override fun paint(g: Graphics2D) {
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
            keyEvents.keys.forEach { action(it) }
        }

        private fun action(keyCode: Int) {
            if (PlayerManager.binds[keyCode]) {
                this::class.logger().log(System.Logger.Level.INFO, "Press key: $keyCode")
                keyEvents[keyCode]?.invoke(this)
                PlayerManager.binds[keyCode] = false
            }
        }

        open fun nullAction() {
            when (select) {
                0 -> GenericSystem.gameState.set(GenericSystem.STATE_PLAYING)
                1 -> Test.menu()
                2 -> {
                    Test.menu()
                    Test.start(GameSystem.playerIndex)
                }

                else -> { /* do nothing */
                }
            }
        }

        override fun shouldRender(): Boolean = GenericSystem.gameState.get() == GenericSystem.STATE_PAUSE
    }

    override fun paintBackground(r: Graphics2D) {}

    companion object {
        val keyEvents: MutableMap<Int, (PauseMenuItem) -> Unit> = mutableMapOf(
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
            },
            KeyEvent.VK_ESCAPE to {
                GenericSystem.gameState.set(GenericSystem.STATE_PLAYING)
            }
        )
    }
}