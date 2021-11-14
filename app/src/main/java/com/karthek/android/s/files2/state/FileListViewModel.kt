package com.karthek.android.s.files2.state

import android.app.Application
import android.database.Cursor
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.karthek.android.s.files2.FileOpsHandler
import com.karthek.android.s.files2.helpers.FileType
import com.karthek.android.s.files2.helpers.SFile
import com.karthek.android.s.files2.helpers.getComparator
import com.karthek.android.s.files2.ops.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists


@HiltViewModel
class FileListViewModel @Inject constructor(
    private val application: Application,
    private val prefs: Prefs,
    val fileType: FileType
) : ViewModel(),
    FileOpsHandler {
    private var cwd: File = Environment.getExternalStorageDirectory()
    var selectedFile: SFile? by mutableStateOf(null)
    var nest by mutableStateOf(0)
    val fileList = mutableStateListOf<SFile>()
    var loading by mutableStateOf(true)
    var selectedFileList = mutableStateListOf<String>()
    private var observer: FileObserver? = null
    private var sFileComparator = getComparator()
    var backStack = Stack<IntArray>()
    var curState: IntArray? = null
    private var loadJob: Job? = null
    var showHidden by mutableStateOf(false)
    var sortPreference by mutableStateOf(1)
    var showEditDialog by mutableStateOf(false)
    var showRenameDialog by mutableStateOf(false)
    var showInfoDialog by mutableStateOf(false)
    var showFindAppDialog by mutableStateOf<String?>(null)
    var inSearchMode by mutableStateOf(false)
    var query by mutableStateOf("")
    private var searchJob: Job? = null
    var clipBoard = mutableStateListOf<String>()
    private var org = true

    fun onBackClick() {
        nest--
        curState = backStack.pop()
        cwd.parentFile?.let { onCurrentDirChange(it) }
    }

    fun onCurrentDirChange(dir: File) {
        cwd = dir
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            observeChanges()
            refresh()
        }
    }

    private suspend fun refresh() {
        loading = true
        fileList.clear()
        val showHidden = this.showHidden
        fileList += withContext(Dispatchers.IO) {
            getFiles(cwd.path, showHidden).apply {
                sortWith(sFileComparator)
                yield()
            }
        }
        loading = false
    }

    private fun getFiles(path: String, showHidden: Boolean): MutableList<SFile> {
        val sFiles = if (showHidden) {
            File(path).listFiles()
        } else {
            File(path).listFiles { file -> !file.isHidden }
        }?.map { SFile(it, it.length(), it.lastModified()) }
        return sFiles?.toMutableList() ?: mutableListOf()
    }

    private fun observeChanges() {
        observer?.stopWatching()
        observer = object : FileObserver(cwd.path, fileObserverMask) {
            var job: Job? = null
            override fun onEvent(event: Int, path: String?) {
                if (path == null) return
                job?.cancel()
                job = when {
                    event and (DELETE or MOVED_FROM) != 0 -> viewModelScope.launch(Dispatchers.Main) { fileList.removeAll { it.file.name == path } }
                    event and fileObserverMask != 0 -> viewModelScope.launch(Dispatchers.Main) {
                        delay(150); refresh()
                    }
                    else -> null
                }
            }
        }.apply { startWatching() }
    }

    fun onSelect(selected: Boolean, sFile: SFile) {
        with(selectedFileList) {
            val path = sFile.file.absolutePath
            if (selected) remove(path)
            else add(path)
        }
    }

    fun inActionMode() = selectedFileList.isNotEmpty()

    fun clearActionMode() {
        selectedFileList.clear()
    }

    fun onShowHiddenChange(showHidden: Boolean) {
        viewModelScope.launch {
            prefs.onShowHiddenChange(showHidden)
        }
    }

    fun onSortPrefChanged(pref: Int) {
        viewModelScope.launch {
            prefs.onSortPrefChange(pref)
        }
    }

    /* TODO service */
    fun touchDir(name: String) {
        val path = Path(cwd.path, name)
        try {
            path.createDirectory()
        } catch (e: IOException) {
            if (e is FileAlreadyExistsException || path.exists())
                throw IOException("Directory already exists")
            else
                throw IOException("Directory creation failed")
        }
    }

    fun renameTo(name: String): Boolean {
        val sFile = selectedFile ?: return false
        var r = false
        viewModelScope.launch(Dispatchers.IO) {
            r = sFile.file.renameTo(File(sFile.file.parent, name))
        }
        return r
    }

    override fun copy() {
        setClipBoard()
    }

    override fun cut() {
        org = false
        setClipBoard()
    }

    private fun setClipBoard() {
        clipBoard.clear()
        if (inActionMode()) {
            clipBoard += selectedFileList
            clearActionMode()
        } else {
            val filePath = selectedFile?.file?.absolutePath ?: return
            clipBoard += filePath
        }
    }

    override fun delete() {
        val filePaths = if (inActionMode()) {
            selectedFileList.toTypedArray().also { clearActionMode() }
        } else {
            val filePath = selectedFile?.file?.absolutePath ?: return
            arrayOf(filePath)
        }
        val workRequest = OneTimeWorkRequestBuilder<DeleteWorker>()
            .setInputData(workDataOf(KEY_DEL to filePaths))
            .build()
        WorkManager.getInstance(application).enqueue(workRequest)
    }

    override fun rename() {
        showRenameDialog = true
    }

    override fun info() {
        showInfoDialog = true
    }

    fun paste() {
        val workRequest = OneTimeWorkRequestBuilder<PasteWorker>()
            .setInputData(
                workDataOf(
                    KEY_SOURCE to clipBoard.toTypedArray(),
                    KEY_TARGET to cwd.absolutePath,
                    KEY_ORG to org
                )
            )
            .build()
        WorkManager.getInstance(application).enqueue(workRequest)
    }

    fun search(s: String) {
        query = s
        if (query.length < 3) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getSearchFiles(s)
            }
        }
    }

    fun onSearchClose() {
        searchJob?.cancel()
        query = ""
        viewModelScope.launch { refresh() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun getSearchFiles(query: String) {
        loading = true
        fileList.clear()
        fileList += withContext(Dispatchers.Default) {
            var relPath = ""
            val cPath = cwd.toPath()
            if (cPath.nameCount > 3) {
                relPath = cPath.subpath(3, cPath.nameCount).toString()
            }
            val sFileList: MutableList<SFile> = ArrayList()
            val uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
            )
            val selection =
                MediaStore.Files.FileColumns.RELATIVE_PATH + " LIKE " + "'" + relPath + "%'" + " and" +
                        " " + MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE " +
                        "'%" + query + "%'"
            val cursor: Cursor = application.contentResolver.query(
                uri,
                projection, selection, null, null
            ) ?: return@withContext listOf<SFile>()
            val pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            //val nameColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val lmColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val path = cursor.getString(pathColumn)
                //val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val lm = cursor.getLong(lmColumn) * 1000
                val mime = cursor.getString(mimeColumn)
                sFileList.add(SFile(File(path), size, lm, mime))
            }
            cursor.close()
            sFileList
        }
        loading = false
    }

    init {
        viewModelScope.launch {
            prefs.prefsFlow.collect {
                showHidden = it.showHidden
                //sFileComparator.SType = it.sortPref
                sFileComparator = getComparator(it.sortPref)
                sortPreference = it.sortPref
                refresh()
            }
        }
        observeChanges()
    }

    companion object {
        const val fileObserverMask =
            FileObserver.CREATE or FileObserver.DELETE or FileObserver.MOVED_FROM or FileObserver.MOVED_TO
    }

}