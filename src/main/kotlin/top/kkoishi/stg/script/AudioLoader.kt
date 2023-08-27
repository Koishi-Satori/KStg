package top.kkoishi.stg.script

import top.kkoishi.stg.Loader
import top.kkoishi.stg.Loader.Companion.register
import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.script.execution.*
import top.kkoishi.stg.script.execution.InfoParser
import top.kkoishi.stg.script.execution.InstructionSequence
import top.kkoishi.stg.script.execution.Lexer
import top.kkoishi.stg.script.execution.ParseInfo
import top.kkoishi.stg.script.execution.ReaderIterator
import top.kkoishi.stg.script.execution.ResourcesInstructions
import top.kkoishi.stg.script.execution.ResourcesScriptLexer
import top.kkoishi.stg.script.execution.ScriptConstants.SCOPE_AUDIO_LOADER
import top.kkoishi.stg.script.execution.Type
import java.io.File
import java.nio.file.Path

class AudioLoader(private val root: Path) : LocalVariables(SCOPE_AUDIO_LOADER), Loader {
    init {
        LocalVariables[scopeName] = this
    }

    constructor(root: String) : this(Path.of(root)) {
        LocalVariables[scopeName] = this
    }

    private val logger = AudioLoader::class.logger()
    override fun loadDefinitions() {
        logger.log(System.Logger.Level.INFO, "Load audios from scripts.")
        for (path in root.toFile().listFiles()!!) {
            if (path.isFile)
                try {
                    logger.log(System.Logger.Level.INFO, "Try to audios textures from $path")
                    register(path.canonicalPath.toString())
                    loadDefinitionFile(path)
                    logger.log(System.Logger.Level.INFO, "Success to load all the audios from $path")
                } catch (e: Exception) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
        }
    }

    private fun loadDefinitionFile(path: File) {
        val rest = ReaderIterator(path.bufferedReader())
        val lexer = ResourcesScriptLexer(rest)
        val parser = AudioScriptParser(lexer)
        val instructions = parser.parse()

        for (it in instructions) {
            try {
                if (it.needVars()) it.invoke(this@AudioLoader) else it.invoke()
            } catch (e: Exception) {
                logger.log(System.Logger.Level.ERROR, e)
            }
        }
        rest.close()
    }

    private inner class AudioScriptParser(lexer: Lexer) : InfoParser(lexer, "sounds", scopeName) {
        override fun buildParseInfo() {
            root.add(ResourcesInstructions.LoopInfo())
            root.add(audioInfo())
        }

        override fun parseComplexParameter(name: String): InstructionSequence {
            if (name == "load") {
                check(Type.L_BLANKET_L)
                val instructions = parseInstructions()
                check(Type.L_BLANKET_R)
                return instructions
            }
            throw ScriptException()
        }
    }

    private inner class audio(private val name: String, private val path: String) : VM.Instruction(0x24) {
        override fun needVars(): Boolean = false
        override fun invoke() {
            Sounds[name] = "$root/$path"
        }
    }

    private inner class audioInfo : ParseInfo.InstructionInfo("audio") {
        init {
            add(ParseInfo.CommonParameterInfo("name"))
            add(ParseInfo.CommonParameterInfo("path"))
        }

        override fun allocate(parameters: Map<String, Any>): VM.Instruction {
            return audio(parameters["name"]!!.toString(), parameters["path"]!!.toString())
        }
    }
}