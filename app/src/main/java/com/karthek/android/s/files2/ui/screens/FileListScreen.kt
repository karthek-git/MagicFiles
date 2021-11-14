package com.karthek.android.s.files2.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import com.karthek.android.s.files2.FileOpsHandler
import com.karthek.android.s.files2.SettingsActivity
import com.karthek.android.s.files2.helpers.SFile
import com.karthek.android.s.files2.state.FileListViewModel
import com.karthek.android.s.files2.ui.components.*
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FileListScreen(
    viewModel: FileListViewModel = viewModel(),
    handleFile: (SFile) -> Unit,
) {
    val prefsSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val opsSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val inActionMode = viewModel.selectedFileList.isNotEmpty()

    BackHandler(viewModel.nest > 0, viewModel::onBackClick)
    BackHandler(prefsSheetState.isVisible || opsSheetState.isVisible) {
        scope.launch {
            prefsSheetState.hide()
            opsSheetState.hide()
        }
    }
    BackHandler(inActionMode) { viewModel.clearActionMode() }

    if (viewModel.showEditDialog) {
        CreateDirectory(
            title = "Create new directory",
            onDismissRequest = { viewModel.showEditDialog = false },
            onCreate = { viewModel.touchDir(it); viewModel.showEditDialog = false }
        )
    }
    if (viewModel.showRenameDialog) {
        CreateDirectory(
            title = "Rename",
            name = viewModel.selectedFile?.file?.name ?: "",
            onDismissRequest = { viewModel.showRenameDialog = false },
            onCreate = { viewModel.renameTo(it); viewModel.showRenameDialog = false })
    }
    if (viewModel.showInfoDialog) FileInfoDialog(viewModel.selectedFile!!, viewModel.fileType) {
        viewModel.showInfoDialog = false
    }

    if (viewModel.showFindAppDialog != null) {
        val context = LocalContext.current
        FindAppDialog(onDismissCallback = { viewModel.showFindAppDialog = null }) {
            context.findOnGooglePlay(viewModel.showFindAppDialog!!)
            viewModel.showFindAppDialog = null
        }
    }

    ModalBottomSheetLayout(
        sheetState = prefsSheetState,
        sheetContent = { PrefsBottomSheet(viewModel) }) {
        ModalBottomSheetLayout(
            sheetState = opsSheetState,
            sheetContent = {
                OpsBottomSheet(
                    viewModel,
                    viewModel,
                ) { scope.launch { opsSheetState.hide() } }
            }) {
            Scaffold(
                topBar = {
                    when {
                        inActionMode -> {
                            TopActionBar(viewModel.selectedFileList.size) { viewModel.clearActionMode() }
                        }
                        viewModel.inSearchMode -> {
                            Searchbar(viewModel = viewModel)
                        }
                        else -> {
                            TopAppBar(
                                onSearchClick = { viewModel.inSearchMode = true },
                                sortCallback = { scope.launch { prefsSheetState.show() } })
                        }
                    }
                },
                floatingActionButton = {
                    if (!inActionMode && !viewModel.inSearchMode) {
                        AddFab(
                            showPaste = viewModel.clipBoard.isNotEmpty(),
                            onPasteClick = viewModel::paste
                        ) { viewModel.showEditDialog = true }
                    }
                },
                modifier = Modifier.navigationBarsPadding(bottom = false)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    FileListView(
                        viewModel = viewModel,
                        bottomSheetCallback = {
                            scope.launch {
                                viewModel.selectedFile = it
                                opsSheetState.show()
                            }
                        },
                        handleFile = {
                            try {
                                handleFile(it)
                            } catch (e: ActivityNotFoundException) {
                                viewModel.showFindAppDialog = it.mimeType
                            }
                        },
                    )
                    if (inActionMode)
                        ActionToolbar(viewModel, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
fun TopAppBar(onSearchClick: () -> Unit, sortCallback: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Files",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        contentPadding = rememberInsetsPaddingValues(insets = LocalWindowInsets.current.statusBars),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        actions = {
            ActionItem(
                imageVector = Icons.Outlined.Search,
                contentDescription = "",
                onClick = onSearchClick
            )
            ActionItem(
                imageVector = Icons.Outlined.Sort,
                contentDescription = "",
                onClick = sortCallback
            )
            val context = LocalContext.current
            ActionItem(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = ""
            ) {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
        }
    )
}

@Composable
fun Searchbar(viewModel: FileListViewModel) {
    val textInputService = LocalTextInputService.current
    val focusHandler = LocalFocusManager.current
    val focusCancel = {
        textInputService?.hideSoftwareKeyboard()
        focusHandler.clearFocus()
    }
    TopAppBar(
        title = {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = { viewModel.search(it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onAny = { focusCancel() }),
                singleLine = true,
                placeholder = { Text(text = "Search") },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.body1,
                modifier = Modifier.fillMaxWidth()
            )
        },
        contentPadding = rememberInsetsPaddingValues(insets = LocalWindowInsets.current.statusBars),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        navigationIcon = {
            IconButton(
                onClick = {
                    focusCancel()
                    viewModel.onSearchClose()
                    viewModel.inSearchMode = false
                    viewModel.search("")
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "",
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                    tint = MaterialTheme.colors.onSurface
                )
            }
        },
        actions = {
            if (viewModel.query.isNotEmpty()) {
                IconButton(onClick = { viewModel.search("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "",
                        tint = MaterialTheme.colors.onSurface,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 16.dp)
                    )
                }
            }
        }
    )
}


@Composable
fun TopActionBar(num: Int, callback: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "$num",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        contentPadding = rememberInsetsPaddingValues(insets = LocalWindowInsets.current.statusBars),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        navigationIcon = {
            IconButton(onClick = callback) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "",
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    )
}

@Composable
fun ActionToolbar(fileOpsHandler: FileOpsHandler, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = 16.dp,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 32.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            ToolbarItem(Icons.Outlined.FileCopy, "Copy") { fileOpsHandler.copy() }
            ToolbarItem(Icons.Outlined.ContentCut, title = "Cut") { fileOpsHandler.cut() }
            ToolbarItem(Icons.Outlined.Delete, title = "Delete") { fileOpsHandler.delete() }
        }
    }
}

@Composable
fun ToolbarItem(imageVector: ImageVector, title: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 32.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = title,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(text = title, style = MaterialTheme.typography.button)
    }
}

@ExperimentalMaterialApi
@Composable
fun ModalBottomSheetLayout(
    sheetState: ModalBottomSheetState,
    sheetContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val scrimColor = if (MaterialTheme.colors.isLight) {
        MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
    } else {
        MaterialTheme.colors.surface.copy(alpha = 0.5f)
    }
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = scrimColor,
        sheetContent = { sheetContent() }) {
        content()
    }
}

