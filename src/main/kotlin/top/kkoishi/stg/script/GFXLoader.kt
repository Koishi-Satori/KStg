package top.kkoishi.stg.script

import top.kkoishi.stg.Loader
import top.kkoishi.stg.Loader.Companion.register
import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.script.ScriptConstants.SCOPE_GFX_LOADER
import top.kkoishi.stg.script.VM.Instruction
import top.kkoishi.stg.script.VM.processVars
import java.io.File
import java.nio.file.Path

@Suppress("ClassName")
class GFXLoader @JvmOverloads constructor(private val root: Path, private val useVram: Boolean = false) :
    LocalVariables(SCOPE_GFX_LOADER), Loader {

    init {
        LocalVariables[scopeName] = this
    }

    @JvmOverloads
    constructor(root: String, useVRAM: Boolean = false) : this(Path.of(root), useVRAM) {
        LocalVariables[scopeName] = this
    }

    private val logger = GFXLoader::class.logger()
    override fun loadDefinitions() {
        logger.log(System.Logger.Level.INFO, "Load textures from scripts.")
        for (path in root.toFile().listFiles()!!) {
            if (path.isFile)
                try {
                    logger.log(System.Logger.Level.INFO, "Try to load textures from $path")
                    register(path.canonicalPath.toString())
                    loadDefinitionFile(path)
                    logger.log(System.Logger.Level.INFO, "Success to load all the textures from $path")
                } catch (e: Exception) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
        }
    }

    private fun loadDefinitionFile(path: File) {
        val rest = ReaderIterator(path.bufferedReader())
        val lexer = ResourcesScriptLexer(rest)
        val parser = GFXScriptParser(lexer)
        val instructions = parser.parse()

        for (it in instructions) {
            try {
                if (it.needVars()) it.invoke(this@GFXLoader) else it.invoke()
            } catch (e: Exception) {
                logger.log(System.Logger.Level.ERROR, e)
            }
        }
        rest.close()
    }

    private inner class GFXScriptParser(lexer: Lexer) : InfoParser(lexer, "gfx_items", scopeName) {
        override fun buildParseInfo() {
            root.add(loopInfo())
            root.add(gfxInfo())
            root.add(shearInfo())
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

    private operator fun InstructionSequence.invoke(begin: Int, end: Int, varName: String?, vars: LocalVariables) =
        if (varName != null)
            (begin..end).forEach {
                vars.setVar<Number>(varName, it)
                forEach { inst -> if (inst.needVars()) inst(vars) else inst() }
            }
        else
            (begin..end).forEach { _ -> forEach { inst -> inst(vars) } }

    private inner class loop(
        private val begin: Int,
        private val end: Int,
        private val name: String?,
        private val instructions: InstructionSequence,
    ) : Instruction(0x20) {
        override fun needVars(): Boolean = true

        override fun invoke(vars: LocalVariables) {
            instructions(begin, end, name, vars)
        }
    }

    private inner class gfx(private val name: String, private val path: String) : Instruction(0x21) {
        override fun needVars(): Boolean = false
        override fun invoke() {
            GFX[name, useVram] = "$root/$path"
        }
    }

    private inner class shear(
        private val name: String,
        private val key: String,
        private val x: String,
        private val y: String,
        private val w: String,
        private val h: String,
    ) : Instruction(0x22) {
        override fun needVars(): Boolean = false

        @Throws(FailedLoadingResourceException::class)
        override fun invoke() {
            GFX.shearTexture(
                processVars<String>(key, scopeName),
                processVars<String>(name, scopeName),
                processVars<Int>(x, scopeName),
                processVars<Int>(y, scopeName),
                processVars<Int>(w, scopeName),
                processVars<Int>(h, scopeName)
            )
        }
    }

    internal inner class gfxInfo : ParseInfo.InstructionInfo("gfx") {
        init {
            add(ParseInfo.CommonParameterInfo("name"))
            add(ParseInfo.CommonParameterInfo("path"))
        }

        override fun allocate(parameters: Map<String, Any>): Instruction {
            return gfx(parameters["name"]!!.toString(), parameters["path"]!!.toString())
        }
    }

    internal inner class shearInfo : ParseInfo.InstructionInfo("shear") {
        init {
            add(ParseInfo.CommonParameterInfo("name"))
            add(ParseInfo.CommonParameterInfo("key"))
            add(ParseInfo.CommonParameterInfo("x"))
            add(ParseInfo.CommonParameterInfo("y"))
            add(ParseInfo.CommonParameterInfo("w"))
            add(ParseInfo.CommonParameterInfo("h"))
        }

        override fun allocate(parameters: Map<String, Any>): Instruction {
            return shear(
                parameters["name"]!!.toString(),
                parameters["key"]!!.toString(),
                parameters["x"]!!.toString(),
                parameters["y"]!!.toString(),
                parameters["w"]!!.toString(),
                parameters["h"]!!.toString()
            )
        }
    }

    internal inner class loopInfo : ParseInfo.InstructionInfo("loop") {
        init {
            add(ParseInfo.OptionalParameterInfo("begin", 0))
            add(ParseInfo.OptionalParameterInfo("end", 0))
            add(ParseInfo.CommonParameterInfo("var_name"))
            add(ParseInfo.ComplexParameterInfo("load"))
        }

        @Suppress("UNCHECKED_CAST")
        override fun allocate(parameters: Map<String, Any>): Instruction {
            return loop(
                parameters["begin"]!!.toString().toInt(),
                parameters["end"]!!.toString().toInt(),
                parameters["var_name"]!!.toString(),
                (parameters["load"]!! as InstructionSequence)
            )
        }
    }
}