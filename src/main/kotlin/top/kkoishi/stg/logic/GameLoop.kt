package top.kkoishi.stg.logic

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.coordinatespace.SubChunks
import top.kkoishi.stg.logic.keys.KeyBinds
import top.kkoishi.stg.util.Options
import top.kkoishi.stg.util.Timer
import java.util.concurrent.atomic.AtomicLong

/**
 * Control the logic of the game.
 *
 * For the four state of the game, GameLoop class has different way of processing, and if the state is not
 * one of them, nothing will ve invoked.
 *
 * * [GenericSystem.STATE_PLAYING] - update the player logic, then the stage and ui, finally will be bullets
 * and enemies.
 * * [GenericSystem.STATE_PAUSE] - only ui will be updated.
 * * [GenericSystem.STATE_MENU] - same as pause state.
 * * [GenericSystem.STATE_LOADING] - only [ObjectPool.loadingContent].
 *
 * If you want to start this thread, please use the method [GameLoop.start].
 *
 * Also, you can use [addTask] method to add tasks, which will be invoked after the main logic is executed.
 *
 * @author KKoishi_
 */
class GameLoop private constructor() : Runnable {
    private val logicFrame = AtomicLong(0L)
    private val tasks = ArrayDeque<Task>(8)
    private val lock = Any()

    /**
     * upgrade the game states.
     */
    fun update() {
        val logger = GameLoop::class.logger()
        when (GenericSystem.gameState.get()) {
            GenericSystem.STATE_PLAYING -> {
                try {
                    // update player logic first.
                    ObjectPool.player().update()

                    // update the stage logic.
                    // if current should be updated to next stage, then invoke Stage::nextStage and cur will be
                    // set to the next stage.
                    // finally, invoke Stage::action method of cur.
                    var cur = PlayerManager.curStage
                    if (cur.toNextStage()) {
                        cur = cur.nextStage()
                        PlayerManager.curStage = cur
                    }
                    cur.action()

                    // update the UIObjects like SideBar.
                    ObjectPool.uiObjects().forEach { it.update() }

                    // update the state of bullets and objects.
                    var index = 0
                    for (o in ObjectPool.objects()) {
                        if (o.update())
                            ObjectPool.removeObject(index--)
                        ++index
                    }

                    index = 0
                    for (b in ObjectPool.bullets()) {
                        if (b.update())
                            ObjectPool.removeBullet(index--)
                        ++index
                    }

                    // reindex the sub-chunks of the virtual space calculated from the screen space.
                    // this can be used for optimizing the performance.
                    SubChunks.updateSpace()
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            GenericSystem.STATE_PAUSE -> {
                // menu logic only.
                try {
                    ObjectPool.uiObjects().forEach { it.update() }
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            GenericSystem.STATE_MENU -> {
                // menu logic only.
                try {
                    ObjectPool.uiObjects().forEach { it.update() }
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            GenericSystem.STATE_LOADING -> {
                // loading content
                try {
                    ObjectPool.loadingContents().forEach { it.update() }
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            else -> {
                logger.log(System.Logger.Level.ERROR, "Error game state!")
            }
        }

        // invoke the methods of Generic Key Binds.
        KeyBinds.invokeGenericBinds()

        // update the submitted tasks from user threads.
        val curFrame = logicFrame.get()
        synchronized(lock) {
            val removeElements = ArrayDeque<Task>(tasks.size)
            tasks.forEachIndexed { index, it ->
                if (it(curFrame)) {
                    if (Options.State.debug)
                        logger.log(System.Logger.Level.DEBUG, "Task$index is ended, task=$it")
                    removeElements.addLast(it)
                }
            }

            while (removeElements.isNotEmpty()) {
                tasks.remove(removeElements.removeFirst())
            }
        }

        logicFrame.getAndIncrement()
    }

    override fun run() {
        update()
    }

    private fun buildTask(
        beginFrame: Long,
        timer: Timer,
        shouldEnd: (Long, Long, Long) -> Boolean,
        action: () -> Unit,
        exceptionHandler: (Throwable) -> Unit,
    ) = Task(beginFrame, timer, shouldEnd, action, exceptionHandler)

    private fun addTask(task: Task) {
        synchronized(lock) {
            tasks.addLast(task)
        }
    }

    private inner class Task(
        private val beginFrame: Long,
        private val timer: Timer,
        private val shouldEnd: (Long, Long, Long) -> Boolean,
        private val action: () -> Unit,
        private val exceptionHandler: (Throwable) -> Unit,
    ) {
        private var invokeCount = 0L

        operator fun invoke(curFrame: Long): Boolean {
            if (timer.end()) {
                ++invokeCount
                try {
                    action()
                } catch (e: Throwable) {
                    exceptionHandler(e)
                }
                if (timer !is Timer.Loop)
                    return true
                return shouldEnd(beginFrame, curFrame, invokeCount)
            }
            return false
        }

        override fun toString(): String = "Task(beginFrame=$beginFrame, timer=$timer, invokeCount=$invokeCount)"
    }

    @Suppress("UNUSED_PARAMETER", "FunctionName", "unused")
    companion object {
        @JvmStatic
        private val instance = GameLoop()

        @JvmStatic
        private fun DEFAULT_SHOULD_END(p1: Long, p2: Long, p3: Long) = false

        @JvmStatic
        private fun DEFAULT_EXCEPTION_HANDLE(t: Throwable) {
        }

        /**
         * Start the GameLoop.
         */
        @JvmStatic
        fun start(threads: Threads) {
            threads.schedule(instance, Threads.period(), 1L)
        }

        @JvmStatic
        fun logicFrame() = instance.logicFrame.get()

        @JvmStatic
        private fun addTaskImpl(
            timer: Timer,
            shouldEnd: (Long, Long, Long) -> Boolean,
            action: () -> Unit,
            exceptionHandler: (Throwable) -> Unit,
        ) {
            val curFrame = logicFrame()
            instance.addTask(instance.buildTask(curFrame, timer, shouldEnd, action, exceptionHandler))
        }

        @JvmStatic
        fun addTask(timer: Timer, action: () -> Unit) =
            addTaskImpl(timer, this::DEFAULT_SHOULD_END, action, this::DEFAULT_EXCEPTION_HANDLE)

        @JvmStatic
        fun addTask(timer: Timer.Loop, shouldEnd: (Long, Long, Long) -> Boolean, action: () -> Unit) =
            addTask(timer, shouldEnd, action, this::DEFAULT_EXCEPTION_HANDLE)

        @JvmStatic
        fun addTask(timer: Timer, action: () -> Unit, exceptionHandler: (Throwable) -> Unit) =
            addTaskImpl(timer, this::DEFAULT_SHOULD_END, action, exceptionHandler)

        @JvmStatic
        fun addTask(
            timer: Timer.Loop,
            shouldEnd: (Long, Long, Long) -> Boolean,
            action: () -> Unit,
            exceptionHandler: (Throwable) -> Unit,
        ) = addTaskImpl(timer, shouldEnd, action, exceptionHandler)

        @JvmStatic
        fun addTask(interval: Long, times: Long, action: () -> Unit, exceptionHandler: (Throwable) -> Unit) =
            addTaskImpl(
                buildLoopTimer(interval),
                buildShouldEnd(times),
                action,
                exceptionHandler
            )

        @JvmStatic
        private fun buildLoopTimer(interval: Long): Timer = Timer.Loop(interval)

        private fun buildShouldEnd(times: Long): (Long, Long, Long) -> Boolean =
            { _: Long, _: Long, invokeCount: Long -> times >= invokeCount }
    }
}