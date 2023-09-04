package top.kkoishi.stg.boot

import top.kkoishi.stg.Resources
import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.gfx.Renderer
import top.kkoishi.stg.logic.*
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.keys.KeyBinds
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.JFrame

object FastBootstrapper {
    @JvmStatic
    @Suppress("NOTHING_TO_INLINE")
    inline fun setAccelerationProperties() {
        System.setProperty("sun.java2d.ddscale", "true")
        System.setProperty("sun.java2d.opengl", "true")
        System.setProperty("swing.aatext", "true")
        System.setProperty("awt.nativeDoubleBuffering", "true")
    }

    @JvmStatic
    fun setIconImage(f: JFrame, pathName: String): JFrame {
        try {
            f.iconImage = ImageIO.read(File(pathName))
        } catch (e: IOException) {
            FastBootstrapper::class.logger().log(System.Logger.Level.WARNING, e)
        }
        return f
    }

    fun useEngineIconImage(f: JFrame): JFrame {
        try {
            f.iconImage = ImageIO.read(Resources.getEngineResources())
        } catch (e: IOException) {
            FastBootstrapper::class.logger().log(System.Logger.Level.WARNING, e)
        }
        return f
    }

    @JvmStatic
    fun keyBinds(f: JFrame): JFrame {
        KeyBinds.bind(f)
        return f
    }

    @JvmStatic
    fun autoSync(f: JFrame): JFrame {
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

        return f
    }

    @JvmStatic
    @JvmOverloads
    fun display(f: JFrame, width: Int, height: Int, uiInsets: Insets, fullscreen: Boolean = false): JFrame {
        f.isResizable = false
        f.isUndecorated = fullscreen
        f.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        f.ignoreRepaint = true
        if (fullscreen) {
            f.setSize(width, height)
            f.isVisible = true
            Graphics.refresh(f)
            Graphics.setScreenSize(Dimension(width, height))
            Graphics.setBufferSize(width, height)

            if (GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.isFullScreenSupported)
                GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.fullScreenWindow = f
            else
                f.size = Renderer.monitorSize()
            Renderer.fullScreen()
        } else {
            f.setSize(width + 14, height + 37)
            f.isVisible = true
            Graphics.refresh(f)
            Graphics.setScreenSize(Dimension(width, height))
            Graphics.setBufferSize(width, height)
        }
        with(uiInsets) {
            Graphics.setUIInsets(top, left, bottom, right)
        }
        //f.createBufferStrategy(2)

        return f
    }

    @JvmStatic
    fun display(f: JFrame, width: Int, height: Int, uiInsets: Insets, scaledWidth: Int, scaledHeight: Int): JFrame {
        f.isResizable = false
        f.setSize(width, height)
        f.isVisible = true
        Graphics.refresh(f)
        Graphics.setScreenSize(Dimension(width, height))
        Graphics.setBufferSize(width, height)

        f.size = Renderer.actualScaledSize(scaledWidth, scaledHeight)
        Renderer.scale(scaledWidth, scaledHeight)

        with(uiInsets) {
            Graphics.setUIInsets(top, left, bottom, right)
        }

        return f
    }

    @JvmStatic
    fun beginThreads() {
        val inst = Threads.getInstance()
        InfoSystem.start(inst)
        Renderer.start(inst)
        GameLoop.start(inst)
        AudioPlayer.start(inst)
    }
}