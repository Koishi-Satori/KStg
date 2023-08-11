package top.kkoishi.stg.test.common.stages

import top.kkoishi.stg.common.Stage
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Texture

class Stage1: Stage() {
    override fun background(): Texture = GFX.getTexture("bg_1_0")

    override fun action() {}

    override fun toNextStage(): Boolean = false

    override fun nextStage(): Stage {
        TODO("Not yet implemented")
    }
}