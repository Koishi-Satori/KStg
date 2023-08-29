package top.kkoishi.stg.test.common.render

import top.kkoishi.stg.common.AnimatedRenderableObject

class TestStageClearObject(initialX: Int, initialY: Int) :
    AnimatedRenderableObject(initialX, initialY, 1000, "stage_clear") {
    override fun texture(curFrame: Long): String = "stage_clear"

    override fun transform(curFrame: Long) {
    }
}