package top.kkoishi.stg.script

import top.kkoishi.stg.DefinitionsLoader
import top.kkoishi.stg.DefinitionsLoader.Companion.register
import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.exceptions.ScriptException
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.TexturedFont
import top.kkoishi.stg.gfx.Texture
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

/**
 * GFXLoader can load map textures ([Texture]) from local scripts, and five instructions are provided in
 * the loading scripts: gfx/shear/loop/font/shear_font. Also, some instructions are provided in [VM].
 *
 * ## The instructions for using the script
 *
 * The script header is gfx_items and follows the following format:
 * ```
 * gfx_items = {
 *     ...
 * }
 * ```
 * All instructions follow the format ``` name = {parameters} ```, where parameters are key-value pairs,
 * which values can be variables, strings, and numbers, and variables can be referenced within strings.
 *
 * ## Ways to reference variables
 * Variable names should be preceded by symbols, and if the variable is surrounded by characters,
 * parentheses '(', ')' should be added around its name
 * ```
 * xxx$variable_name
 * xxx$(variable_name)xxx
 * ```
 * The variable format: $variable_name
 *
 * ## Format of instructions
 * | Instruction Name | Parameters  | Parameters Instructions | Functions |
 * | :--------------: | :---------- | ----------------------- | --------- |
 * |        gfx       | name, path | name = "xxx", fill in the name of the texture. <br/> path = "xxx", fill in the path to the image file corresponding to the texture. | Loads a texture map based on the given file path |
 * |       shear      | name, key, x, y, w, h | name is same as ```gfx```. <br/> key = "xxx", the key of the source texture to fill in the clipping texture. <br/> x = [number], fill in the x coordinate where cropping starts. <br/> y = [number], fill in the y coordinate to start clipping. <br/> w = [number], fill in the width of the crop area. <br/> h = [number], fill in the height of the cropping area. | Cut one texture from another. |
 * |       loop       | var_name, begin, end, load | var_name = "xxx", fill in the variable declared while loop, its value will be set from "begin" to "end" during invoking the loop. <br/> begin = [number], optional parameter, the number at which the loop starts. The loop will continue until the previous variable is set to end. The default is 0. <br/> end = [number], optional parameter, the number to terminate the loop, the default is 0. <br/> load = {} Composite parameters, inside the brackets are a series of instructions that need to be executed during the loop. | Execute loop. |
 *
 * e.g.
 * ```
 *     gfx = {
 *         name = "planes_koishi"
 *         path = "./icons/planes_koishi.png"
 *     }
 *     loop = {
 *         var_name = "state"
 *         end = 2
 *         load = {
 *             loop = {
 *                 var_name = "i"
 *                 end = 7
 *                 load = {
 *                     set_var = { name = "x" value = $i }
 *                     set_var = { name = "y" value = $state }
 *                     mul_var = { name = "x" value = 32 }
 *                     mul_var = { name = "y" value = 48 }
 *                     shear = {
 *                         name = "plane_koishi_$(state)_$i"
 *                         key = "planes_koishi"
 *                         x = $x
 *                         y = $y
 *                         w = 32
 *                         h = 48
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * ```
 *
 * It needs to provide the root of the directory to be loaded, and all image paths are given as paths relative to the root.
 *
 * @author KKoishi_
 */
