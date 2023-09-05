package top.kkoishi.stg.boot

import top.kkoishi.stg.DefinitionsLoader
import top.kkoishi.stg.Resources
import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.exceptions.CrashReporter
import top.kkoishi.stg.exceptions.ThreadExceptionHandler
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.gfx.Renderer
import top.kkoishi.stg.logic.GameLoop
import top.kkoishi.stg.logic.InfoSystem
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.SingleInstanceEnsurer
import top.kkoishi.stg.logic.Threads
import top.kkoishi.stg.logic.keys.KeyBinds
import top.kkoishi.stg.util.OptimizedContainer
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.Insets
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import kotlin.system.exitProcess

class Bootstrapper {
    private val logger = Bootstrapper::class.logger()
    private val definitionsLoaders = ArrayDeque<DefinitionsLoader>()
    private val uiInsets: Insets = Insets(0, 0, 0, 0)
    private var container: JFrame = OptimizedContainer()
    private var title: String = ""
    private var initMethod: () -> Unit = {}

    /**
     * If use fullscreen mode.
     */
    private var fullscreen = false

    /**
     * The scale
     */
    private var scale: Pair<Int, Int>? = null

    /**
     * If automatically sync logic to actual frame rate
     */
    private var autoSync = false

    /**
     * The icon of the container
     */
    private var icon: Image? = null

    /**
     * The width of the container.
     */
    private var width = 514

    /**
     * The height of the container.
     */
    private var height = 514

    private var useVRAM = false

    fun size(width: Int, height: Int): Bootstrapper {
        this.width = width
        this.height = height
        return this
    }

    fun icon(iconPath: String): Bootstrapper {
        try {
            icon = ImageIO.read(File(iconPath))
        } catch (e: Exception) {
            logger.log(System.Logger.Level.TRACE, e)
        }
        return this
    }

    @JvmOverloads
    fun autoSync(enable: Boolean = true): Bootstrapper {
        autoSync = enable
        return this
    }

    fun scale(targetSize: Pair<Int, Int>?): Bootstrapper {
        scale = targetSize
        return this
    }

    @JvmOverloads
    fun fullscreen(enable: Boolean = true): Bootstrapper {
        fullscreen = enable
        return this
    }

    @JvmOverloads
    fun useVRAM(enable: Boolean = true): Bootstrapper {
        useVRAM = enable
        return this
    }

    fun initMethod(mth: () -> Unit): Bootstrapper {
        initMethod = mth
        return this
    }

    fun container(f: JFrame): Bootstrapper {
        container = f
        return this
    }

    fun containerTitle(title: String): Bootstrapper {
        this.title = title
        return this
    }

    fun uiInsets(top: Int, left: Int, bottom: Int, right: Int): Bootstrapper {
        uiInsets.set(top, left, bottom, right)
        return this
    }

    fun useEngineDefaultIcon(): Bootstrapper {
        try {
            icon = ImageIO.read(Resources.getEngineResources())
        } catch (e: Exception) {
            logger.log(System.Logger.Level.ERROR, e)
        }
        return this
    }

    fun append(loader: DefinitionsLoader): Bootstrapper {
        definitionsLoaders.addLast(loader)
        return this
    }

    fun start() {
        definitionsLoaders.forEach(DefinitionsLoader::loadDefinitions)
        container.isResizable = false
        container.isUndecorated = fullscreen
        container.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        container.ignoreRepaint = true
        container.title = title
        if (icon != null)
            container.iconImage = icon
        container.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                logger.log(System.Logger.Level.INFO, "Window is closing.")
                SingleInstanceEnsurer.release()
                exitProcess(CrashReporter.EXIT_OK)
            }
        })

        if (autoSync)
            container.addFocusListener(
                object : FocusListener {
                    override fun focusGained(e: FocusEvent) {
                        this::class.logger().log(System.Logger.Level.INFO, "Gained focus, try to resync again.")
                        Renderer.syncFrame()
                        InfoSystem.syncFrame()
                    }

                    override fun focusLost(e: FocusEvent) {
                        this::class.logger().log(System.Logger.Level.WARNING, "Lose focus!")
                    }
                }
            )
        KeyBinds.attach(container)

        when {
            fullscreen -> {
                container.setSize(width, height)
                container.isVisible = true
                Graphics.refresh(container)
                Graphics.setScreenSize(Dimension(width, height))
                Graphics.setBufferSize(width, height)

                if (GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.isFullScreenSupported)
                    GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.fullScreenWindow = container
                else
                    container.size = Renderer.monitorSize()
                Renderer.fullscreen()
            }

            scale != null -> {
                val cpy = scale!!
                val scaledWidth = cpy.first
                val scaledHeight = cpy.second
                container.isResizable = false
                container.setSize(width, height)
                container.isVisible = true
                Graphics.refresh(container)
                Graphics.setScreenSize(Dimension(width, height))
                Graphics.setBufferSize(width, height)

                container.size = Renderer.actualScaledSize(scaledWidth, scaledHeight)
                Renderer.scale(scaledWidth, scaledHeight)
            }

            else -> {
                container.setSize(width + 14, height + 37)
                container.isVisible = true
                Graphics.refresh(container)
                Graphics.setScreenSize(Dimension(width, height))
                Graphics.setBufferSize(width, height)
            }
        }
        with(uiInsets) {
            Graphics.setUIInsets(top, left, bottom, right)
        }

        initMethod()
        Player.registerKeyEvents()

        val inst = Threads.getInstance()
        InfoSystem.start(inst)
        Renderer.start(inst)
        GameLoop.start(inst)
        AudioPlayer.start(inst)
    }

    companion object {
        @JvmStatic
        @Suppress("NOTHING_TO_INLINE")
        inline fun enableHardwareAccelerationProperties() {
            System.setProperty("sun.java2d.ddscale", "true")
            System.setProperty("sun.java2d.opengl", "true")
            System.setProperty("swing.aatext", "true")
            System.setProperty("awt.nativeDoubleBuffering", "true")
        }

        @JvmStatic
        fun readEngineSettings() {
            val settings = Settings.INI("./engine.ini")
            if (settings.read())
                settings.load()
            else
                Bootstrapper::class.logger().log(System.Logger.Level.WARNING, "Failed to read ${settings.file}")
        }
    }
}