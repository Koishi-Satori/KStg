package top.kkoishi.stg.boot.ui

import top.kkoishi.stg.Resources
import top.kkoishi.stg.boot.Bootstrapper
import top.kkoishi.stg.boot.Settings
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.localization.ClassLocalization
import top.kkoishi.stg.localization.LocalizationKey
import top.kkoishi.stg.logic.GenericSystem
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.Threads
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.plaf.FontUIResource
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode
import kotlin.collections.ArrayDeque
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.inputStream

object DanmakuDesigner : JFrame() {
    init {
        Bootstrapper.enableHardwareAccelerationProperties()
        GenericSystem.logToFile = true
        Bootstrapper.readEngineSettings()
    }

    @JvmStatic
    private val ICON = ImageIcon(ImageIO.read(Resources.getEngineResources<InputStream>()))

    @JvmStatic
    private val DESIGNER_DIR = "${Threads.workdir()}/designer"

    @JvmStatic
    private val DESIGNER_SETTINGS: Settings<String> = Settings.INI("$DESIGNER_DIR/config.ini")

    @JvmStatic
    private var FONT_SIZE = 12

    @JvmStatic
    private var FONT_TYPE = Font.PLAIN

    @JvmStatic
    private var FONT_PATH: String? = null

    @JvmStatic
    private val DEFAULT_FONT: Font

    @JvmStatic
    private var FONT: Font

