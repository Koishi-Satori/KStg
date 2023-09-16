@file:Suppress("MemberVisibilityCanBePrivate")

package top.kkoishi.stg.util

import io.sigpipe.jbsdiff.DefaultDiffSettings
import io.sigpipe.jbsdiff.Diff
import io.sigpipe.jbsdiff.InvalidHeaderException
import io.sigpipe.jbsdiff.Patch
import org.apache.commons.compress.compressors.CompressorException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.System.arraycopy

/**
 * A simple interface for the bsdiff algorithm.
 *
 * This class uses ```io.sigpipe:jbsdiff:1.0```.
 *
 * @author KKoishi_
 */
object Bsdiff {
    /**
     * The compressor used for compress the patch file.
     *
     * You can set the compressor by setting the jvm system property "kkoishi.bsdiff.compressor".
     *
     * e.g. -Dkkoishi.bsdiff.compressor=bzip2
     */
    @JvmStatic
    val compressType: String = System.getProperty("kkoishi.bsdiff.compressor", "bzip2")

    /**
     * The default patch file name.
     */
    const val DEFAULT_PATCH_FILE = "patch.bin"

    /**
     * Using two different versions of a file/binary data, generate a bsdiff patch file, which its name is given,
     * that can be applied to the old file to create the new file.
     *
     * If the patch file name is not given, it will be [DEFAULT_PATCH_FILE] by default;if the give patch file already
     * exists, it will be overwritten.
     *
     * @param oldInputStream an InputStream which holds the old version binary data/file.
     * @param newInputStream an InputStream which holds the new version binary data/file.
     * @param patchFileName the path where the patch file will be stored, and it will be [DEFAULT_PATCH_FILE] by
     * default if this parameter is not specified.
     *
     * @throws IOException when an I/O error occurs / when an error occurs writing the bsdiff control blocks.
     * @throws CompressorException when a compression error occurs.
     * @throws InvalidHeaderException when the bsdiff header is malformed or not present.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class, CompressorException::class, InvalidHeaderException::class)
    fun diff(
        oldInputStream: InputStream,
        newInputStream: InputStream,
        patchFileName: String = DEFAULT_PATCH_FILE,
    ): File {
        val oldBytes = oldInputStream.readContent()
        val newBytes = newInputStream.readAllBytes()
        val patchFile = File(patchFileName)
        if (!patchFile.exists())
            patchFile.createNewFile()
        val out = patchFile.outputStream()

        Diff.diff(oldBytes, newBytes, out, DefaultDiffSettings(compressType))
        out.close()
        return patchFile
    }

    /**
     * Using the old version of a file/binary data and its accompanying patch file, this method generates a new
     * (updated) version of file/binary data and writes it to the given OutputStream.
     *
     * The OutputStream will be auto closed, if [autoClose] is true.
     *
     * @param oldInputStream an InputStream which holds the old version binary data/file.
     * @param out an OutputStream which used for write the new version binary data/file.
     * @param patch a binary patch file to apply to the old state
     * @param autoClose if auto close [out].
     *
     * @throws IOException when an I/O error occurs.
     * @throws CompressorException when a compression error occurs.
     * @throws InvalidHeaderException when the bsdiff header is malformed or not present.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class, CompressorException::class, InvalidHeaderException::class)
    fun patch(oldInputStream: InputStream, out: OutputStream, patch: File, autoClose: Boolean = true) {
        val oldBytes = oldInputStream.readContent()
        val patchBytes = patch.inputStream().readContent()

        Patch.patch(oldBytes, patchBytes, out)
        if (autoClose)
            out.close()
    }

    @JvmStatic
    @JvmOverloads
    fun diff(oldFile: File, newFile: File, patchFileName: String = DEFAULT_PATCH_FILE): File =
        diff(oldFile.inputStream(), newFile.inputStream(), patchFileName)

    @JvmStatic
    @JvmOverloads
    fun diff(oldFileName: String, newFileName: String, patchFileName: String = DEFAULT_PATCH_FILE): File =
        diff(File(oldFileName), File(newFileName), patchFileName)

    @JvmStatic
    @JvmOverloads
    fun patch(oldFile: File, outFile: File, patch: File, autoClose: Boolean = true) =
        patch(oldFile.inputStream(), outFile.outputStream(), patch, autoClose)

    @JvmStatic
    @JvmOverloads
    fun patch(oldFileName: String, outFileName: String, patch: File, autoClose: Boolean = true) =
        patch(File(oldFileName), File(outFileName), patch, autoClose)

    /**
     * Read the content of this InputStream into an array, and return this array.
     *
     * @param autoClose if auto close this stream.
     * @return an array contains all the bytes in the InputStream.
     */
    @JvmStatic
    @JvmOverloads
    fun InputStream.readContent(autoClose: Boolean = true): ByteArray {
        var res: ByteArray? = null
        val buf = ByteArray(1024)
        var len: Int

        while (true) {
            len = read(buf)
            if (len == -1)
                break
            if (res == null) {
                res = ByteArray(len)
                arraycopy(buf, 0, res, 0, len)
            } else {
                val nArr = res
                res = nArr + buf
            }
        }

        if (autoClose)
            close()
        return res ?: ByteArray(0)
    }
}