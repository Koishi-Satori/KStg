import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.common.Player
import top.kkoishi.stg.common.PlayerBullet
import top.kkoishi.stg.common.Stage
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Renderer
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.logic.*
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import kotlin.system.exitProcess

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        GenericFlags.gameState.set(GenericFlags.STATE_PLAYING)
        GFX.loadTexture("test_img", "./test/gfx/icons/test_img.png")
        Sounds.loadAudio("test_bk", "./test/audio/sounds/test_bk.wav")

        //println(GFX.getTexture("test_img"))
        val f = JFrame("test")
        f.setSize(500, 500)
        f.isVisible = true
        f.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exitProcess(514)
            }
        })

        Graphics.refresh(f)
        PlayerManager.cur = Stage.Companion.EmptyStage()

        val t = GFX.getTexture("test_img")
        ObjectPool.player = object : Player(250, 30) {
            override fun bulletDamage(): Int = 10

            override fun bullet(): PlayerBullet = object : PlayerBullet(this.x.get(), this.y.get()) {
                override fun move() {
                    y.set(y.get() + 5)
                }

                override fun shape(): Shape = CollideSystem.Circle(Point(x.get(), y.get()), 5)

                override fun paint(g: Graphics2D) {
                    t.paint(Graphics.render(), t.normalMatrix(), x.get(), y.get())
                }
            }

            override fun shot() {
                println("shot")
                super.shot()
            }

            override fun texture(): String = "NOT_FOUND"

            override fun bomb() {
                println("bomb")
            }

            override fun dead() {
                println("dead")
            }

            override fun shape(): Shape = CollideSystem.Circle(Point(x.get(), y.get()), 5)
        }

        val inst = Threads.getInstance()
        Renderer.start(inst)
        GameLoop.start(inst)
        AudioPlayer.start(inst)

        AudioPlayer.setBackground(Sounds.getAudio("test_bk"))
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
            Player.VK_Z
        )
    }
}