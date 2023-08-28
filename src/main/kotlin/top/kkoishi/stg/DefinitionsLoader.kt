package top.kkoishi.stg

interface DefinitionsLoader {
    fun loadDefinitions()

    companion object {
        private val registeredNames = ArrayDeque<String>()

        @JvmStatic
        fun register(name: String) = synchronized(registeredNames) {
            registeredNames.addLast(name)
        }

        @JvmStatic
        fun scriptNames(): Iterator<String> = synchronized(registeredNames) {
            return registeredNames.toTypedArray().iterator()
        }
    }
}