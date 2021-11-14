package com.karthek.android.s.files2.ops

import android.content.Context
import android.os.Build
import android.os.FileUtils
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.moveTo
import kotlin.io.path.name


class PasteWorker(context: Context, workerParameters: WorkerParameters) :
    OpsWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val sourceFiles = inputData.getStringArray(KEY_SOURCE) ?: return Result.failure()
        val targetInput = inputData.getString(KEY_TARGET) ?: return Result.failure()
        val org = inputData.getBoolean(KEY_ORG, true)
        val totalProgress = sourceFiles.size
        sourceFiles.forEachIndexed { i, sourcePath ->
            val sourceName = Path(sourcePath).name
            val target = "${targetInput}/$sourceName"
            setForeground(
                createForegroundInfo(
                    NOTIFICATION_ID_PASTE,
                    "Pasting...",
                    sourceName,
                    i,
                    totalProgress
                )
            )
            val result = runCatching {
                paste(sourcePath, target, org)
            }
            if (result.isFailure) return Result.failure()
        }
        notificationManager.cancel(NOTIFICATION_ID_PASTE)
        return Result.success()
    }

    private fun paste(source: String, target: String, org: Boolean) {
        if (File(source).isDirectory) pasteDir(source, target, org)
        else pasteFile(source, target, org)
    }

    private fun pasteDir(source: String, target: String, org: Boolean) {
        if (org) File(source).copyRecursively(File(target), true)
        else Path(source).moveTo(Path(target), true) /* TODO handle diff fs */
    }

    private fun pasteFile(source: String, target: String, org: Boolean) {
        if (org) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FileUtils.copy(FileInputStream(source), FileOutputStream(target), null, null, { })
            } else {
                Path(source).copyTo(Path(target), true)
            }
        } else {
            Path(source).moveTo(Path(target), true)
        }
    }
}


const val KEY_SOURCE = "source"
const val KEY_TARGET = "target"
const val KEY_ORG = "org"