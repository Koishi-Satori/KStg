package top.kkoishi.stg.test

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.boot.Bootstrapper
import top.kkoishi.stg.boot.Settings
import top.kkoishi.stg.boot.ui.LoadingFrame
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.exceptions.InternalError
import top.kkoishi.stg.exceptions.ThreadExceptionHandler
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.gfx.Renderer
import top.kkoishi.stg.gfx.Screen
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.keys.KeyBindEventWithCaller
import top.kkoishi.stg.logic.keys.KeyBinds
import top.kkoishi.stg.replay.ReplayRecorder
import top.kkoishi.stg.script.AudioLoader
import top.kkoishi.stg.script.DialogsLoader
import top.kkoishi.stg.script.GFXLoader
import top.kkoishi.stg.test.common.GameSystem
import top.kkoishi.stg.test.common.stages.Stage1
import top.kkoishi.stg.util.Option
import top.kkoishi.stg.util.Options
import java.awt.Font
import java.awt.Insets

object Test {
    private const val WIDTH = 640
    private const val HEIGHT = 480

    /** Constant for the F11 function key.  */
    private const val VK_F11 = 0x7A

    @JvmStatic
    val UI_INSETS = Insets(16, 36, 16, 220)

    @JvmStatic
    var invincible = false

    @JvmStatic
    private var fullScreen: Boolean = false

    @JvmStatic
    private var scale: Pair<Int, Int>? = null

    @JvmStatic
    private var useVRAM = false

    @JvmStatic
    fun main(args: Array<String>) {
        // enable hardware acceleration for java2d.
        Bootstrapper.enableHardwareAccelerationProperties()
        handleArgs(args)
        readSettings()
        GenericSystem.logToFile = true
        val load = LoadingFrame("${Threads.workdir()}/test/load.jpg")
        initThreadHandler()
        // ensure single instance of this engine.
        if (SingleInstanceEnsurer.setLockedFile("./.lock") == null) {
            if (Options.State.debug) {
                Test::class.logger().log(System.Logger.Level.DEBUG, "Debug mode on.")
                Test::class.logger().log(System.Logger.Level.DEBUG, "Program arguments: ${args.contentToString()}")
            }
            // fast bootstrap
            Bootstrapper().size(WIDTH, HEIGHT).autoSync().containerTitle("KKoishi_ Stg Engine Test")
                .fullscreen(fullScreen).scale(scale).useEngineDefaultIcon().useVRAM(useVRAM).uiInsets(
                    UI_INSETS.top, UI_INSETS.left, UI_INSETS.bottom, UI_INSETS.right
                ).append(GFXLoader("${Threads.workdir()}/test/gfx"))
                .append(DialogsLoader("${Threads.workdir()}/test/common/dialogs").localization(TestDialog_zh_CN))
                .append(AudioLoader("${Threads.workdir()}/test/audio")).initMethod {
                    keyBinds()
                    initPauseMenu()
                    initMainMenu()
                    load.end()
                    mainMenu()
                }.start()
        }
    }

    private fun readSettings() {
        Bootstrapper.readEngineSettings()
        val fonts = Settings.INI("${Threads.workdir()}/test/fonts.ini")
        var dialogFontName = ""
        var dialogFontStyle = Font.BOLD
        var dialogFontSize = 20
        fonts.addHandler("dialog::name") { dialogFontName = it }
        fonts.addHandler("dialog::style") { dialogFontStyle = Graphics.parseFontStyle(it) }
        fonts.addHandler("dialog::size") { dialogFontSize = it.toInt() }

        if (fonts.read())
            fonts.load()
        if (dialogFontName.isNotEmpty()) {
            Graphics.setFont(dialogFontName, Font(dialogFontName, dialogFontStyle, dialogFontSize))
            Test::class.logger().log(System.Logger.Level.INFO, "Set dialog font to $dialogFontName")
        }
    }

    /**
     * Process the arguments.
     *
     * @param args arguments.
     */
    private fun handleArgs(args: Array<String>) {
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
        Options.addOption(Option(false, "invincible", "inv") { _, _ -> invincible = true })
        Options.handleArguments(args)
    }

    private fun keyBinds() {
        Player.keyEvents[Player.VK_ESCAPE] = {
            GameSystem.pauseMenu.restore()
            val gameState = GenericSystem.gameState.get()
            if (gameState == GenericSystem.STATE_PLAYING) {
                GenericSystem.gameState.set(GenericSystem.STATE_PAUSE)
                it.logger.log(System.Logger.Level.INFO, "Pause the game.")
            } else if (gameState == GenericSystem.STATE_PAUSE) {
                GenericSystem.gameState.set(GenericSystem.STATE_PLAYING)
                it.logger.log(System.Logger.Level.INFO, "Continue the game.")
            }
            KeyBinds.release(Player.VK_ESCAPE)
        }
        KeyBinds.bindGeneric(
            VK_F11, KeyBindEventWithCaller(null) { _: Object? ->
                Screen.takeScreenshot("${Threads.workdir()}/screenshots/screenshot_${System.currentTimeMillis()}_${Renderer.frame()}.png")
            })
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

    private fun initMainMenu() {
        val mainMenu = GameSystem.mainMenu
        if (!ObjectPool.containsUIObject(mainMenu))
            ObjectPool.addUIObject(mainMenu)
    }

    fun mainMenu() {
        AudioPlayer.setBackground(Sounds.getAudio("bk_0"))
        GameSystem.mainMenu.restore()
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
        PlayerManager.curStage = Stage1(player, playerIndex)
        val sideBar = GameSystem.sideBar
        sideBar.reset()
        if (!ObjectPool.containsUIObject(sideBar))
            ObjectPool.addUIObject(sideBar)

        // start game
        GenericSystem.gameState.set(GenericSystem.STATE_PLAYING)
    }
}