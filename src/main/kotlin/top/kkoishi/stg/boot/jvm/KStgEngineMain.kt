package top.kkoishi.stg.boot.jvm

import top.kkoishi.stg.boot.Bootstrapper
import top.kkoishi.stg.canAccessUnsafe
import top.kkoishi.stg.exceptions.InternalError
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.script.reflect.Reflection
import top.kkoishi.stg.unsafeAllocateInstance
import top.kkoishi.stg.util.Options
import java.net.URL
import java.nio.file.Path
import java.util.*
import java.util.jar.JarFile
import kotlin.collections.ArrayDeque
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * The main class for loading the plugin jars.
 *
 * @author KKoishi_
 */
internal object KStgEngineMain {
    @JvmStatic
    private var plugin_dir = "./plugins"

    @JvmStatic
    private lateinit var PCL: PluginClassLoader

    @JvmStatic
    private val pluginClasses = ArrayDeque<String>(128)

    @JvmStatic
    private val pluginMains = ArrayDeque<Class<out JvmPlugin>>(1)

    /**
     * Change the plugin dir.
     *
     * @param nDir the new dir.
     */
    @JvmStatic
    fun plugin_dir(nDir: String) {
        plugin_dir = nDir
    }

    @JvmStatic
    fun main(args: Array<String>) {
        Bootstrapper.enableHardwareAccelerationProperties()
        Bootstrapper.readEngineSettings()
        Options.handleArguments(args)
        preloadPlugins()
        loadPlugin(args)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    private fun preloadPlugins() {
        with(KStgEngineMain::class.logger()) {
            val dir = Path.of(plugin_dir)
            if (!dir.isDirectory() && !dir.exists()) {
                log(System.Logger.Level.ERROR, "Given plugin dir $dir is not a directory or not exists.")
                throw InternalError()
            }

            // iterate the dir, and skip the dir and non-jar files.
            // then, open the jar file to seek all the class files.
            // finally, map it to an URL stream, and convert it to an array.
            val jarURLs =
                Arrays.stream((dir.toFile().listFiles() ?: throw InternalError()))
                    .filter {
                        if (it.isDirectory) {
                            log(System.Logger.Level.INFO, "Skip dir: $it")
                            false
                        } else true
                    }.filter {
                        if (it.extension != "jar") {
                            log(System.Logger.Level.INFO, "Skip non-jar file: $it")
                            false
                        } else true
                    }.peek {
                        JarFile(it).entries().iterator().forEach { entry ->
                            val name = entry.name
                            if (entry.isDirectory) {
                                if (Options.State.debug)
                                    log(System.Logger.Level.DEBUG, "Skip Dir Entry: $entry")
                            } else if (name.endsWith(".class"))
                                pluginClasses.addLast(name.replace('/', '.').removeSuffix(".class"))
                        }
                    }.map { URL("file:${it.canonicalPath}") }.toList().toTypedArray()

            // load classes.
            PCL = PluginClassLoader(jarURLs)
            while (pluginClasses.isNotEmpty()) {
                val className = pluginClasses.removeFirst()
                val clz = PCL.loadClass(className)
                if (Options.State.debug)
                    log(System.Logger.Level.DEBUG, "Loading class: $clz")
                if (PCL.checkClass(clz)) {
                    log(System.Logger.Level.INFO, "Find plugin main class: $clz")
                    pluginMains.addLast(clz as Class<out JvmPlugin>)
                }
            }
        }
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    private fun loadPlugin(args: Array<String>) {
        val main = Options.State.mainPlugin
        if (main == null) {
            KStgEngineMain::class.logger().log(
                System.Logger.Level.WARNING,
                "Plugin Main class is not specified, try to run the first pre-loaded main class."
            )
            if (pluginMains.isEmpty()) {
                KStgEngineMain::class.logger()
                    .log(System.Logger.Level.ERROR, "No pre-loaded main class is found.")
                throw InternalError()
            }
            runPlugin(pluginMains.first(), args)
        } else {
            val mainClass = PCL.loadClass(main)
            if (pluginMains.contains(mainClass))
                runPlugin(mainClass as Class<out JvmPlugin>, args)
            else {
                KStgEngineMain::class.logger()
                    .log(System.Logger.Level.ERROR, "Plugin Main Class $mainClass is invalid.")
                throw InternalError()
            }
        }
    }

    @JvmStatic
    private fun runPlugin(clz: Class<out JvmPlugin>, args: Array<String>) {
        var instance: JvmPlugin? = null

        try {
            val method = clz.getDeclaredMethod("getInstance")
            if (Reflection.isStatic(method))
                instance = method(null) as JvmPlugin
        } catch (nsme: NoSuchMethodException) {
            KStgEngineMain::class.logger().log(System.Logger.Level.TRACE, nsme)
        }

        if (instance == null) {
            if (canAccessUnsafe)
                instance = unsafeAllocateInstance(clz) as JvmPlugin
            else {
                try {
                    val constructor = clz.getDeclaredConstructor()
                    instance = constructor.newInstance()
                } catch (t: Throwable) {
                    KStgEngineMain::class.logger().log(System.Logger.Level.TRACE, t)
                }
            }
        }
        if (instance == null) {
            KStgEngineMain::class.logger().log(
                System.Logger.Level.ERROR,
                "Can not allocate instance for $clz, abort."
            )
            throw InternalError()
        }

        KStgEngineMain::class.logger().log(System.Logger.Level.INFO, "Plugin Info: ")
        instance.info().forEach { KStgEngineMain::class.logger().log(System.Logger.Level.INFO, it) }
        KStgEngineMain::class.logger().log(System.Logger.Level.INFO, "Run plugin: $clz")
        instance.main(args)
    }

    /**
     * If any plugins are used.
     *
     * This method is used in [Reflection], and when a class can not be loaded by [Class.forName] with its class
     * name, this method can determine whether using [findClass] to find the required class.
     *
     * @return if any plugins are used.
     */
    @JvmStatic
    fun usePlugin() = this::PCL.isInitialized

    /**
     * Find the classes loaded by the [PluginClassLoader].
     *
     * @param name class name.
     */
    @JvmStatic
    fun findClass(name: String) = PCL.loadClass(name)
}