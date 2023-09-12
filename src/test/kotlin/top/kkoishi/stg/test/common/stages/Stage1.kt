package top.kkoishi.stg.test.common.stages

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.common.*
import top.kkoishi.stg.common.entities.Player
import top.kkoishi.stg.logic.GenericSystem
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.logic.Threads
import top.kkoishi.stg.replay.ReplayRecorder
import top.kkoishi.stg.test.common.GameSystem
import top.kkoishi.stg.test.common.actions.TestBoss0Action0
import top.kkoishi.stg.test.common.enemy.TestBoss0
import top.kkoishi.stg.test.common.enemy.TestEnemy0
import top.kkoishi.stg.test.common.enemy.TestEnemy1
import top.kkoishi.stg.test.common.render.TestStageClearObject
import java.awt.Graphics2D
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayDeque

class Stage1(player: Player, playerIndex: Int) : AbstractStage() {
    init {
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
        addAction(StageAction(10) {
            recorder.start()
            AudioPlayer.setBackground(Sounds.getAudio("bk_1"))
        })
        addAction(StageAction(40) {
            ObjectPool.addObject(TestEnemy0(230, 270, 100, "mirror") { _, _ -> })
        })
        addAction(object : StageAction(250L, action = {
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
        addAction(object : StageAction(100L, action = {
            val dialog = Dialogs.getDialog("test_0")
            if (dialog != null)
                ObjectPool.addObject(dialog)
            ObjectPool.addObject(
                TestBoss0(
                    230,
                    270,
                    TestBoss0Action0(2000, 2000L) { dialog?.isEnd() ?: true })
            )
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
        addAction(WaitStageAction(50L, action = {
            ObjectPool.addObject(TestStageClearObject(100, 100))
        }))
        addAction(WaitStageAction(100L, action = {
            AudioPlayer.setBackground(Sounds.getAudio("bk_0"))
            // switch to menu
            GameSystem.mainMenu.restore()
            GenericSystem.gameState.set(GenericSystem.STATE_MENU)
            recorder.save(
                "${Threads.workdir()}/replay",
                SimpleDateFormat("'KStg-TestReplay-'yyyy-MM-dd_HH.mm.ss").format(Date.from(Instant.now())),
                0L
            )
        }))
    }

    override fun backgroundName(): String = "bg_1_0"

    override fun nextStageImpl(): Stage? = null

    override fun toNextStage(): Boolean = false
}