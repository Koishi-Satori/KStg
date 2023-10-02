package top.kkoishi.stg.boot.ui

import top.kkoishi.stg.Resources
import top.kkoishi.stg.boot.Bootstrapper
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Texture
import top.kkoishi.stg.localization.ClassLocalization
import top.kkoishi.stg.localization.LocalizationKey
import top.kkoishi.stg.logic.Threads
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode
import kotlin.collections.ArrayDeque
import kotlin.io.path.exists

object DanmakuDesigner : JFrame() {
    init {
        Bootstrapper.enableHardwareAccelerationProperties()
        Bootstrapper.readEngineSettings()
    }

    @JvmStatic
    val FONT = Font("JetBrains Mono", Font.BOLD, 13)

    @JvmStatic
    val ICON = ImageIcon(ImageIO.read(Resources.getEngineResources<InputStream>()))

    @JvmStatic
    fun main(args: Array<String>) {
        if (!checkLocale())
            locale = Locale.US
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        iconImage = ICON.image
        add(DesignerPanel)
        setSize(400, 400)
        jMenuBar = DesignerMenuBar
        title = DesignerLocalization.DESIGNER_UI_TITLE
        defaultCloseOperation = EXIT_ON_CLOSE

        isVisible = true
    }

    @JvmStatic
    private fun checkLocale(): Boolean {
        val ymlPath = "${Threads.workdir()}/localizations/designer_${locale}.yml"
        return Path.of(ymlPath).exists()
    }

    @JvmStatic
    private var locale = Locale.getDefault() ?: Locale.US

    private object DesignerPanel : JPanel(BorderLayout()) {
        private val editor = JTextPane()
        private val realBackground = createImage()
        private val backWidth = realBackground.width
        private val backHeight = realBackground.height
        private val rate = backWidth.toDouble() / backHeight

        var GFX_NODE: SourceTree.Node
        var SOUNDS_NODE: SourceTree.Node
        var FILE_NODE: SourceTree.Node

        val tabbed = JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
        val addedTabs = ArrayDeque<String>(8)

        private fun createImage(): BufferedImage {
            val texture = Texture(ImageIO.read(File("./test/load.jpg")))
            val op = texture.averageConvolve33(0.2f)
            val dst = op.createDestImage(texture())
            op.apply(dst.createGraphics(), texture())
            return dst
        }

        init {
            isOpaque = false
            editor.font = FONT
            editor.background = Color.BLACK
            editor.caretColor = Color.WHITE
            editor.foreground = Color.WHITE
            editor.isOpaque = false
            editor.autoscrolls = true
            val default = object : JPanel(BorderLayout()) {
                init {
                    isOpaque = false
                    background = Color.BLACK
                    val scroll = JScrollPane(editor)
                    scroll.isOpaque = false
                    scroll.viewport.isOpaque = false
                    add(scroll, BorderLayout.CENTER)
                }

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
                tree.addTreeSelectionListener {
                    val node = it.path.lastPathComponent
                    if (node is SourceTree.Node)
                        node()
                }
                val view = JScrollPane(tree)
                val pane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, view, tabbed)
                pane.dividerSize = 1
                pane.isOneTouchExpandable = true
                add(pane, BorderLayout.CENTER)
            }
            GFX_NODE.addNode(SourceTree.Node(0, "NOT_FOUND", false, action = {
                if (!addedTabs.contains("NOT_FOUND")) {
                    val insertPos = tabbed.tabCount
                    tabbed.insertTab("NOT_FOUND", ICON, TextureDisplayPanel(GFX.notFound()), "", insertPos)
                    tabbed.setTabComponentAt(insertPos, CloseableTab("NOT_FOUND", tabbed))
                    addedTabs.add("NOT_FOUND")
                }
            }))
            SOUNDS_NODE.addNode(SourceTree.Node(0, "NOT_FOUND", false))
            FILE_NODE.addNode(SourceTree.Node(2, DesignerLocalization.TREE_NODE_DEFINES))
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
            font = FONT
            val render = DefaultTreeCellRenderer()
            cellRenderer = render
        }

        class Node @JvmOverloads constructor(
            initialChildrenCount: Int,
            initValue: Any?,
            private val allowChildren: Boolean = true,
            parentNode: Node? = null,
            val action: () -> Unit = {},
        ) :
            MutableTreeNode {
            private val children = ArrayDeque<Node>(initialChildrenCount)

            @field: Volatile
            private var value: Any? = initValue

            @field: Volatile
            private var parent: Node? = parentNode

            operator fun invoke() = action()

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
                gfx.add(DesignerLocalization.FUNC_ADD_GFX)
                add(gfx)
                val sound = JMenu(DesignerLocalization.TITLE_MENU_SOUNDS)
                add(sound)
            }
            add(resources)
        }
    }

    private object DesignerLocalization :
        ClassLocalization<DesignerLocalization>(
            locale,
            DesignerLocalization::class.java,
            "${Threads.workdir()}/localizations/designer_${locale}.yml"
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
        @field: LocalizationKey("tree.root.info")
        lateinit var TREE_ROOT_INFO: String

        @JvmStatic
        @field: LocalizationKey("tree.node.defines")
        lateinit var TREE_NODE_DEFINES: String

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
        @field: LocalizationKey("btn.close.tooltip")
        lateinit var BTN_CLOSE_TOOLTIP: String
    }
}