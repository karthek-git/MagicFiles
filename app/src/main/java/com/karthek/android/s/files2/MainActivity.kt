package com.karthek.android.s.files2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import com.karthek.android.s.files2.helpers.FileIconFetcher
import com.karthek.android.s.files2.helpers.FileType
import com.karthek.android.s.files2.helpers.SFile
import com.karthek.android.s.files2.helpers.SFileIconKeyer
import com.karthek.android.s.files2.providers.FileProvider.Companion.MIME_TYPE_KEY
import com.karthek.android.s.files2.ui.components.ScaleIndication
import com.karthek.android.s.files2.ui.screens.FileListScreen
import com.karthek.android.s.files2.ui.screens.Perms
import com.karthek.android.s.files2.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var fileType: FileType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Coil.setImageLoader {
            ImageLoader.Builder(this)
                .components({
                    add(SFileIconKeyer())
                    add(FileIconFetcher.Factory(fileType))
                })
                .build()
        }
        setContent { ActivityContent() }
    }

    @Composable
    fun ActivityContent() {
        AppTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalIndication provides ScaleIndication) {
                    Perms(navigateToSettingsScreen = {
                        startActivity(
                            Intent(
                                ACTION_APPLICATION_DETAILS_SETTINGS,
                                "package:${BuildConfig.APPLICATION_ID}".toUri()
                            )
                        )
                    }) {
                        FileListScreen(
                            handleFile = ::handleFile,
                        )
                    }
                }
            }
        }
    }

    private fun handleFile(sFile: SFile) {
        val intent = Intent(Intent.ACTION_VIEW)
        val mimeType = sFile.toMimeType(fileType)
        val uri = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            sFile.file
        ).buildUpon().appendQueryParameter(MIME_TYPE_KEY, mimeType).build()
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

}

interface FileOpsHandler {
    fun copy()
    fun cut()
    fun delete()
    fun rename()
    fun info()
}

suspend fun notification(context: Context, string: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(context)
    }
    val intent = Intent(context, MainActivity::class.java).apply {

    }
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    val builder = NotificationCompat.Builder(context, "sme")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Pasting")
        .setContentText(string)
        .setStyle(NotificationCompat.BigTextStyle().bigText(string))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setContentIntent(pendingIntent)
        .addAction(R.drawable.ic_launcher_foreground, "Cancel", pendingIntent)
        .setAutoCancel(true)
    NotificationManagerCompat.from(context).apply {
        var progress = 0
        builder.setProgress(100, progress, false)
        notify(0, builder.build())
        while (progress < 100) {
            delay(100)
            progress++
            builder.setProgress(100, progress, false)
            notify(0, builder.build())
        }
        builder.setContentText("Complete").setProgress(0, 0, false)
        notify(0, builder.build())
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createNotificationChannel(context: Context) {
    val channel =
        NotificationChannel(
            "sme",
            "channelname",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "somedesc"
        }
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}