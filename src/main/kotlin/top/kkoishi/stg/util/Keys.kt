package top.kkoishi.stg.util

import top.kkoishi.stg.script.reflect.Reflection
import java.awt.event.KeyEvent
import java.util.TreeMap

object Keys {
    private val keyNames = initNames()

    private fun initNames(): TreeMap<Int, String> {
        val map = TreeMap<Int, String>()
        KeyEvent::class.java.fields.forEach {
            if (Reflection.isStatic(it) && Reflection.isFinal(it) && it.name.startsWith("VK_")) {
                val keyCode = it[null] as Int
                map[keyCode] = "${it.name.lowercase()}($keyCode)"
            }
        }
        return map
    }

    operator fun get(keyCode: Int): String {
        return keyNames[keyCode] ?: "unknown key($keyCode)"
    }
}