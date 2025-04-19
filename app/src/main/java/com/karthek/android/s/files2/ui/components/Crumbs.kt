package com.karthek.android.s.files2.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Crumb(path: List<String> = listOf("/", "Downloads", "some1", "some2")) {
    val lazyListState = rememberLazyListState()
    LazyRow(state = lazyListState) {
        items(path) { ent ->
            CrumbItem(s = ent)
        }
    }
}

@Composable
fun CrumbItem(s: String = "some") {
    Row(modifier = Modifier.padding(8.dp)) {
        Icon(imageVector = Icons.Outlined.KeyboardArrowRight, contentDescription = "")
        Text(text = s, modifier = Modifier.padding(0.dp))
    }
}