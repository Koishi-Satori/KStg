package top.kkoishi.stg.boot.jvm

import top.kkoishi.stg.logic.InfoSystem
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import java.io.File
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodHandles.Lookup
import java.net.JarURLConnection
import java.net.URL
import java.net.URLDecoder
import kotlin.system.exitProcess

@GameMainClass(main = "test", public = true)
object LibMain {
    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("sun.java2d.ddscale", "true")
        System.setProperty("sun.java2d.opengl", "true")
        System.setProperty("swing.aatext", "true")
        System.setProperty("awt.nativeDoubleBuffering", "true")

        val logger = LibMain::class.logger()
        try {
            mainImpl(args, logger)
        } catch (e: Exception) {
            logger.log(System.Logger.Level.ERROR, e)
            exitProcess(1)
        }
    }

    @Throws(Exception::class)
    private fun mainImpl(args: Array<String>, logger: InfoSystem.Companion.Logger) {
        try {
            val arr = ArrayDeque<Class<*>>()
            val lookup: Lookup = MethodHandles.lookup()
            println(Class.forName("top.kkoishi.stg.script.GFXLoader").classLoader)
            if (args.isEmpty()) {
                // scan this jar file
                val thisLoader = Thread.currentThread().contextClassLoader
                thisLoader.definedPackages.forEach { pack ->
                    logger.log(System.Logger.Level.INFO, "Scanning package: $pack")
                    val resources = thisLoader.getResources(pack.name)
                    resources.asIterator().forEach {it.handleURL(arr, logger, thisLoader)}
                }
                // do something
            } else {

            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun URL.handleURL(arr: ArrayDeque<Class<*>>, logger: InfoSystem.Companion.Logger, loader: ClassLoader) {
        when (protocol) {
            "jar" -> {
                val jar = (openConnection() as JarURLConnection).jarFile
                assert(jar != null) { throw UnknownError("Can not open connection with current jar file!") }
                jar.entries().asIterator().forEach { entry ->
                    var name = entry.name
                    if (name[0] == '/')
                        name = name.substring(1)
                    // val idx = name.lastIndexOf('/')
                    if (name.endsWith(".class")) {
                        name = name.substring(0..name.length - 7)
                        try {
                            val clz = Class.forName(name.replace('/', '.'))
                            //println(clz)
                            val anno = clz.getAnnotation(GameMainClass::class.java)
                            if (anno != null) {
                                logger.log(System.Logger.Level.INFO, "Find a game main class:$clz")
                                arr.addLast(clz)
                            }
                        } catch (cnf: Throwable) {
                            logger.log(System.Logger.Level.WARNING, cnf)
                        }
                    } else {
                        loader.getResources(name).iterator().forEach { println(it.protocol) }
                    }
                }
            }

            "file" -> {
                val path = URLDecoder.decode(file, Charsets.UTF_8)
                findClass(arr, File(path), "")
            }

            else -> {
                logger.log(System.Logger.Level.ERROR, "Failed to identify the protocol.")
                exitProcess(1)
            }
        }
    }

    private fun findClass(arr: ArrayDeque<Class<*>>, file: File, packageName: String) {
        if (file.isFile) {
            val name = file.name
            if (name.endsWith(".class")) {
                val className = name.substring(0..name.length - 7)
                println("$packageName/$className")
            }
        } else {
            file.listFiles()?.forEach { findClass(arr, it, file.name.replace('/', '.')) }
        }
    }
}