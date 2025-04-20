package com.karthek.android.s.files2.ui.components

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.karthek.android.s.files2.BuildConfig
import com.karthek.android.s.files2.FileOpsHandler
import com.karthek.android.s.files2.helpers.SFile
import com.karthek.android.s.files2.state.FileListViewModel
import kotlinx.coroutines.launch


@Composable
fun ActionItem(imageVector: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileViewItem(
    sFile: SFile,
    selected: Boolean,
    onClick: (SFile) -> Unit,
    onLongClick: (SFile) -> Unit,
    bottomSheetCallback: (SFile) -> Unit
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current).data(data = sFile).apply(block = fun ImageRequest.Builder.() {
            transformations(RoundedCornersTransformation(8f))
        }).build()
    )
    val size = if (sFile.isDir) {
        val context = LocalContext.current
        remember { sFile.getDirFormattedSize(context) }.collectAsState(initial = "...").value
    } else {
        Formatter.formatFileSize(LocalContext.current, sFile.size)
    }
    val bgColor =
        if (selected) MaterialTheme.colorScheme.primary.copy(0.33f) else Color.Transparent
    Row(
        modifier = Modifier
            .background(bgColor)
            .combinedClickable(onLongClick = { onLongClick(sFile) }, onClick = { onClick(sFile) })
            .padding(10.dp)
    ) {
        //var checked by app.isSelected
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(36.dp)
                .align(Alignment.CenterVertically)
        ) {
            if (painter.state !is AsyncImagePainter.State.Success) {
                Icon(
                    imageVector = sFile.res.icon,
                    contentDescription = "",
                    tint = if (sFile.isDir) LocalContentColor.current
                    else sFile.res.tintColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            if (!sFile.isDir) {
                Image(
                    painter = painter,
                    contentDescription = "",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = sFile.file.name,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                text = size,
                modifier = Modifier
                    .alpha(0.7f)
                    .padding(start = 8.dp, top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = { bottomSheetCallback(sFile) },
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = ""
            )
        }
    }
}

@Composable
fun OpsBottomSheet(
    viewModel: FileListViewModel,
    fileOpsHandler: FileOpsHandler,
    onOptionSelect: () -> Unit
) {
    Column(modifier = Modifier.navigationBarsPadding()) {
        OpsItem(icon = Icons.Outlined.FileCopy, text = "Copy") {
            onOptionSelect()
            fileOpsHandler.copy()
        }
        OpsItem(icon = Icons.Outlined.ContentCut, text = "Cut") {
            onOptionSelect()
            fileOpsHandler.cut()
        }
        OpsItem(icon = Icons.Outlined.Delete, text = "Delete") {
            onOptionSelect()
            fileOpsHandler.delete()
        }
        OpsItem(icon = Icons.Outlined.DriveFileRenameOutline, text = "Rename") {
            onOptionSelect()
            fileOpsHandler.rename()
        }
        if (viewModel.selectedFile?.isDir != true) {
            val context = LocalContext.current
            OpsItem(icon = Icons.Outlined.OpenWith, text = "Open with") {
                viewModel.selectedFile?.let { context.opw(it, it.mimeType) }
                onOptionSelect()
            }
            OpsItem(icon = Icons.AutoMirrored.Outlined.OpenInNew, text = "Open as") {
                viewModel.selectedFile?.let { context.opw(it, "*/*") }
                onOptionSelect()
            }
            OpsItem(icon = Icons.Outlined.Share, text = "Share") {
                viewModel.selectedFile?.let { context.share(it) }
                onOptionSelect()
            }
        }
        OpsItem(icon = Icons.Outlined.Info, text = "Info") {
            onOptionSelect()
            fileOpsHandler.info()
        }
    }
}

private fun Context.opw(sFile: SFile, mimeType: String?) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndTypeAndNormalize(
        FileProvider.getUriForFile(this,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            sFile.file),
        mimeType
    )
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(intent, sFile.file.name))
}

private fun Context.share(sFile: SFile) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(
        Intent.EXTRA_STREAM, FileProvider.getUriForFile(
            this,
            "com.karthek.android.s.files2.fileprovider",
            sFile.file
        )
    )
    intent.type = sFile.mimeType
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(intent, sFile.file.name))
}


@Composable
fun OpsItem(icon: ImageVector, text: String, onClick: () -> Unit = {}) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = text) },
        headlineContent = { Text(text, fontWeight = FontWeight.SemiBold) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surface
                        .copy(0.88f)
                        .compositeOver(MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 2.dp)
        ) {
            Box {
                Box(modifier = Modifier.padding(bottom = 48.dp)) { content() }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun AddFab(showPaste: Boolean, onPasteClick: () -> Unit, onClick: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(initialValue = 1f) }
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(end = 8.dp)
    ) {
        AnimatedVisibility(showPaste) {
            FloatingActionButton(onClick = onPasteClick) {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = "paste",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    scale.animateTo(0.9f)
                    scale.animateTo(1f)
                    onClick()
                }
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .scale(scale.value)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

        }
    }
}