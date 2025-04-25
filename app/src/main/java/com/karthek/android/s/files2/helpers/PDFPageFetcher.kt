package com.karthek.android.s.files2.helpers

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

class PDFPageFetcher(
    private val options: Options,
    private val data: Int,
    private val renderer: PdfRenderer
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return DrawableResult(
            getPdfThumbnail(data).toDrawable(options.context.resources),
            true,
            DataSource.DISK
        )
    }

    class Factory(private val renderer: PdfRenderer) : Fetcher.Factory<Int> {
        override fun create(
            data: Int,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return PDFPageFetcher(options, data, renderer)
        }
    }

    private fun getPdfThumbnail(pageNum: Int): Bitmap {
        val page = renderer.openPage(pageNum)
        val bitmap = createBitmap(
            (page.width * 1.7).toInt(),
            (page.height * 1.7).toInt(),
            Bitmap.Config.ARGB_8888
        )
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }
}

