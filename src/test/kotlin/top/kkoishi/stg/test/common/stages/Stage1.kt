package top.kkoishi.stg.test.common.stages

import top.kkoishi.stg.common.AbstractStage
import top.kkoishi.stg.common.Stage

class Stage1: AbstractStage() {
    override fun backgroundName(): String = "bg_1_0"

    override fun nextStageImpl(): Stage? = null

    override fun toNextStage(): Boolean = false
}