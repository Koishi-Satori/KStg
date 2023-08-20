package top.kkoishi.stg.test.common

import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.gfx.Graphics
import top.kkoishi.stg.logic.Threads

object GameSystem {
    val mainMenu = MainMenu()
    val rootMainMenu =
        MainMenu.MainMenuItem(
            50,
            150,
            mainMenu,
            ArrayDeque(
                listOf(
                    GFX.getTexture("main_menu_select_0"),
                    GFX.getTexture("main_menu_select_1"),
                    GFX.getTexture("main_menu_select_2"),
                    GFX.getTexture("main_menu_select_3")
                )
            )
        )

    init {
        val startSelectPlayer = MainMenu.PlayerSelectMenuItem(
            150,
            0,
            mainMenu,
            ArrayDeque(listOf(GFX.getTexture("player_koishi_me")))
        )
        startSelectPlayer.items.add(GFX.getTexture("player_koishi_description"))
        startSelectPlayer.children.add(null)
        startSelectPlayer.parent = rootMainMenu
        rootMainMenu.items.add(GFX.getTexture("main_menu_start"))
        rootMainMenu.items.add(GFX.getTexture("main_menu_extra_start"))
        rootMainMenu.items.add(GFX.getTexture("main_menu_practice"))
        rootMainMenu.items.add(GFX.getTexture("main_menu_exit"))
        rootMainMenu.children.add(startSelectPlayer)
        rootMainMenu.children.add(null)
        rootMainMenu.children.add(null)
        rootMainMenu.children.add(null)
        mainMenu.setRoot(rootMainMenu)
    }

    var randomSeed = Threads.randomSeed()
    var rand = Threads.random()
    val sideBar = GameSideBar()
    val players: Array<Player> = arrayOf(
        TestPlayerKoishi(Graphics.getCenterX(), 55, "bullet_koishi")
    )
}