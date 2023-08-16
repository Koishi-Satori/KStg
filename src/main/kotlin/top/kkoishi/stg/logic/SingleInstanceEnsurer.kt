package top.kkoishi.stg.logic

import top.kkoishi.stg.exceptions.InternalError
import top.kkoishi.stg.logic.InfoSystem.Companion.logger
import top.kkoishi.stg.logic.InfoSystem.Companion.log
import top.kkoishi.stg.logic.InfoSystem.Companion.with
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

object SingleInstanceEnsurer {
    lateinit var lockedFile: RandomAccessFile
    lateinit var fileLock: FileLock

    inline fun <reified T> setLockedFile(file: T) {
        val logger = this::class.logger()
        try {
            logger log "Try to lock $file" with System.Logger.Level.INFO
            lockedFile = if (T::class == File::class)
                RandomAccessFile(file as File, "rw")
            else if (T::class == String::class)
                RandomAccessFile(file as String, "rw")
            else
                throw InternalError("The type of $file should be File or String.")

            fileLock = lockedFile.channel.lock()
            logger log "$file is locked" with System.Logger.Level.INFO
        } catch (r: Throwable) {
            logger log r with System.Logger.Level.WARNING
        }
    }

    fun release() {
        if (this::fileLock.isInitialized) {
            fileLock.release()
            SingleInstanceEnsurer::class.logger() log "Release the file lock on $lockedFile" with System.Logger.Level.INFO
        }
        else
            SingleInstanceEnsurer::class.logger() log
                    "Failed to release file lock: FileLock is not initialized!" with System.Logger.Level.WARNING
    }
}