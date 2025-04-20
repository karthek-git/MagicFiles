package com.karthek.android.s.files2.helpers

import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.ImageDecoder.*
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.BitmapParams
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.Px
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.exifinterface.media.ExifInterface
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import coil.size.pxOrElse
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.util.*
import java.util.function.ToIntFunction
import kotlin.coroutines.coroutineContext


class FileIconFetcher(
    private val options: Options,
    private val data: SFile,
    private val fileType: FileType
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val type = data.toMimeType(fileType)
        val width = options.size.width.pxOrElse { 0 }
        val height = options.size.height.pxOrElse { 0 }
        val bitmap = runCatching {
            when {
                type.startsWith("image/") -> {
                    createImageThumbnail(data.file, width, height, type)
                }

                type.startsWith("audio/") -> {
                    createAudioThumbnail(data.file, width, height)
                }

                type.startsWith("video/") -> {
                    createVideoThumbnail(data.file, width, height)
                }

                type == "application/vnd.android.package-archive" -> {
                    getAppThumbnail(
                        data.file,
                        width,
                        height,
                        options.context.packageManager
                    )
                }

                type == "application/pdf" -> {
                    getPdfThumbnail(data.file, width, height)
                }

                else -> {
                    throw Exception()
                }
            }
        }
        val b = bitmap.getOrThrow()
        /*Log.v(
            TAG,
            "fetch $type: (${pixelSize.width},${pixelSize.height}) ; (${b.width},${b.height})"
        )*/
        return DrawableResult(b.toDrawable(options.context.resources), true, DataSource.DISK)
    }

    class Factory(private val fileType: FileType) : Fetcher.Factory<SFile> {
        override fun create(
            data: SFile,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return FileIconFetcher(options, data, fileType)
        }
    }

}

@Throws(IOException::class)
suspend fun createImageThumbnail(
    file: File,
    @Px reqWidth: Int,
    @Px reqHeight: Int,
    mimeType: String,
): Bitmap {
    // Checkpoint before going deeper
    coroutineContext.ensureActive()
    var bitmap: Bitmap? = null
    val exif = ExifInterface(file)
    var orientation = 0

    // get orientation
    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> orientation = 90
        ExifInterface.ORIENTATION_ROTATE_180 -> orientation = 180
        ExifInterface.ORIENTATION_ROTATE_270 -> orientation = 270
    }
    if (mimeType == "image/heif" || mimeType == "image/heif-sequence" || mimeType == "image/heic" || mimeType == "image/heic-sequence") {
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(file.absolutePath)
                /* TODO heif */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    bitmap = retriever.getImageAtIndex(
                        -1,
                        BitmapParams()
                    )
                }
            }
        } catch (e: RuntimeException) {
            throw IOException("Failed to create thumbnail", e)
        }
    }
    if (bitmap == null) {
        val raw = exif.thumbnailBytes
        if (raw != null) {
            try {
                bitmap = decodeBitmap(raw, reqWidth, reqHeight)
            } catch (e: IOException) {
                Log.w(TAG, e)
            }
        }
    }

    // Checkpoint before going deeper
    coroutineContext.ensureActive()
    if (bitmap == null) {
        bitmap = decodeBitmap(file.path, reqWidth, reqHeight)
        // Use ImageDecoder to do full file decoding, we don't need to handle the orientation
        return bitmap ?: throw IOException("Failed to decode file")
    }

    // Transform the bitmap if the orientation of the image is not 0.
    if (orientation != 0 && bitmap != null) {
        val width = bitmap!!.width
        val height = bitmap!!.height
        val m = Matrix()
        m.setRotate(orientation.toFloat(), (width / 2).toFloat(), (height / 2).toFloat())
        bitmap = Bitmap.createBitmap(bitmap!!, 0, 0, width, height, m, false)
    }
    return bitmap ?: throw IOException("Failed to create thumbnail")
}

