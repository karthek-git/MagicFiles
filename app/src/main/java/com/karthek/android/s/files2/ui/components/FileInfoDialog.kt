package com.karthek.android.s.files2.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthek.android.s.files2.helpers.FileType
import com.karthek.android.s.files2.helpers.SFile
import kotlinx.coroutines.Dispatchers
import java.util.*

@Composable
fun FileInfoDialog(sFile: SFile, fileType: FileType,onDismissRequest: () -> Unit) {
    val size = if (sFile.isDir) {
        val context = LocalContext.current
        remember { sFile.dSizeCal(context) }.collectAsState(initial = "...", Dispatchers.IO).value
    } else {
        Formatter.formatFileSize(LocalContext.current, sFile.size)
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(android.R.string.ok))
            }
        }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            FileInfoHeaderItem(text = "File path")
            FileInfoItem(text = sFile.file.absolutePath)
            FileInfoHeaderItem(text = "Size")
            FileInfoItem(text = size)
            FileInfoHeaderItem(text = "Last modified")
            FileInfoItem(text = Date(sFile.modified).toString())
            if (!sFile.isDir) {
                FileInfoHeaderItem(text = "MIME-Type")
                FileInfoItem(text = sFile.toMimeType(fileType))
                FileInfoHeaderItem(text = "File info")
                FileInfoItem(text = fileType.getFileMInfo(sFile.file.path))
            }
        }
    }
}

@Composable
fun FileInfoHeaderItem(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp)
    )
}

@Composable
fun FileInfoItem(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(0.75f),
        modifier = Modifier.padding(bottom = 8.dp, start = 17.dp, end = 16.dp)
    )
}