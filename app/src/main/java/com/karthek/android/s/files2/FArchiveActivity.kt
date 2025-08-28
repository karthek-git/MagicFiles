package com.karthek.android.s.files2

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import com.karthek.android.s.files2.helpers.FArchive
import com.karthek.android.s.files2.helpers.getArchiveFileName
import com.karthek.android.s.files2.ui.screens.FArchiveScreen
import com.karthek.android.s.files2.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists

@AndroidEntryPoint
class FArchiveActivity : ComponentActivity() {
    var fd: Int = 0
    lateinit var targetDir: Path

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ScreenContent() }
        val uri = intent.data
        if (uri == null) {
            finish()
        }
        val file = File(Uri.decode(uri!!.encodedPath?.substring(4)))
        targetDir = Path(file.parent, getArchiveFileName(file.toPath()))
        fd = contentResolver.openFileDescriptor(uri, "r")?.detachFd() ?: return
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScreenContent() {
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
                    sheetContent = { FArchiveScreen { archiveExtract() } })
            }

        }
    }

    fun archiveExtract() {
        if (targetDir.createDirectory().exists()) {
            val text = if (FArchive.extractArchive(fd, targetDir.toString()) == 0) {
                "Extracted"
            } else {
                "Extraction failed"
            }
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