    init {
        DESIGNER_SETTINGS.addHandler("Fonts::size") {
            try {
                FONT_SIZE = it.toInt()
                DanmakuDesigner::class.logger().log(System.Logger.Level.INFO, "Load font size: $it")
            } catch (e: NumberFormatException) {
                DanmakuDesigner::class.logger().log(System.Logger.Level.WARNING, "Can not initialize font size.")
                DanmakuDesigner::class.logger().log(System.Logger.Level.TRACE, e)
            }
        }
        DESIGNER_SETTINGS.addHandler("Fonts::type") {
            when (it.hashCode()) {
                "PLAIN".hashCode() -> if (it == "PLAIN")
                    FONT_TYPE = Font.PLAIN

                "ITALIC".hashCode() -> if (it == "ITALIC")
                    FONT_TYPE = Font.ITALIC

                "BOLD".hashCode() -> if (it == "BOLD")
                    FONT_TYPE = Font.BOLD

                "ITALIC_BOLD".hashCode() -> if (it == "ITALIC_BOLD")
                    FONT_TYPE = Font.ITALIC + Font.BOLD

                else -> {
                    DanmakuDesigner::class.logger()
                        .log(java.lang.System.Logger.Level.WARNING, "Can not initialize font type: $it")
                    return@addHandler
                }
            }
            DanmakuDesigner::class.logger().log(System.Logger.Level.INFO, "Load font style: $it")
        }
        DESIGNER_SETTINGS.addHandler("Fonts::path") {
            try {
                val verifyPath = Path.of("$DESIGNER_DIR/$it")
                FONT_PATH = verifyPath.toRealPath().toString()
                DanmakuDesigner::class.logger().log(System.Logger.Level.INFO, "Load font path: $FONT_PATH")
            } catch (e: IOException) {
                DanmakuDesigner::class.logger().log(System.Logger.Level.WARNING, "Can not initialize font path: $it")
                DanmakuDesigner::class.logger().log(System.Logger.Level.TRACE, e)
            }
        }

        val succ = DESIGNER_SETTINGS.read()
        val defaultFontPath = Path.of("$DESIGNER_DIR/fonts/JetBrainsMono-Medium.ttf")
        DEFAULT_FONT = if (!defaultFontPath.exists())
            GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts[0]
        else
            Font.createFont(
                Font.TRUETYPE_FONT,
                Path.of("$DESIGNER_DIR/fonts/JetBrainsMono-Medium.ttf").inputStream()
            )

        FONT = DEFAULT_FONT

        if (succ) {
            DESIGNER_SETTINGS.load()
            if (FONT_PATH != null) {
                val ttfFont = Font.createFont(Font.TRUETYPE_FONT, FONT_PATH?.let { Path.of(it).inputStream() })
                FONT = ttfFont.deriveFont(FONT_TYPE).deriveFont(FONT_SIZE.toFloat())
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (!checkLocale())
            locale = Locale.US
        runCatching { UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel") }
            .onFailure {
                DanmakuDesigner::class.logger().log(System.Logger.Level.TRACE, it)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            }
        val fontResources = FontUIResource(FONT)
        UIManager.getDefaults().keys.forEach {
            UIManager.get(it).castApply { _: FontUIResource -> UIManager.put(it, fontResources) }
        }

        iconImage = ICON.image
        add(DesignerPanel)
        setSize(400, 400)
        jMenuBar = DesignerMenuBar
        title = DesignerLocalization.DESIGNER_UI_TITLE
        defaultCloseOperation = EXIT_ON_CLOSE

        isVisible = true
    }

    @JvmStatic
    private inline fun <T, reified R> T.castApply(action: (R) -> Any?) {
        run { }
        if (this is R)
            action(this)
    }

    @JvmStatic
    private fun <T, R> T.runInCycle(interval: Long = 20, action: T.() -> R?): R {
        var result: R?
        while (true) {
            result = action()
            if (result != null)
                break
            Thread.sleep(interval)
        }
        return result!!
    }

    @JvmStatic
    private fun checkLocale(): Boolean {
        val ymlPath = "${Threads.workdir()}/localizations/designer_${locale}.yml"
        return Path.of(ymlPath).exists()
    }

    @JvmStatic
    private var locale = Locale.getDefault() ?: Locale.US

    internal object DesignerPanel : JPanel(BorderLayout()) {
        private val editor = JTextPane()
        private val realBackground = createImage()
        private val backWidth = realBackground.width
        private val backHeight = realBackground.height
        private val rate = backWidth.toDouble() / backHeight

        private var GFX_NODE: SourceTree.Node
        private var SOUNDS_NODE: SourceTree.Node
        private var FILE_NODE: SourceTree.Node

        private val tabbed = JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
        private val addedTabs = ArrayDeque<String>(8)

        private val gfxEditor = JTextPane()
        private val soundsEditor = JTextPane()

        private fun createImage(): BufferedImage {
            val texture = Texture(ImageIO.read(Resources.getEngineResources<InputStream>()))
            val op = texture.averageConvolve33(0.2f)
            val dst = op.createDestImage(texture())
            op.apply(dst.createGraphics(), texture())
            return dst
        }

        init {
            isOpaque = false
            with(editor) {
                background = Color.BLACK
                caretColor = Color.WHITE
                foreground = Color.WHITE
                isOpaque = false
                autoscrolls = true
            }
            with(gfxEditor) {
                background = Color.BLACK
                caretColor = Color.WHITE
                foreground = Color.WHITE
                isOpaque = false
                autoscrolls = true
            }
            with(soundsEditor) {
                background = Color.BLACK
                caretColor = Color.WHITE
                foreground = Color.WHITE
                isOpaque = false
                autoscrolls = true
            }

            val default = createBackgroundPanel().apply { isOpaque = false }.apply { background = Color.BLACK }
            val scroll = JScrollPane(editor)
            default.add(scroll.apply { isOpaque = false }.apply { viewport.isOpaque = false }, BorderLayout.CENTER)

            val gfx = createBackgroundPanel().apply { isOpaque = false }.apply { background = Color.BLACK }
            val gfxScroll = JScrollPane(gfxEditor)
            gfx.add(gfxScroll.apply { isOpaque = false }.apply { viewport.isOpaque = false }, BorderLayout.CENTER)
            val sounds = createBackgroundPanel().apply { isOpaque = false }.apply { background = Color.BLACK }
            val soundsScroll = JScrollPane(soundsEditor)
            sounds.add(soundsScroll.apply { isOpaque = false }.apply { viewport.isOpaque = false }, BorderLayout.CENTER)

            tabbed.insertTab(
                DesignerLocalization.TAB_TITLE_DEFAULT,
                ICON,
                default,
                DesignerLocalization.TAB_TOOLTIP_DEFAULT,
                0
            )
            tabbed.setTabComponentAt(0, CloseableTab(DesignerLocalization.TAB_TITLE_DEFAULT, tabbed))
            tabbed.isOpaque = true

            with(SourceTree.Node(3, DesignerLocalization.TREE_ROOT_INFO)) {
                GFX_NODE = addNode(SourceTree.Node(16, DesignerLocalization.TREE_ROOT_GFX))
                SOUNDS_NODE = addNode(SourceTree.Node(16, DesignerLocalization.TREE_ROOT_SOUNDS))
                FILE_NODE = addNode(SourceTree.Node(16, DesignerLocalization.TREE_ROOT_FILE))
                val tree = SourceTree(this)
                tree.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        tree.getPathForLocation(e.x, e.y)?.let {
                            it.lastPathComponent.castApply { node: SourceTree.Node -> node(e.button, e.clickCount) }
                        }
                    }
                })
                val view = JScrollPane(tree)
                val pane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, view, tabbed)
                pane.dividerSize = 1
                pane.isOneTouchExpandable = true
                add(pane, BorderLayout.CENTER)
            }
            GFX_NODE.addNode(SourceTree.Node(0, "NOT_FOUND", false) { btn, count ->
                if (btn == 1 && count == 2 && !addedTabs.contains("NOT_FOUND")) {
                    val insertPos = tabbed.tabCount
                    tabbed.insertTab("NOT_FOUND", ICON, TextureDisplayPanel(GFX.notFound()), "", insertPos)
                    tabbed.setTabComponentAt(insertPos, CloseableTab("NOT_FOUND", tabbed))
                    addedTabs.add("NOT_FOUND")
                    tabbed.selectedIndex = insertPos
                }
            })
            SOUNDS_NODE.addNode(SourceTree.Node(0, "NOT_FOUND", false))
            FILE_NODE.addNode(SourceTree.Node(2, DesignerLocalization.TREE_NODE_DEFINES)).apply {
                addNode(SourceTree.Node(0, DesignerLocalization.TREE_NODE_DEFINES_GFX, false) { btn, count ->
                    val key = DesignerLocalization.TAB_TITLE_EDITOR_DEFINES.format(DesignerLocalization.CONST_DEFINES_GFX)
                    if (btn == 1 && count == 2 && !addedTabs.contains(key)) {
                        val insertPos = tabbed.tabCount
                        tabbed.insertTab(
                            key,
                            ICON,
                            gfx,
                            DesignerLocalization.TAB_TOOLTIP_DEFAULT,
                            insertPos
                        )
                        tabbed.setTabComponentAt(insertPos, CloseableTab(key, tabbed))
                        addedTabs.add(key)
                        tabbed.selectedIndex = insertPos
                    }
                })
                addNode(SourceTree.Node(0, DesignerLocalization.TREE_NODE_DEFINES_SOUNDS, false) { btn, count ->
                    val key = DesignerLocalization.TAB_TITLE_EDITOR_DEFINES.format(DesignerLocalization.CONST_DEFINES_SOUNDS)
                    if (btn == 1 && count == 2 && !addedTabs.contains(key)) {
                        val insertPos = tabbed.tabCount
                        tabbed.insertTab(
                            key,
                            ICON,
                            sounds,
                            DesignerLocalization.TAB_TOOLTIP_DEFAULT,
                            insertPos
                        )
                        tabbed.setTabComponentAt(insertPos, CloseableTab(key, tabbed))
                        addedTabs.add(key)
                        tabbed.selectedIndex = insertPos
                    }
                })
            }
            FILE_NODE.addNode(SourceTree.Node(2, DesignerLocalization.TREE_NODE_IMPL))
        }

