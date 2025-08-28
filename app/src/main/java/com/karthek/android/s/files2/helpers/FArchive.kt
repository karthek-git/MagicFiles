package com.karthek.android.s.files2.helpers

import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension


object FArchive {
    @Synchronized
    private external fun c_archive_extract(i: Int, target: String): Int

    @Synchronized
    private external fun c_archive_list(i: Int): Long

    fun listArchiveEntries(fileName: String) {
        c_archive_list(getNativeFD(fileName))
    }

    fun extractArchive(filename: String, targetDirectoryPath: String) {
        c_archive_extract(getNativeFD(filename), targetDirectoryPath)
    }

    fun extractArchive(fd: Int, target: String): Int {
        return c_archive_extract(fd, target)
    }

    init {
        System.loadLibrary("archive-wrapper")
    }
}

fun getArchiveFileName(path: Path) = path.nameWithoutExtension.substringBeforeLast(".tar")