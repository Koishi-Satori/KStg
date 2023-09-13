/*
 *
 * This file is only used for testing, and there is no need for you to add this
 * module "KStg.test" when build this engine.
 *
 *
 * Some resources for art and sound in this test module are from a touhou STG game
 * which called "东方夏夜祭", for the author is in lack of synthesizing music and
 * game painting. :(
 *
 *
 *
 */

package top.kkoishi.stg.test.common.ui

import top.kkoishi.stg.common.ui.BaseMenu
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.GenericSystem
import top.kkoishi.stg.test.Test
import top.kkoishi.stg.test.common.GameSystem
import java.awt.Graphics2D
import java.awt.event.KeyEvent


class PauseMenu : BaseMenu(GenericSystem.STATE_PAUSE) {
    override fun initKeyEvents() {
        super.initKeyEvents()
        keyEvents[KeyEvent.VK_ESCAPE] = {
            GenericSystem.gameState.set(GenericSystem.STATE_PLAYING)
        }
    }

    open class PauseMenuItem(x: Int, y: Int, menu: BaseMenu, selectedTextures: ArrayDeque<Texture>) :
        AbstractMenuItem(x, y, menu, selectedTextures) {
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

        override fun invoke() {
            when (select) {
                0 -> GenericSystem.gameState.set(GenericSystem.STATE_PLAYING)
                1 -> Test.mainMenu()
                2 -> {
                    Test.mainMenu()
                    Test.start(GameSystem.playerIndex)
                }

                else -> { /* do nothing */
                }
            }
        }

        override fun shouldRender(): Boolean = GenericSystem.gameState.get() == GenericSystem.STATE_PAUSE
    }

    override fun paintBackground(r: Graphics2D) {}
}