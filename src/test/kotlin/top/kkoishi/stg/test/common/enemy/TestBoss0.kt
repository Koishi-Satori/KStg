package top.kkoishi.stg.test.common.enemy

import top.kkoishi.stg.audio.AudioPlayer
import top.kkoishi.stg.common.BossAction
import top.kkoishi.stg.common.entities.BaseBoss
import top.kkoishi.stg.common.entities.Object
import top.kkoishi.stg.gfx.CollideSystem
import top.kkoishi.stg.logic.ObjectPool
import top.kkoishi.stg.test.common.GameSystem
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Shape
import kotlin.math.absoluteValue

class TestBoss0(initialX: Int, initialY: Int, firstAction: BossAction) :
    BaseBoss(initialX, initialY, firstAction) {
    var state: Int = 0
    var which: Int = 0
    override fun texture(): String {
        if (which >= 40)
            which = 0
        return "test_boss_${state}_${(which++) / 10}"
    }

    override fun paintOthers(r: Graphics2D) {}

    override fun paintBossBar(r: Graphics2D) {}

    override fun dead() {
        AudioPlayer.addTask("enemy_dead")
        ObjectPool.player().setPlayerPower(3.1f)
        GameSystem.sideBar.add(100000L)
    }

    override fun beingHit(o: Object) {
        synchronized(lock) {
            health -= ObjectPool.player().bulletDamage()
            AudioPlayer.addTask("th15_enemy_damage_0${(GameSystem.rand.nextInt() % 2).absoluteValue + 1}")
        }
    }

    override fun move() {}
    override fun shape(): Shape = CollideSystem.Circle(Point(x().toInt(), y().toInt()), 5)
}