@Throws(IOException::class)
suspend fun createAudioThumbnail(file: File, @Px reqWidth: Int, @Px reqHeight: Int): Bitmap {
    // Checkpoint before going deeper
    coroutineContext.ensureActive()
    try {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(file.absolutePath)
            val raw = retriever.embeddedPicture
            if (raw != null) {
                return decodeBitmap(raw, reqWidth, reqHeight)
            }
        }
    } catch (e: RuntimeException) {
        throw IOException("Failed to create thumbnail", e)
    }

    // Only poke around for files on external storage
    if (Environment.MEDIA_UNKNOWN == Environment.getExternalStorageState(file)) {
        throw IOException("No embedded album art found")
    }

    // Ignore "Downloads" or top-level directories
    val parent = file.parentFile
    val grandParent = parent?.parentFile
    if (parent != null
        && parent.name == Environment.DIRECTORY_DOWNLOADS
    ) {
        throw IOException("No thumbnails in Downloads directories")
    }
    if (grandParent != null
        && Environment.MEDIA_UNKNOWN == Environment.getExternalStorageState(grandParent)
    ) {
        throw IOException("No thumbnails in top-level directories")
    }

    // If no embedded image found, look around for best standalone file
    val found: Array<File> = parent?.listFiles { _: File, name: String ->
        val lower = name.lowercase()
        lower.endsWith(".jpg") || lower.endsWith(".png")
    } ?: throw IOException("No thumb")
    val score = ToIntFunction { f: File? ->
        val lower = f!!.name.lowercase()
        if (lower == "albumart.jpg") return@ToIntFunction 4
        if (lower.startsWith("albumart") && lower.endsWith(".jpg")) return@ToIntFunction 3
        if (lower.contains("albumart") && lower.endsWith(".jpg")) return@ToIntFunction 2
        if (lower.endsWith(".jpg")) return@ToIntFunction 1
        0
    }
    val bestScore = Comparator { a: File?, b: File? ->
        score.applyAsInt(a) - score.applyAsInt(b)
    }
    val bestFile = listOf(*found).stream().max(bestScore).orElse(null)
        ?: throw IOException("No album art found")

    // Checkpoint before going deeper
    coroutineContext.ensureActive()
    return decodeBitmap(bestFile.path, reqWidth, reqHeight)
}

@Throws(IOException::class)
suspend fun createVideoThumbnail(file: File, @Px reqWidth: Int, @Px reqHeight: Int): Bitmap {
    // Checkpoint before going deeper
    coroutineContext.ensureActive()
    try {
        MediaMetadataRetriever().use { mmr ->
            mmr.setDataSource(file.absolutePath)

            // Try to retrieve thumbnail from metadata
            val raw = mmr.embeddedPicture
            if (raw != null) {
                return decodeBitmap(raw, reqWidth, reqHeight)
            }
            val width =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
            val height =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
            // Fall back to middle of video
            // Note: METADATA_KEY_DURATION unit is in ms, not us.
            val thumbnailTimeUs =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                    .toLong() * 1000 / 2

            // If we're okay with something larger than native format, just
            // return a frame without up-scaling it
            return if (reqWidth < width && reqHeight < height && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                mmr.getScaledFrameAtTime(
                    thumbnailTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    reqWidth, reqHeight
                )!!
            } else {
                mmr.getFrameAtTime(
                    thumbnailTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )!!
            }
        }
    } catch (e: RuntimeException) {
        throw IOException("Failed to create thumbnail", e)
    }
}

private fun decodeBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, this)

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false

        BitmapFactory.decodeFile(path, this)
    }
}

private fun decodeBitmap(raw: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(raw, 0, raw.size, this)

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false

        BitmapFactory.decodeByteArray(raw, 0, raw.size, this)
    }
}

private fun getAppThumbnail(
    file: File,
    @Px width: Int,
    @Px height: Int,
    pm: PackageManager
): Bitmap {
    val packageInfo = pm.getPackageArchiveInfo(file.path, 0)
    return pm.getApplicationIcon(packageInfo!!.applicationInfo!!).toBitmap(width, height)
}

private fun getPdfThumbnail(
    file: File,
    @Px width: Int,
    @Px height: Int
): Bitmap {
    val renderer =
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
    val page = renderer.openPage(0)
    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.WHITE)
    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    page.close()
    renderer.close()
    return bitmap
}

const val TAG = "SIconFetcher"


fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

class SFileIconKeyer : Keyer<SFile> {
    override fun key(data: SFile, options: Options): String {
        // Log.v(TAG, "key ${data.file.path}:${data.modified}")
        return "${data.file.path}:${data.modified}"
    }
}