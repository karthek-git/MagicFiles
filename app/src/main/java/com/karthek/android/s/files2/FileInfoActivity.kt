package com.karthek.android.s.files2

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.karthek.android.s.files2.helpers.FileType
import com.karthek.android.s.files2.ui.screens.FilePreviewScreen
import com.karthek.android.s.files2.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FileInfoActivity : ComponentActivity() {

    @Inject
    lateinit var fileType: FileType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val uri = intent.data
        if (uri == null) {
            finish()
        }
        setContent { ScreenContent(uri!!) }
    }

    private fun getFMedia(uri: Uri): FMedia {
        //todo handle uri metadata properly
        val cursor =
            contentResolver.query(uri, null, null, null, null)
                ?: return FMedia(uri, "", "", "", "")
        val name: String
        val size: String
        cursor.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            it.moveToFirst()
            name = it.getString(nameIndex)
            it.getLong(sizeIndex).also { it1 ->
                size = Formatter.formatFileSize(this, it1)
            }
        }

        val mimeType = probeFMedia(uri) {
            fileType.getMimeType(it)
        }
        val filetypeInfo = probeFMedia(uri) {
            fileType.getFileMInfo(it)
        }

        return FMedia(uri, name, mimeType, filetypeInfo, size)
    }

    private fun probeFMedia(uri: Uri, func: (Int) -> String): String {
        val r: String
        contentResolver.openFileDescriptor(uri, "r").use {
            val nativeFD = it?.detachFd()
            r = if (nativeFD != null) {
                func(nativeFD)
            } else {
                contentResolver.getType(uri).toString()
            }
        }
        return r
    }

    data class FMedia(
        val uri: Uri,
        val name: String,
        val mimeType: String,
        val filetypeInfo: String,
        val size: String
    )


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScreenContent(uri: Uri) {
        AppTheme {
            var openSheet by rememberSaveable { mutableStateOf(true) }
            val sheetState = rememberModalBottomSheetState()

            if (openSheet) {
                ModalBottomSheetLayout(
                    onDismissRequest = {
                        openSheet = false
                        finish()
                    },
                    sheetState = sheetState,
                    sheetContent = { FilePreviewScreen(getFMedia(uri)) })
            }

        }
    }
}