@Suppress("ClassName")
class GFXLoader
/**
 * Construct a GFXLoader instance.
 *
 * @param root the root path.
 * @param useVram if using the VRAM to store the textures([java.awt.image.VolatileImage])
 */ @JvmOverloads constructor(private val root: Path, private val useVram: Boolean = false) :
    LocalVariables(SCOPE_GFX_LOADER), DefinitionsLoader {

    @JvmOverloads
    constructor(root: String, useVRAM: Boolean = false) : this(Path.of(root), useVRAM) {
        LocalVariables[scopeName] = this
    }

    private val unloaded: HashMap<String, Instruction>

    private val unnamedUnloaded: ArrayDeque<Instruction>

    private val logger: InfoSystem.Companion.Logger

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

        tryLoadUnloaded()
    }

    private fun tryLoadUnloaded() {
        // load named first.
        // if catch an exception, will load the instruction directly.
        val rest = unloaded.map { it.key }.toCollection(ArrayDeque(unloaded.size))
        var name: String? = null
        while (rest.isNotEmpty()) {
            if (name == null)
                name = rest.removeFirst()
            val inst = unloaded[name] ?: continue

            val newName = tryLoadInstruction(inst as InstWithReference)
            if (newName != null) {
                if (unloaded[newName] == null) {
                    try {
                        if (inst.needVars()) inst(this@GFXLoader) else inst()
                    } catch (e: Exception) {
                        logger.log(System.Logger.Level.ERROR, e)
                    }
                    name = null
                    continue
                }
                rest.addFirst(name)
            }
            name = newName
        }
        unloaded.clear()

        // load the unnamed.
        unnamedUnloaded.forEach {
            try {
                if (it.needVars()) it(this@GFXLoader) else it()
            } catch (e: Exception) {
                logger.log(System.Logger.Level.ERROR, e)
            }
        }
        unnamedUnloaded.clear()
    }

    /**
     * Try to load [InstWithReference], if the reference [InstWithReference.key] is not loaded, will return
     * its name, or null.
     */
    private fun tryLoadInstruction(inst: InstWithReference): String? {
        val key = inst.key
        return if (GFX[key] == GFX.notFound())
            key
        else {
            val it = inst as Instruction
            if (it.needVars()) it(this@GFXLoader) else it()
            null
        }
    }

    private fun loadDefinitionFile(path: File) {
        val rest = ReaderIterator(path.bufferedReader())
        val lexer = ResourcesScriptLexer(rest)
        val parser = GFXScriptParser(lexer)
        val instructions = parser.parse()

        for (it in instructions) {
            try {
                if (it.needVars()) it(this@GFXLoader) else it()
            } catch (e: Exception) {
                logger.log(System.Logger.Level.TRACE, e)
                logger.log(System.Logger.Level.INFO, "$it will be load at last.")
                if (it is InstWithReference)
                    unloaded[it.name] = it
                else
                    unnamedUnloaded.addLast(it)
            }
        }
        rest.close()
    }

    private inner class GFXScriptParser(lexer: Lexer) : InfoParser(lexer, "gfx_items", scopeName) {
        override fun buildParseInfo() {
            root.add(ResourcesInstructions.LoopInfo())
            root.add(gfxInfo())
            root.add(shearInfo())
            root.add(fontInfo())
            root.add(shear_fontInfo())
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
        override val name: String,
        override val key: String,
        private val x: String,
        private val y: String,
        private val w: String,
        private val h: String,
    ) : Instruction(0x22), InstWithReference {
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
    private inner class shear_font(
        override val name: String,
        override val key: String,
        private val width: Int,
        private val height: Int,
        private val jvm_method_descriptor: String,
    ) : Instruction(0x23), InstWithReference {
        override fun needVars(): Boolean = false

        private fun getFunction(): (Char) -> Pair<Int, Int> {
            return ScriptLinker.bind(jvm_method_descriptor, ScriptLinker.function1Allocator())
        }

        override fun invoke() {
            val texture = GFX.getTexture(key)
            if (texture == GFX.notFound())
                throw FailedLoadingResourceException("Can not find texture: $key")
            val find: (Char) -> Pair<Int, Int> = getFunction()
            GFX.register(name, TexturedFont(texture(), width, height, find))
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

    internal inner class shear_fontInfo : ParseInfo.InstructionInfo("shear_font") {
        init {
            add(ParseInfo.CommonParameterInfo("name"))
            add(ParseInfo.CommonParameterInfo("key"))
            add(ParseInfo.CommonParameterInfo("width"))
            add(ParseInfo.CommonParameterInfo("height"))
            add(ParseInfo.CommonParameterInfo("jvm_md"))
        }

        override fun allocate(parameters: Map<String, Any>): Instruction {
            return shear_font(
                parameters["name"]!!.toString(),
                parameters["key"]!!.toString(),
                parameters["width"]!!.toString().toInt(),
                parameters["height"]!!.toString().toInt(),
                parameters["jvm_md"]!!.toString()
            )
        }
    }

    private interface InstWithReference {
        val name: String
        val key: String
    }

    init {
        LocalVariables[scopeName] = this
        this.unloaded = HashMap()
        this.unnamedUnloaded = ArrayDeque()
        this.logger = GFXLoader::class.logger()
    }
}