package top.kkoishi.stg.boot.jvm

/**
 * The entrance method of a plugin.
 *
 * The plugin should be a jar file which is located in [KStgEngineMain.plugin_dir], you can use the property
 * ```engine::plugin_dir``` in engine.ini to specify the dir stored plugins.
 *
 * If you want the plugin can be executed, you should implement this class as the plugin's main class.
 *
 * To implement this class, you also need a static method: ```fun getInstance(): JvmPlugin``` to allocate the instance, or the
 * engine will try to use reflection or unsafe to allocate.
 *
 * @author KKoishi_
 */
interface JvmPlugin {
    /**
     * Run the plugin with program arguments.
     *
     * @param args the program arguments.
     */
    fun main(args: Array<String>)

    /**
     * The plugin info, which will be print while loading.
     */
    fun info(): Array<String>
}