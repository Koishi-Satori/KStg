package top.kkoishi.stg

import top.kkoishi.stg.audio.Sounds
import top.kkoishi.stg.boot.Bootstrapper
import top.kkoishi.stg.boot.ui.DanmakuDesigner
import top.kkoishi.stg.exceptions.CrashReportGenerator
import top.kkoishi.stg.exceptions.FailedLoadingResourceException
import top.kkoishi.stg.gfx.GFX
import top.kkoishi.stg.logic.InfoSystem.Companion.logger

/**
 * An interface declared that the class which implements this, is used to manage the game resources
 * like texture, sounds, and so on.
 *
 * @see top.kkoishi.stg.gfx.GFX
 * @see top.kkoishi.stg.audio.Sounds
 * @param ResourceType the type of game resources.
 * @param LoadType the type of value required to load the game resource.
 * @author KKoishi_
 */
internal interface Resources<ResourceType, LoadType> {
    /**
     * Get the [ResourceType] by provide the specified key.
     *
     * @param key the key of the resource.
     * @return [ResourceType]
     */
    operator fun get(key: String): ResourceType

    /**
     * Load the [ResourceType] by provide the specified [LoadType], and related it with the key..
     *
     * @param key the key of the resource.
     * @param value [LoadType]
     */

    @Throws(FailedLoadingResourceException::class)
    operator fun set(key: String, value: LoadType)

    fun keys(): Array<String>

    fun resources() = keys().map { it to this[it] }.toTypedArray()

    companion object {
        internal const val KEY_NOT_FOUND = "NOT_FOUND"

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        internal inline fun <T> getEngineResources(): T? {
            val callerClass = getCallerClass()
            callerClass.kotlin.logger()
                .log(System.Logger.Level.INFO, "Try to get engine resources, CallerClass: $callerClass")
            return when (callerClass) {
                GFX.javaClass ->
                    Companion::class.java.getResourceAsStream("TEXTURE_NOT_FOUND.png") as T

                Sounds.javaClass ->
                    Companion::class.java.getResourceAsStream("SOUND_NOT_FOUND.wav") as T

                CrashReportGenerator::class.java ->
                    Companion::class.java.getResourceAsStream(".comments") as T

                Bootstrapper::class.java, DanmakuDesigner::class.java ->
                    Companion::class.java.getResourceAsStream("logo.ico") as T

                DanmakuDesigner.DesignerPanel::class.java ->
                    Companion::class.java.getResourceAsStream("designer_background.jpg") as T

                else -> throw SecurityException("$callerClass is not permitted.")
            }
        }
    }
}