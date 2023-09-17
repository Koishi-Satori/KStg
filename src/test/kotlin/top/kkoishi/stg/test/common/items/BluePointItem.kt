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

package top.kkoishi.stg.test.common.items

import top.kkoishi.stg.common.entities.BaseItem
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.test.common.GameSystem
import java.awt.Point
import java.awt.Shape

class BluePointItem(initialX: Int, initialY: Int, speed: Double = 3.0) : BaseItem(initialX, initialY, speed) {
    override fun texture(): String = "item_bluepoint"

    override fun getItem(player: Player) {
        GameSystem.sideBar.add(1000L)
    }

    override fun shape(): Shape = CollideSystem.Circle(Point(x(), y()), 5)
}