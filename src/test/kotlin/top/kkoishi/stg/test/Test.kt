package top.kkoishi.stg.test

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.common.AbstractStage
import top.kkoishi.stg.common.StageAction
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.gfx.*
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.boot.ui.LoadingFrame
import top.kkoishi.stg.exceptions.CrashReporter
import top.kkoishi.stg.exceptions.InternalError
import top.kkoishi.stg.exceptions.ThreadExceptionHandler
import top.kkoishi.stg.gfx.replay.ReplayRecorder
import top.kkoishi.stg.script.GFXLoader
import top.kkoishi.stg.test.common.GameSystem
import top.kkoishi.stg.test.common.actions.TestBoss0Action0
import top.kkoishi.stg.test.common.enemy.TestBoss0
import top.kkoishi.stg.test.common.enemy.TestEnemy0
import top.kkoishi.stg.test.common.enemy.TestEnemy1
import top.kkoishi.stg.test.common.stages.Stage1
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFrame
import kotlin.system.exitProcess

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        //-Dsun.java2d.ddscale=true
        //-Dsun.java2d.opengl=true
        //-Dswing.aatext=true
        //-Dawt.nativeDoubleBuffering=true
        System.setProperty("sun.java2d.ddscale", "true")
        System.setProperty("sun.java2d.opengl", "true")
        System.setProperty("swing.aatext", "true")
        System.setProperty("awt.nativeDoubleBuffering", "true")

        val curPath = Path.of("./").toAbsolutePath()
        ThreadExceptionHandler.initialize(ProcessBuilder("java", "-jar", "\"$curPath/crash_handle/KStg.CrashHandler.jar\""))

        if (SingleInstanceEnsurer.setLockedFile("./.lock") == null)
            load(args)
    }

    private fun load(args: Array<String>) {
        val load = LoadingFrame(ImageIO.read(File("./test/load.jpg")))

        ThreadExceptionHandler.addHandler(ThreadExceptionHandler.HandleInfo("Internal Error") {
            if (it is InternalError)
                return@HandleInfo ThreadExceptionHandler.HANDLE_LEVEL_CRASH
            else return@HandleInfo ThreadExceptionHandler.HANDLE_LEVEL_OFF
        })

        var fullScreen = false
        InfoSystem.logToFile = true

        if (args.isNotEmpty() && args[0] == "fullscreen") {
            fullScreen = true
        }

        // load textures from scripts
        GFXLoader(Path.of("./test/gfx")).loadDefinitions()

        Sounds.loadAudio("bk_0", "./test/audio/sounds/bk_0.wav")
        Sounds.loadAudio("bk_1", "./test/audio/sounds/bk_1.wav")
        Sounds.loadAudio("test_player_shot", "./test/audio/sounds/th15_player_shot_0.wav")
        Sounds.loadAudio("th15_enemy_damage_01", "./test/audio/sounds/th15_enemy_damage_01.wav")
        Sounds.loadAudio("th15_enemy_damage_02", "./test/audio/sounds/th15_enemy_damage_02.wav")
        Sounds.loadAudio("enemy_dead", "./test/audio/sounds/enemy_dead_0.wav")
        Sounds.loadAudio("enemy_shoot", "./test/audio/sounds/enemy_shoot.wav")
        Sounds.loadAudio("test_boss_0_bgm", "./test/audio/sounds/test_boss_0_bgm.wav")

        val f = JFrame("KKoishi_ STG Engine test")
        if (fullScreen)
            f.setSize(640, 480)
        else
            f.setSize(640 + 14, 480 + 37)
        f.isResizable = false
        f.iconImage = ImageIO.read(File("./resources/logo.ico"))
        f.isUndecorated = fullScreen
        f.isVisible = true
        f.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                SingleInstanceEnsurer.release()
                exitProcess(CrashReporter.EXIT_OK)
            }
        })
        f.addFocusListener(
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
        PlayerManager.keyBinds(
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

        Graphics.refresh(f)
        Graphics.setScreenSize(Dimension(640, 480))
        Graphics.setBufferSize(640, 480)
        Graphics.setUIInsets(16, 36, 16, 220)
        Graphics.setFont("sidebar", Font("Times New Roman", Font.PLAIN, 20))
        load.end()
        if (fullScreen) {
            if (GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.isFullScreenSupported)
                GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.fullScreenWindow = f
            else
                f.size = Renderer.monitorSize()
            Renderer.fullScreen()
        }
        menu()
        beginThreads()
    }

    private fun beginThreads() {
        val inst = Threads.getInstance()
        InfoSystem.start(inst)
        Renderer.start(inst)
        GameLoop.start(inst)
        AudioPlayer.start(inst)
    }

    private fun menu() {
        AudioPlayer.setBackground(Sounds.getAudio("bk_0"))
        val mainMenu = GameSystem.mainMenu
        ObjectPool.addUIObject(mainMenu)
        // switch to menu
        GenericFlags.gameState.set(GenericFlags.STATE_MENU)
    }

    fun start(playerIndex: Int = 0) {
        Threads.refreshRandomSeed()
        GameSystem.randomSeed = Threads.randomSeed()
        GameSystem.rand = Threads.random()
        val player = GameSystem.players[playerIndex]
        player.x.set(Graphics.getCenterX().toDouble())
        player.y.set(355.0)
        ObjectPool.player(player)
        val recorder = ReplayRecorder(
            GameSystem.randomSeed, player, intArrayOf(
                Player.VK_C,
                Player.VK_DOWN,
                Player.VK_ESCAPE,
                Player.VK_LEFT,
                Player.VK_RIGHT,
                Player.VK_SHIFT,
                Player.VK_UP,
                Player.VK_X,
                Player.VK_Z,
            )
        ) { playerIndex }
        val stage1 = Stage1()
        stage1.addAction(StageAction(10) {
            recorder.start()
            AudioPlayer.setBackground(Sounds.getAudio("bk_1"))
        })
        stage1.addAction(StageAction(40) {
            ObjectPool.addObject(TestEnemy0(230, 270, 100, "mirror") { _, _ -> })
        })
        stage1.addAction(object : StageAction(250L, action = {
            ObjectPool.addObject(TestEnemy1(104, 50))
            ObjectPool.addObject(TestEnemy1(124, 50))
            ObjectPool.addObject(TestEnemy1(204, 50))
            ObjectPool.addObject(TestEnemy1(224, 50))
        }) {
            override fun canAction(): Boolean {
                if (!ObjectPool.objects().hasNext())
                    return super.canAction()
                return false
            }
        })
        stage1.addAction(object : StageAction(100L, action = {
            ObjectPool.addObject(TestBoss0(230, 270, TestBoss0Action0(2000, 2000L)))
        }) {
            override fun invoke(stage: AbstractStage) {
                AudioPlayer.setBackground(Sounds.getAudio("test_boss_0_bgm"))
                super.invoke(stage)
            }

            override fun canAction(): Boolean {
                if (!ObjectPool.objects().hasNext())
                    return super.canAction()
                return false
            }
        })
        stage1.addAction(object : StageAction(500L, action = {
            AudioPlayer.setBackground(Sounds.getAudio("bk_0"))
            GameSystem.mainMenu.curLevel = GameSystem.rootMainMenu
            // switch to menu
            GenericFlags.gameState.set(GenericFlags.STATE_MENU)
            recorder.save(
                Path.of("./replay"), SimpleDateFormat("'KStg-TestReplay-'yyyy-MM-dd_HH.mm.ss").format(
                    Date.from(Instant.now())
                ), 0L
            )
        }) {
            override fun canAction(): Boolean {
                if (!ObjectPool.objects().hasNext())
                    return super.canAction()
                return false
            }
        })
        PlayerManager.cur = stage1
        val sideBar = GameSystem.sideBar
        ObjectPool.addUIObject(sideBar)

        // start game
        GenericFlags.gameState.set(GenericFlags.STATE_PLAYING)
    }
}