@Composable
fun FileListView(
    viewModel: FileListViewModel,
    bottomSheetCallback: (SFile) -> Unit,
    handleFile: (SFile) -> Unit,
) {

    val fileList = viewModel.fileList
    if (viewModel.loading) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .size(64.dp)
                .wrapContentSize(Alignment.Center),
            strokeWidth = 4.dp
        )
    } else {
        FileListViewContent(
            fileList = fileList,
            selectedFileList = viewModel.selectedFileList,
            bottomSheetCallback = bottomSheetCallback,
            curState = viewModel.curState,
            onLongClick = { selected, sFile ->
                viewModel.onSelect(selected, sFile)
            },
            onClick = { sFile, selected, index, offset ->
                if (viewModel.inActionMode()) {
                    viewModel.onSelect(selected, sFile)
                } else {
                    if (sFile.isDir) {
                        viewModel.nest++
                        viewModel.curState = null
                        viewModel.backStack.push(intArrayOf(index, offset))
                        viewModel.onCurrentDirChange(sFile.file)
                    } else {
                        handleFile(sFile)
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FileListViewContent(
    fileList: List<SFile>,
    selectedFileList: List<String>,
    bottomSheetCallback: (SFile) -> Unit,
    curState: IntArray?,
    onClick: (SFile, Boolean, Int, Int) -> Unit,
    onLongClick: (Boolean, SFile) -> Unit
) {
    val lazyListState =
        curState?.let { rememberLazyListState(it[0], it[1]) } ?: rememberLazyListState()
    val animationState = remember {
        MutableTransitionState(false).apply {
            // Start the animation immediately.
            targetState = true
        }
    }
    val density = LocalDensity.current
    AnimatedVisibility(
        visibleState = animationState,
        enter = slideInVertically(
            initialOffsetY = { with(density) { 14.dp.roundToPx() } },
            animationSpec = tween(durationMillis = 280, easing = LinearEasing)
        )
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = rememberInsetsPaddingValues(
                insets = LocalWindowInsets.current.navigationBars,
                applyStart = false,
                applyEnd = false,
                additionalTop = 8.dp,
                additionalBottom = 88.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            //item { ListHeader(showSystem, onShowSystem) }
            if (fileList.isEmpty()) {
                item {
                    Text(
                        text = "Nothing found",
                        modifier = Modifier
                            .fillParentMaxSize()
                            .wrapContentSize(Alignment.Center)
                    )
                }
            }
            items(fileList) { sFile ->
                val selected = selectedFileList.contains(sFile.file.absolutePath)
                FileViewItem(
                    sFile = sFile,
                    selected = selected,
                    onClick = {
                        onClick(
                            it,
                            selected,
                            lazyListState.firstVisibleItemIndex,
                            lazyListState.firstVisibleItemScrollOffset
                        )
                    },
                    onLongClick = { onLongClick(selected, it) },
                    bottomSheetCallback = bottomSheetCallback
                )
            }
        }
    }
}

@Composable
fun CreateDirectory(
    title: String,
    name: String = "",
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit
) {
    /* TODO this goes to viewModel*/
    var fieldValue by remember { mutableStateOf(TextFieldValue(name, TextRange(0, name.length))) }
    var isError by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                try {
                    onCreate(fieldValue.text)
                } catch (e: IOException) {
                    e.message?.let { message = it }
                }
            }, enabled = !isError && fieldValue.text != name) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }) {
        Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
            Title {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                )
            }
            OutlinedTextField(
                value = fieldValue,
                onValueChange = {
                    fieldValue = it
                    isError = with(fieldValue.text) {
                        length > 255 || startsWith('.') || contains('/')
                    }
                },
                label = { Text(text = "Name") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                isError = isError,
                //TODO fix padding
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


