package top.kkoishi.stg.util

/**
 * The option class is designed to handle the program arguments.
 *
 * @param name the Option name.
 * @param hasArgs if the option has extra argument.
 * @param action the action.
 * @author KKoishi_
 */
@Suppress("MemberVisibilityCanBePrivate")
class Option(val hasArgs: Boolean, vararg val names: String, private val action: (Option, String) -> Unit) {
    private var hasOption: Boolean = false

    @Suppress("unused")
    fun hasOption() = hasOption

    fun matches(arg: String): Boolean {
        names.forEach {
            if (arg == it)
                return true
        }
        return false
    }

    /**
     * Invoke the option.
     *
     * @param extraArg extra arg if this option has extra argument, and it will be empty string if no extra arg.
     */
    @JvmOverloads
    operator fun invoke(extraArg: String = "") {
        action(this, extraArg)
        hasOption = true
    }

    override fun toString(): String {
        return "Option(hasArgs=$hasArgs, names=${names.contentToString()})"
    }

}