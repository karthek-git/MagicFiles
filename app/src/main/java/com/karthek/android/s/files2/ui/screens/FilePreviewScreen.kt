package com.karthek.android.s.files2.ui.screens

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.karthek.android.s.files2.FileInfoActivity
import com.karthek.android.s.files2.ui.components.FileInfoHeaderItem
import com.karthek.android.s.files2.ui.components.FileInfoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(fMedia: FileInfoActivity.FMedia) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        var state by remember { mutableIntStateOf(0) }
        val titles = listOf("Preview", "Info")
        Column {
            SecondaryTabRow(selectedTabIndex = state) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = state == index,
                        onClick = { state = index },
                        text = {
                            Text(
                                text = title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            if (state == 0) {
                FilePreview(fMedia, innerPadding)
            } else {
                FileInfo(fMedia, innerPadding)
            }
        }
    }
}

@Composable
fun FilePreview(fMedia: FileInfoActivity.FMedia, contentPadding: PaddingValues) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        if (fMedia.mimeType.startsWith("image/")) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(fMedia.uri).build(),
                contentDescription = "",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        } else if (fMedia.mimeType.startsWith("video/")) {
            VideoViewComponent(fMedia.uri)
        } else if (fMedia.mimeType.startsWith("audio/")) {
            AudioPlayerComponent(fMedia = fMedia)
        } else if (fMedia.mimeType.startsWith("text/")) {
            TextFilePreviewComponent(fMedia.uri)
        } else {
            Text(
                text = "No preview available, See Info Tab for more details",
                modifier = Modifier
            )
        }
    }
}

@Composable
fun FileInfo(fMedia: FileInfoActivity.FMedia, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        FileInfoHeaderItem(text = "Name")
        FileInfoItem(text = fMedia.name)
        FileInfoHeaderItem(text = "Size")
        FileInfoItem(text = fMedia.size)
        FileInfoHeaderItem(text = "MIME-Type")
        FileInfoItem(text = fMedia.mimeType)
        FileInfoHeaderItem(text = "File info")
        FileInfoItem(text = fMedia.filetypeInfo)
        FileInfoHeaderItem(text = "URI")
        FileInfoItem(text = fMedia.uri.toString())
    }
}


@Composable
fun VideoViewComponent(uri: Uri) {
    //todo functional refactor
    var playing by remember { mutableStateOf(true) }

    Box(contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setZOrderOnTop(true)
                }
            },
            modifier = Modifier.wrapContentHeight(),
            update = { videoView ->
                videoView.setVideoURI(uri)
                videoView.start()
                videoView.setOnClickListener {
                    if (videoView.isPlaying) {
                        videoView.pause()
                        playing = false
                    } else {
                        videoView.start()
                        playing = true
                    }
                }
            }
        )
        if (!playing) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun AudioPlayerComponent(
    fMedia: FileInfoActivity.FMedia,
    viewModel: AudioPlayerViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(fMedia) {
        Log.i("lll", "AudioPlayerComponent: lau")
        viewModel.initMediaPlayer(fMedia)

        onDispose {
            Log.i("lll", "AudioPlayerComponent: dis")
            viewModel.closeMediaPlayer()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.mediaPlayer.pause()
                viewModel.playing = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel.playing) {
        while (viewModel.playing && viewModel.duration > 0) {
            val currentPosition = viewModel.mediaPlayer.currentPosition
            viewModel.progressState = currentPosition.toFloat() / viewModel.duration
            delay(1000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (viewModel.imageArt == null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Audiotrack,
                        contentDescription = "",
                        modifier = Modifier
                    )
                }
            }
        } else {
            viewModel.imageArt?.let {
                Image(
                    bitmap = it,
                    contentDescription = "",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Text(text = viewModel.audioName, modifier = Modifier.padding(8.dp))
        Row {
            AudioPlayerControlButton(imageVector = Icons.Outlined.SkipPrevious) { }
            AudioPlayerControlButton(
                imageVector = if (viewModel.playing) Icons.Outlined.Pause
                else Icons.Outlined.PlayArrow
            ) {
                if (viewModel.mediaPlayer.isPlaying) {
                    viewModel.playing = false
                    viewModel.mediaPlayer.pause()
                } else {
                    viewModel.playing = true
                    viewModel.mediaPlayer.start()
                }
            }
            AudioPlayerControlButton(imageVector = Icons.Outlined.SkipNext) { }
        }
        Slider(
            value = viewModel.progressState,
            onValueChange = {
                viewModel.progressState = it
                viewModel.mediaPlayer.seekTo(((viewModel.progressState * viewModel.duration).toInt()))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun AudioPlayerControlButton(imageVector: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = "",
        )
    }
}


@Composable
fun TextFilePreviewComponent(uri: Uri) {
    val context = LocalContext.current
    Text(
        text = readTextFromUri(context, uri),
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.verticalScroll(rememberScrollState())
    )
}

fun readTextFromUri(context: Context, uri: Uri): String {
    val stringBuilder = StringBuilder()
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.appendLine(line)
                line = reader.readLine()
            }
        }
    }
    return stringBuilder.toString()
}

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(private val application: Application) :
    ViewModel() {

    lateinit var fMedia: FileInfoActivity.FMedia
    var audioName by mutableStateOf("")
    var imageArt by mutableStateOf<ImageBitmap?>(null)
    lateinit var mediaPlayer: MediaPlayer
    var progressState by mutableFloatStateOf(0f)
    var duration = 0
    var playing by mutableStateOf(false)

    fun initMediaPlayer(fMedia: FileInfoActivity.FMedia) {
        this.fMedia = fMedia
        viewModelScope.launch {
            mediaPlayer = MediaPlayer()
            setAudioInfo()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setOnPreparedListener {
                it.start()
                playing = true
                duration = mediaPlayer.duration
            }
            mediaPlayer.setOnCompletionListener {
                progressState = 1f
                playing = false
            }
            try {
                mediaPlayer.setDataSource(application, fMedia.uri)
                mediaPlayer.prepareAsync()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun setAudioInfo() {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(application, fMedia.uri);
        audioName =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: fMedia.name
        mediaMetadataRetriever.embeddedPicture?.let {
            imageArt = BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap()
        }
    }

    fun closeMediaPlayer() {
        mediaPlayer.pause()
        mediaPlayer.release()
    }

}
