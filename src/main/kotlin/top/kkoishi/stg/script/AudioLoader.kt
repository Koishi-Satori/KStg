package top.kkoishi.stg.script

import top.kkoishi.stg.Loader
import top.kkoishi.stg.logic.InfoSystem.Companion.logger

class AudioLoader : LocalVariables("audio_loader"), Loader {

    init {
        LocalVariables[scopeName] = this
    }

    private val logger = AudioLoader::class.logger()
    override fun loadDefinitions() {

    }
}