package top.kkoishi.stg.test.common.ui

import top.kkoishi.stg.common.ui.BaseMenu
import top.kkoishi.stg.exceptions.CrashReporter
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.GenericSystem
import top.kkoishi.stg.logic.SingleInstanceEnsurer
import top.kkoishi.stg.test.Test
import java.awt.Graphics2D
import kotlin.system.exitProcess

class MainMenu : BaseMenu(GenericSystem.STATE_MENU) {
    private val bg = GFX.getTexture("menu_bg")

    override fun paintBackground(r: Graphics2D) {
        bg.paint(r, bg.normalMatrix(), x, y)
    }

    open class MainMenuItem(x: Int, y: Int, menu: BaseMenu, selectedTextures: ArrayDeque<Texture>) :
        AbstractMenuItem(x, y, menu, selectedTextures) {

        // 灵梦白丝
        private val baisi = GFX.getTexture("reimu_baisi")
        override fun paint(g: Graphics2D) {
            baisi.paint(g, baisi.alphaConvolve(0.5f), 0, 0)
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
                3 -> {
                    SingleInstanceEnsurer.release()
                    exitProcess(CrashReporter.EXIT_OK)
                }

                0, 1, 2 -> Test.start(0)
                else -> { /* do nothing */
                }
            }
        }
    }

    class PlayerSelectMenuItem(
        x: Int,
        y: Int,
        menu: BaseMenu,
        selectedTextures: ArrayDeque<Texture>,
    ) : MainMenuItem(
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

        override fun invoke() {
            when (select) {
                0 -> Test.start(0)
                else -> { /* do nothing */
                }
            }
        }
    }
}