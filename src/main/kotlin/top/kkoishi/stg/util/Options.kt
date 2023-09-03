package top.kkoishi.stg.util

object Options {
    @JvmStatic
    private val recognizedOptions = ArrayDeque(
        listOf(Option(false, "-debug") { _, _ -> State.debug = true })
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

    internal object State {
        var debug: Boolean = false
    }
}