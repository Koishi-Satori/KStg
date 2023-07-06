import top.kkoishi.stg.common.Stage
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.Graphics
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.logic.PlayerManager
import javax.swing.JFrame
import kotlin.coroutines.suspendCoroutine

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        GFX.loadTexture("test_img", "./test/gfx/icons/test_img.png")
        //println(GFX.getTexture("test_img"))
        val f = JFrame("test")
        f.setSize(100, 100)
        f.isVisible = true
        Graphics.refresh(f)
        PlayerManager.cur = Stage.Companion.EmptyStage()
        ObjectPool.player
    }
}