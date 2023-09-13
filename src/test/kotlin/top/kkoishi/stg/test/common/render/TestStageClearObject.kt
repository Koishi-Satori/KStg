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

package top.kkoishi.stg.test.common.render

import top.kkoishi.stg.common.AnimatedRenderableObject

class TestStageClearObject(initialX: Int, initialY: Int) :
    AnimatedRenderableObject(initialX, initialY, 1000, "stage_clear") {
    override fun texture(curFrame: Long): String = "stage_clear"

    override fun transform(curFrame: Long) {
    }
}