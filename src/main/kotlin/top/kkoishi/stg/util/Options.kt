package top.kkoishi.stg.util

import top.kkoishi.stg.boot.jvm.KStgEngineMain

object Options {
    @JvmStatic
    private val recognizedOptions = ArrayDeque(
        listOf(
            Option(false, "-debug") { _, _ -> State.debug = true },
            Option(true, "-main-plugin", "-m") { _, plugin ->
                State.mainPlugin = plugin
            },
            Option(true, "-plugin-dir") { _, dir ->
                KStgEngineMain.plugin_dir(dir)
            }
        )
    )

    fun addOption(option: Option) = recognizedOptions.addLast(option)

    @Throws(IllegalArgumentException::class)
    fun handleArguments(args: Array<String>) = handleArguments(args.iterator())

    @Throws(IllegalArgumentException::class)
    private fun handleArguments(rest: Iterator<String>) {
        while (rest.hasNext()) {
            val arg = rest.next()
            handleOptions(arg, rest)
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun handleOptions(arg: String, rest: Iterator<String>) {
        recognizedOptions.forEach {
            if (it.matches(arg)) {
                if (it.hasArgs) {
                    if (!rest.hasNext())
                        throw IllegalArgumentException("$it requires extra arguments.")
                    it(rest.next())
                } else
                    it()
                return
            }
        }
    }

    object State {
        @JvmStatic
        var debug: Boolean = false

        @JvmStatic
        var mainPlugin: String? = null
    }
}