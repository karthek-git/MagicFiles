package com.karthek.android.s.files2.ops

import android.content.Context
import androidx.work.WorkerParameters
import java.io.File

class DeleteWorker(context: Context, workerParameters: WorkerParameters) :
    OpsWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val filePaths = inputData.getStringArray(KEY_DEL) ?: return Result.failure()
        filePaths.forEachIndexed { index, s ->
            setForeground(
                createForegroundInfo(
                    NOTIFICATION_ID_DELETE,
                    "Deleting...",
                    s,
                    index,
                    filePaths.size
                )
            )
            if (!File(s).deleteRecursively()) return Result.failure()
        }
        notificationManager.cancel(NOTIFICATION_ID_DELETE)
        return Result.success()
    }
}

const val KEY_DEL = "rm"