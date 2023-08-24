package top.kkoishi.stg

import top.kkoishi.stg.exceptions.FailedLoadingResourceException

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
interface Resources<ResourceType, LoadType> {
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

    companion object {
        internal const val KEY_NOT_FOUND = "NOT_FOUND"
    }
}