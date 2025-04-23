package com.karthek.android.s.files2.helpers

import android.app.Application
import android.content.res.AssetManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FileType @Inject constructor(application: Application) {

    @Synchronized
    private external fun c_magic_open(assetManager: AssetManager): Int

    @Synchronized
    private external fun c_magic_descriptor(fd: Int): String?

    @Synchronized
    private external fun c_magic_setflags(Flag: Int)

    @Synchronized
    private external fun c_magic_error(): String?


    fun getMimeType(fileName: String): String {
        val fd: Int = getNativeFD(fileName)
        return getMimeType(fd)
    }

    fun getMimeType(fd: Int): String {
        var mime: String? = null

        if (fd != -1) {
            mime = c_magic_descriptor(fd)
        }
        return if (mime == null) {
            Log.e(TAG, "from file:$mime ${c_magic_error()}")
            "application/octet-stream"
        } else {
            mime
        }
    }

    fun getFileMInfo(filename: String): String {
        return getFileMInfo(getNativeFD(filename))
    }

    fun getFileMInfo(fd: Int): String {
        c_magic_setflags(1)
        val string = c_magic_descriptor(fd)
        c_magic_setflags(0)
        return string ?: "data"
    }

    init {
        System.loadLibrary("magic-wrapper")
        c_magic_open(application.assets)
    }

    companion object {
        const val TAG = "libmagic"
    }
}