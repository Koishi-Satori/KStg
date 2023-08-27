package top.kkoishi.stg.script

import top.kkoishi.stg.Loader
import top.kkoishi.stg.Loader.Companion.register
import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.TexturedFont
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.script.execution.*
import top.kkoishi.stg.script.execution.InfoParser
import top.kkoishi.stg.script.execution.InstructionSequence
import top.kkoishi.stg.script.execution.Lexer
import top.kkoishi.stg.script.execution.ParseInfo
import top.kkoishi.stg.script.execution.ReaderIterator
import top.kkoishi.stg.script.execution.ResourcesInstructions
import top.kkoishi.stg.script.execution.ResourcesScriptLexer
import top.kkoishi.stg.script.execution.ScriptConstants.SCOPE_GFX_LOADER
import top.kkoishi.stg.script.execution.Type
import top.kkoishi.stg.script.execution.VM.Instruction
import top.kkoishi.stg.script.execution.VM.processVars
import top.kkoishi.stg.script.reflect.ScriptLinker
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.exists
import kotlin.io.path.inputStream

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
            root.add(ResourcesInstructions.LoopInfo())
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

    @Suppress("PrivatePropertyName")
    private inner class font(
        private val name: String,
        private val path: String,
        private val width: Int,
        private val height: Int,
        private val jvm_method_descriptor: String,
    ) : Instruction(0x23) {
        override fun needVars(): Boolean = false

        @Suppress("UNCHECKED_CAST")
        private fun getFunction(): (Char) -> Pair<Int, Int> {
            return ScriptLinker.bind(jvm_method_descriptor) { methodHandle, method ->
                if (methodHandle != null) {
                    return@bind { methodHandle(it) as Pair<Int, Int> }
                } else if (method != null) {
                    return@bind { method(null, it) as Pair<Int, Int> }
                } else {
                    throw ScriptException("Can not link to method $jvm_method_descriptor")
                }
            }
        }

        override fun invoke() {
            val p = Path.of(path)
            if (!p.exists())
                throw FailedLoadingResourceException("Can not find texture: $path")
            val img = ImageIO.read(ImageIO.createImageInputStream(p.inputStream()))
            val find: (Char) -> Pair<Int, Int> = getFunction()
            GFX.register(name, TexturedFont(img, width, height, find))
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

    internal inner class fontInfo : ParseInfo.InstructionInfo("font") {
        init {
            add(ParseInfo.CommonParameterInfo("name"))
            add(ParseInfo.CommonParameterInfo("path"))
            add(ParseInfo.CommonParameterInfo("width"))
            add(ParseInfo.CommonParameterInfo("height"))
            add(ParseInfo.CommonParameterInfo("jvm_md"))
        }

        override fun allocate(parameters: Map<String, Any>): Instruction {
            return font(
                parameters["name"]!!.toString(),
                parameters["path"]!!.toString(),
                parameters["width"]!!.toString().toInt(),
                parameters["height"]!!.toString().toInt(),
                parameters["jvm_md"]!!.toString()
            )
        }
    }
}