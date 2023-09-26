package top.kkoishi.stg.boot.jvm

import java.net.URL
import java.net.URLClassLoader

class PluginClassLoader(urls: Array<out URL>): URLClassLoader(urls) {
    /**
     * Load and resolve class.
     */
    override fun loadClass(name: String?): Class<*> {
        return super.loadClass(name, true)
    }

    fun checkClass(clz: Class<*>): Boolean {
        val interfaces = clz.interfaces
        if (interfaces.isEmpty() || !interfaces.contains(JvmPlugin::class.java)) {
            val superClass = clz.superclass
            if (superClass == null || superClass == Any::class.java)
                return false
            return checkClass(superClass)
        }
        return true
    }
}