package top.kkoishi.stg.boot.jvm

/**
 * An annotation used to find the main class of the game.
 * The main method should be identified using its **jvm method descriptor without the type of the return value**.
 * And you should make sure the main method return **an int as the process's exit number** and it must be **static**.
 *
 * Simple illustration:
 * * The basic data type and "void" (void return type) are represented by a single letter in **upper case**, and what
 * is used to describe the type of object instance, is the **full-qualified class name** of its class.
 * * The full-qualified name is composed by the package path and the class name, using '\' as the separator among
 * different names, and it must end with a ';'.
 * * The representations of basic data type and void type: B-byte/[Byte], C-char/[Char], D-double/[Double], I-int/[Int],
 * J-long/[Long], S-short/[Short], Z-boolean,[Boolean], V-void/[Unit]
 * * For array instance, it uses a proposed '[' to represent a dimension. To illustrate, int[] can be parsed to [int,
 * and String[][] is [[Ljava/lang/String; .
 * * The method descriptor used in this annotation consists of the full-qualified name of the class to which the method
 * belongs, the method name, and the parameter list in the form of a descriptor, separated by dot, and the parameter
 * list is enclosed in parentheses, and because we do not care about the type of the return value, only require the
 * return of an int type, there is no descriptor for the return value.
 * (这个注解所使用的方法描述符由该方法所属类的全限定名、方法名称和描述符形式的参数列表组成，前两者中间使用dot隔开，并且参数列表使用括号括起来。
 * 同时因为我们不关心返回值的类型，只要求返回一个int类型，所以没有返回值的描述符。*他妈的什么长难句翻译，跟汉弗莱似的*)
 * * And if the static method belongs to the class which annotated with this, you can ignore the class name part, just
 * use the method name and its arguments simply.
 *
 * Some examples:
 * * the method descriptor of java.lang.String::contains(CharSequence) is
 * Ljava/lang/String.contains(Ljava/langCharSequence/;)
 * * the method descriptor of java.lang.String::replace(char oldChar, char newChar) is Ljava/lang/String.replace(CC)
 *
 * @author KKoishi_
 * @param main the jvm method descriptor of the main method.
 * @param public identify whether the method has public access.
 */
annotation class GameMainClass constructor(val main: String, val public: Boolean)
