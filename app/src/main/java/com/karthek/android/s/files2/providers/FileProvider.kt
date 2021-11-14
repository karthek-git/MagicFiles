package com.karthek.android.s.files2.providers

import android.net.Uri
import androidx.core.content.FileProvider

class FileProvider : FileProvider() {
    override fun getType(uri: Uri): String? {
        return uri.getQueryParameter(MIME_TYPE_KEY) ?: super.getType(uri)
    }

    companion object {
        const val MIME_TYPE_KEY = "mimeType"
    }
}