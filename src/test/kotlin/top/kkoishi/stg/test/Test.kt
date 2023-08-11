package top.kkoishi.stg.test

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.gfx.*
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.main.ui.LoadingFrame
import top.kkoishi.stg.script.GFXLoader
import top.kkoishi.stg.test.common.GameSystem
import top.kkoishi.stg.test.common.enemy.TestEnemy0
import top.kkoishi.stg.test.common.stages.Stage1
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.nio.file.Path
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
        val load = LoadingFrame(ImageIO.read(File("./test/load.jpg")))
        InfoSystem.logToFile = true

        // load textures from scripts
        GFXLoader(Path.of("./test/gfx")).loadDefinitions()
//        for (state in 0..2) {
//            for (i in 0 until 8) {
//                GFX.shearTexture("planes_koishi", "plane_koishi_${state}_$i", 32 * i, 48 * state, 32, 48)
//            }
//        }

        Sounds.loadAudio("bk_0", "./test/audio/sounds/bk_0.wav")
        Sounds.loadAudio("bk_1", "./test/audio/sounds/bk_1.wav")
        Sounds.loadAudio("test_player_shot", "./test/audio/sounds/th15_player_shot_0.wav")
        Sounds.loadAudio("th15_enemy_damage_01", "./test/audio/sounds/th15_enemy_damage_01.wav")
        Sounds.loadAudio("th15_enemy_damage_02", "./test/audio/sounds/th15_enemy_damage_02.wav")
        Sounds.loadAudio("enemy_dead", "./test/audio/sounds/enemy_dead_0.wav")
        Sounds.loadAudio("enemy_shoot", "./test/audio/sounds/enemy_shoot.wav")

        val f = JFrame("KKoishi_ STG Engine test")
        f.setSize(640 + 14, 480 + 37)
        f.isResizable = false
        f.iconImage = ImageIO.read(File("./resources/logo.ico"))
        f.isUndecorated = true
        f.isVisible = true
        f.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exitProcess(514)
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

        Graphics.refresh(f)
        Graphics.setScreenSize(Dimension(640, 480))
        Graphics.setBufferSize(640, 480)
        Graphics.setUIInsets(16, 36, 16, 220)
        Graphics.setFont("sidebar", Font("Times New Roman", Font.BOLD, 20))
        f.size = Renderer.monitorSize()
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
        load.end()
        Renderer.fullScreen()
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
        PlayerManager.cur = Stage1()
        val player = GameSystem.players[playerIndex]
        player.x.set(Graphics.getCenterX().toDouble())
        player.y.set(55.0)
        ObjectPool.player = player
        val sideBar = GameSystem.sideBar
        ObjectPool.addUIObject(sideBar)
        AudioPlayer.setBackground(Sounds.getAudio("bk_1"))
        ObjectPool.addObject(TestEnemy0(230, 270, 100, "mirror") { _, _ -> })

        // start game
        GenericFlags.gameState.set(GenericFlags.STATE_PLAYING)
    }
}