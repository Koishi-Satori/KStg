package top.kkoishi.stg.common

import top.kkoishi.stg.logic.ObjectPool

/**
 * StageAction class determines the behalf of a [Stage], and before executing the action, it will wait
 * for [beforeDelay] in logical frames
 *
 * @param beforeDelay the delay before the action is invoked.
 * @param action the action to be invoked.
 * @author KKoishi_
 */
open class StageAction(
    private var beforeDelay: Long,
    val action: (AbstractStage) -> Unit,
) {
    open fun canAction(): Boolean = beforeDelay-- <= 0

    open operator fun invoke(stage: AbstractStage) = action(stage)
}

/**
 * This class will execute the logic in [StageAction] after all enemy objects in the [ObjectPool] disappear,
 * and equals to a class which extends StageAction and overrides the [canAction] method in next way:
 *
 * ```kotlin
 * override fun canAction(): Boolean {
 *         if (ObjectPool.objects().hasNext())
 *             return false
 *         return super.canAction()
 *     }
 * ```
 *
 * @author KKoishi_
 */
class WaitStageAction(beforeDelay: Long, action: (AbstractStage) -> Unit) : StageAction(beforeDelay, action) {
    override fun canAction(): Boolean {
        if (ObjectPool.objects().hasNext())
            return false
        return super.canAction()
    }
}