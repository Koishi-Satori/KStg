package top.kkoishi.stg.common

import top.kkoishi.stg.exceptions.InternalError
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Texture
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractStage(initialAmount: Int = 32) : Stage() {
    val toNext: AtomicBoolean = AtomicBoolean(false)
    protected val actionsList: ArrayDeque<StageAction> = ArrayDeque(initialAmount)

    abstract fun backgroundName(): String

    abstract fun nextStageImpl(): Stage?

    override fun background(): Texture = GFX.getTexture(backgroundName())

    override fun action() {
        synchronized(actionsList) {
            while (actionsList.isNotEmpty()) {
                val action = actionsList.first()
                if (action.canAction()) {
                    actionsList.removeFirst()
                    action(this)
                } else
                    break
            }
        }
    }

    fun addAction(action: StageAction) = synchronized(action) {
        actionsList.addLast(action)
    }

    override fun toNextStage(): Boolean = toNext.get()

    override fun nextStage(): Stage = nextStageImpl() ?: throw InternalError("No next stage.")
}