package top.kkoishi.stg.script

import top.kkoishi.stg.DefinitionsLoader
import top.kkoishi.stg.common.Dialogs
import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.localization.Localization
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.script.execution.contentIterator
import top.kkoishi.stg.script.execution.processEscapes
import java.awt.Graphics2D
import java.io.File
import java.io.FileNotFoundException
import java.lang.StringBuilder
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class DialogsLoader @JvmOverloads constructor(dir: String, private val separatorChar: Char = ',') :
    DefinitionsLoader {

    private val root = Path.of(dir)

    var localization: Localization<String, String>? = null

    init {
        if (!root.exists())
            throw FileNotFoundException(root.absolutePathString())
        if (root.isRegularFile())
            throw ScriptException("${root.absolutePathString()} should be directory")
    }

    private val logger = DialogsLoader::class.logger()

    fun localization(localization: Localization<String, String>): DialogsLoader {
        this.localization = localization
        return this
    }

    override fun loadDefinitions() {
        logger.log(System.Logger.Level.INFO, "Load dialogs from scripts.")
        for (path in root.toFile().listFiles()!!) {
            if (path.isFile)
                try {
                    logger.log(System.Logger.Level.INFO, "Try to load dialogs from $path")
                    DefinitionsLoader.register(path.canonicalPath.toString())
                    load(path)
                    logger.log(System.Logger.Level.INFO, "Success to load all the dialogs from $path")
                } catch (e: Exception) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
        }
    }

    private fun load(csv: File) {
        val localization = this.localization
        val rest = CSVAnalysis(csv.contentIterator())
        val name = rest.next().value()
        val background = rest.next().value()
        val messageX = rest.next().num()
        val messageY = rest.next().num()
        val x = rest.next().num()
        val y = rest.next().num()
        val dialogs = ArrayDeque<Dialogs.Dialog>()
        var index = 0

        while (rest.hasNext()) {
            var message = rest.next().value()
            val amount = rest.next().num()
            val faces = ArrayDeque<Dialogs.Face>(amount)
            (0 until amount).forEach { _ ->
                val face = rest.next().value()
                val faceX = rest.next().num()
                val faceY = rest.next().num()
                val state = Dialogs.FaceState.valueOf(rest.next().value())
                faces.addLast(Dialogs.Face(face, faceX, faceY, state))
            }

            if (localization != null) {
                message = localization[message] ?: "${message}_value"
            }
            val dialog = Dialogs.Dialog(faces, message.processEscapes())
            DialogsLoader::class.logger().log(System.Logger.Level.INFO, "CSV Dialog: ${index++} -> $dialog")
            dialogs.addLast(dialog)
        }

        Dialogs.addDialog(name, ScriptedDialogs(dialogs, messageX, messageY, x, y, background))
    }

    private data class CSVItem(val value: Any, val isNumber: Boolean = false) {
        fun num(): Int = if (isNumber)
            value as Int
        else
            throw ClassCastException(value())

        fun value() = value.toString()
    }

    private inner class CSVAnalysis(val rest: CharIterator) : Iterator<CSVItem> {
        var lookup = '\u0001'

        override fun hasNext(): Boolean = rest.hasNext()

        override fun next(): CSVItem {
            if (rest.hasNext())
                nextChar()
            else
                return NULL
            if (lookup == separatorChar || lookup == '\n')
                return next()
            return if (lookup.isDigit()) {
                number()
            } else
                string()
        }

        private fun number(): CSVItem {
            val buf = StringBuilder()
            while (true) {
                if (lookup == separatorChar || !lookup.isDigit())
                    break
                buf.append(lookup)
                if (rest.hasNext())
                    nextChar()
                else
                    break
            }
            return CSVItem(buf.toString().toInt(), true)
        }

        private fun string(): CSVItem {
            val buf = StringBuilder()
            while (true) {
                if (lookup == separatorChar || lookup == '\n' || lookup == '\r')
                    break
                buf.append(lookup)
                if (rest.hasNext())
                    nextChar()
                else
                    break
            }
            return CSVItem(buf.toString(), true)
        }

        private fun nextChar() {
            lookup = rest.nextChar()
        }
    }

    private class ScriptedDialogs(
        dialogs: ArrayDeque<Dialog>,
        messageX: Int,
        messageY: Int,
        initialX: Int,
        initialY: Int,
        val background: String,
    ) :
        Dialogs(dialogs, messageX, messageY, initialX, initialY) {
        override fun paintBackground(g: Graphics2D) {
            val texture = GFX[background]
            texture.paint(g, texture.normalMatrix(), x().toInt(), y().toInt())
        }
    }

    companion object {
        @JvmStatic
        private val NULL = CSVItem(Any())
    }
}