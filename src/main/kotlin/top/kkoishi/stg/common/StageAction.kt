package top.kkoishi.stg.common

open class StageAction(
    private var beforeDelay: Long,
    val action: (AbstractStage) -> Unit,
) {
    open fun canAction(): Boolean = beforeDelay-- <= 0

    open operator fun invoke(stage: AbstractStage) = action(stage)
}