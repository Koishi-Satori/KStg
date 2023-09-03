package top.kkoishi.stg.test

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.boot.FastBootstrapper
import top.kkoishi.stg.boot.Settings
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.boot.ui.LoadingFrame
import top.kkoishi.stg.exceptions.CrashReporter
import top.kkoishi.stg.exceptions.InternalError
import top.kkoishi.stg.exceptions.ThreadExceptionHandler
import top.kkoishi.stg.gfx.Renderer
import top.kkoishi.stg.replay.ReplayRecorder
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.script.AudioLoader
import top.kkoishi.stg.script.GFXLoader
import top.kkoishi.stg.test.common.GameSystem
import top.kkoishi.stg.test.common.stages.Stage1
import top.kkoishi.stg.util.OptimizedContainer
import top.kkoishi.stg.util.Option
import top.kkoishi.stg.util.Options
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

object Test {
    private const val WIDTH = 640
    private const val HEIGHT = 480

    @JvmStatic
    val UI_INSETS = Insets(16, 36, 16, 220)

    @JvmStatic
    private var fullScreen: Boolean = false

    @JvmStatic
    private var useVRAM: Boolean = false

    @JvmStatic
    private var scale: Pair<Int, Int>? = null

    @JvmStatic
    fun main(args: Array<String>) {
        FastBootstrapper.setAccelerationProperties()
        GenericSystem.logToFile = true
        val settings = Settings.INI("./engine.ini")
        if (settings.read())
            settings.load()
        else
            Test::class.logger().log(System.Logger.Level.WARNING, "Failed to read ${settings.file}")
        initThreadHandler()
        initOptions()
        if (SingleInstanceEnsurer.setLockedFile("./.lock") == null)
            load(args)
    }

    private fun initOptions() {
        Options.addOption(Option(false, "fullscreen") { _, _ -> fullScreen = true })
        Options.addOption(Option(true, "scale") { o, arg ->
            if (fullScreen)
                return@Option
            val l = arg.split(':')
            if (l.size != 2)
                throw IllegalArgumentException("$o needs extra arg like '1600:900'.")
            try {
                val width = l[0].toInt()
                val height = l[1].toInt()
                scale = width to height
            } catch (e: Exception) {
                Test::class.logger().log(System.Logger.Level.TRACE, e)
            }
        })
        Options.addOption(Option(false, "useVRAM", "vram") { _, _ -> useVRAM = true })
    }

    private fun load(args: Array<String>) {
        Options.handleArguments(args)
        if (Options.State.debug) {
            Test::class.logger().log(System.Logger.Level.DEBUG, "Debug mode on.")
            Test::class.logger().log(System.Logger.Level.DEBUG, "Program arguments: ${args.contentToString()}")
        }
        if (useVRAM)
            Renderer.useVRAM()
        val load = LoadingFrame(ImageIO.read(File("${Threads.workdir()}/test/load.jpg")))
        loadResources()
        Graphics.setFont("sidebar", Font("Times New Roman", Font.PLAIN, 20))
        initJFrame()
        load.end()
        initPauseMenu()
        menu()
        FastBootstrapper.beginThreads()
    }

    private fun initJFrame() {
        val f = OptimizedContainer("KKoishi_ Stg Engine Test")
        f.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                Test::class.logger().log(System.Logger.Level.INFO, "Window is closing.")
                SingleInstanceEnsurer.release()
                exitProcess(CrashReporter.EXIT_OK)
            }
        })
        FastBootstrapper.useEngineIconImage(f)
        FastBootstrapper.autoSync(f)

        val scale = scale
        if (scale != null)
            FastBootstrapper.display(f, WIDTH, HEIGHT, UI_INSETS, scale.first, scale.second)
        else
            FastBootstrapper.display(f, WIDTH, HEIGHT, UI_INSETS, fullScreen)

        FastBootstrapper.keyBinds(
            f,
            Player.VK_C,
            Player.VK_DOWN,
            Player.VK_ESCAPE,
            Player.VK_LEFT,
            Player.VK_RIGHT,
            Player.VK_SHIFT,
            Player.VK_UP,
            Player.VK_X,
            Player.VK_Z,
            KeyEvent.VK_F11
        )
    }

    private fun loadResources() {
        // load from scripts
        GFXLoader("${Threads.workdir()}/test/gfx").loadDefinitions()
        AudioLoader("${Threads.workdir()}/test/audio").loadDefinitions()
    }

    private fun initThreadHandler() {
        ThreadExceptionHandler.addHandler(ThreadExceptionHandler.HandleInfo("Internal Error") {
            if (it is InternalError)
                return@HandleInfo ThreadExceptionHandler.HANDLE_LEVEL_CRASH
            else return@HandleInfo ThreadExceptionHandler.HANDLE_LEVEL_OFF
        })
        ThreadExceptionHandler.initialize(
            ProcessBuilder(
                "java",
                "-jar",
                "\"${Threads.workdir()}/crash_handle/KStg.CrashHandler.jar\""
            )
        )
    }

    private fun initPauseMenu() {
        val pauseMenu = GameSystem.pauseMenu
        if (!ObjectPool.containsUIObject(pauseMenu))
            ObjectPool.addUIObject(pauseMenu)
    }

    fun menu() {
        AudioPlayer.setBackground(Sounds.getAudio("bk_0"))
        val mainMenu = GameSystem.mainMenu
        mainMenu.curLevel = GameSystem.rootMainMenu
        if (!ObjectPool.containsUIObject(mainMenu))
            ObjectPool.addUIObject(mainMenu)
        // switch to menu
        GenericSystem.gameState.set(GenericSystem.STATE_MENU)
    }

    fun start(playerIndex: Int = 0) {
        GameSystem.playerIndex = playerIndex
        if (ReplayRecorder.hasRunningRecorder())
            ReplayRecorder.tryDisposeRecorder()
        ObjectPool.clearAll()
        Threads.refreshRandomSeed()
        GameSystem.randomSeed = Threads.randomSeed()
        GameSystem.rand = Threads.random()
        val player = GameSystem.players[playerIndex]
        player.reinitialize()
        ObjectPool.player(player)
        PlayerManager.cur = Stage1(player, playerIndex)
        val sideBar = GameSystem.sideBar
        sideBar.reset()
        if (!ObjectPool.containsUIObject(sideBar))
            ObjectPool.addUIObject(sideBar)

        // start game
        GenericSystem.gameState.set(GenericSystem.STATE_PLAYING)
    }
}