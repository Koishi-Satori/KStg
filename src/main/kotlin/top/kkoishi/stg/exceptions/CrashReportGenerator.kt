package top.kkoishi.stg.exceptions

import top.kkoishi.stg.DefinitionsLoader
import top.kkoishi.stg.Resources
import java.lang.management.ManagementFactory
import java.text.DateFormat
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.abs
import kotlin.random.Random

@Suppress("MemberVisibilityCanBePrivate", "unused")
class CrashReportGenerator @JvmOverloads constructor(val head: String = DEFAULT_HEAD) {
    private val time: String = DateFormat.getDateTimeInstance().format(Date.from(Instant.now()))
    private var description: String = "none"
    var implementationVersion: String = "none"
    private var extraInfo = "none"

    fun description(description: String): String {
        val old = this.description
        this.description = description
        return old
    }

    fun extraInfo(info: String) {
        extraInfo = info
    }

    fun generate(r: Throwable): String {
        val buffer = StringBuilder()
        buffer.append(head).append("\n// ").append(randomComment()).append("\n\nEngine Version: ")
            .append(engineVersion()).append("\nImplementation Version: ").append(implementationVersion)
            .append("\nTime: ").append(time)
            .append("\nDescription: ").append(description).append("\n\n")
            .append("Message: ").append(r.message)

        // print stack traces
        buffer.append("\nStack Trace: \n").append(r.stackTraceToString()).append("\nLoaded Scripts: ")

        val restNames = DefinitionsLoader.scriptNames()
        while (restNames.hasNext()) {
            buffer.append(restNames.next())
            if (!restNames.hasNext())
                break
            buffer.append(", ")
        }

        buffer.append("\nDetails: \n")
        getOSInfo(buffer)
        getJVMInfo(buffer)
        getJITInfo(buffer)
        buffer.append("\nExtra Info: \n").append(extraInfo)

        return buffer.toString()
    }

    private fun randomComment(): String {
        val comments = comments()
        return comments[abs(Random(System.currentTimeMillis()).nextInt()) % comments.size]
    }

    companion object {
        @JvmStatic
        val DEFAULT_HEAD = "---- Engine Internal Report ----"

        @JvmStatic
        private val properties = System.getProperties()

        @JvmStatic
        private val comments: ArrayDeque<String> = ArrayDeque(16)

        init {
            val br = Resources.getEngineResources()!!.bufferedReader()
            br.lineSequence().forEach(::addComment)
            br.close()
            if (comments.isEmpty())
                addComment("114514")
        }

        @JvmStatic
        fun comments(): Array<String> = synchronized(comments) {
            return comments.toTypedArray()
        }

        @JvmStatic
        fun addComment(comment: String) = synchronized(comments) {
            comments.addLast(comment)
        }

        @JvmStatic
        private fun engineVersion(): String = "1.0-preview"

        @JvmStatic
        private fun getOSInfo(buffer: StringBuilder) {
            val mxBean = ManagementFactory.getOperatingSystemMXBean()
            buffer.append("\tOperating System: ").append(mxBean.name).append(" (").append(mxBean.arch).append(") ")
                .append(mxBean.version)
                .append("\n\tCPUs: ").append(mxBean.availableProcessors).append('\n')
        }

        @JvmStatic
        private fun getJVMInfo(buffer: StringBuilder) {
            val mxBean = ManagementFactory.getRuntimeMXBean()
            val r = Runtime.getRuntime()
            buffer.append("\tProcess Arguments: ").append(mxBean.inputArguments)
                .append("\n\tClass Path: ").append(mxBean.classPath)
                .append("\n\tProcess ID: ").append(mxBean.name)
                .append("\n\tJava Version: ").append(properties.getProperty("java.version"))
                .append("\n\tJava VM Version: ").append(mxBean.vmName).append(' ').append(mxBean.vmVendor).append(' ')
                .append(mxBean.vmVersion)
                .append("\n\tJVM uptime(ms): ").append(mxBean.uptime)
                .append("\n\tJVM Execute Mode: ").append(properties.getProperty("java.vm.info"))
                .append("\n\tJVM Free Memory: ").append(r.freeMemory())
                .append("\n\tJVM Total Memory: ").append(r.totalMemory())
                .append("\n\tJVM Max Memory: ").append(r.maxMemory())
                .append("\n\tGPU Acceleration: ").append(properties.getProperty("sun.java2d.opengl") == "true")
                .append("\n\tAnti-aliasing: ").append(properties.getProperty("swing.aatext") == "true")
                .append("\n\tNative Double Buffering: ")
                .append(properties.getProperty("awt.nativeDoubleBuffering") == "true")
        }

        @JvmStatic
        private fun getJITInfo(buffer: StringBuilder) {
            val mxBean = ManagementFactory.getCompilationMXBean()
            if (mxBean == null)
                buffer.append("\n\tJIT Compiler: No Compiler")
            else
                buffer.append("\n\tJIT Compiler: ").append(mxBean.name)
        }
    }
}