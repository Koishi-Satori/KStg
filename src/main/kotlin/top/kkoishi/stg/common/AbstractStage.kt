package top.kkoishi.stg.common

import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.GameLoop

abstract class AbstractStage(initialAmount: Int = 32) : Stage() {
    protected var toNext: Boolean = false
    protected val actionsList: ArrayDeque<Pair<Long, () -> Unit>> = ArrayDeque(initialAmount)
    protected val beginFrame: Long = GameLoop.logicFrame()

    abstract fun backgroundName(): String

    override fun background(): Texture = GFX.getTexture(backgroundName())

    override fun action() {
        while (actionsList.isNotEmpty()) {
            val action = actionsList.first()
            if (action.first <= (GameLoop.logicFrame() - beginFrame)) {
                actionsList.removeFirst()
                action.second()
            } else
                break
        }
    }

    fun addAction() {
        TODO()
    }

    override fun toNextStage(): Boolean = toNext

    override fun nextStage(): Stage {
        TODO("Not yet implemented")
    }
}