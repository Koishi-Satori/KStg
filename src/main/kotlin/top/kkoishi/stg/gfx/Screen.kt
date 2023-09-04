package top.kkoishi.stg.gfx

import top.kkoishi.stg.logic.GenericSystem
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.logic.PlayerManager
import top.kkoishi.stg.logic.keys.KeyBinds
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream

object Screen {
    fun takeScreenshot(path: String) {
        val logger = KeyBinds::class.logger()
        KeyBinds.release(KeyEvent.VK_F11)
        // save screenshot
        logger.log(System.Logger.Level.INFO, "Ready to take screenshot.")
        val screen = Path.of(path)
        val out = screen.outputStream()
        val imageOut = ImageIO.createImageOutputStream(out)
        val img = BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB)
        val bRender = img.createGraphics()
        when (GenericSystem.gameState.get()) {
            GenericSystem.STATE_PLAYING -> {
                try {
                    PlayerManager.curStage.paint(bRender)
                    ObjectPool.player().paint(bRender)
                    ObjectPool.objects().forEach { it.paint(bRender) }
                    ObjectPool.bullets().forEach { it.paint(bRender) }
                    ObjectPool.uiObjects().forEach { it.paint(bRender) }
                    bRender.dispose()
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }

            GenericSystem.STATE_PAUSE -> {
                PlayerManager.curStage.paint(bRender)
                ObjectPool.player().paint(bRender)
                ObjectPool.objects().forEach { it.paint(bRender) }
                ObjectPool.bullets().forEach { it.paint(bRender) }
                ObjectPool.uiObjects().forEach { it.paint(bRender) }
            }

            GenericSystem.STATE_MENU -> {
                try {
                    // main menu
                    ObjectPool.uiObjects().forEach { it.paint(bRender) }
                } catch (e: Throwable) {
                    logger.log(System.Logger.Level.ERROR, e)
                }
            }
        }
        bRender.dispose()
        ImageIO.write(img, "png", imageOut)
        imageOut.close()
        out.close()
        logger.log(System.Logger.Level.INFO, "Take screenshot as ${screen.absolutePathString()}")
    }
}