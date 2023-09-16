package top.kkoishi.stg.test

import top.kkoishi.stg.util.Bsdiff

fun main(vararg args: String) {
    assert(args.size >= 3)

    val patchFile = Bsdiff.diff(args[0], args[1])
    Bsdiff.patch(args[0], args[2], patchFile)
    //FileUI.diff(oldFile, newFile, patchFile, compressionType)
    //FileUI.patch(oldFile, outFile, patchFile)
}