        fun removeTab(title: String, comp: Component) {
            val index = tabbed.indexOfTabComponent(comp)
            if (index != -1 && tabbed.tabCount > 1) {
                tabbed.selectedIndex = if (index > 0) index - 1 else 0
                tabbed.removeTabAt(index)
                addedTabs.remove(title)
            }
        }

        private fun createBackgroundPanel() = object : JPanel(BorderLayout()) {
            override fun paint(g: Graphics) {
                val targetRate = width / height.toDouble()
                var dx = 0
                var dy = 0
                val oScale: Double
                if (targetRate > rate) {
                    oScale = width.toDouble() / backWidth
                    dx = ((width - oScale * backWidth) / 2).toInt()
                } else {
                    oScale = height.toDouble() / backHeight
                    dy = ((height - oScale * backHeight) / 2).toInt()
                }
                (g as Graphics2D).drawImage(
                    realBackground,
                    AffineTransformOp(
                        AffineTransform.getScaleInstance(oScale, oScale),
                        AffineTransformOp.TYPE_NEAREST_NEIGHBOR
                    ), dx, dy
                )
                super.paint(g)
            }
        }

        fun addGFX(key: String, path: String): Boolean {
            if (GFX[key] != GFX.notFound())
                return false
            GFX.loadTexture(key, path)
            val texture = GFX[key]
            GFX_NODE.addNode(SourceTree.Node(0, key, false) { btn, count ->
                if (btn == 1 && count == 2) {
                    val insertPos = tabbed.tabCount
                    tabbed.insertTab(key, ICON, TextureDisplayPanel(texture), path, insertPos)
                    tabbed.setTabComponentAt(insertPos, CloseableTab(key, tabbed))
                    addedTabs.add(key)
                }
            })
            return true
        }
    }

    private class CloseableTab(tabTitle: String, private val tab: JTabbedPane) : JPanel(BorderLayout()) {
        init {
            val title = JLabel(tabTitle)
            title.isOpaque = false
            add(title, BorderLayout.CENTER)
            val btn = JButton("x")
            btn.addActionListener {
                val index = tab.indexOfTabComponent(this)
                if (index != -1 && tab.tabCount > 1)
                    DesignerPanel.removeTab(tabTitle, this)
            }
            btn.toolTipText = DesignerLocalization.BTN_CLOSE_TOOLTIP
            add(btn, BorderLayout.EAST)
        }
    }

    private class TextureDisplayPanel(texture: Texture) : JPanel(BorderLayout()) {
        init {
            add(object : JLabel() {
                override fun paint(g: Graphics?) {
                    if (g is Graphics2D)
                        texture.paint(g, texture.normalMatrix(), 0, 0)
                    else
                        g?.drawImage(texture(), 0, 0, null)
                }
            }, BorderLayout.CENTER)
        }
    }

    private class SourceTree(initialNode: Node) : JTree(initialNode) {
        init {
            val render = DefaultTreeCellRenderer()
            cellRenderer = render
        }

        class Node @JvmOverloads constructor(
            initialChildrenCount: Int,
            initValue: Any?,
            private val allowChildren: Boolean = true,
            parentNode: Node? = null,
            val action: (Int, Int) -> Unit = { _, _ -> },
        ) :
            MutableTreeNode {
            private val children = ArrayDeque<Node>(initialChildrenCount)

            @field: Volatile
            private var value: Any? = initValue

            @field: Volatile
            private var parent: Node? = parentNode

            operator fun invoke(button: Int, count: Int) = action(button, count)

            fun addNode(nNode: Node): Node {
                children.addLast(nNode)
                nNode.parent = this
                return nNode
            }

            private fun getEnumeration() = with(children.iterator()) {
                object : Enumeration<Node> {
                    override fun hasMoreElements(): Boolean = hasNext()

                    override fun nextElement(): Node = next()
                }
            }

            override fun getChildAt(childIndex: Int): Node = children[childIndex]

            override fun getChildCount(): Int = children.size

            override fun getParent(): Node? = parent

            override fun getIndex(node: TreeNode?): Int = children.indexOf(node)

            override fun getAllowsChildren(): Boolean = allowChildren

            override fun isLeaf(): Boolean = children.isEmpty()

            override fun children(): Enumeration<out TreeNode> = getEnumeration()

            override fun insert(child: MutableTreeNode?, index: Int) = children.add(index, child as Node)

            override fun remove(index: Int) {
                children.removeAt(index)
            }

            override fun remove(node: MutableTreeNode?) {
                val index = getIndex(node)
                if (index != -1)
                    remove(index)
            }

            override fun setUserObject(obj: Any?) {
                value = obj
            }

            override fun removeFromParent() {
                this.parent?.remove(this)
                this.parent = null
            }

            override fun setParent(newParent: MutableTreeNode?) {
                if (newParent !is Node?)
                    throw IllegalArgumentException("$newParent should be an instance of ${Node::class}")
                newParent?.insert(this, newParent.childCount)
                parent = newParent
            }

            override fun toString(): String = value.toString()
        }
    }

    private object DesignerMenuBar : JMenuBar() {
        init {
            val resources = JMenu(DesignerLocalization.TITLE_MENU_RESOURCES)
            with(resources) {
                val gfx = JMenu(DesignerLocalization.TITLE_MENU_GFX)
                val addGFX = gfx.add(DesignerLocalization.FUNC_ADD_GFX)
                addGFX.addActionListener {
                    thread {
                        val input = AddInputDialog(
                            DesignerLocalization.TITLE_DIALOG_ADD,
                            DesignerLocalization.LAB_ADD_KEY,
                            DesignerLocalization.LAB_ADD_VALUE
                        )
                        val res = runInCycle { input.inputReturn() }
                        with(DanmakuDesigner::class.logger()) {
                            log(System.Logger.Level.INFO, "Dialog Result: $res")
                            if (res.first) {
                                val key = res.second
                                val value = res.third
                                DesignerPanel.addGFX(key, value)
                            }
                        }
                    }
                }
                add(gfx)
                val sound = JMenu(DesignerLocalization.TITLE_MENU_SOUNDS)
                add(sound)
            }
            add(resources)
        }
    }

    private class AddInputDialog(title: String, inputKeyKey: String, inputValueKey: String) : JDialog(this, title) {
        private var closeNormal = false
        private val inputKeyKeyField = JTextField(20)
        private val inputValueKeyField = JTextField(20)

        init {
            val mainPanel = JPanel(GridLayout(3, 1))
            val keyPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            keyPanel.add(JLabel(inputKeyKey))
            keyPanel.add(inputKeyKeyField)
            mainPanel.add(keyPanel)
            val valuePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            valuePanel.add(JLabel(inputValueKey))
            valuePanel.add(inputValueKeyField)
            mainPanel.add(valuePanel)
            val btnPanel = JPanel(FlowLayout())
            btnPanel.add(JButton(DesignerLocalization.BTN_CONFIRM_TITLE).apply {
                addActionListener {
                    closeNormal = true
                    dispose()
                }
            })
            btnPanel.add(JButton(DesignerLocalization.BTN_CANCEL_TITLE).apply {
                addActionListener {
                    dispose()
                }
            })
            mainPanel.add(btnPanel)
            add(mainPanel)
            setSize(250, 125)
            isResizable = false
            isVisible = true
        }

        fun inputReturn(): Triple<Boolean, String, String>? =
            if (isShowing) null else Triple(closeNormal, inputKeyKeyField.text, inputValueKeyField.text)
    }

    private object DesignerLocalization :
        ClassLocalization<DesignerLocalization>(
            locale,
            DesignerLocalization::class.java,
            "$DESIGNER_DIR/designer_ui_${locale}.yml"
        ) {
        override fun constantFieldsName(): Array<String>? = null

        override fun reference(): DesignerLocalization = this

        @JvmStatic
        @field: LocalizationKey("designer.ui.title")
        lateinit var DESIGNER_UI_TITLE: String

        @JvmStatic
        @field: LocalizationKey("title.menu.resources")
        lateinit var TITLE_MENU_RESOURCES: String

        @JvmStatic
        @field: LocalizationKey("title.menu.gfx")
        lateinit var TITLE_MENU_GFX: String

        @JvmStatic
        @field: LocalizationKey("title.menu.sounds")
        lateinit var TITLE_MENU_SOUNDS: String

        @JvmStatic
        @field: LocalizationKey("title.dialog.add")
        lateinit var TITLE_DIALOG_ADD: String

        @JvmStatic
        @field: LocalizationKey("tree.root.info")
        lateinit var TREE_ROOT_INFO: String

        @JvmStatic
        @field: LocalizationKey("tree.node.defines")
        lateinit var TREE_NODE_DEFINES: String

        @JvmStatic
        @field: LocalizationKey("tree.node.defines.gfx")
        lateinit var TREE_NODE_DEFINES_GFX: String

        @JvmStatic
        @field: LocalizationKey("tree.node.defines.sounds")
        lateinit var TREE_NODE_DEFINES_SOUNDS: String

        @JvmStatic
        @field: LocalizationKey("tree.node.impl")
        lateinit var TREE_NODE_IMPL: String

        @JvmStatic
        @field: LocalizationKey("tree.root.gfx")
        lateinit var TREE_ROOT_GFX: String

        @JvmStatic
        @field: LocalizationKey("tree.root.sounds")
        lateinit var TREE_ROOT_SOUNDS: String

        @JvmStatic
        @field: LocalizationKey("tree.root.file")
        lateinit var TREE_ROOT_FILE: String

        @JvmStatic
        @field: LocalizationKey("func.add.gfx")
        lateinit var FUNC_ADD_GFX: String

        @JvmStatic
        @field: LocalizationKey("tabs.title.default")
        lateinit var TAB_TITLE_DEFAULT: String

        @JvmStatic
        @field: LocalizationKey("tabs.tooltip.default")
        lateinit var TAB_TOOLTIP_DEFAULT: String

        @JvmStatic
        @field: LocalizationKey("tab.title.editor.defines")
        lateinit var TAB_TITLE_EDITOR_DEFINES: String

        @JvmStatic
        @field: LocalizationKey("btn.close.tooltip")
        lateinit var BTN_CLOSE_TOOLTIP: String

        @JvmStatic
        @field: LocalizationKey("btn.confirm.title")
        lateinit var BTN_CONFIRM_TITLE: String

        @JvmStatic
        @field: LocalizationKey("btn.cancel.title")
        lateinit var BTN_CANCEL_TITLE: String

        @JvmStatic
        @field: LocalizationKey("lab.add.key")
        lateinit var LAB_ADD_KEY: String

        @JvmStatic
        @field: LocalizationKey("lab.add.value")
        lateinit var LAB_ADD_VALUE: String

        @JvmStatic
        @field: LocalizationKey("const.defines.gfx")
        lateinit var CONST_DEFINES_GFX: String


        @JvmStatic
        @field: LocalizationKey("const.defines.sounds")
        lateinit var CONST_DEFINES_SOUNDS: String
    }
}