package com.karthek.android.s.files2.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun Crumb(
    path: File,
    paddingValues: PaddingValues
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    //var pathList = listOf("/", "Downloads", "some1", "some2")
    val pathList = path.path.split("/")
    LazyRow(
        state = lazyListState,
        contentPadding = paddingValues,
        modifier = Modifier.padding(top = 12.dp, end = 8.dp)
    ) {
        item { CrumbItem(dirName = "Internal Storage") {} }
        if (pathList.size > 4) {
            items(pathList.size - 4) { i ->
                CrumbItem(dirName = pathList[i + 4]) {

                }
            }
            item { Spacer(modifier = Modifier.size(height = 4.dp, width = 64.dp)) }
            coroutineScope.launch {
                lazyListState.animateScrollToItem(pathList.size - 3)
            }
        }
    }
}

@Composable
fun CrumbItem(dirName: String, callback: () -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 2.dp)) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary
        )
       // TextButton(onClick = callback, contentPadding = PaddingValues(0.dp)) {
            Text(
                text = dirName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(0.dp)
            )
        //}
    }
}