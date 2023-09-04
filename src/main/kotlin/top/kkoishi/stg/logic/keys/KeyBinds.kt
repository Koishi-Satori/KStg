package top.kkoishi.stg.logic.keys

import top.kkoishi.stg.common.entities.Object
import java.awt.Graphics2D
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.*
import javax.swing.JFrame

object KeyBinds {
    @JvmStatic
    private val lock = Any()

    @JvmStatic
    internal val keys = BooleanArray(526)

    @JvmStatic
    private val bindEvents = TreeMap<Int, KeyBindEvent<Object, Any>>()

    @JvmStatic
    private val genericBinds = TreeSet<Int>()

    @JvmStatic
    fun isPressed(keyCode: Int): Boolean = synchronized(lock) {
        return keys[keyCode]
    }

    @JvmStatic
    fun release(keyCode: Int) = synchronized(lock) {
        keys[keyCode] = false
    }

    @JvmStatic
    fun bind(f: JFrame) {
        f.addKeyListener(SimpleListener())
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : Object, R : Any> bind(keyCode: Int, event: KeyBindEvent<T, R>) {
        bindEvents[keyCode] = event as KeyBindEvent<Object, Any>
    }

    fun <T : Object, R : Any> bindGeneric(keyCode: Int, event: KeyBindEventWithCaller<T, R>) {
        bind(keyCode, event)
        genericBinds.add(keyCode)
    }

    @JvmStatic
    operator fun invoke(keyCode: Int, obj: Object) {
        synchronized(lock) {
            bindEvents[keyCode]?.invoke(obj)
        }
    }

    @JvmStatic
    internal fun invokeGenericBinds() = synchronized(lock) {
        genericBinds.forEach { if (keys[it]) bindEvents[it]?.invoke(FakeObject()) }
    }

    private class SimpleListener : KeyListener {
        override fun keyTyped(e: KeyEvent) {
        }

        override fun keyPressed(e: KeyEvent) {
            keys[e.keyCode] = true
        }

        override fun keyReleased(e: KeyEvent) {
            keys[e.keyCode] = false
        }
    }

    class FakeObject : Object {
        override val uuid: UUID
            get() = UUID.randomUUID()

        override fun update(): Boolean = false

        override fun collide(o: Object): Boolean = false

        override fun paint(g: Graphics2D) {}
    }
}