package top.kkoishi.stg.exceptions

import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.io.RandomAccessFile
import java.nio.file.Path
import kotlin.Exception
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

class ThreadExceptionHandler private constructor() : Thread.UncaughtExceptionHandler {
    private var handleProcessPid: Long = -1

    init {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                if (CrashReporter.reportCount.get() == 0L) {
                    val bin = RandomAccessFile("${CrashReporter.reportDir}/crash_logs/state.bin", "rw")
                    try {
                        val lock = bin.channel.tryLock()
                        bin.setLength(0)
                        lock.release()
                    } catch (e: Exception) {
                        bin.setLength(0)
                        CrashReporter::class.logger().log(System.Logger.Level.WARNING, e)
                    } finally {
                        bin.close()
                    }
                    CrashReporter::class.logger().log(System.Logger.Level.INFO, "Bin file cleared.")
                }
            }
        })
    }

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (t != null && e != null) {
            synchronized(lock) {
                handlers.forEach {
                    when (it(e)) {
                        HANDLE_LEVEL_OFF -> {}
                        HANDLE_LEVEL_LOG ->
                            ThreadExceptionHandler::class.logger().log(System.Logger.Level.WARNING, e)

                        HANDLE_LEVEL_CRASH -> {
                            if (handleProcessPid < 0)
                                ThreadExceptionHandler::class.logger()
                                    .log(System.Logger.Level.WARNING, "No CrashHandler Provided!")
                            val generator = CrashReportGenerator()
                            generator.description(it.description)
                            CrashReporter().report(generator.generate(e))
                            exitProcess(CrashReporter.EXIT_CRASH)
                        }
                    }
                }
            }

            ThreadExceptionHandler::class.logger().log(System.Logger.Level.WARNING, e)
        }
    }

    private fun setCommand(crashHandlerCommand: ProcessBuilder) {
        try {
            val oldCommands = crashHandlerCommand.command()
            val newCommands = ArrayList<String>(oldCommands.size + 2)
            newCommands.addAll(oldCommands)
            newCommands.add(Path.of(CrashReporter.reportDir).absolutePathString())
            newCommands.add(ProcessHandle.current().pid().toString())
            crashHandlerCommand.command(newCommands)
            ThreadExceptionHandler::class.logger()
                .log(System.Logger.Level.INFO, "Crash Handler Command: ${crashHandlerCommand.command()}")

            val process = crashHandlerCommand.start()
            handleProcessPid = process.toHandle().pid()
            Thread.setDefaultUncaughtExceptionHandler(this)
            ThreadExceptionHandler::class.logger()
                .log(System.Logger.Level.INFO, "Set Crash Handler, pid: $handleProcessPid")
        } catch (e: Exception) {
            ThreadExceptionHandler::class.logger().log(System.Logger.Level.WARNING, e)
        }
    }

    /**
     * This is used to determine how to handle the exception, and the returned value of [handleLevel] should be one of
     * [ThreadExceptionHandler.HANDLE_LEVEL_OFF], [ThreadExceptionHandler.HANDLE_LEVEL_CRASH]
     * and [ThreadExceptionHandler.HANDLE_LEVEL_LOG].
     *
     * The [description] is used to describe the crash reason.
     *
     * @author KKoishi_
     */
    data class HandleInfo(val description: String, val handleLevel: (Throwable) -> Int) {
        operator fun invoke(r: Throwable) = handleLevel(r)
    }

    companion object {
        const val HANDLE_LEVEL_OFF = 0
        const val HANDLE_LEVEL_CRASH = 1
        const val HANDLE_LEVEL_LOG = 2

        @JvmStatic
        private val inst = ThreadExceptionHandler()

        @JvmStatic
        private val lock = Any()

        @JvmStatic
        private val handlers = ArrayDeque<HandleInfo>(4)

        /**
         * Initialize the Crash Handler and the ThreadExceptionHandler.
         *
         * This method should invoke only once.
         *
         * # Notice
         * ## The pid of current process will be passed to the crash handler through the program arguments!
         * ## So please handle the argument correctly, or just use the template code in KStg.CrashHandler Module.
         * ### The pid is provided as the last argument.
         * ### Before the pid is [CrashReporter.reportDir].
         *
         *
         * @param crashHandlerCommand the command of the crash handler.
         */
        @JvmStatic
        fun initialize(crashHandlerCommand: ProcessBuilder) {
            inst.setCommand(crashHandlerCommand)
        }

        @JvmStatic
        fun addHandler(info: HandleInfo) {
            synchronized(handlers) {
                handlers.addLast(info)
            }
        }
    }
}
