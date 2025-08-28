package com.karthek.android.s.files2.ui.screens

import android.content.ActivityNotFoundException
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.karthek.android.s.files2.FileOpsHandler
import com.karthek.android.s.files2.ModalBottomSheetLayout
import com.karthek.android.s.files2.helpers.SFile
import com.karthek.android.s.files2.state.FileListViewModel
import com.karthek.android.s.files2.state.FileListViewModel2
import com.karthek.android.s.files2.ui.components.ActionItem
import com.karthek.android.s.files2.ui.components.AddFab
import com.karthek.android.s.files2.ui.components.Crumb
import com.karthek.android.s.files2.ui.components.Dialog
import com.karthek.android.s.files2.ui.components.FileInfoDialog
import com.karthek.android.s.files2.ui.components.FileViewItem
import com.karthek.android.s.files2.ui.components.FindAppDialog
import com.karthek.android.s.files2.ui.components.OpsBottomSheet
import com.karthek.android.s.files2.ui.components.PrefsBottomSheet
import com.karthek.android.s.files2.ui.components.add
import com.karthek.android.s.files2.ui.components.findOnGooglePlay
import com.karthek.android.s.files2.ui.screens.navigation.Screen
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    viewModel: FileListViewModel = hiltViewModel(),
    handleFile: (SFile) -> Unit,
    onMoreClick: () -> Unit,
    showInterstitialAd: () -> Unit
) {
    var openPrefsSheet by rememberSaveable { mutableStateOf(false) }
    val prefsSheetState = rememberModalBottomSheetState()

    var openOpsSheet by rememberSaveable { mutableStateOf(false) }
    val opsSheetState = rememberModalBottomSheetState()

    val scope = rememberCoroutineScope()
    val inActionMode = viewModel.selectedFileList.isNotEmpty()

    // BackHandler(viewModel.nest > 0, viewModel::onBackClick)
    BackHandler(openPrefsSheet || openOpsSheet) {
        openPrefsSheet = false
        openOpsSheet = false
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

    if (openPrefsSheet) {
        ModalBottomSheetLayout(
            onDismissRequest = { openPrefsSheet = false },
            sheetState = prefsSheetState,
            sheetContent = { PrefsBottomSheet(viewModel) })
    }

    if (openOpsSheet) {
        ModalBottomSheetLayout(
            onDismissRequest = { openOpsSheet = false },
            sheetState = opsSheetState,
            sheetContent = {
                OpsBottomSheet(
                    viewModel,
                    viewModel,
                ) { openOpsSheet = false }
            })
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pathScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            Column {
                when {
                    inActionMode -> {
                        TopActionBar(viewModel.selectedFileList.size, pathScrollBehavior) {
                            viewModel.clearActionMode()
                        }
                    }

                    viewModel.inSearchMode -> {
                        Searchbar(viewModel = viewModel, scrollBehavior = pathScrollBehavior)
                    }

                    else -> {
                        TopAppBar(
                            onSearchClick = { viewModel.inSearchMode = true },
                            sortCallback = { openPrefsSheet = true },
                            scrollBehavior = scrollBehavior,
                            onMoreClick = onMoreClick
                        )
                    }
                }
                TopAppBar(
                    title = {
                        Crumb(path = viewModel.crumbPath)
                    },
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                start = 8.dp,
                                top = 12.dp,
                                bottom = 8.dp,
                                end = 4.dp
                            )
                        )
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    scrollBehavior = pathScrollBehavior,
                    modifier = Modifier.height(48.dp)
                )
            }
        },
        floatingActionButton = {
            if (!inActionMode && !viewModel.inSearchMode) {
                AddFab(
                    showPaste = viewModel.clipBoard.isNotEmpty(),
                    onPasteClick = viewModel::paste,
                    showExtractHere = viewModel.selectedArchiveFile != null,
                    onExtractHereClick = viewModel::extractFile
                ) { viewModel.showEditDialog = true }
            }
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(pathScrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            FileListView(
                gViewModel = viewModel,
                bottomSheetCallback = {
                    scope.launch {
                        viewModel.selectedFile = it
                        openOpsSheet = true
                    }
                },
                paddingValues = paddingValues,
                handleFile = {
                    try {
                        handleFile(it)
                    } catch (e: ActivityNotFoundException) {
                        viewModel.showFindAppDialog = it.mimeType
                    }
                },
                showInterstitialAd = showInterstitialAd
            )
            if (inActionMode)
                ActionToolbar(
                    viewModel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    onSearchClick: () -> Unit,
    sortCallback: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onMoreClick: () -> Unit
) {

    TopAppBar(
        title = {
            Text(
                text = "Files",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            ActionItem(
                imageVector = Icons.Outlined.Search,
                contentDescription = "",
                onClick = onSearchClick
            )
            ActionItem(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = "",
                onClick = sortCallback
            )
            val context = LocalContext.current
            ActionItem(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "",
                onClick = onMoreClick
            )
        },
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Searchbar(viewModel: FileListViewModel, scrollBehavior: TopAppBarScrollBehavior) {
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
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        },
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
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = ""
                )
            }
        },
        actions = {
            if (viewModel.query.isNotEmpty()) {
                IconButton(onClick = { viewModel.search("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopActionBar(num: Int, scrollBehavior: TopAppBarScrollBehavior, callback: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "$num",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = callback) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = ""
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun ActionToolbar(fileOpsHandler: FileOpsHandler, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(16.dp),
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
        Text(text = title, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun FileListView(
    gViewModel: FileListViewModel,
    paddingValues: PaddingValues,
    bottomSheetCallback: (SFile) -> Unit,
    handleFile: (SFile) -> Unit,
    showInterstitialAd: () -> Unit,
) {
    val backStack =
        rememberNavBackStack(Screen.PathScreen(Environment.getExternalStorageDirectory().path))

    NavDisplay(
        backStack = backStack,
        onBack = {
            repeat(it) {
                backStack.removeAt(backStack.lastIndex)
                val path = (backStack.last() as Screen.PathScreen).path
                gViewModel.onCurrentDirChange(File(path))
            }
        },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = {
            slideInHorizontally(animationSpec = tween(500), initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(animationSpec = tween(500), initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {
            entry<Screen.PathScreen> { path ->
                val viewModel: FileListViewModel2 =
                    hiltViewModel<FileListViewModel2, FileListViewModel2.Factory> {
                        it.create(File(path.path))
                    }
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
                        fileList = if (gViewModel.inSearchMode) gViewModel.fileList else viewModel.fileList,
                        selectedFileList = gViewModel.selectedFileList,
                        paddingValues = paddingValues,
                        bottomSheetCallback = bottomSheetCallback,
                        curState = null,
                        onClick = { sFile, selected, index, offset ->
                            if (gViewModel.inActionMode()) {
                                gViewModel.onSelect(selected, sFile)
                            } else {
                                if (sFile.isDir) {
                                    gViewModel.onCurrentDirChange(sFile.file)
                                    backStack.add(Screen.PathScreen(sFile.file.absolutePath))
                                    showInterstitialAd()
                                } else {
                                    handleFile(sFile)
                                }
                            }
                        },
                        onLongClick = { selected, sFile ->
                            gViewModel.onSelect(selected, sFile)
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun FileListViewContent(
    fileList: SnapshotStateList<SFile>,
    selectedFileList: List<String>,
    paddingValues: PaddingValues,
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
//    AnimatedVisibility(
//        visibleState = animationState,
//        enter = slideInVertically(
//            initialOffsetY = { with(density) { 14.dp.roundToPx() } },
//            animationSpec = tween(durationMillis = 280, easing = LinearEasing)
//        )
//    ) {
    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues.add(bottom = 64.dp),
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
        items(fileList, key = { it.file.name }) { sFile ->
            val selected = selectedFileList.contains(sFile.file.absolutePath)
            FileViewItem(
                sFile = sFile,
                selected = selected,
                modifier = Modifier.animateItem(),
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
    // }
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
            Text(
                text = title,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

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
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


