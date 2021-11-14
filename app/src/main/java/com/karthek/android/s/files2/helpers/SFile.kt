package com.karthek.android.s.files2.helpers

import android.content.Context
import android.os.ParcelFileDescriptor
import android.text.format.Formatter.formatFileSize
import android.webkit.MimeTypeMap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.karthek.android.s.files2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.Collator


data class SFile(
    val file: File,
    var size: Long = -1,
    val modified: Long = 0,
    var mimeType: String? = getMimeTypeFromExtension(file.name)
) {
    var isDir = false
    private var probed = false
    private var dTraversed = false
    private var dSize = 0L
    private var dDirs = -1
    private var dFiles = 0
    private var _res: SFileIcon? = null
    val res: SFileIcon
        get() {
            if (_res == null) {
                _res = getMimeIcon()
            }
            return _res ?: throw AssertionError("race")
        }

    fun getDirFormattedSize(context: Context): Flow<String> {
        return flow {
            if (size < 0) {
                withContext(Dispatchers.IO) {
                    size = file.list()?.size?.toLong() ?: 0
                }
            }
            emit(
                context.resources.getQuantityString(
                    R.plurals.items,
                    size.toInt(),
                    size
                )
            )
        }
    }

    fun toMimeType(fileType: FileType): String {
        if (!probed && mimeType!!.contains("oct")) {
            mimeType = fileType.getMimeType(file.path)
            _res = getMimeIcon()
            probed = true
        }
        return mimeType!!
    }

    private fun getMimeIcon(): SFileIcon {
        return when {
            isDir -> SFileIcon.DIRECTORY
            mimeType!!.startsWith("image/") -> SFileIcon.IMAGE
            mimeType!!.startsWith("audio/") -> SFileIcon.AUDIO
            mimeType!!.startsWith("video/") -> SFileIcon.VIDEO
            mimeType!! == "application/vnd.android.package-archive" -> SFileIcon.APK
            mimeType!! == "application/pdf" -> SFileIcon.PDF
            else -> SFileIcon.File
        }
    }

    fun dSizeCal(context: Context): Flow<String> {
        return flow {
            if (dTraversed) {
                emit(dSizeFormat(context))
            } else {
                dDirs = -1; dFiles = 0; dSize = 0
                val iterator = file.walkTopDown().iterator()
                while (iterator.hasNext()) {
                    val file = iterator.next()
                    if (file.isDirectory) dDirs++ else dFiles++
                    dSize += file.length()
                    emit(dSizeFormat(context))
                }
                dTraversed = true
            }
        }
    }

    private fun dSizeFormat(context: Context): String {
        return context.resources.getQuantityString(
            R.plurals.d_items, dFiles, dDirs, dFiles, formatFileSize(context, dSize)
        )
    }

    init {
        if (mimeType == null || file.isDirectory) {
            size = -1
            isDir = true
        }
    }

}

private fun getMimeTypeFromExtension(name: String): String {
    val lastDot = name.lastIndexOf('.')
    if (lastDot >= 0) {
        val extension = name.substring(lastDot + 1)
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        mime?.let { return it }
    }
    return "application/octet-stream"
}

fun getNativeFD(filename: String): Int {
    return try {
        ParcelFileDescriptor.open(File(filename), ParcelFileDescriptor.MODE_READ_ONLY)?.detachFd()
            ?: throw IOException("failed getting native fd")
    } catch (e: IOException) {
        e.printStackTrace()
        -1
    }
}

class SFileComparator(var SType: Int = 1) : Comparator<SFile> {

    private val collator = Collator.getInstance()

    override fun compare(o1: SFile?, o2: SFile?): Int {
        if (o1 == null || o2 == null) return 0
        val b1 = o1.isDir
        val b2 = o2.isDir
        return if (b1 && !b2) {
            -1
        } else if (!b1 && b2) {
            1
        } else {
            when (SType) {
                1 -> collator.compare(o1.file.name, o2.file.name)
                2 -> collator.compare(o2.file.name, o1.file.name)
                3 -> o1.size.compareTo(o2.size)
                4 -> o2.size.compareTo(o1.size)
                5 -> o1.modified.compareTo(o2.modified)
                6 -> o2.modified.compareTo(o1.modified)
                else -> 0
            }
        }
    }
}

fun getComparator(SType: Int = 1): Comparator<SFile> {
    val comparator = compareBy<SFile> { it.isDir }.reversed().let { comparator ->
        when (SType) {
            1 -> comparator.then(compareBy(String.CASE_INSENSITIVE_ORDER) { it.file.name })
            2 -> comparator.thenDescending(compareBy(String.CASE_INSENSITIVE_ORDER) { it.file.name })
            3 -> comparator.then(compareBy { it.size })
            4 -> comparator.thenDescending(compareBy { it.size })
            5 -> comparator.then(compareBy { it.modified })
            6 -> comparator.thenDescending(compareBy { it.modified })
            else -> throw Exception("")
        }
    }
    return comparator
}

enum class SFileIcon(val icon: ImageVector, val tintColor: Color) {
    DIRECTORY(Icons.Outlined.Folder, Color(0xFF5F6368)),
    File(Icons.Outlined.InsertDriveFile, Color(0xFF24C1E0)),
    IMAGE(Icons.Outlined.Image, Color(0xFFEA4335)),
    AUDIO(Icons.Outlined.Audiotrack, Color(0xFFA142F4)),
    VIDEO(Icons.Outlined.Movie, Color(0xFF34A853)),
    APK(Icons.Outlined.Android, Color(0xFF5F6368)),
    PDF(Icons.Outlined.PictureAsPdf, Color(0xFFDB4437)),
    ARCHIVE(Icons.Outlined.Archive, Color(0xFF5A2C2C))
}