package top.kkoishi.stg.exceptions

import top.kkoishi.stg.logic.Threads
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.*
import kotlin.system.exitProcess

class CrashReporter {
    private val logFile: Path = Path.of(
        "$reportDir/crash_logs/${
            SimpleDateFormat("'crash_report-'yyyy-MM-dd_HH.mm.ss'.log'").format(
                Date.from(Instant.now())
            )
        }"
    )

    init {
        val dir = Path.of("$reportDir/crash_logs")
        if (!dir.exists())
            dir.createDirectory()
        if (!logFile.exists())
            logFile.createFile()
    }

    private val out = PrintStream(logFile.outputStream(), false, Charsets.UTF_8)

    fun report(crashReport: String, exitState: Int = EXIT_CRASH) {
        reportCount.incrementAndGet()
        out.println(crashReport)
        out.flush()
        out.close()

        val state = Path.of("$reportDir/crash_logs/state.bin")
        if (!state.exists())
            state.createFile()
        with(FileOutputStream(state.toFile(), false)) {
            write(exitState ushr 24 and 0xFF)
            write(exitState ushr 16 and 0xFF)
            write(exitState ushr 8 and 0xFF)
            write(exitState ushr 0 and 0xFF)
            val bytes = logFile.toAbsolutePath().toString().toByteArray()
            val len = bytes.size
            write(len ushr 24 and 0xFF)
            write(len ushr 16 and 0xFF)
            write(len ushr 8 and 0xFF)
            write(len ushr 0 and 0xFF)
            write(bytes)
            flush()
            close()
        }

        if (exitState == EXIT_CRASH)
            exitProcess(EXIT_CRASH)
    }

    companion object {
        const val EXIT_OK = 0
        const val EXIT_CRASH = 1
        const val EXIT_WARNING = 2

        @JvmStatic
        val reportCount: AtomicLong = AtomicLong(0)

        @JvmStatic
        private val lock = Any()

        @JvmStatic
        var reportDir = Threads.workdir()
            get() {
                synchronized(lock) {
                    return field
                }
            }
            set(value) {
                synchronized(lock) {
                    field = value
                }
            }
    }
}