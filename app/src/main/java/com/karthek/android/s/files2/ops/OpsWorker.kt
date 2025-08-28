package com.karthek.android.s.files2.ops

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.karthek.android.s.files2.R

abstract class OpsWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createForegroundInfo(
        notificationId: Int,
        title: String,
        text: String,
        progress: Int,
        totalProgress: Int
    ): ForegroundInfo {
        val cancel = applicationContext.getString(android.R.string.cancel)
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, "sme")
            .setSmallIcon(R.drawable.ic_stat_default)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(text)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(totalProgress, progress, false)
            .build()

        return ForegroundInfo(notificationId, notification)
    }

    // TODO: handle notification runtime permission
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                "sme",
                "channelname",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "somedesc"
            }
        notificationManager.createNotificationChannel(channel)
    }
}

const val NOTIFICATION_ID_PASTE = 1
const val NOTIFICATION_ID_DELETE = 2
const val NOTIFICATION_ID_EXTRACT = 3
