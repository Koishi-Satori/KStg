package top.kkoishi.stg.test

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.boot.FastBootstrapper
import top.kkoishi.stg.common.AbstractStage
import top.kkoishi.stg.common.StageAction
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.*
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
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFrame
import kotlin.system.exitProcess

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        FastBootstrapper.setAccelerationProperties()
        initThreadHandler()
        if (SingleInstanceEnsurer.setLockedFile("./.lock") == null)
            load(args)
    }

    private fun load(args: Array<String>) {
        val load = LoadingFrame(ImageIO.read(File("${Threads.workdir()}/test/load.jpg")))
        val fullScreen = args.isNotEmpty() && args[0] == "fullscreen"
        InfoSystem.logToFile = true
        loadResources()
        initJFrame(fullScreen)
        Graphics.setFont("sidebar", Font("Times New Roman", Font.PLAIN, 20))
        load.end()
        menu()
        FastBootstrapper.beginThreads()
    }

    private fun initJFrame(fullScreen: Boolean) {
        val f = JFrame("KKoishi_ STG Engine test")
        f.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                SingleInstanceEnsurer.release()
                exitProcess(CrashReporter.EXIT_OK)
            }
        })
        FastBootstrapper.setIconImage(f, "${Threads.workdir()}/resources/logo.ico")
        FastBootstrapper.autoSync(f)
        FastBootstrapper.display(f, 640, 480, Insets(16, 36, 16, 220), fullScreen)
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
        // load textures from scripts
        GFXLoader("${Threads.workdir()}/test/gfx").loadDefinitions()

        Sounds.loadAudio("bk_0", "${Threads.workdir()}/test/audio/sounds/bk_0.wav")
        Sounds.loadAudio("bk_1", "${Threads.workdir()}/test/audio/sounds/bk_1.wav")
        Sounds.loadAudio("test_player_shot", "${Threads.workdir()}/test/audio/sounds/th15_player_shot_0.wav")
        Sounds.loadAudio("th15_enemy_damage_01", "${Threads.workdir()}/test/audio/sounds/th15_enemy_damage_01.wav")
        Sounds.loadAudio("th15_enemy_damage_02", "${Threads.workdir()}/test/audio/sounds/th15_enemy_damage_02.wav")
        Sounds.loadAudio("enemy_dead", "${Threads.workdir()}/test/audio/sounds/enemy_dead_0.wav")
        Sounds.loadAudio("enemy_shoot", "${Threads.workdir()}/test/audio/sounds/enemy_shoot.wav")
        Sounds.loadAudio("test_boss_0_bgm", "${Threads.workdir()}/test/audio/sounds/test_boss_0_bgm.wav")
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
                "${Threads.workdir()}/replay",
                SimpleDateFormat("'KStg-TestReplay-'yyyy-MM-dd_HH.mm.ss").format(Date.from(Instant.now())),
                0L
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