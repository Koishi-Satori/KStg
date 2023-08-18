package top.kkoishi.stg.exceptions

import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.PrintStream
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.swing.JFrame
import javax.swing.JTextArea
import kotlin.io.path.*
import kotlin.system.exitProcess

class CrashReporter @JvmOverloads constructor(
    title: String,
    private val logFile: Path = Path.of(
        "./crash_logs/${
            SimpleDateFormat("'crash_report-'yyyy-MM-dd_HH.mm.ss'.log'").format(
                Date.from(Instant.now())
            )
        }"
    ),
) : JFrame(title) {
    init {
        val dir = Path.of("./crash_logs")
        if (!dir.exists())
            dir.createDirectory()
        if (!logFile.exists())
            logFile.createFile()
    }

    private val out = PrintStream(logFile.outputStream(), false, Charsets.UTF_8)
    private val area = JTextArea()

    init {
        setSize(320, 180)
        area.size = Dimension(160, 90)
        area.autoscrolls = true
        add(area)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                exitProcess(1)
            }
        })
    }

    @JvmOverloads
    fun report(crashReport: String, displayText: String = crashReport) {
        out.println(crashReport)
        out.flush()
        out.close()

        area.text = "$displayText\nCrash Report has been saved as ${logFile.toAbsolutePath()}"
        isResizable = false
        isVisible = true
    }
}