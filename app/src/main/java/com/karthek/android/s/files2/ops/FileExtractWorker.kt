package com.karthek.android.s.files2.ops

import android.content.Context
import androidx.work.WorkerParameters
import com.karthek.android.s.files2.helpers.FArchive
import com.karthek.android.s.files2.helpers.getArchiveFileName
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists


class FileExtractWorker(context: Context, workerParameters: WorkerParameters) :
    OpsWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val sourceFile = inputData.getString(KEY_SOURCE) ?: return Result.failure()
        val targetDirectory = inputData.getString(KEY_TARGET) ?: return Result.failure()
        val totalProgress = 100
        val sourceName = getArchiveFileName(Path(sourceFile))
        val target = Path(targetDirectory, sourceName)
        setForeground(
            createForegroundInfo(
                NOTIFICATION_ID_EXTRACT,
                "Extracting...",
                sourceName,
                50,
                totalProgress
            )
        )
        val result = runCatching {
            if(target.createDirectory().exists()) {
                FArchive.extractArchive(sourceFile, target.toString())
            }
        }
        if (result.isFailure) return Result.failure()
        notificationManager.cancel(NOTIFICATION_ID_EXTRACT)
        return Result.success()
    }
}